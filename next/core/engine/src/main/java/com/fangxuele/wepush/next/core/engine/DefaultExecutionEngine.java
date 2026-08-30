package com.fangxuele.wepush.next.core.engine;

import com.fangxuele.wepush.next.core.api.CommandResult;
import com.fangxuele.wepush.next.core.api.ExecutionError;
import com.fangxuele.wepush.next.core.api.ExecutionEngine;
import com.fangxuele.wepush.next.core.api.ExecutionPolicies;
import com.fangxuele.wepush.next.core.api.ExecutionPorts;
import com.fangxuele.wepush.next.core.api.ExecutionRejectedException;
import com.fangxuele.wepush.next.core.api.ItemResult;
import com.fangxuele.wepush.next.core.api.ItemState;
import com.fangxuele.wepush.next.core.api.ProviderRef;
import com.fangxuele.wepush.next.core.api.RecipientRecord;
import com.fangxuele.wepush.next.core.api.RunCommand;
import com.fangxuele.wepush.next.core.api.RunEvent;
import com.fangxuele.wepush.next.core.api.RunExecutionSpec;
import com.fangxuele.wepush.next.core.api.RunHandle;
import com.fangxuele.wepush.next.core.api.RunState;
import com.fangxuele.wepush.next.core.api.RunSummary;
import com.fangxuele.wepush.next.provider.spi.ErrorCategory;
import com.fangxuele.wepush.next.provider.spi.ProviderDescriptor;
import com.fangxuele.wepush.next.provider.spi.ProviderFactory;
import com.fangxuele.wepush.next.provider.spi.ProviderOpenContext;
import com.fangxuele.wepush.next.provider.spi.ProviderResult;
import com.fangxuele.wepush.next.provider.spi.ProviderSendRequest;
import com.fangxuele.wepush.next.provider.spi.ProviderSession;
import com.fangxuele.wepush.next.provider.spi.ValidationResult;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * Framework-free execution engine. Each run owns its mutable state and provider session.
 */
public final class DefaultExecutionEngine implements ExecutionEngine {
    private static final int SUPPORTED_SPI_MAJOR = 1;

    private final Map<ProviderKey, ProviderFactory> providers;
    private final ExecutorService executor;
    private final Set<String> acceptedRunIds = ConcurrentHashMap.newKeySet();
    private final Set<RunCoordinator> activeRuns = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean closed = new AtomicBoolean();

    public DefaultExecutionEngine(Collection<? extends ProviderFactory> providerFactories) {
        this(providerFactories, Executors.newVirtualThreadPerTaskExecutor());
    }

    DefaultExecutionEngine(
            Collection<? extends ProviderFactory> providerFactories,
            ExecutorService executor
    ) {
        if (providerFactories == null || providerFactories.isEmpty()) {
            throw new IllegalArgumentException("at least one provider is required");
        }
        this.executor = Objects.requireNonNull(executor, "executor");
        Map<ProviderKey, ProviderFactory> discovered = new HashMap<>();
        for (ProviderFactory factory : providerFactories) {
            ProviderDescriptor descriptor = Objects.requireNonNull(factory, "provider factory").descriptor();
            if (descriptor.spiMajorVersion() != SUPPORTED_SPI_MAJOR) {
                throw new IllegalArgumentException("unsupported provider SPI: " + descriptor.providerId());
            }
            ProviderKey key = new ProviderKey(descriptor.providerId(), descriptor.implementationVersion());
            if (discovered.putIfAbsent(key, factory) != null) {
                throw new IllegalArgumentException("duplicate provider: " + key);
            }
        }
        providers = Map.copyOf(discovered);
    }

    @Override
    public RunHandle start(RunExecutionSpec spec, ExecutionPorts ports) {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(ports, "ports");
        if (closed.get()) {
            throw new ExecutionRejectedException("ENGINE_CLOSED", "Execution engine is closed");
        }
        ProviderFactory factory = resolveProvider(spec.provider());
        validateConfiguration(factory, spec);
        long total = ports.recipientSource().totalCount();
        if (total < 0) {
            throw new ExecutionRejectedException("INVALID_RECIPIENT_COUNT", "Recipient count must be non-negative");
        }

        ProviderDescriptor descriptor = factory.descriptor();
        int maximumConcurrency = Math.min(
                spec.policies().concurrency().maximum(), descriptor.maximumConcurrency());
        if (maximumConcurrency < spec.policies().concurrency().minimum()) {
            throw new ExecutionRejectedException(
                    "PROVIDER_CONCURRENCY_INCOMPATIBLE",
                    "Provider concurrency is below the configured minimum");
        }
        int initialConcurrency = Math.min(spec.policies().concurrency().target(), maximumConcurrency);
        if (!acceptedRunIds.add(spec.runId())) {
            throw new ExecutionRejectedException("DUPLICATE_RUN", "Run ID was already accepted");
        }

        ProviderSession session;
        try {
            session = factory.open(new ProviderOpenContext(spec, ports.secretResolver(), ports.clock()));
            if (session == null) {
                throw new IllegalStateException("provider returned null session");
            }
        } catch (Exception exception) {
            acceptedRunIds.remove(spec.runId());
            throw new ExecutionRejectedException(
                    "PROVIDER_OPEN_FAILED",
                    "Provider session could not be opened: " + exception.getClass().getSimpleName());
        }

        RunCoordinator coordinator = new RunCoordinator(
                spec, ports, session, descriptor, total, initialConcurrency, maximumConcurrency);
        activeRuns.add(coordinator);
        try {
            executor.execute(coordinator::execute);
        } catch (RuntimeException exception) {
            activeRuns.remove(coordinator);
            acceptedRunIds.remove(spec.runId());
            closeQuietly(session);
            throw exception;
        }
        return coordinator;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        activeRuns.forEach(RunCoordinator::cancelForEngineShutdown);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    private ProviderFactory resolveProvider(ProviderRef provider) {
        ProviderFactory factory = providers.get(new ProviderKey(
                provider.providerId(), provider.implementationVersion()));
        if (factory == null) {
            throw new ExecutionRejectedException(
                    "PROVIDER_NOT_AVAILABLE",
                    "Requested provider version is not installed");
        }
        return factory;
    }

    private static void validateConfiguration(ProviderFactory factory, RunExecutionSpec spec) {
        ValidationResult account = factory.validateAccount(spec.accountConfig());
        ValidationResult message = factory.validateMessage(spec.messageConfig());
        if (account == null || message == null) {
            throw new ExecutionRejectedException(
                    "PROVIDER_VALIDATION_FAILED", "Provider returned no validation result");
        }
        List<ValidationResult.Violation> violations = new ArrayList<>();
        violations.addAll(account.violations());
        violations.addAll(message.violations());
        if (!violations.isEmpty()) {
            String codes = violations.stream().map(ValidationResult.Violation::code).distinct()
                    .reduce((left, right) -> left + "," + right).orElse("INVALID");
            throw new ExecutionRejectedException(
                    "PROVIDER_CONFIG_INVALID", "Provider configuration failed validation: " + codes);
        }
    }

    private static void closeQuietly(ProviderSession session) {
        try {
            session.close();
        } catch (Exception ignored) {
            // No run exists yet to receive a suppressed close error.
        }
    }

    private record ProviderKey(String providerId, String version) {
    }

    private final class RunCoordinator implements RunHandle {
        private final RunExecutionSpec spec;
        private final ExecutionPorts ports;
        private final ProviderSession session;
        private final ProviderDescriptor descriptor;
        private final long total;
        private final int maximumConcurrency;
        private final ResizableSemaphore concurrency;
        private final RateGate rateGate;
        private final AtomicReference<RunState> state = new AtomicReference<>(RunState.PENDING);
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicReference<Throwable> asynchronousFailure = new AtomicReference<>();
        private final CompletableFuture<RunSummary> completion = new CompletableFuture<>();
        private final ConcurrentHashMap<String, CommandResult> commandResults = new ConcurrentHashMap<>();
        private final Phaser inFlight = new Phaser(1);
        private final Object pauseMonitor = new Object();
        private final Object resultMonitor = new Object();
        private final Object eventMonitor = new Object();
        private final AtomicLong eventSequence = new AtomicLong();
        private final LongAdder dispatched = new LongAdder();
        private final LongAdder succeeded = new LongAdder();
        private final LongAdder failed = new LongAdder();
        private final LongAdder unknown = new LongAdder();
        private final LongAdder skipped = new LongAdder();
        private final LongAdder retried = new LongAdder();
        private final List<ExecutionError> suppressedErrors = java.util.Collections.synchronizedList(new ArrayList<>());
        private volatile Instant startedAt;
        private volatile Instant runDeadline;

        private RunCoordinator(
                RunExecutionSpec spec,
                ExecutionPorts ports,
                ProviderSession session,
                ProviderDescriptor descriptor,
                long total,
                int initialConcurrency,
                int maximumConcurrency
        ) {
            this.spec = spec;
            this.ports = ports;
            this.session = session;
            this.descriptor = descriptor;
            this.total = total;
            this.maximumConcurrency = maximumConcurrency;
            concurrency = new ResizableSemaphore(initialConcurrency);
            rateGate = new RateGate(spec.policies().rateLimit(), ports.clock());
        }

        @Override
        public String runId() {
            return spec.runId();
        }

        @Override
        public RunState state() {
            return state.get();
        }

        @Override
        public CommandResult submit(RunCommand command) {
            Objects.requireNonNull(command, "command");
            return commandResults.computeIfAbsent(command.commandId(), ignored -> apply(command));
        }

        @Override
        public CompletionStage<RunSummary> completion() {
            return completion;
        }

        private void execute() {
            startedAt = ports.clock().now();
            runDeadline = startedAt.plus(spec.policies().timeout().runTimeout());
            Throwable fatal = null;
            try {
                if (cancelled.get()) {
                    state.set(RunState.CANCELLING);
                } else {
                    state.set(RunState.RUNNING);
                    emit(RunEvent.Type.RUN_STARTED, Map.of(
                            "providerId", descriptor.providerId(),
                            "providerVersion", descriptor.implementationVersion(),
                            "total", Long.toString(total)));
                    dispatchRecipients();
                }
            } catch (Throwable exception) {
                fatal = exception;
                cancelled.set(true);
                state.set(RunState.CANCELLING);
            } finally {
                inFlight.arriveAndAwaitAdvance();
                Throwable async = asynchronousFailure.get();
                if (fatal == null) {
                    fatal = async;
                } else if (async != null && async != fatal) {
                    fatal.addSuppressed(async);
                }
                finish(fatal);
                activeRuns.remove(this);
            }
        }

        private void dispatchRecipients() throws InterruptedException {
            boolean sourceExhausted = false;
            int batchSize = spec.policies().result().batchSize();
            while (!cancelled.get() && !sourceExhausted) {
                awaitResumed();
                if (!ports.clock().now().isBefore(runDeadline)) {
                    throw new RunDeadlineExceededException();
                }
                List<RecipientRecord> batch = ports.recipientSource().nextBatch(batchSize);
                if (batch == null) {
                    throw new IllegalStateException("RecipientSource returned null batch");
                }
                if (batch.size() > batchSize) {
                    throw new IllegalStateException("RecipientSource exceeded requested batch size");
                }
                sourceExhausted = batch.isEmpty();
                for (RecipientRecord recipient : batch) {
                    if (cancelled.get()) {
                        break;
                    }
                    awaitResumed();
                    concurrency.acquire();
                    awaitResumed();
                    if (cancelled.get()) {
                        concurrency.release();
                        break;
                    }
                    dispatched.increment();
                    if (dispatched.longValue() > total) {
                        concurrency.release();
                        throw new IllegalStateException("RecipientSource produced more records than declared");
                    }
                    inFlight.register();
                    try {
                        executor.execute(() -> executeRecipient(recipient));
                    } catch (RuntimeException exception) {
                        inFlight.arriveAndDeregister();
                        concurrency.release();
                        throw exception;
                    }
                }
                if (asynchronousFailure.get() != null) {
                    cancelled.set(true);
                }
            }
            if (!cancelled.get() && dispatched.longValue() != total) {
                throw new IllegalStateException("RecipientSource count does not match declared total");
            }
        }

        private void executeRecipient(RecipientRecord recipient) {
            try {
                ItemResult result = cancelled.get()
                        ? unsent(recipient)
                        : sendWithRetry(recipient);
                persistResult(result);
                increment(result.state());
                emitSafely(RunEvent.Type.ITEM_COMPLETED, Map.of(
                        "itemId", result.itemId(),
                        "state", result.state().name(),
                        "attempts", Integer.toString(result.attempts())));
            } catch (Throwable exception) {
                unknown.increment();
                asynchronousFailure.compareAndSet(null, exception);
                cancelled.set(true);
                signalPauseMonitor();
            } finally {
                concurrency.release();
                inFlight.arriveAndDeregister();
            }
        }

        private ItemResult sendWithRetry(RecipientRecord recipient) throws InterruptedException {
            ExecutionPolicies.RetryPolicy retryPolicy = spec.policies().retry();
            Instant itemDeadline = minimum(
                    ports.clock().now().plus(spec.policies().timeout().itemTimeout()), runDeadline);
            int attempt = 0;
            ProviderResult result;
            while (true) {
                attempt++;
                if (cancelled.get()) {
                    return attempt == 1 ? unsent(recipient) : unknownAfterCancellation(recipient, attempt - 1);
                }
                rateGate.acquire();
                result = invokeProvider(recipient, attempt, itemDeadline);
                boolean canRetry = result.retryable()
                        && attempt < retryPolicy.maxAttempts()
                        && ports.clock().now().isBefore(itemDeadline)
                        && !cancelled.get();
                if (!canRetry) {
                    break;
                }
                retried.increment();
                Duration delay = result.retryAfter() == null
                        ? retryDelay(retryPolicy, attempt)
                        : result.retryAfter();
                if (ports.clock().now().plus(delay).isAfter(itemDeadline)) {
                    break;
                }
                ports.clock().sleep(delay);
            }
            Map<String, String> metadata = new LinkedHashMap<>(result.metadata());
            metadata.put("wepush.errorCategory", result.category().name());
            return new ItemResult(
                    spec.runId(), recipient.itemId(), attempt, result.outcome(), result.code(),
                    result.diagnostic(), result.externalRequestId(), ports.clock().now(), metadata);
        }

        private ProviderResult invokeProvider(
                RecipientRecord recipient,
                int attempt,
                Instant itemDeadline
        ) throws InterruptedException {
            ProviderSendRequest request = new ProviderSendRequest(
                    spec.runId(), recipient.itemId(), attempt, recipient, spec.messageConfig(),
                    spec.runId() + ":" + recipient.itemId(), itemDeadline);
            Future<ProviderResult> call = executor.submit(() -> invokeSession(request));
            long remainingNanos = Duration.between(ports.clock().now(), itemDeadline).toNanos();
            if (remainingNanos <= 0) {
                call.cancel(true);
                return timeoutResult();
            }
            try {
                ProviderResult result = call.get(remainingNanos, TimeUnit.NANOSECONDS);
                if (result == null) {
                    return ProviderResult.failure(
                            "PROVIDER_NULL_RESULT", ErrorCategory.INTERNAL, false,
                            "Provider returned no result");
                }
                return result;
            } catch (TimeoutException exception) {
                call.cancel(true);
                return timeoutResult();
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();
                return ProviderResult.failure(
                        "PROVIDER_EXCEPTION", ErrorCategory.INTERNAL, false,
                        cause == null ? "Provider call failed" : cause.getClass().getSimpleName());
            } catch (InterruptedException exception) {
                call.cancel(true);
                throw exception;
            }
        }

        private ProviderResult invokeSession(ProviderSendRequest request) throws Exception {
            if (descriptor.threadSafetyMode() == ProviderDescriptor.ThreadSafetyMode.THREAD_SAFE) {
                return session.send(request, cancelled::get);
            }
            synchronized (session) {
                return session.send(request, cancelled::get);
            }
        }

        private ProviderResult timeoutResult() {
            boolean retryable = spec.policies().retry().retryTimeouts();
            return new ProviderResult(
                    ItemState.UNKNOWN, "ITEM_TIMEOUT", ErrorCategory.TIMEOUT, retryable,
                    null, "Provider result is unknown after timeout", "", Map.of());
        }

        private ItemResult unsent(RecipientRecord recipient) {
            return new ItemResult(
                    spec.runId(), recipient.itemId(), 0, ItemState.UNSENT,
                    "CANCELLED_BEFORE_SEND", "", "", ports.clock().now(), Map.of());
        }

        private ItemResult unknownAfterCancellation(RecipientRecord recipient, int attempts) {
            return new ItemResult(
                    spec.runId(), recipient.itemId(), attempts, ItemState.UNKNOWN,
                    "CANCELLED_IN_FLIGHT", "Provider outcome may be unknown", "",
                    ports.clock().now(), Map.of());
        }

        private void persistResult(ItemResult result) {
            synchronized (resultMonitor) {
                ports.resultSink().append(List.of(result));
            }
        }

        private void increment(ItemState outcome) {
            switch (outcome) {
                case SUCCEEDED -> succeeded.increment();
                case FAILED -> failed.increment();
                case UNKNOWN -> unknown.increment();
                case SKIPPED -> skipped.increment();
                case UNSENT -> {
                    // Unsent is derived from total minus final item outcomes.
                }
            }
        }

        private CommandResult apply(RunCommand command) {
            if (command instanceof RunCommand.PauseRun) {
                return pause(command.commandId());
            }
            if (command instanceof RunCommand.ResumeRun) {
                return resume(command.commandId());
            }
            if (command instanceof RunCommand.CancelRun) {
                return cancel(command.commandId());
            }
            RunCommand.ChangeConcurrency change = (RunCommand.ChangeConcurrency) command;
            return changeConcurrency(change);
        }

        private CommandResult pause(String commandId) {
            if (!state.compareAndSet(RunState.RUNNING, RunState.PAUSED)) {
                return CommandResult.rejected(commandId, "RUN_STATE_CONFLICT", "Run is not running");
            }
            emitSafely(RunEvent.Type.STATE_CHANGED, Map.of("state", RunState.PAUSED.name()));
            return CommandResult.accepted(commandId, "RUN_PAUSED");
        }

        private CommandResult resume(String commandId) {
            if (!state.compareAndSet(RunState.PAUSED, RunState.RUNNING)) {
                return CommandResult.rejected(commandId, "RUN_STATE_CONFLICT", "Run is not paused");
            }
            signalPauseMonitor();
            emitSafely(RunEvent.Type.STATE_CHANGED, Map.of("state", RunState.RUNNING.name()));
            return CommandResult.accepted(commandId, "RUN_RESUMED");
        }

        private CommandResult cancel(String commandId) {
            RunState current = state.get();
            if (current.terminal()) {
                return CommandResult.rejected(commandId, "RUN_STATE_CONFLICT", "Run is already terminal");
            }
            cancelled.set(true);
            state.set(RunState.CANCELLING);
            signalPauseMonitor();
            emitSafely(RunEvent.Type.STATE_CHANGED, Map.of("state", RunState.CANCELLING.name()));
            return CommandResult.accepted(commandId, "RUN_CANCELLING");
        }

        private CommandResult changeConcurrency(RunCommand.ChangeConcurrency command) {
            RunState current = state.get();
            if (current != RunState.RUNNING && current != RunState.PAUSED) {
                return CommandResult.rejected(
                        command.commandId(), "RUN_STATE_CONFLICT", "Run is not active");
            }
            int minimum = spec.policies().concurrency().minimum();
            if (command.target() < minimum || command.target() > maximumConcurrency) {
                return CommandResult.rejected(
                        command.commandId(), "CONCURRENCY_OUT_OF_RANGE",
                        "Allowed concurrency range is " + minimum + ".." + maximumConcurrency);
            }
            concurrency.resize(command.target());
            emitSafely(RunEvent.Type.CONCURRENCY_CHANGED, Map.of(
                    "target", Integer.toString(command.target())));
            return CommandResult.accepted(command.commandId(), "CONCURRENCY_CHANGED");
        }

        private void awaitResumed() throws InterruptedException {
            synchronized (pauseMonitor) {
                while (state.get() == RunState.PAUSED && !cancelled.get()) {
                    pauseMonitor.wait();
                }
            }
        }

        private void signalPauseMonitor() {
            synchronized (pauseMonitor) {
                pauseMonitor.notifyAll();
            }
        }

        private void cancelForEngineShutdown() {
            cancelled.set(true);
            state.updateAndGet(current -> current.terminal() ? current : RunState.CANCELLING);
            signalPauseMonitor();
        }

        private void finish(Throwable fatal) {
            if (fatal != null) {
                addError("EXECUTION_FAILED", fatal, "execute");
            }

            closeResource("result-flush", ports.resultSink()::flush);
            List<com.fangxuele.wepush.next.core.api.ArtifactRef> artifacts = safeArtifacts();
            closeResource("provider-close", session::close);
            closeResource("recipient-close", ports.recipientSource()::close);
            closeResource("artifact-close", ports.artifactSink()::close);
            closeResource("result-close", ports.resultSink()::close);

            long processed = succeeded.longValue() + failed.longValue()
                    + unknown.longValue() + skipped.longValue();
            long unsent = Math.max(0, total - processed);
            RunState finalState;
            if (fatal != null) {
                finalState = RunState.FAILED;
            } else if (cancelled.get()) {
                finalState = RunState.CANCELLED;
            } else if (failed.longValue() > 0 || unknown.longValue() > 0 || unsent > 0) {
                finalState = RunState.PARTIAL;
            } else {
                finalState = RunState.SUCCEEDED;
            }
            state.set(finalState);
            Instant endedAt = ports.clock().now();
            if (endedAt.isBefore(startedAt)) {
                endedAt = startedAt;
            }

            RunSummary summary = new RunSummary(
                    spec.runId(), finalState, total, succeeded.longValue(), failed.longValue(),
                    unknown.longValue(), unsent, skipped.longValue(), retried.longValue(),
                    startedAt, endedAt, artifacts, List.copyOf(suppressedErrors));
            emitSafely(fatal == null ? RunEvent.Type.RUN_COMPLETED : RunEvent.Type.RUN_FAILED, Map.of(
                    "state", finalState.name(),
                    "succeeded", Long.toString(summary.succeeded()),
                    "failed", Long.toString(summary.failed()),
                    "unknown", Long.toString(summary.unknown()),
                    "unsent", Long.toString(summary.unsent())));
            closeResource("event-flush", ports.eventSink()::flush);
            closeResource("event-close", ports.eventSink()::close);
            completion.complete(summary);
        }

        private List<com.fangxuele.wepush.next.core.api.ArtifactRef> safeArtifacts() {
            try {
                List<com.fangxuele.wepush.next.core.api.ArtifactRef> artifacts = ports.artifactSink().artifacts();
                return artifacts == null ? List.of() : List.copyOf(artifacts);
            } catch (RuntimeException exception) {
                addError("ARTIFACT_LIST_FAILED", exception, "artifact-list");
                return List.of();
            }
        }

        private void emit(RunEvent.Type type, Map<String, String> data) {
            synchronized (eventMonitor) {
                ports.eventSink().append(new RunEvent(
                        spec.runId(), eventSequence.incrementAndGet(), type, ports.clock().now(), data));
            }
        }

        private void emitSafely(RunEvent.Type type, Map<String, String> data) {
            try {
                emit(type, data);
            } catch (RuntimeException exception) {
                addError("EVENT_WRITE_FAILED", exception, "event");
            }
        }

        private void closeResource(String phase, ThrowingAction action) {
            try {
                action.run();
            } catch (Exception exception) {
                addError("RESOURCE_CLOSE_FAILED", exception, phase);
            }
        }

        private void addError(String code, Throwable exception, String phase) {
            suppressedErrors.add(new ExecutionError(
                    code, exception.getClass().getSimpleName(), phase));
        }

        private Duration retryDelay(ExecutionPolicies.RetryPolicy policy, int completedAttempt) {
            double baseMillis = policy.initialDelay().toMillis()
                    * Math.pow(policy.multiplier(), Math.max(0, completedAttempt - 1));
            double cappedMillis = Math.min(baseMillis, policy.maxDelay().toMillis());
            double jitterFactor = policy.jitter() == 0.0
                    ? 1.0
                    : 1.0 + ThreadLocalRandom.current().nextDouble(-policy.jitter(), policy.jitter());
            return Duration.ofMillis(Math.max(0, Math.round(cappedMillis * jitterFactor)));
        }

        private Instant minimum(Instant left, Instant right) {
            return left.isBefore(right) ? left : right;
        }
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }

    private static final class ResizableSemaphore extends Semaphore {
        private final AtomicLong target;

        private ResizableSemaphore(int permits) {
            super(permits, true);
            target = new AtomicLong(permits);
        }

        private synchronized void resize(int newTarget) {
            int previous = Math.toIntExact(target.getAndSet(newTarget));
            int delta = newTarget - previous;
            if (delta > 0) {
                release(delta);
            } else if (delta < 0) {
                reducePermits(-delta);
            }
        }
    }

    private static final class RateGate {
        private final boolean limited;
        private final Duration interval;
        private final com.fangxuele.wepush.next.core.api.ExecutionClock clock;
        private Instant nextPermit;

        private RateGate(
                ExecutionPolicies.RateLimitPolicy policy,
                com.fangxuele.wepush.next.core.api.ExecutionClock clock
        ) {
            limited = policy.limited();
            interval = limited ? policy.period().dividedBy(policy.permits()) : Duration.ZERO;
            this.clock = clock;
        }

        private synchronized void acquire() throws InterruptedException {
            if (!limited) {
                return;
            }
            Instant now = clock.now();
            if (nextPermit == null || !nextPermit.isAfter(now)) {
                nextPermit = now.plus(interval);
                return;
            }
            Duration wait = Duration.between(now, nextPermit);
            nextPermit = nextPermit.plus(interval);
            clock.sleep(wait);
        }
    }

    private static final class RunDeadlineExceededException extends RuntimeException {
    }
}

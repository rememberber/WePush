package com.fangxuele.wepush.next.embedded;

import com.fangxuele.wepush.next.core.api.ArtifactSink;
import com.fangxuele.wepush.next.core.api.ExecutionClock;
import com.fangxuele.wepush.next.core.api.ExecutionEngine;
import com.fangxuele.wepush.next.core.api.ExecutionPorts;
import com.fangxuele.wepush.next.core.api.ExecutionRejectedException;
import com.fangxuele.wepush.next.core.api.RecipientRecord;
import com.fangxuele.wepush.next.core.api.RecipientSource;
import com.fangxuele.wepush.next.core.api.ResultSink;
import com.fangxuele.wepush.next.core.api.RunEventSink;
import com.fangxuele.wepush.next.core.api.RunExecutionSpec;
import com.fangxuele.wepush.next.core.api.RunHandle;
import com.fangxuele.wepush.next.core.api.SecretResolver;
import com.fangxuele.wepush.next.core.engine.DefaultExecutionEngine;
import com.fangxuele.wepush.next.provider.spi.ProviderFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Framework-free facade for running WePush directly inside a Java process.
 *
 * <p>The caller explicitly registers every allowed Provider. This facade does not start
 * Service, discover plugins, create a database, or read Service configuration.</p>
 */
public final class WePushEngine implements AutoCloseable {
    private final ExecutionEngine engine;
    private final SecretResolver secretResolver;
    private final Function<RunExecutionSpec, ResultSink> resultSinks;
    private final Function<RunExecutionSpec, ArtifactSink> artifactSinks;
    private final Function<RunExecutionSpec, RunEventSink> eventSinks;
    private final ExecutionClock clock;
    private final List<AutoCloseable> sharedResources;
    private final AtomicBoolean closed = new AtomicBoolean();

    private WePushEngine(Builder builder) {
        engine = new DefaultExecutionEngine(builder.providers);
        secretResolver = builder.secretResolver;
        resultSinks = builder.resultSinks;
        artifactSinks = builder.artifactSinks;
        eventSinks = builder.eventSinks;
        clock = builder.clock;
        sharedResources = uniqueResources(
                builder.sharedResultSink, builder.sharedArtifactSink, builder.sharedEventSink);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Starts a Run and transfers ownership of the RecipientSource to the engine.
     * It is closed after completion or when the Run is rejected after resource creation.
     */
    public RunHandle start(RunExecutionSpec spec, RecipientSource recipients) {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(recipients, "recipients");
        ensureOpen();

        List<AutoCloseable> acquired = new ArrayList<>();
        acquired.add(recipients);
        try {
            ResultSink resultSink = resource(resultSinks, spec, "ResultSink");
            acquired.add(resultSink);
            ArtifactSink artifactSink = resource(artifactSinks, spec, "ArtifactSink");
            acquired.add(artifactSink);
            RunEventSink eventSink = resource(eventSinks, spec, "RunEventSink");
            acquired.add(eventSink);

            return engine.start(spec, new ExecutionPorts(
                    recipients, secretResolver, resultSink, artifactSink, eventSink, clock));
        } catch (RuntimeException | Error exception) {
            closeReversed(acquired, exception);
            throw exception;
        }
    }

    /**
     * Starts a Run from an immutable snapshot of the supplied recipients.
     */
    public RunHandle start(RunExecutionSpec spec, List<RecipientRecord> recipients) {
        return start(spec, new ListRecipientSource(recipients));
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        RuntimeException failure = null;
        try {
            engine.close();
        } catch (RuntimeException exception) {
            failure = exception;
        }
        for (AutoCloseable resource : sharedResources) {
            try {
                flush(resource);
                resource.close();
            } catch (Exception exception) {
                if (failure == null) {
                    failure = new IllegalStateException("Unable to close an Embedded SDK resource", exception);
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new ExecutionRejectedException(
                    "EMBEDDED_ENGINE_CLOSED", "Embedded WePush Engine is closed");
        }
    }

    private static <T> T resource(
            Function<RunExecutionSpec, ? extends T> factory,
            RunExecutionSpec spec,
            String name
    ) {
        T resource = factory.apply(spec);
        if (resource == null) {
            throw new IllegalStateException(name + " factory returned null");
        }
        return resource;
    }

    private static List<AutoCloseable> uniqueResources(AutoCloseable... candidates) {
        Set<AutoCloseable> identities = Collections.newSetFromMap(new IdentityHashMap<>());
        List<AutoCloseable> resources = new ArrayList<>();
        for (AutoCloseable candidate : candidates) {
            if (candidate != null && identities.add(candidate)) {
                resources.add(candidate);
            }
        }
        return List.copyOf(resources);
    }

    private static void flush(AutoCloseable resource) {
        if (resource instanceof ResultSink resultSink) {
            resultSink.flush();
        } else if (resource instanceof RunEventSink eventSink) {
            eventSink.flush();
        }
    }

    private static void closeReversed(List<AutoCloseable> resources, Throwable failure) {
        for (int index = resources.size() - 1; index >= 0; index--) {
            try {
                resources.get(index).close();
            } catch (Exception closeFailure) {
                failure.addSuppressed(closeFailure);
            }
        }
    }

    public static final class Builder {
        private final List<ProviderFactory> providers = new ArrayList<>();
        private SecretResolver secretResolver = ref -> {
            throw new IllegalStateException("No SecretResolver configured for " + ref.namespace()
                    + "/" + ref.name() + ":" + ref.version());
        };
        private Function<RunExecutionSpec, ResultSink> resultSinks;
        private Function<RunExecutionSpec, ArtifactSink> artifactSinks = ignored -> ArtifactSink.none();
        private Function<RunExecutionSpec, RunEventSink> eventSinks = ignored -> DiscardingEventSink.INSTANCE;
        private ExecutionClock clock = ExecutionClock.system();
        private ResultSink sharedResultSink;
        private ArtifactSink sharedArtifactSink;
        private RunEventSink sharedEventSink;

        private Builder() {
        }

        public Builder provider(ProviderFactory provider) {
            providers.add(Objects.requireNonNull(provider, "provider"));
            return this;
        }

        public Builder providers(Collection<? extends ProviderFactory> providers) {
            Objects.requireNonNull(providers, "providers").forEach(this::provider);
            return this;
        }

        public Builder secretResolver(SecretResolver secretResolver) {
            this.secretResolver = Objects.requireNonNull(secretResolver, "secretResolver");
            return this;
        }

        /**
         * Uses an engine-scoped ResultSink. It must be safe for concurrent Runs and is closed
         * once when WePushEngine is closed, not after each Run.
         */
        public Builder resultSink(ResultSink resultSink) {
            sharedResultSink = Objects.requireNonNull(resultSink, "resultSink");
            resultSinks = ignored -> new SharedResultSink(sharedResultSink);
            return this;
        }

        /**
         * Uses an engine-scoped ArtifactSink. It must be safe for concurrent Runs and is
         * closed once when WePushEngine is closed, not after each Run.
         */
        public Builder artifactSink(ArtifactSink artifactSink) {
            sharedArtifactSink = Objects.requireNonNull(artifactSink, "artifactSink");
            artifactSinks = ignored -> new SharedArtifactSink(sharedArtifactSink);
            return this;
        }

        /**
         * Creates a Run-owned ResultSink. The Core Engine flushes and closes it after the Run.
         */
        public Builder resultSinkFactory(
                Function<? super RunExecutionSpec, ? extends ResultSink> resultSinkFactory
        ) {
            Objects.requireNonNull(resultSinkFactory, "resultSinkFactory");
            sharedResultSink = null;
            resultSinks = spec -> resultSinkFactory.apply(spec);
            return this;
        }

        /**
         * Uses an engine-scoped RunEventSink. It must be safe for concurrent Runs and is closed
         * once when WePushEngine is closed, not after each Run.
         */
        public Builder eventSink(RunEventSink eventSink) {
            sharedEventSink = Objects.requireNonNull(eventSink, "eventSink");
            eventSinks = ignored -> new SharedEventSink(sharedEventSink);
            return this;
        }

        /**
         * Creates a Run-owned RunEventSink. The Core Engine flushes and closes it after the Run.
         */
        public Builder eventSinkFactory(
                Function<? super RunExecutionSpec, ? extends RunEventSink> eventSinkFactory
        ) {
            Objects.requireNonNull(eventSinkFactory, "eventSinkFactory");
            sharedEventSink = null;
            eventSinks = spec -> eventSinkFactory.apply(spec);
            return this;
        }

        /**
         * Creates a Run-owned ArtifactSink. The default is ArtifactSink.none().
         */
        public Builder artifactSinkFactory(
                Function<? super RunExecutionSpec, ? extends ArtifactSink> artifactSinkFactory
        ) {
            Objects.requireNonNull(artifactSinkFactory, "artifactSinkFactory");
            sharedArtifactSink = null;
            artifactSinks = spec -> artifactSinkFactory.apply(spec);
            return this;
        }

        public Builder clock(ExecutionClock clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
            return this;
        }

        public WePushEngine build() {
            if (providers.isEmpty()) {
                throw new IllegalStateException("At least one Provider must be registered");
            }
            if (resultSinks == null) {
                throw new IllegalStateException(
                        "A ResultSink or ResultSink factory must be registered");
            }
            return new WePushEngine(this);
        }
    }

    private static final class ListRecipientSource implements RecipientSource {
        private final List<RecipientRecord> recipients;
        private int cursor;

        private ListRecipientSource(List<RecipientRecord> recipients) {
            this.recipients = List.copyOf(Objects.requireNonNull(recipients, "recipients"));
        }

        @Override
        public long totalCount() {
            return recipients.size();
        }

        @Override
        public synchronized List<RecipientRecord> nextBatch(int maximumSize) {
            if (maximumSize < 1) {
                throw new IllegalArgumentException("maximumSize must be positive");
            }
            if (cursor >= recipients.size()) {
                return List.of();
            }
            int end = Math.min(recipients.size(), cursor + maximumSize);
            List<RecipientRecord> batch = List.copyOf(recipients.subList(cursor, end));
            cursor = end;
            return batch;
        }
    }

    private record SharedResultSink(ResultSink delegate) implements ResultSink {
        @Override
        public void append(List<com.fangxuele.wepush.next.core.api.ItemResult> batch) {
            delegate.append(batch);
        }

        @Override
        public void flush() {
            delegate.flush();
        }

        @Override
        public void close() {
            // Engine-scoped delegate is closed by WePushEngine.close().
        }
    }

    private record SharedEventSink(RunEventSink delegate) implements RunEventSink {
        @Override
        public void append(com.fangxuele.wepush.next.core.api.RunEvent event) {
            delegate.append(event);
        }

        @Override
        public void flush() {
            delegate.flush();
        }

        @Override
        public void close() {
            // Engine-scoped delegate is closed by WePushEngine.close().
        }
    }

    private record SharedArtifactSink(ArtifactSink delegate) implements ArtifactSink {
        @Override
        public List<com.fangxuele.wepush.next.core.api.ArtifactRef> artifacts() {
            return delegate.artifacts();
        }

        @Override
        public com.fangxuele.wepush.next.core.api.ArtifactRef write(
                String type,
                String originalName,
                String contentType,
                ContentWriter writer
        ) throws java.io.IOException {
            return delegate.write(type, originalName, contentType, writer);
        }

        @Override
        public void close() {
            // Engine-scoped delegate is closed by WePushEngine.close().
        }
    }

    private enum DiscardingEventSink implements RunEventSink {
        INSTANCE;

        @Override
        public void append(com.fangxuele.wepush.next.core.api.RunEvent event) {
        }

        @Override
        public void flush() {
        }
    }
}

package com.fangxuele.wepush.next.service.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fangxuele.wepush.next.service.application.ProviderCatalogQuery;
import com.fangxuele.wepush.next.service.application.ProviderRegistry;
import com.fangxuele.wepush.next.service.application.ArtifactApplicationService;
import com.fangxuele.wepush.next.service.application.AgentApplicationService;
import com.fangxuele.wepush.next.service.application.AgentArtifactApplicationService;
import com.fangxuele.wepush.next.service.application.AgentCertificateAuthority;
import com.fangxuele.wepush.next.service.application.AgentControlGateway;
import com.fangxuele.wepush.next.service.application.AgentIdentityService;
import com.fangxuele.wepush.next.service.application.ArtifactStore;
import com.fangxuele.wepush.next.service.application.ArtifactUploadTokenCodec;
import com.fangxuele.wepush.next.service.application.AccountApplicationService;
import com.fangxuele.wepush.next.service.application.AccountAuthCircuitService;
import com.fangxuele.wepush.next.service.application.ApiAccessService;
import com.fangxuele.wepush.next.service.application.AudienceApplicationService;
import com.fangxuele.wepush.next.service.application.AudienceImportApplicationService;
import com.fangxuele.wepush.next.service.application.ControlPlaneQueryService;
import com.fangxuele.wepush.next.service.application.ControlPlaneWakeupPublisher;
import com.fangxuele.wepush.next.service.application.JobApplicationService;
import com.fangxuele.wepush.next.service.application.JsonCodec;
import com.fangxuele.wepush.next.service.application.MessageApplicationService;
import com.fangxuele.wepush.next.service.application.ResourceIdGenerator;
import com.fangxuele.wepush.next.service.application.RunApplicationService;
import com.fangxuele.wepush.next.service.application.RunCommandApplicationService;
import com.fangxuele.wepush.next.service.application.RunCommandGateway;
import com.fangxuele.wepush.next.service.application.RunDispatcher;
import com.fangxuele.wepush.next.service.application.CursorCodec;
import com.fangxuele.wepush.next.service.application.RunResultApplicationService;
import com.fangxuele.wepush.next.service.application.RemoteRunCoordinator;
import com.fangxuele.wepush.next.service.application.SecretApplicationService;
import com.fangxuele.wepush.next.service.application.SecretStore;
import com.fangxuele.wepush.next.service.application.ScheduleApplicationService;
import com.fangxuele.wepush.next.service.application.TransactionRunner;
import com.fangxuele.wepush.next.service.application.WorkspaceApplicationService;
import com.fangxuele.wepush.next.service.application.WorkspaceResourceGovernor;
import com.fangxuele.wepush.next.service.domain.AccountRepository;
import com.fangxuele.wepush.next.service.domain.AccountAuthCircuitRepository;
import com.fangxuele.wepush.next.service.domain.ApiAccessRepository;
import com.fangxuele.wepush.next.service.domain.AuditEventRepository;
import com.fangxuele.wepush.next.service.domain.ArtifactRepository;
import com.fangxuele.wepush.next.service.domain.AgentRepository;
import com.fangxuele.wepush.next.service.domain.AgentLeaseRepository;
import com.fangxuele.wepush.next.service.domain.AgentOutboundMessageRepository;
import com.fangxuele.wepush.next.service.domain.AgentIdentityRepository;
import com.fangxuele.wepush.next.service.domain.AudienceRepository;
import com.fangxuele.wepush.next.service.domain.AudienceImportRepository;
import com.fangxuele.wepush.next.service.domain.JobRepository;
import com.fangxuele.wepush.next.service.domain.MessageRepository;
import com.fangxuele.wepush.next.service.domain.RunRepository;
import com.fangxuele.wepush.next.service.domain.RunCommandRepository;
import com.fangxuele.wepush.next.service.domain.RunResultRepository;
import com.fangxuele.wepush.next.service.domain.ScheduleRepository;
import com.fangxuele.wepush.next.service.domain.WorkspaceRepository;
import com.fangxuele.wepush.next.service.domain.WorkspaceId;
import com.fangxuele.wepush.next.service.domain.WorkspacePolicyRepository;
import com.fangxuele.wepush.next.service.infrastructure.JacksonJsonCodec;
import com.fangxuele.wepush.next.service.infrastructure.JdbcAccountRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcAccountAuthCircuitRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcApiAccessRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcAuditEventRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcArtifactRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcAgentRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcAgentLeaseRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcAgentOutboundMessageRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcAgentIdentityRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcAudienceRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcAudienceImportRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcJobRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcMessageRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcRunRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcRunCommandRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcRunResultRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcScheduleRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcWorkspaceRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcWorkspacePolicyRepository;
import com.fangxuele.wepush.next.service.infrastructure.LocalEnvelopeSecretStore;
import com.fangxuele.wepush.next.service.infrastructure.LocalFileArtifactStore;
import com.fangxuele.wepush.next.service.infrastructure.LocalHmacCursorCodec;
import com.fangxuele.wepush.next.service.infrastructure.LocalAgentCertificateAuthority;
import com.fangxuele.wepush.next.service.infrastructure.ServiceLoaderProviderRegistry;
import com.fangxuele.wepush.next.service.infrastructure.S3ArtifactStore;
import com.fangxuele.wepush.next.service.infrastructure.SQLiteDatabase;
import com.fangxuele.wepush.next.service.infrastructure.PostgreSQLDatabase;
import com.fangxuele.wepush.next.service.infrastructure.SpringTransactionRunner;
import com.fangxuele.wepush.next.service.infrastructure.StandaloneRunExecutor;
import com.fangxuele.wepush.next.service.infrastructure.UuidResourceIdGenerator;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.scheduling.support.CronExpression;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;

@Configuration(proxyBeanMethods = false)
class ServiceComposition {
    @Bean
    SystemOperationsService systemOperationsService(
            JdbcTemplate jdbc, ObjectMapper json, Environment environment,
            @Value("${wepush.version-check.releases-uri:https://api.github.com/repos/rememberber/WePush/releases?per_page=50}") URI releasesUri) {
        return new SystemOperationsService(jdbc, json, environment, releasesUri);
    }
    @Bean(destroyMethod = "close")
    HikariDataSource dataSource(
            @Value("${wepush.database.kind:sqlite}") String kind,
            @Value("${wepush.database.path:.local/data/wepush-next.db}") String path,
            @Value("${wepush.database.url:}") String url,
            @Value("${wepush.database.username:}") String username,
            @Value("${wepush.database.password:}") String password,
            @Value("${wepush.database.maximum-pool-size:16}") int maximumPoolSize) {
        if ("sqlite".equalsIgnoreCase(kind)) return SQLiteDatabase.create(Path.of(path));
        if ("postgresql".equalsIgnoreCase(kind)) {
            return PostgreSQLDatabase.create(url, username, password, maximumPoolSize);
        }
        throw new IllegalArgumentException("wepush.database.kind must be sqlite or postgresql");
    }

    @Bean
    PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    @Bean(initMethod = "migrate")
    Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/sqlite")
                .validateMigrationNaming(true)
                .load();
    }

    @Bean
    TransactionRunner transactionRunner(PlatformTransactionManager transactionManager) {
        return new SpringTransactionRunner(new TransactionTemplate(transactionManager));
    }

    @Bean
    JsonCodec jsonCodec() {
        return new JacksonJsonCodec();
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    ResourceIdGenerator resourceIdGenerator() {
        return new UuidResourceIdGenerator();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    WorkspaceRepository workspaceRepository(JdbcTemplate jdbc, Flyway flyway) {
        return new JdbcWorkspaceRepository(jdbc);
    }

    @Bean
    WorkspacePolicyRepository workspacePolicyRepository(JdbcTemplate jdbc, Flyway flyway) {
        return new JdbcWorkspacePolicyRepository(jdbc);
    }

    @Bean
    WorkspaceResourceGovernor workspaceResourceGovernor(
            WorkspaceRepository workspaces, WorkspacePolicyRepository policies,
            TransactionRunner transactions, Clock clock) {
        return new WorkspaceResourceGovernor(workspaces, policies, transactions, clock);
    }

    @Bean
    AccountRepository accountRepository(JdbcTemplate jdbc, Flyway flyway) {
        return new JdbcAccountRepository(jdbc);
    }

    @Bean
    AccountAuthCircuitRepository accountAuthCircuitRepository(JdbcTemplate jdbc, Flyway flyway) {
        return new JdbcAccountAuthCircuitRepository(jdbc);
    }

    @Bean
    AccountAuthCircuitService accountAuthCircuitService(
            AccountAuthCircuitRepository circuits, JsonCodec json, TransactionRunner transactions, Clock clock,
            @Value("${wepush.reliability.authentication-circuit.threshold:3}") int threshold,
            @Value("${wepush.reliability.authentication-circuit.window:PT15M}") Duration window,
            @Value("${wepush.reliability.authentication-circuit.open-duration:PT15M}") Duration openDuration) {
        return new AccountAuthCircuitService(circuits, json, transactions, clock,
                threshold, window, openDuration);
    }

    @Bean
    AgentRepository agentRepository(JdbcTemplate jdbc, ObjectMapper mapper, Flyway flyway) {
        return new JdbcAgentRepository(jdbc, mapper);
    }

    @Bean
    AgentLeaseRepository agentLeaseRepository(JdbcTemplate jdbc, Flyway flyway) {
        return new JdbcAgentLeaseRepository(jdbc);
    }

    @Bean
    AgentOutboundMessageRepository agentOutboundMessageRepository(JdbcTemplate jdbc, Flyway flyway) {
        return new JdbcAgentOutboundMessageRepository(jdbc);
    }

    @Bean
    AgentIdentityRepository agentIdentityRepository(JdbcTemplate jdbc, Flyway flyway) {
        return new JdbcAgentIdentityRepository(jdbc);
    }

    @Bean
    SecureRandom secureRandom() {
        return new SecureRandom();
    }

    @Bean
    LocalAgentCertificateAuthority agentCertificateAuthority(
            Clock clock, SecureRandom random,
            @Value("${wepush.agent.identity.ca-key-path:.local/agent-ca/ca-key.pem}") String keyPath,
            @Value("${wepush.agent.identity.ca-certificate-path:.local/agent-ca/ca-certificate.pem}") String certificatePath,
            @Value("${wepush.agent.identity.ca-validity:P3650D}") Duration validity) {
        return new LocalAgentCertificateAuthority(Path.of(keyPath), Path.of(certificatePath),
                clock, random, validity);
    }

    @Bean
    AgentIdentityService agentIdentityService(
            AgentIdentityRepository identities, AgentCertificateAuthority certificates,
            WorkspaceRepository workspaces, WorkspaceResourceGovernor resources,
            ResourceIdGenerator ids, TransactionRunner transactions, Clock clock, SecureRandom random,
            @Value("${wepush.agent.identity.credential-ttl:P90D}") Duration credentialTtl) {
        return new AgentIdentityService(identities, certificates, workspaces, resources, ids, transactions,
                clock, random, credentialTtl);
    }

    @Bean
    ArtifactRepository artifactRepository(JdbcTemplate jdbc, Flyway flyway) {
        return new JdbcArtifactRepository(jdbc);
    }

    @Bean
    ApiAccessRepository apiAccessRepository(JdbcTemplate jdbc, Flyway flyway) {
        return new JdbcApiAccessRepository(jdbc);
    }

    @Bean
    AuditEventRepository auditEventRepository(JdbcTemplate jdbc, Flyway flyway) {
        return new JdbcAuditEventRepository(jdbc);
    }

    @Bean
    ApiAccessService apiAccessService(ApiAccessRepository access, WorkspaceRepository workspaces,
                                      ResourceIdGenerator ids,
                                      TransactionRunner transactions, Clock clock, SecureRandom random) {
        return new ApiAccessService(access, workspaces, ids, transactions, clock, random);
    }

    @Bean
    ApplicationRunner initializeApiSecurity(
            ApiAccessService access,
            @Value("${wepush.security.enabled:false}") boolean enabled,
            @Value("${wepush.security.bootstrap-token:}") String bootstrapToken) {
        return arguments -> {
            if (enabled) access.initializeBootstrap(bootstrapToken);
        };
    }

    @Bean
    ApplicationRunner validateServerMode(
            @Value("${wepush.mode:standalone}") String mode,
            @Value("${wepush.database.kind:sqlite}") String databaseKind,
            @Value("${wepush.artifact.kind:local}") String artifactKind,
            @Value("${wepush.security.enabled:false}") boolean securityEnabled,
            @Value("${wepush.agent.grpc.tls.enabled:false}") boolean agentTlsEnabled,
            @Value("${server.address:127.0.0.1}") String bindAddress) {
        return arguments -> validateDeployment(mode, databaseKind, artifactKind, securityEnabled,
                agentTlsEnabled, bindAddress);
    }

    static void validateDeployment(String mode, String databaseKind, String artifactKind,
                                   boolean securityEnabled, boolean agentTlsEnabled,
                                   String bindAddress) {
        if (!AgentGrpcServer.isLoopback(bindAddress) && !securityEnabled) {
            throw new IllegalStateException(
                    "API security is required when HTTP binds outside loopback");
        }
        if ("server".equalsIgnoreCase(mode)) {
            if (!"postgresql".equalsIgnoreCase(databaseKind)
                    || !"s3".equalsIgnoreCase(artifactKind)
                    || !securityEnabled || !agentTlsEnabled) {
                throw new IllegalStateException("Server mode requires PostgreSQL, S3 Artifact Store, "
                        + "API security, and Agent gRPC TLS");
            }
        }
    }

    @Bean
    ArtifactStore artifactStore(
            @Value("${wepush.artifact.kind:local}") String kind,
            @Value("${wepush.artifact.root:.local/artifacts}") String root,
            @Value("${wepush.artifact.environment:standalone}") String environment,
            @Value("${wepush.artifact.s3.bucket:}") String bucket,
            @Value("${wepush.artifact.s3.region:us-east-1}") String region,
            @Value("${wepush.artifact.s3.endpoint:}") String endpoint,
            @Value("${wepush.artifact.s3.path-style-access:false}") boolean pathStyleAccess,
            @Value("${wepush.artifact.s3.access-key:}") String accessKey,
            @Value("${wepush.artifact.s3.secret-key:}") String secretKey,
            @Value("${wepush.artifact.s3.server-side-encryption:AUTO}") String serverSideEncryption) {
        if ("local".equalsIgnoreCase(kind)) return new LocalFileArtifactStore(Path.of(root), environment);
        if ("s3".equalsIgnoreCase(kind)) {
            return new S3ArtifactStore(new S3ArtifactStore.Configuration(bucket, region, endpoint,
                    pathStyleAccess, accessKey, secretKey, environment, serverSideEncryption));
        }
        throw new IllegalArgumentException("wepush.artifact.kind must be local or s3");
    }

    @Bean
    MessageRepository messageRepository(JdbcTemplate jdbc, Flyway flyway) {
        return new JdbcMessageRepository(jdbc);
    }

    @Bean
    AudienceRepository audienceRepository(JdbcTemplate jdbc, Flyway flyway) {
        return new JdbcAudienceRepository(jdbc);
    }

    @Bean
    AudienceImportRepository audienceImportRepository(JdbcTemplate jdbc, Flyway flyway) {
        return new JdbcAudienceImportRepository(jdbc);
    }

    @Bean
    JobRepository jobRepository(JdbcTemplate jdbc, Flyway flyway) {
        return new JdbcJobRepository(jdbc);
    }

    @Bean
    RunRepository runRepository(JdbcTemplate jdbc, Flyway flyway) {
        return new JdbcRunRepository(jdbc);
    }

    @Bean
    RunCommandRepository runCommandRepository(JdbcTemplate jdbc, Flyway flyway) {
        return new JdbcRunCommandRepository(jdbc);
    }

    @Bean
    RunResultRepository runResultRepository(JdbcTemplate jdbc, Flyway flyway) {
        return new JdbcRunResultRepository(jdbc);
    }

    @Bean
    ScheduleRepository scheduleRepository(JdbcTemplate jdbc, Flyway flyway) {
        return new JdbcScheduleRepository(jdbc);
    }

    @Bean
    ScheduleApplicationService.ScheduleCalculator scheduleCalculator() {
        return (expression, timezone, afterExclusive) -> {
            try {
                ZoneId zone = ZoneId.of(timezone);
                ZonedDateTime next = CronExpression.parse(expression)
                        .next(ZonedDateTime.ofInstant(afterExclusive, zone));
                if (next == null) throw new IllegalArgumentException("Cron has no next occurrence");
                return next.toInstant();
            } catch (RuntimeException problem) {
                throw new com.fangxuele.wepush.next.service.application.ApplicationProblem(
                        com.fangxuele.wepush.next.service.application.ApplicationProblem.Kind.BAD_REQUEST,
                        "SCHEDULE_CRON_INVALID", "Schedule cron expression or timezone is invalid");
            }
        };
    }

    @Bean(destroyMethod = "close")
    ScheduleLeadership scheduleLeadership(DataSource dataSource,
            @Value("${wepush.database.kind:sqlite}") String databaseKind) {
        if ("postgresql".equalsIgnoreCase(databaseKind)) {
            return new PostgresScheduleLeadership(dataSource);
        }
        return () -> true;
    }

    @Bean
    ScheduleApplicationService scheduleApplicationService(
            WorkspaceRepository workspaces, JobRepository jobs, ScheduleRepository schedules,
            RunApplicationService runs, ScheduleApplicationService.ScheduleCalculator calculator,
            ResourceIdGenerator ids, TransactionRunner transactions, Clock clock) {
        return new ScheduleApplicationService(workspaces, jobs, schedules, runs, calculator,
                ids, transactions, clock);
    }

    @Bean
    ScheduleScanner scheduleScanner(ScheduleApplicationService schedules,
                                    ScheduleLeadership leadership) {
        return new ScheduleScanner(schedules, leadership);
    }

    @Bean(destroyMethod = "close")
    LocalEnvelopeSecretStore secretStore(
            JdbcTemplate jdbc, Flyway flyway, Clock clock,
            @Value("${wepush.secret.master-key-path:.local/secrets/master-key.json}") String masterKeyPath,
            @Value("${wepush.secret.master-key-base64:}") String injectedMasterKey,
            @Value("${wepush.mode:standalone}") String mode) {
        return new LocalEnvelopeSecretStore(jdbc, Path.of(masterKeyPath), injectedMasterKey,
                "standalone".equalsIgnoreCase(mode), clock);
    }

    @Bean(destroyMethod = "close")
    LocalHmacCursorCodec cursorCodec(
            JdbcTemplate jdbc, SecretStore secretStore,
            @Value("${wepush.secret.master-key-path:.local/secrets/master-key.json}") String masterKeyPath,
            @Value("${wepush.secret.master-key-base64:}") String injectedMasterKey,
            @Value("${wepush.mode:standalone}") String mode) {
        Integer records = jdbc.queryForObject("SELECT COUNT(*) FROM secret_record", Integer.class);
        return new LocalHmacCursorCodec(Path.of(masterKeyPath), injectedMasterKey,
                "standalone".equalsIgnoreCase(mode), records != null && records > 0);
    }

    @Bean(initMethod = "start", destroyMethod = "close")
    PostgresNotificationBus postgresNotificationBus(
            DataSource dataSource, JdbcTemplate jdbc,
            @Value("${wepush.database.kind:sqlite}") String databaseKind) {
        return new PostgresNotificationBus(dataSource, jdbc, databaseKind);
    }

    @Bean
    LocalRunEventHub runEventHub(JsonCodec json, PostgresNotificationBus notifications) {
        return new LocalRunEventHub(json, notifications);
    }

    @Bean
    ControlPlaneWakeupPublisher controlPlaneWakeups(PostgresNotificationBus notifications) {
        return new ControlPlaneWakeupPublisher() {
            @Override
            public void runPending(WorkspaceId workspaceId, String runId) {
                notifications.publish(PostgresNotificationBus.RUN_PENDING,
                        workspaceId.value() + ":" + runId);
            }

            @Override
            public void agentOutbox(String agentId) {
                notifications.publish(PostgresNotificationBus.AGENT_OUTBOX, agentId);
            }
        };
    }

    @Bean
    RunEventPoller runEventPoller(LocalRunEventHub events, RunApplicationService runs,
                                  PostgresNotificationBus notifications) {
        return new RunEventPoller(events, runs, notifications);
    }

    @Bean(destroyMethod = "close")
    StandaloneRunExecutor standaloneRunExecutor(
            WorkspaceRepository workspaces, RunRepository runs, RunResultRepository results,
            AudienceRepository audiences, ProviderRegistry providers,
            AccountAuthCircuitService authenticationCircuits, SecretStore secrets, JsonCodec json, TransactionRunner transactions,
            LocalRunEventHub eventHub, Clock clock) {
        return new StandaloneRunExecutor(workspaces, runs, results, audiences, providers,
                authenticationCircuits, secrets, json,
                transactions, eventHub, clock);
    }

    @Bean
    WorkspaceApplicationService workspaceApplicationService(
            WorkspaceRepository workspaces, WorkspacePolicyRepository policies, ResourceIdGenerator ids,
            TransactionRunner transactions, Clock clock) {
        return new WorkspaceApplicationService(workspaces, policies, ids, transactions, clock);
    }

    @Bean
    ApplicationRunner recoverPendingRuns(
            StandaloneRunExecutor embedded, RemoteRunCoordinator remote,
            @Value("${wepush.execution.mode:embedded}") String mode) {
        return arguments -> {
            if ("embedded".equalsIgnoreCase(mode)) embedded.recoverPending();
            else if ("remote".equalsIgnoreCase(mode)) remote.recoverPending();
            else throw new IllegalArgumentException("wepush.execution.mode must be embedded or remote");
        };
    }

    @Bean
    AccountApplicationService accountApplicationService(
            WorkspaceRepository workspaces, AccountRepository accounts, ProviderRegistry providers,
            JsonCodec json, ResourceIdGenerator ids, TransactionRunner transactions,
            SecretStore secrets, Clock clock) {
        return new AccountApplicationService(workspaces, accounts, providers, json, ids, transactions,
                secrets, clock);
    }

    @Bean
    MessageApplicationService messageApplicationService(
            WorkspaceRepository workspaces, MessageRepository messages, ProviderRegistry providers,
            JsonCodec json, ResourceIdGenerator ids, TransactionRunner transactions, Clock clock) {
        return new MessageApplicationService(workspaces, messages, providers, json, ids, transactions, clock);
    }

    @Bean
    AudienceApplicationService audienceApplicationService(
            WorkspaceRepository workspaces, AudienceRepository audiences, JsonCodec json,
            ResourceIdGenerator ids, TransactionRunner transactions, Clock clock) {
        return new AudienceApplicationService(workspaces, audiences, json, ids, transactions, clock);
    }

    @Bean
    AudienceImportApplicationService audienceImportApplicationService(
            WorkspaceRepository workspaces, AudienceRepository audiences, AudienceImportRepository imports,
            JsonCodec json, ResourceIdGenerator ids, TransactionRunner transactions, Clock clock) {
        return new AudienceImportApplicationService(workspaces, audiences, imports, json, ids,
                transactions, clock);
    }

    @Bean
    ControlPlaneQueryService controlPlaneQueryService(
            WorkspaceRepository workspaces, AccountRepository accounts, MessageRepository messages,
            AudienceRepository audiences, JobRepository jobs, RunRepository runs,
            ScheduleRepository schedules, AuditEventRepository audits, CursorCodec cursors, Clock clock) {
        return new ControlPlaneQueryService(workspaces, accounts, messages, audiences, jobs, runs,
                schedules, audits, cursors, clock);
    }

    @Bean
    JobApplicationService jobApplicationService(
            WorkspaceRepository workspaces, AccountRepository accounts, MessageRepository messages,
            AudienceRepository audiences, JobRepository jobs, JsonCodec json, ResourceIdGenerator ids,
            TransactionRunner transactions, Clock clock) {
        return new JobApplicationService(workspaces, accounts, messages, audiences, jobs,
                json, ids, transactions, clock);
    }

    @Bean
    SecretApplicationService secretApplicationService(
            WorkspaceRepository workspaces, SecretStore secrets, TransactionRunner transactions) {
        return new SecretApplicationService(workspaces, secrets, transactions);
    }

    @Bean
    RunResultApplicationService runResultApplicationService(
            RunRepository runs, RunResultRepository results, CursorCodec cursors) {
        return new RunResultApplicationService(runs, results, cursors);
    }

    @Bean
    RunCommandApplicationService runCommandApplicationService(
            RunRepository runs, RunCommandRepository commands, RunCommandGateway gateway,
            JsonCodec json, TransactionRunner transactions, LocalRunEventHub events, Clock clock) {
        return new RunCommandApplicationService(runs, commands, gateway, json, transactions, events, clock);
    }

    @Bean
    ArtifactApplicationService artifactApplicationService(
            RunRepository runs, RunResultRepository results, ArtifactRepository artifacts,
            WorkspaceResourceGovernor resources, ArtifactStore store, ResourceIdGenerator ids, TransactionRunner transactions,
            JsonCodec json, LocalRunEventHub events, Clock clock,
            @Value("${wepush.artifact.export-retention:PT24H}") Duration exportRetention) {
        return new ArtifactApplicationService(runs, results, artifacts, resources, store, ids,
                transactions, json, events, clock, exportRetention);
    }

    @Bean
    ArtifactUploadTokenCodec artifactUploadTokenCodec(
            @Value("${wepush.agent.artifact-signing-key-base64:}") String configured) {
        byte[] key;
        if (configured == null || configured.isBlank()) {
            key = new byte[32];
            new SecureRandom().nextBytes(key);
        } else {
            key = Base64.getDecoder().decode(configured);
        }
        return new ArtifactUploadTokenCodec(key);
    }

    @Bean
    AgentArtifactApplicationService agentArtifactApplicationService(
            AgentLeaseRepository leases, ArtifactRepository artifacts, RunRepository runs,
            WorkspaceResourceGovernor resources, ArtifactStore store, ArtifactUploadTokenCodec tokens, ResourceIdGenerator ids,
            TransactionRunner transactions, JsonCodec json, LocalRunEventHub events, Clock clock,
            @Value("${wepush.agent.public-base-url:http://127.0.0.1:18990}") String publicBaseUrl,
            @Value("${wepush.agent.artifact-upload-ttl:PT15M}") Duration uploadTtl,
            @Value("${wepush.agent.artifact-retention:P7D}") Duration retention,
            @Value("${wepush.agent.artifact-maximum-bytes:5497558138880}") long maximumBytes,
            @Value("${wepush.agent.artifact-multipart-threshold-bytes:1073741824}") long multipartThresholdBytes) {
        return new AgentArtifactApplicationService(leases, artifacts, runs, resources, store, tokens, ids,
                transactions, json, events, clock, publicBaseUrl, uploadTtl, retention, maximumBytes,
                multipartThresholdBytes);
    }

    @Bean
    AgentApplicationService agentApplicationService(
            AgentRepository agents, ResourceIdGenerator ids, TransactionRunner transactions, Clock clock,
            @Value("${wepush.agent.grpc.heartbeat-interval:PT10S}") Duration heartbeatInterval,
            @Value("${wepush.agent.grpc.maximum-message-bytes:1048576}") long maximumMessageBytes) {
        return new AgentApplicationService(agents, ids, transactions, clock,
                heartbeatInterval, maximumMessageBytes);
    }

    @Bean
    AgentStreamGateway agentStreamGateway(AgentApplicationService agents) {
        return new AgentStreamGateway(agents);
    }

    @Bean
    RemoteRunCoordinator remoteRunCoordinator(
            WorkspaceRepository workspaces, RunRepository runs, RunResultRepository results,
            AccountAuthCircuitService authenticationCircuits, AudienceRepository audiences,
            AgentRepository agents, AgentLeaseRepository leases,
            AgentIdentityService agentIdentities,
            AgentOutboundMessageRepository outbound, ArtifactRepository artifacts,
            AgentControlGateway gateway,
            SecretStore secrets, JsonCodec json, ResourceIdGenerator ids, TransactionRunner transactions,
            LocalRunEventHub events, ControlPlaneWakeupPublisher wakeups, Clock clock,
            @Value("${wepush.agent.public-base-url:http://127.0.0.1:18990}") String publicBaseUrl,
            @Value("${wepush.agent.lease-offer-ttl:PT1M}") Duration offerTtl,
            @Value("${wepush.agent.recovery-grace:PT30S}") Duration recoveryGrace) {
        return new RemoteRunCoordinator(runs, workspaces, results, authenticationCircuits,
                audiences, agents, agentIdentities, leases,
                outbound, artifacts, gateway,
                secrets, json, ids, transactions, events, wakeups, clock,
                publicBaseUrl, offerTtl, recoveryGrace);
    }

    @Bean
    AgentOutboxScheduler agentOutboxScheduler(
            RemoteRunCoordinator remoteRuns, PostgresNotificationBus notifications,
            @Value("${wepush.execution.mode:embedded}") String mode) {
        return new AgentOutboxScheduler(remoteRuns, "remote".equalsIgnoreCase(mode), notifications);
    }

    @Bean
    @Primary
    RunDispatcher runDispatcher(StandaloneRunExecutor embedded, RemoteRunCoordinator remote,
                                @Value("${wepush.execution.mode:embedded}") String mode) {
        if ("embedded".equalsIgnoreCase(mode)) return embedded::dispatch;
        if ("remote".equalsIgnoreCase(mode)) return remote::dispatch;
        throw new IllegalArgumentException("wepush.execution.mode must be embedded or remote");
    }

    @Bean
    @Primary
    RunCommandGateway runCommandGateway(StandaloneRunExecutor embedded, RemoteRunCoordinator remote,
                                        @Value("${wepush.execution.mode:embedded}") String mode) {
        if ("embedded".equalsIgnoreCase(mode)) return embedded::submit;
        if ("remote".equalsIgnoreCase(mode)) return remote::submit;
        throw new IllegalArgumentException("wepush.execution.mode must be embedded or remote");
    }

    @Bean
    AgentControlGrpcService agentControlGrpcService(
            AgentApplicationService agents, AgentStreamGateway streams, RemoteRunCoordinator remoteRuns,
            @Value("${wepush.execution.mode:embedded}") String mode) {
        return new AgentControlGrpcService(agents, streams, remoteRuns, "remote".equalsIgnoreCase(mode));
    }

    @Bean
    AgentGrpcServer agentGrpcServer(
            AgentControlGrpcService service, AgentIdentityService identities,
            @Value("${wepush.agent.grpc.address:127.0.0.1}") String address,
            @Value("${wepush.agent.grpc.port:19090}") int port,
            @Value("${wepush.agent.grpc.token:}") String token,
            @Value("${wepush.agent.grpc.tls.enabled:false}") boolean tlsEnabled,
            @Value("${wepush.agent.grpc.tls.certificate-chain:}") String certificateChain,
            @Value("${wepush.agent.grpc.tls.private-key:}") String privateKey,
            @Value("${wepush.agent.grpc.tls.trust-certificates:}") String trustCertificates,
            @Value("${wepush.agent.grpc.tls.require-client-certificate:false}") boolean requireClientCertificate,
            @Value("${wepush.agent.grpc.maximum-message-bytes:1048576}") long maximumMessageBytes) {
        AgentGrpcServer.TlsConfiguration tls = new AgentGrpcServer.TlsConfiguration(tlsEnabled,
                optionalPath(certificateChain), optionalPath(privateKey), optionalPath(trustCertificates),
                requireClientCertificate);
        return new AgentGrpcServer(address, port, token, maximumMessageBytes, service, identities, tls);
    }

    private static Path optionalPath(String value) {
        return value == null || value.isBlank() ? null : Path.of(value).toAbsolutePath().normalize();
    }

    @Bean
    RunApplicationService runApplicationService(
            WorkspaceRepository workspaces, AccountRepository accounts, MessageRepository messages,
            AudienceRepository audiences, JobRepository jobs, RunRepository runs, RunResultRepository results,
            WorkspaceResourceGovernor resources, AccountAuthCircuitService authenticationCircuits,
            ProviderRegistry providers,
            JsonCodec json, ResourceIdGenerator ids, TransactionRunner transactions,
            LocalRunEventHub eventHub, RunDispatcher dispatcher, ControlPlaneWakeupPublisher wakeups,
            CursorCodec cursors, Clock clock) {
        return new RunApplicationService(workspaces, accounts, messages, audiences, jobs, runs, results, resources,
                authenticationCircuits,
                providers, json, ids, transactions, eventHub, dispatcher, wakeups, cursors, clock);
    }

    @Bean
    ProviderRegistry providerRegistry() {
        return new ServiceLoaderProviderRegistry(Thread.currentThread().getContextClassLoader());
    }

    @Bean
    ProviderCatalogQuery providerCatalogQuery(ProviderRegistry registry) {
        return new ProviderCatalogQuery(registry);
    }

}

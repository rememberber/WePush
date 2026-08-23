package com.fangxuele.wepush.next.service.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fangxuele.wepush.next.service.application.ProviderCatalogQuery;
import com.fangxuele.wepush.next.service.application.ProviderRegistry;
import com.fangxuele.wepush.next.service.application.ArtifactApplicationService;
import com.fangxuele.wepush.next.service.application.AgentApplicationService;
import com.fangxuele.wepush.next.service.application.AgentControlGateway;
import com.fangxuele.wepush.next.service.application.ArtifactStore;
import com.fangxuele.wepush.next.service.application.AccountApplicationService;
import com.fangxuele.wepush.next.service.application.AudienceApplicationService;
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
import com.fangxuele.wepush.next.service.application.TransactionRunner;
import com.fangxuele.wepush.next.service.domain.AccountRepository;
import com.fangxuele.wepush.next.service.domain.ArtifactRepository;
import com.fangxuele.wepush.next.service.domain.AgentRepository;
import com.fangxuele.wepush.next.service.domain.AgentLeaseRepository;
import com.fangxuele.wepush.next.service.domain.AudienceRepository;
import com.fangxuele.wepush.next.service.domain.JobRepository;
import com.fangxuele.wepush.next.service.domain.MessageRepository;
import com.fangxuele.wepush.next.service.domain.RunRepository;
import com.fangxuele.wepush.next.service.domain.RunCommandRepository;
import com.fangxuele.wepush.next.service.domain.RunResultRepository;
import com.fangxuele.wepush.next.service.domain.WorkspaceRepository;
import com.fangxuele.wepush.next.service.infrastructure.JacksonJsonCodec;
import com.fangxuele.wepush.next.service.infrastructure.JdbcAccountRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcArtifactRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcAgentRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcAgentLeaseRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcAudienceRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcJobRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcMessageRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcRunRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcRunCommandRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcRunResultRepository;
import com.fangxuele.wepush.next.service.infrastructure.JdbcWorkspaceRepository;
import com.fangxuele.wepush.next.service.infrastructure.LocalEnvelopeSecretStore;
import com.fangxuele.wepush.next.service.infrastructure.LocalFileArtifactStore;
import com.fangxuele.wepush.next.service.infrastructure.LocalHmacCursorCodec;
import com.fangxuele.wepush.next.service.infrastructure.ServiceLoaderProviderRegistry;
import com.fangxuele.wepush.next.service.infrastructure.SQLiteDatabase;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
class ServiceComposition {
    @Bean(destroyMethod = "close")
    HikariDataSource dataSource(@Value("${wepush.database.path:.local/data/wepush-next.db}") String path) {
        return SQLiteDatabase.create(Path.of(path));
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
    AccountRepository accountRepository(JdbcTemplate jdbc, Flyway flyway) {
        return new JdbcAccountRepository(jdbc);
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
    ArtifactRepository artifactRepository(JdbcTemplate jdbc, Flyway flyway) {
        return new JdbcArtifactRepository(jdbc);
    }

    @Bean
    LocalFileArtifactStore artifactStore(
            @Value("${wepush.artifact.root:.local/artifacts}") String root,
            @Value("${wepush.artifact.environment:standalone}") String environment) {
        return new LocalFileArtifactStore(Path.of(root), environment);
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

    @Bean
    LocalRunEventHub runEventHub(JsonCodec json) {
        return new LocalRunEventHub(json);
    }

    @Bean(destroyMethod = "close")
    StandaloneRunExecutor standaloneRunExecutor(
            RunRepository runs, RunResultRepository results, AudienceRepository audiences, ProviderRegistry providers,
            SecretStore secrets, JsonCodec json, TransactionRunner transactions,
            LocalRunEventHub eventHub, Clock clock) {
        return new StandaloneRunExecutor(runs, results, audiences, providers, secrets, json,
                transactions, eventHub, clock);
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
            JsonCodec json, ResourceIdGenerator ids, TransactionRunner transactions, Clock clock) {
        return new AccountApplicationService(workspaces, accounts, providers, json, ids, transactions, clock);
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
            ArtifactStore store, ResourceIdGenerator ids, TransactionRunner transactions,
            JsonCodec json, LocalRunEventHub events, Clock clock,
            @Value("${wepush.artifact.export-retention:PT24H}") Duration exportRetention) {
        return new ArtifactApplicationService(runs, results, artifacts, store, ids,
                transactions, json, events, clock, exportRetention);
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
            RunRepository runs, RunResultRepository results, AudienceRepository audiences,
            AgentRepository agents, AgentLeaseRepository leases, AgentControlGateway gateway,
            SecretStore secrets, JsonCodec json, ResourceIdGenerator ids, TransactionRunner transactions,
            LocalRunEventHub events, Clock clock,
            @Value("${wepush.agent.public-base-url:http://127.0.0.1:18990}") String publicBaseUrl,
            @Value("${wepush.agent.lease-offer-ttl:PT1M}") Duration offerTtl,
            @Value("${wepush.agent.recovery-grace:PT30S}") Duration recoveryGrace) {
        return new RemoteRunCoordinator(runs, results, audiences, agents, leases, gateway,
                secrets, json, ids, transactions, events, clock, publicBaseUrl, offerTtl, recoveryGrace);
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
            AgentControlGrpcService service,
            @Value("${wepush.agent.grpc.address:127.0.0.1}") String address,
            @Value("${wepush.agent.grpc.port:19090}") int port,
            @Value("${wepush.agent.grpc.token:}") String token,
            @Value("${wepush.agent.grpc.maximum-message-bytes:1048576}") long maximumMessageBytes) {
        return new AgentGrpcServer(address, port, token, maximumMessageBytes, service);
    }

    @Bean
    RunApplicationService runApplicationService(
            WorkspaceRepository workspaces, AccountRepository accounts, MessageRepository messages,
            AudienceRepository audiences, JobRepository jobs, RunRepository runs, ProviderRegistry providers,
            JsonCodec json, ResourceIdGenerator ids, TransactionRunner transactions,
            LocalRunEventHub eventHub, RunDispatcher dispatcher, Clock clock) {
        return new RunApplicationService(workspaces, accounts, messages, audiences, jobs, runs, providers,
                json, ids, transactions, eventHub, dispatcher, clock);
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

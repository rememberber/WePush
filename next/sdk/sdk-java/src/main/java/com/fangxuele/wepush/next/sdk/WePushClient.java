package com.fangxuele.wepush.next.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;

public final class WePushClient implements AutoCloseable {
    private final HttpTransport transport;
    private final SystemClient system;
    private final ProvidersClient providers;
    private final AgentsClient agents;
    private final SecurityClient security;
    private final WorkspacesClient workspaces;

    private WePushClient(Builder builder) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(builder.connectTimeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_2)
                .build();
        this.transport = new HttpTransport(
                builder.endpoint,
                httpClient,
                new ObjectMapper().findAndRegisterModules(),
                builder.tokenProvider,
                builder.requestTimeout,
                builder.retryPolicy);
        this.system = new SystemClient(transport);
        this.providers = new ProvidersClient(transport);
        this.agents = new AgentsClient(transport);
        this.security = new SecurityClient(transport);
        this.workspaces = new WorkspacesClient(transport);
    }

    public static Builder builder() {
        return new Builder();
    }

    public SystemClient system() {
        return system;
    }

    public ProvidersClient providers() {
        return providers;
    }

    public AgentsClient agents() {
        return agents;
    }

    public SecurityClient security() {
        return security;
    }

    public WorkspacesClient workspaces() {
        return workspaces;
    }

    public WorkspaceClient workspace(String workspaceId) {
        return new WorkspaceClient(transport, workspaceId);
    }

    public URI endpoint() {
        return transport.endpoint();
    }

    @Override
    public void close() {
        transport.close();
    }

    public static final class Builder {
        private URI endpoint;
        private TokenProvider tokenProvider = TokenProvider.none();
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration requestTimeout = Duration.ofSeconds(30);
        private RetryPolicy retryPolicy = RetryPolicy.defaults();

        private Builder() {
        }

        public Builder endpoint(URI endpoint) {
            this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
            return this;
        }

        public Builder token(TokenProvider tokenProvider) {
            this.tokenProvider = Objects.requireNonNull(tokenProvider, "tokenProvider");
            return this;
        }

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = positive(connectTimeout, "connectTimeout");
            return this;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = positive(requestTimeout, "requestTimeout");
            return this;
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy");
            return this;
        }

        public WePushClient build() {
            if (endpoint == null || !SetOfSchemes.HTTP.contains(endpoint.getScheme())) {
                throw new IllegalStateException("endpoint must use http or https");
            }
            if (endpoint.getUserInfo() != null || endpoint.getQuery() != null || endpoint.getFragment() != null) {
                throw new IllegalStateException("endpoint must not contain user info, query, or fragment");
            }
            return new WePushClient(this);
        }

        private static Duration positive(Duration duration, String name) {
            Objects.requireNonNull(duration, name);
            if (duration.isZero() || duration.isNegative()) {
                throw new IllegalArgumentException(name + " must be positive");
            }
            return duration;
        }
    }

    private static final class SetOfSchemes {
        private static final java.util.Set<String> HTTP = java.util.Set.of("http", "https");
    }
}

package com.fangxuele.wepush.next.service.app;

import com.fangxuele.wepush.next.service.application.ApiAccessService;
import com.fangxuele.wepush.next.service.application.JsonCodec;
import com.fangxuele.wepush.next.service.application.ResourceIdGenerator;
import com.fangxuele.wepush.next.service.application.TransactionRunner;
import com.fangxuele.wepush.next.service.domain.ApiAccessRepository;
import com.fangxuele.wepush.next.service.domain.AuditEventRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
final class ApiSecurityFilter extends OncePerRequestFilter {
    static final String IDENTITY_ATTRIBUTE = ApiSecurityFilter.class.getName() + ".identity";
    private static final Pattern WORKSPACE = Pattern.compile("^/api/v1/workspaces/([^/]+)(?:/.*)?$");

    private final boolean enabled;
    private final ApiAccessService access;
    private final AuditEventRepository audits;
    private final ResourceIdGenerator ids;
    private final TransactionRunner transactions;
    private final JsonCodec json;
    private final Clock clock;

    ApiSecurityFilter(@Value("${wepush.security.enabled:false}") boolean enabled,
                      ApiAccessService access, AuditEventRepository audits,
                      ResourceIdGenerator ids, TransactionRunner transactions,
                      JsonCodec json, Clock clock) {
        this.enabled = enabled;
        this.access = access;
        this.audits = audits;
        this.ids = ids;
        this.transactions = transactions;
        this.json = json;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/") || path.startsWith("/api/v1/openapi")
                || path.equals("/api/v1/system/info");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String workspaceId = workspace(request.getRequestURI());
        ApiAccessRepository.AccessIdentity identity = null;
        if (enabled) {
            String authorization = request.getHeader("Authorization");
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                denied(response, HttpServletResponse.SC_UNAUTHORIZED, "API_TOKEN_REQUIRED");
                audit(request, workspaceId, "anonymous", "DENIED", 401);
                return;
            }
            identity = access.authenticate(authorization.substring("Bearer ".length()).trim());
            if (identity == null) {
                denied(response, HttpServletResponse.SC_UNAUTHORIZED, "API_TOKEN_INVALID");
                audit(request, workspaceId, "anonymous", "DENIED", 401);
                return;
            }
            ApiAccessRepository.Role required = requiredRole(request);
            if (!authorized(identity, request.getRequestURI(), workspaceId, required)) {
                denied(response, HttpServletResponse.SC_FORBIDDEN, "WORKSPACE_ACCESS_DENIED");
                audit(request, workspaceId, identity.principalId(), "DENIED", 403);
                return;
            }
            request.setAttribute(IDENTITY_ATTRIBUTE, identity);
        }
        int before = response.getStatus();
        chain.doFilter(request, response);
        if (!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())) {
            String actor = identity == null ? "standalone-local" : identity.principalId();
            audit(request, workspaceId, actor, response.getStatus() < 400 ? "SUCCESS" : "FAILURE",
                    response.getStatus() == 0 ? before : response.getStatus());
        }
    }

    private static boolean authorized(ApiAccessRepository.AccessIdentity identity, String path,
                                      String workspaceId,
                                      ApiAccessRepository.Role required) {
        if (identity.systemAdministrator()) return true;
        if (workspaceId != null) {
            ApiAccessRepository.Role role = identity.roleFor(workspaceId);
            return role != null && role.grants(required);
        }
        if (path.startsWith("/api/v1/system/") || path.startsWith("/api/v1/agents")
                || path.equals("/api/v1/workspaces")) return false;
        return identity.roles().stream().anyMatch(binding -> binding.role().grants(required));
    }

    private static ApiAccessRepository.Role requiredRole(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/system/") || path.startsWith("/api/v1/agents")
                || path.equals("/api/v1/workspaces")
                || path.endsWith("/policy")
                || path.endsWith("/authentication-circuit")
                || path.contains("/secrets") || path.contains("/audit")
                || path.contains("/api-tokens")
                || path.contains("/agent-enrollment-tokens")) {
            return ApiAccessRepository.Role.ADMIN;
        }
        return ("GET".equals(request.getMethod()) || "HEAD".equals(request.getMethod()))
                ? ApiAccessRepository.Role.VIEWER : ApiAccessRepository.Role.OPERATOR;
    }

    private void audit(HttpServletRequest request, String workspaceId, String actorId,
                       String result, int status) {
        try {
            String[] segments = request.getRequestURI().split("/");
            String resourceType = segments.length > 5 ? segments[5] : "system";
            String resourceId = segments.length > 6 ? segments[6] : "";
            AuditEventRepository.AuditEvent event = new AuditEventRepository.AuditEvent(
                    ids.next("audit"), workspaceId, "API_PRINCIPAL", actorId,
                    request.getMethod() + " " + request.getRequestURI(), resourceType, resourceId,
                    result, json.canonicalize(Map.of("httpStatus", status)), clock.instant());
            transactions.required(() -> audits.append(event));
        } catch (RuntimeException ignored) {
            // Audit failure must not replace the original HTTP outcome; health/metrics surface DB failures.
        }
    }

    private static String workspace(String path) {
        Matcher matcher = WORKSPACE.matcher(path);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private static void denied(HttpServletResponse response, int status, String code) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"code\":\"" + code
                + "\",\"detail\":\"Authentication or Workspace permission is insufficient\"}");
    }
}

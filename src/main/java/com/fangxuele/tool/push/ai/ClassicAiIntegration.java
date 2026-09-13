package com.fangxuele.tool.push.ai;

import com.fangxuele.tool.push.util.ConfigUtil;

import java.io.IOException;
import java.nio.file.Path;

public final class ClassicAiIntegration {
    private static ClassicAiServer server;
    private static ClassicAiService service;
    private static boolean shutdownHookAdded;

    private ClassicAiIntegration() { }

    public static synchronized boolean isRunning() { return server != null; }

    public static synchronized void enable() throws IOException {
        if (server != null) return;
        Path directory = Path.of(System.getProperty("user.home"), ".WePush5", "ai");
        if (service == null) service = new ClassicAiService(new ClassicTaskGateway(), directory);
        server = new ClassicAiServer(directory, service);
        if (!shutdownHookAdded) {
            Runtime.getRuntime().addShutdownHook(new Thread(ClassicAiIntegration::stop, "classic-ai-shutdown"));
            shutdownHookAdded = true;
        }
        ConfigUtil.getInstance().setAiIntegrationEnabled(true);
        ConfigUtil.getInstance().save();
    }

    public static synchronized void disable() {
        stop();
        ConfigUtil.getInstance().setAiIntegrationEnabled(false);
        ConfigUtil.getInstance().save();
    }

    public static synchronized void stop() {
        if (server == null) return;
        try { server.close(); }
        catch (IOException ignored) { /* Connection token is no longer served even if file cleanup fails. */ }
        finally { server = null; }
    }
}

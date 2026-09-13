package com.fangxuele.tool.push.ai;

import com.fangxuele.tool.push.dao.*;
import com.fangxuele.tool.push.domain.*;
import com.fangxuele.tool.push.logic.MessageTypeEnum;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/** Actual MyBatis + sender + task runner, in a child JVM so the user's Classic DB is never opened. */
public class ClassicTaskGatewayTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void isolatedClassicDryRunAndApprovedSendUseFrozenResources() throws Exception {
        Path home = temporary.newFolder().toPath();
        Path log = home.resolve("smoke.log");
        String java = Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        Process child = new ProcessBuilder(java, "-Djava.awt.headless=true", "-Duser.home=" + home,
                "-cp", System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                ClassicTaskGatewayTest.class.getName()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        boolean finished = child.waitFor(45, TimeUnit.SECONDS);
        if (!finished) child.destroyForcibly();
        assertTrue("Classic smoke timed out: " + Files.readString(log), finished);
        assertEquals(Files.readString(log), 0, child.exitValue());
        assertTrue(Files.readString(log).contains("CLASSIC_AI_SMOKE_OK"));
    }

    public static void main(String[] ignored) throws Exception {
        HttpServer receiver = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        List<String> deliveries = Collections.synchronizedList(new ArrayList<>());
        receiver.createContext("/send", exchange -> {
            deliveries.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, 2);
            exchange.getResponseBody().write("OK".getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        receiver.start();
        try {
            com.fangxuele.tool.push.util.UpgradeUtil.smoothUpgrade();
            fixture("http://127.0.0.1:" + receiver.getAddress().getPort() + "/send");
            List<Runnable> queue = new ArrayList<>();
            Path directory = Path.of(System.getProperty("user.home"), ".WePush5", "ai");
            ClassicAiService service = new ClassicAiService(new ClassicTaskGateway(), directory, Clock.systemUTC(), queue::add);
            String preview = service.call("wepush_classic_get_task", AiJson.object("taskId", 1)).toString();
            assertFalse(preview.contains("fixture-credential"));
            assertFalse(preview.contains("fixture-header"));
            assertTrue(preview.contains("original-body"));
            assertTrue(deliveries.isEmpty());

            JsonObject dry = AiJson.object("taskId", 1, "idempotencyKey", UUID.randomUUID().toString());
            service.call("wepush_classic_dry_run", dry);
            queue.removeFirst().run();
            JsonObject dryResult = service.call("wepush_classic_get_run", AiJson.object("runId", dry.get("idempotencyKey"))).getAsJsonObject();
            assertEquals("FINISHED", dryResult.get("state").getAsString());
            assertEquals(2, dryResult.getAsJsonObject("result").get("successCount").getAsInt());
            assertTrue("dry run must not deliver", deliveries.isEmpty());

            String token = ClassicAiServiceTest.preview(service);
            TPeopleData changed = mapper(TPeopleDataMapper.class).selectByPrimaryKey(1);
            changed.setVarData("[\"changed-person\"]");
            mapper(TPeopleDataMapper.class).updateByPrimaryKey(changed);
            assertThrows(IllegalArgumentException.class, () -> service.call("wepush_classic_send_run", ClassicAiServiceTest.sendArgs(token)));

            JsonObject send = ClassicAiServiceTest.sendArgs(ClassicAiServiceTest.preview(service));
            service.call("wepush_classic_send_run", send);
            TMsg message = mapper(TMsgMapper.class).selectByPrimaryKey(1);
            message.setContent(message.getContent().replace("original-body", "unapproved-body"));
            mapper(TMsgMapper.class).updateByPrimaryKey(message);
            // The send has been accepted. UI edits after acceptance must not change its payload.
            queue.removeFirst().run();
            assertEquals(List.of("original-body", "original-body"), deliveries);
            service.call("wepush_classic_send_run", send);
            assertTrue(queue.isEmpty());
            assertEquals(2, deliveries.size());
            JsonObject result = service.call("wepush_classic_get_run", AiJson.object("runId", send.get("idempotencyKey"))).getAsJsonObject();
            assertEquals("FINISHED", result.get("state").getAsString());
            assertEquals(2, result.getAsJsonObject("result").get("successCount").getAsInt());
            assertEquals(2, service.call("wepush_classic_list_history", AiJson.object("taskId", 1)).getAsJsonObject().getAsJsonArray("items").size());
            System.out.println("CLASSIC_AI_SMOKE_OK: dry-run=0 deliveries, send=2 loopback deliveries, duplicate=0 extra deliveries");
        } finally {
            receiver.stop(0);
            com.fangxuele.tool.push.util.HttpClientRegistry.shutdown();
            if (MybatisUtil.getSqlSession() != null) MybatisUtil.getSqlSession().close();
        }
    }

    private static <T> T mapper(Class<T> type) { return MybatisUtil.getSqlSession().getMapper(type); }

    private static void fixture(String endpoint) {
        TAccount account = new TAccount();
        account.setId(1);
        account.setMsgType(MessageTypeEnum.HTTP_CODE);
        account.setAccountName("Loopback test");
        account.setAccountConfig("{\"useProxy\":false,\"password\":\"fixture-credential\"}");
        mapper(TAccountMapper.class).insert(account);
        TMsg message = new TMsg();
        message.setId(1);
        message.setMsgType(MessageTypeEnum.HTTP_CODE);
        message.setAccountId(1);
        message.setMsgName("Test message");
        message.setContent(AiJson.object("url", endpoint, "method", "POST", "bodyType", "text/plain", "body", "original-body",
                "params", "[]", "headers", "[{\"name\":\"Authorization\",\"value\":\"fixture-header\"}]", "cookies", "[]").toString());
        mapper(TMsgMapper.class).insert(message);
        TPeople people = new TPeople();
        people.setId(1);
        people.setMsgType(MessageTypeEnum.HTTP_CODE);
        people.setAccountId(1);
        people.setPeopleName("Test audience");
        mapper(TPeopleMapper.class).insert(people);
        for (int i = 1; i <= 2; i++) {
            TPeopleData row = new TPeopleData();
            row.setId(i);
            row.setPeopleId(1);
            row.setVarData("[\"person-" + i + "\"]");
            mapper(TPeopleDataMapper.class).insert(row);
        }
        TTask task = new TTask();
        task.setId(1);
        task.setTitle("AI loopback smoke");
        task.setMsgType(MessageTypeEnum.HTTP_CODE);
        task.setAccountId(1);
        task.setMessageId(1);
        task.setPeopleId(1);
        task.setTaskMode(1);
        task.setTaskPeriod(1);
        task.setThreadCnt(2);
        task.setResultAlert(0);
        task.setSaveResult(0);
        task.setIntervalPush(0);
        task.setReimportPeople(0);
        mapper(TTaskMapper.class).insert(task);
    }
}

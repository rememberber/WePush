package com.fangxuele.tool.push.util;

import cn.hutool.extra.mail.MailAccount;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.client.methods.AsyncCharConsumer;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.CharBuffer;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * <pre>
 * 通用测试
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/17.
 */
public class CommonTest {
    @Test
    public void test() {
        MailAccount account = new MailAccount();
        account.setHost("smtp.163.com");
        account.setPort(25);
        account.setAuth(true);
        account.setFrom("rememberber@163.com");
        account.setUser("rememberber");
        account.setPass("******");


//        for (int i = 0; i < 20; i++) {
//            ThreadUtil.execute(new Runnable() {
//                @Override
//                public void run() {
//                    MailUtil.send(account, "rememberber@163.com", "测试", "邮件来自Hutool测试", false);
//                }
//            });
//        }
//
//        ThreadUtil.safeSleep(10000);
    }

    /**
     * HttpAsyncClient Official Quick Start
     */
    @Test
    public void testAsyncHttpClient() {
        CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault();
        try {
            // Start the client
            httpclient.start();

            // Execute request
            final HttpGet request1 = new HttpGet("http://www.apache.org/");
            Future<HttpResponse> future = httpclient.execute(request1, null);
            // and wait until a response is received
            HttpResponse response1 = future.get();
            System.out.println(request1.getRequestLine() + "->" + response1.getStatusLine());

            // One most likely would want to use a callback for operation result
            final CountDownLatch latch1 = new CountDownLatch(1);
            final HttpGet request2 = new HttpGet("http://www.apache.org/");
            httpclient.execute(request2, new FutureCallback<HttpResponse>() {

                public void completed(final HttpResponse response2) {
                    latch1.countDown();
                    System.out.println(request2.getRequestLine() + "->" + response2.getStatusLine());
                }

                public void failed(final Exception ex) {
                    latch1.countDown();
                    System.out.println(request2.getRequestLine() + "->" + ex);
                }

                public void cancelled() {
                    latch1.countDown();
                    System.out.println(request2.getRequestLine() + " cancelled");
                }

            });
            latch1.await();

            // In real world one most likely would also want to stream
            // request and response body content
            final CountDownLatch latch2 = new CountDownLatch(1);
            final HttpGet request3 = new HttpGet("http://www.apache.org/");
            HttpAsyncRequestProducer producer3 = HttpAsyncMethods.create(request3);
            AsyncCharConsumer<HttpResponse> consumer3 = new AsyncCharConsumer<HttpResponse>() {

                HttpResponse response;

                @Override
                protected void onResponseReceived(final HttpResponse response) {
                    this.response = response;
                }

                @Override
                protected void onCharReceived(final CharBuffer buf, final IOControl ioctrl) throws IOException {
                    // Do something useful
                }

                @Override
                protected void releaseResources() {
                }

                @Override
                protected HttpResponse buildResult(final HttpContext context) {
                    return this.response;
                }

            };
            httpclient.execute(producer3, consumer3, new FutureCallback<HttpResponse>() {

                public void completed(final HttpResponse response3) {
                    latch2.countDown();
                    System.out.println(request3.getRequestLine() + "->" + response3.getStatusLine());
                }

                public void failed(final Exception ex) {
                    latch2.countDown();
                    System.out.println(request3.getRequestLine() + "->" + ex);
                }

                public void cancelled() {
                    latch2.countDown();
                    System.out.println(request3.getRequestLine() + " cancelled");
                }

            });
            latch2.await();

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void myAsyncHttpClientTest() {
        CloseableHttpAsyncClient httpclient = HttpAsyncClients.createDefault();
        try {
            // Start the client
            httpclient.start();

            // One most likely would want to use a callback for operation result
            final CountDownLatch latch1 = new CountDownLatch(1);
            final HttpPost request2 = new HttpPost("http://localhost:9000/qian/api/test/lucky?msg=asdf");
            System.err.println(SqliteUtil.nowDateForSqlite() + "开始 " + System.currentTimeMillis());
            httpclient.execute(request2, new FutureCallback<HttpResponse>() {

                public void completed(final HttpResponse response2) {
                    System.err.println("完成1 " + SqliteUtil.nowDateForSqlite());
                    latch1.countDown();
                    System.err.println(latch1.getCount());
                    System.err.println("完成2 " + SqliteUtil.nowDateForSqlite());
                    try {
                        System.err.println(handleResponse(response2));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println(request2.getRequestLine() + "->" + response2.getStatusLine());
                }

                public void failed(final Exception ex) {
                    System.err.println("failed1 " + SqliteUtil.nowDateForSqlite());
                    latch1.countDown();
                    System.err.println(latch1.getCount());
                    System.err.println("failed2 " + SqliteUtil.nowDateForSqlite());
                    System.out.println(request2.getRequestLine() + "->" + ex);
                }

                public void cancelled() {
                    latch1.countDown();
                    System.out.println(request2.getRequestLine() + " cancelled");
                }

            });
            System.err.println(SqliteUtil.nowDateForSqlite() + "结束1 " + System.currentTimeMillis());
            latch1.await();
            System.err.println(SqliteUtil.nowDateForSqlite() + "结束2 " + System.currentTimeMillis());
            System.err.println(latch1.getCount());

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String handleResponse(final HttpResponse response) throws IOException {
        final StatusLine statusLine = response.getStatusLine();
        final HttpEntity entity = response.getEntity();
        if (statusLine.getStatusCode() >= 300) {
            EntityUtils.consume(entity);
            throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
        }
        return entity == null ? null : EntityUtils.toString(entity, Consts.UTF_8);
    }

    @Test
    public void testHikari() {
        HikariDataSource ds = null;
        if (ds == null || ds.isClosed()) {
            ds = new HikariDataSource();
            ds.setJdbcUrl("jdbc:mysql://localhost:3306/ocs-test");
            ds.setUsername("root");
            ds.setPassword("123456");
        }
        String sql = "select * from test";
        try {
            PreparedStatement preparedStatement = ds.getConnection().prepareStatement(sql);
            ResultSet sr = preparedStatement.executeQuery();
            System.err.println(sr);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        ds.close();
    }
}

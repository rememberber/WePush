package com.fangxuele.tool.push.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import com.fangxuele.tool.push.ui.form.MainWindow;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * <pre>
 * Mybatis工具
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/5/9.
 */
@Slf4j
public class MybatisUtil {
    private static SqlSession sqlSession = null;

    /**
     * 共享 SqlSession 的全局锁（SqlSession 非线程安全）
     */
    private static final Object SESSION_LOCK = new Object();

    /**
     * 是否需要初始化
     */
    private static boolean needInit = false;

    private static File dbFile = new File(SystemUtil.CONFIG_HOME + "WePush.db");

    static {
        try {
            if (!dbFile.exists()) {
                initDbFile();
            }
            String resource = "mybatis-config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            Properties properties = new Properties();
            properties.setProperty("url", "jdbc:sqlite:" + dbFile.getAbsolutePath());
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream, properties);
            // 全局仅一个 SqlSession：用同步代理串行化所有访问，避免多线程
            // ExecutionPlaceholder ClassCastException
            sqlSession = threadSafeSession(sqlSessionFactory.openSession(true));
            inputStream.close();

            initTables();
        } catch (Exception e) {
            log.error("get sqlSession error!", e);
        }
    }

    private MybatisUtil() {

    }

    public static SqlSession getSqlSession() {
        return sqlSession;
    }

    /**
     * 包装 SqlSession。注意：getMapper() 返回的 Mapper 绑定的是原始 Session，
     * 必须再包装 Mapper，否则多线程仍会绕过锁直接打到同一 Executor。
     */
    private static SqlSession threadSafeSession(SqlSession delegate) {
        return (SqlSession) Proxy.newProxyInstance(
                SqlSession.class.getClassLoader(),
                new Class<?>[]{SqlSession.class},
                (proxy, method, args) -> {
                    if ("getMapper".equals(method.getName()) && args != null && args.length == 1 && args[0] instanceof Class) {
                        Object mapper = invokeQuietly(delegate, method, args);
                        return threadSafeMapper(mapper, (Class<?>) args[0]);
                    }
                    return invokeSynced(delegate, method, args);
                });
    }

    private static Object threadSafeMapper(Object mapper, Class<?> mapperType) {
        return Proxy.newProxyInstance(
                mapperType.getClassLoader(),
                new Class<?>[]{mapperType},
                (proxy, method, args) -> invokeSynced(mapper, method, args));
    }

    private static Object invokeSynced(Object target, java.lang.reflect.Method method, Object[] args) throws Throwable {
        synchronized (SESSION_LOCK) {
            return invokeQuietly(target, method, args);
        }
    }

    private static Object invokeQuietly(Object target, java.lang.reflect.Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw cause != null ? cause : e;
        }
    }

    /**
     * 初始化数据库文件
     */
    public static void initDbFile() throws SQLException {
        File configHomeDir = new File(SystemUtil.CONFIG_HOME);
        if (!configHomeDir.exists()) {
            configHomeDir.mkdirs();
        }
        // 不存在db文件时会自动创建一个
        String sql = FileUtil.readString(MainWindow.class.getResource("/db_init.sql"), CharsetUtil.UTF_8);
        executeSql(sql);
        needInit = true;
    }

    /**
     * 初始化数据库表
     */
    private static void initTables() {
        if (needInit) {
            // doesn't work
//            InitMapper initMapper = sqlSession.getMapper(InitMapper.class);
//            initMapper.createAllTables();
        }
    }

    /**
     * 执行sql
     *
     * @param sql
     */
    public static void executeSql(String sql) throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(sql);
        stmt.close();
        connection.close();
    }
}

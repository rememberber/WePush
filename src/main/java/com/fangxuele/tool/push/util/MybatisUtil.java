package com.fangxuele.tool.push.util;

import cn.hutool.core.io.FileUtil;
import com.fangxuele.tool.push.dao.InitMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.File;
import java.io.InputStream;
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
     * 是否需要初始化
     */
    private static boolean needInit = false;

    private static File dbFile = new File(SystemUtil.configHome + "WePush.db");

    static {
        try {
            initDbFile();

            String resource = "mybatis-config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            Properties properties = new Properties();
            properties.setProperty("url", "jdbc:sqlite:" + dbFile.getAbsolutePath());
            SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream, properties);
            sqlSession = sqlSessionFactory.openSession(true);
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
     * 初始化数据库文件
     */
    private static void initDbFile() {
        if (!dbFile.exists()) {
            FileUtil.touch(dbFile);
            needInit = true;
        }
    }

    /**
     * 初始化数据库表
     */
    private static void initTables() {
        if (needInit) {
            InitMapper initMapper = sqlSession.getMapper(InitMapper.class);
            initMapper.createAllTables();
        }
    }
}

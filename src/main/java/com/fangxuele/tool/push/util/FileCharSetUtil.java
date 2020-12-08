package com.fangxuele.tool.push.util;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import info.monitorenter.cpdetector.io.ASCIIDetector;
import info.monitorenter.cpdetector.io.ByteOrderMarkDetector;
import info.monitorenter.cpdetector.io.CodepageDetectorProxy;
import info.monitorenter.cpdetector.io.JChardetFacade;
import info.monitorenter.cpdetector.io.ParsingDetector;
import info.monitorenter.cpdetector.io.UnicodeDetector;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * <pre>
 * 字符集工具类
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2018/12/25.
 */
public class FileCharSetUtil {
    private static final Log logger = LogFactory.get();

    /**
     * 获取文件字符集名称
     *
     * @param file 文件
     * @return 字符集名称
     */
    public static String getCharSetName(File file) {
        logger.warn(getCharSet(file).name());
        return getCharSet(file).name();
    }

    /**
     * 获取文件字符集
     *
     * @param file 文件
     * @return 字符集
     */
    public static Charset getCharSet(File file) {
        CodepageDetectorProxy detector = CodepageDetectorProxy.getInstance();
        detector.add(new ByteOrderMarkDetector());
        detector.add(new ParsingDetector(true));
        detector.add(JChardetFacade.getInstance());
        detector.add(ASCIIDetector.getInstance());
        detector.add(UnicodeDetector.getInstance());

        Charset charset = null;
        try {
            charset = detector.detectCodepage(file.toURI().toURL());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (charset != null) {
            return charset;
        } else {
            return StandardCharsets.UTF_8;
        }
    }
}

package com.fangxuele.tool.push.util;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.PathNotFoundException;
import org.apache.commons.lang3.StringUtils;

/**
 * 按当前操作系统与 CPU 架构选择 download_links.json 中的安装包地址。
 */
public class DownloadLinkSelector {

    public static String select(DocumentContext links) {
        return select(System.getProperty("os.name"), System.getProperty("os.arch"), links);
    }

    static String select(String osName, String osArch, DocumentContext links) {
        if (contains(osName, "Windows")) {
            return links.read("$.windows");
        }

        if (contains(osName, "Mac")) {
            if ("aarch64".equals(osArch)) {
                String appleSiliconLink = readOptional(links, "$.macSilicon");
                if (StringUtils.isNotEmpty(appleSiliconLink)) {
                    return appleSiliconLink;
                }
            }
            return links.read("$.mac");
        }

        if (contains(osName, "Linux")) {
            return links.read("$.linux");
        }

        return "";
    }

    private static boolean contains(String value, String searchText) {
        return value != null && value.contains(searchText);
    }

    private static String readOptional(DocumentContext links, String path) {
        try {
            return links.read(path);
        } catch (PathNotFoundException e) {
            return "";
        }
    }
}

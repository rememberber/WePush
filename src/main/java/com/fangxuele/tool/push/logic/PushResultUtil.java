package com.fangxuele.tool.push.logic;

import cn.hutool.json.JSONUtil;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * 推送结果差集等工具，避免 O(n·m) 的 remove/filter。
 */
public final class PushResultUtil {

    private PushResultUtil() {
    }

    /**
     * 从待发送列表中剔除已成功/失败项，得到未发送列表。
     *
     * @param toSendList      原始待发送（与发送时同一批数组引用）
     * @param sendSuccessList 成功列表（HTTP 保存结果时末尾多一列 body）
     * @param sendFailList    失败列表
     * @param httpSaveResult  是否 HTTP 且开启了保存响应
     */
    public static List<String[]> computeUnsent(List<String[]> toSendList,
                                               List<String[]> sendSuccessList,
                                               List<String[]> sendFailList,
                                               boolean httpSaveResult) {
        if (toSendList == null || toSendList.isEmpty()) {
            return new ArrayList<>();
        }
        if ((sendSuccessList == null || sendSuccessList.isEmpty())
                && (sendFailList == null || sendFailList.isEmpty())) {
            return new ArrayList<>(toSendList);
        }

        if (httpSaveResult) {
            Set<String> processedKeys = new HashSet<>(
                    (sendSuccessList == null ? 0 : sendSuccessList.size())
                            + (sendFailList == null ? 0 : sendFailList.size()));
            addHttpProcessedKeys(processedKeys, sendSuccessList);
            addHttpProcessedKeys(processedKeys, sendFailList);

            List<String[]> unsent = new ArrayList<>();
            for (String[] str : toSendList) {
                if (!processedKeys.contains(JSONUtil.toJsonStr(str))) {
                    unsent.add(str);
                }
            }
            return unsent;
        }

        Set<String[]> processed = Collections.newSetFromMap(new IdentityHashMap<>());
        if (sendSuccessList != null) {
            processed.addAll(sendSuccessList);
        }
        if (sendFailList != null) {
            processed.addAll(sendFailList);
        }
        List<String[]> unsent = new ArrayList<>(Math.max(0, toSendList.size() - processed.size()));
        for (String[] str : toSendList) {
            if (!processed.contains(str)) {
                unsent.add(str);
            }
        }
        return unsent;
    }

    private static void addHttpProcessedKeys(Set<String> processedKeys, List<String[]> list) {
        if (list == null) {
            return;
        }
        for (String[] str : list) {
            if (str == null || str.length == 0) {
                processedKeys.add(JSONUtil.toJsonStr(str));
            } else {
                processedKeys.add(JSONUtil.toJsonStr(ArrayUtils.remove(str, str.length - 1)));
            }
        }
    }
}

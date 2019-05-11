package com.fangxuele.tool.push.util;

import javax.swing.*;
import javax.swing.table.TableColumn;

/**
 * <pre>
 * JTableUtil
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">RememBerBer</a>
 * @since 2019/5/12.
 */
public class JTableUtil {
    /**
     * 隐藏表格中的某一列
     *
     * @param table
     * @param index
     */
    public static void hideColumn(JTable table, int index) {
        TableColumn tableColumn = table.getColumnModel().getColumn(index);
        tableColumn.setMaxWidth(0);
        tableColumn.setMinWidth(0);
        tableColumn.setPreferredWidth(0);
        tableColumn.setWidth(0);

        table.getTableHeader().getColumnModel().getColumn(index).setMaxWidth(0);
        table.getTableHeader().getColumnModel().getColumn(index).setMinWidth(0);
    }
}

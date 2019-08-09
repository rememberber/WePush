package com.fangxuele.tool.push.util;

import lombok.extern.slf4j.Slf4j;

import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.Field;

/**
 * <pre>
 * 撤销/重做工具
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/8/9.
 */
@Slf4j
public class UndoUtil {

    /**
     * 给对象中的文本框和文本域添加撤销/重做事件
     *
     * @param object
     */
    public static void register(Object object) {
        Class strClass = object.getClass();
        Field[] declaredFields = strClass.getDeclaredFields();
        for (Field field : declaredFields) {
            if (JTextComponent.class.isAssignableFrom(field.getType())) {
                UndoManager undoManager = new UndoManager();
                try {
                    field.setAccessible(true);
                    ((JTextComponent) field.get(object)).getDocument().addUndoableEditListener(undoManager);
                    ((JTextComponent) field.get(object)).addKeyListener(new KeyListener() {
                        @Override
                        public void keyReleased(KeyEvent arg0) {
                        }

                        @Override
                        public void keyPressed(KeyEvent evt) {
                            if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_Z) {
                                if (undoManager.canUndo()) {
                                    undoManager.undo();
                                }
                            }
                            if (evt.isControlDown() && evt.getKeyCode() == KeyEvent.VK_Y) {
                                if (undoManager.canRedo()) {
                                    undoManager.redo();
                                }
                            }
                        }

                        @Override
                        public void keyTyped(KeyEvent arg0) {
                        }
                    });
                } catch (IllegalAccessException e) {
                    log.error(e.toString());
                }
            }
        }
    }
}

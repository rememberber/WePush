package com.fangxuele.tool.push.ui.form.account;

import com.fangxuele.tool.push.logic.carriersms.CarrierSmsProtocol;
import com.fangxuele.tool.push.ui.form.msg.CarrierSmsMsgForm;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CarrierSmsAccountFormTest {

    @Test
    public void switchesProtocolSpecificFieldsAndUpdatesCharacterCount() throws Exception {
        AtomicReference<CarrierSmsAccountForm> accountFormReference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            CarrierSmsAccountForm form = CarrierSmsAccountForm.getInstance();
            form.clear();
            accountFormReference.set(form);
        });
        CarrierSmsAccountForm accountForm = accountFormReference.get();
        JComboBox<?> protocolCombo = findFirst(accountForm.getMainPanel(), JComboBox.class);

        SwingUtilities.invokeAndWait(() -> protocolCombo.setSelectedItem(CarrierSmsProtocol.SGIP));
        List<String> sgipLabels = visibleLabelTexts(accountForm.getMainPanel());
        assertTrue(sgipLabels.contains("SGIP NodeId *"));
        assertTrue(sgipLabels.contains("SGIP CorpId *"));
        assertFalse(sgipLabels.contains("SMPP SystemType"));

        SwingUtilities.invokeAndWait(() -> protocolCombo.setSelectedItem(CarrierSmsProtocol.SMPP));
        List<String> smppLabels = visibleLabelTexts(accountForm.getMainPanel());
        assertTrue(smppLabels.contains("SMPP SystemType"));
        assertTrue(smppLabels.contains("SMPP 源 TON / NPI"));
        assertFalse(smppLabels.contains("SGIP NodeId *"));

        CarrierSmsMsgForm messageForm = CarrierSmsMsgForm.getInstance();
        SwingUtilities.invokeAndWait(() -> messageForm.getContentTextArea().setText("A😀中"));
        assertTrue(visibleLabelTexts(messageForm.getMainPanel()).stream()
                .anyMatch(text -> text.startsWith("字符数：3")));
    }

    private static List<String> visibleLabelTexts(Container root) {
        List<String> labels = new ArrayList<>();
        for (Component component : root.getComponents()) {
            if (component instanceof JLabel label && label.isVisible()) {
                labels.add(label.getText());
            }
            if (component instanceof Container child) {
                labels.addAll(visibleLabelTexts(child));
            }
        }
        return labels;
    }

    private static <T extends Component> T findFirst(Container root, Class<T> type) {
        T found = findFirstOrNull(root, type);
        if (found == null) {
            throw new AssertionError("component not found: " + type.getSimpleName());
        }
        return found;
    }

    private static <T extends Component> T findFirstOrNull(Container root, Class<T> type) {
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) {
                return type.cast(component);
            }
            if (component instanceof Container child) {
                T found = findFirstOrNull(child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}

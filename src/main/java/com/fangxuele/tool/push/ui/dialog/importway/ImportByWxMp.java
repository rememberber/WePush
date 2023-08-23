package com.fangxuele.tool.push.ui.dialog.importway;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.fangxuele.tool.push.App;
import com.fangxuele.tool.push.dao.TPeopleDataMapper;
import com.fangxuele.tool.push.dao.TPeopleImportConfigMapper;
import com.fangxuele.tool.push.dao.TPeopleMapper;
import com.fangxuele.tool.push.dao.TWxMpUserMapper;
import com.fangxuele.tool.push.domain.TPeople;
import com.fangxuele.tool.push.domain.TPeopleData;
import com.fangxuele.tool.push.domain.TPeopleImportConfig;
import com.fangxuele.tool.push.domain.TWxMpUser;
import com.fangxuele.tool.push.logic.PeopleImportWayEnum;
import com.fangxuele.tool.push.logic.msgsender.WxMpTemplateMsgSender;
import com.fangxuele.tool.push.ui.UiConsts;
import com.fangxuele.tool.push.ui.dialog.importway.config.WxMpImportConfig;
import com.fangxuele.tool.push.ui.form.PeopleEditForm;
import com.fangxuele.tool.push.util.ComponentUtil;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.SqliteUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import me.chanjar.weixin.mp.bean.result.WxMpUserList;
import me.chanjar.weixin.mp.bean.tag.WxTagListUser;
import me.chanjar.weixin.mp.bean.tag.WxUserTag;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.*;

@Getter
public class ImportByWxMp extends JDialog {
    private JPanel contentPane;
    private JComboBox memberImportTagComboBox;
    private JButton memberImportAllButton;
    private JButton memberImportTagFreshButton;
    private JButton memberImportTagButton;
    private JButton memberImportTagRetainButton;
    private JPanel importOptionPanel;
    private JCheckBox importOptionOpenIdCheckBox;
    private JCheckBox importOptionBasicInfoCheckBox;
    private JCheckBox importOptionAvatarCheckBox;
    private JButton clearDbCacheButton;

    private static final Log logger = LogFactory.get();

    private static TPeopleDataMapper peopleDataMapper = MybatisUtil.getSqlSession().getMapper(TPeopleDataMapper.class);
    private static TPeopleMapper peopleMapper = MybatisUtil.getSqlSession().getMapper(TPeopleMapper.class);
    private static TPeopleImportConfigMapper peopleImportConfigMapper = MybatisUtil.getSqlSession().getMapper(TPeopleImportConfigMapper.class);
    private static TWxMpUserMapper tWxMpUserMapper = MybatisUtil.getSqlSession().getMapper(TWxMpUserMapper.class);

    public static Map<String, Long> userTagMap = new HashMap<>();
    /**
     * 用于导入多个标签的用户时去重判断
     */
    private Set<String> tagUserSet;

    private Integer peopleId;

    private TPeople tPeople;

    public ImportByWxMp(Integer peopleId) {
        super(App.mainFrame, "通过微信公众平台导入人群");
        setContentPane(contentPane);
        setModal(true);
        ComponentUtil.setPreferSizeAndLocateToCenter(this, 0.3, 0.3);
        getRootPane().setDefaultButton(memberImportAllButton);

        this.peopleId = peopleId;
        tPeople = peopleMapper.selectByPrimaryKey(peopleId);

        memberImportAllButton.setIcon(new FlatSVGIcon("icon/import.svg"));
        memberImportTagButton.setIcon(new FlatSVGIcon("icon/import.svg"));
        memberImportTagRetainButton.setIcon(new FlatSVGIcon("icon/import.svg"));
        memberImportTagFreshButton.setIcon(new FlatSVGIcon("icon/refresh.svg"));

        // 公众号-导入全员按钮事件
        memberImportAllButton.addActionListener(e -> {
            ThreadUtil.execute(() -> importWxAll(this));
            dispose();
        });

        // 公众号-刷新可选的标签按钮事件
        memberImportTagFreshButton.addActionListener(e -> {

            WxMpService wxMpService = WxMpTemplateMsgSender.getWxMpService(tPeople.getAccountId());
            if (wxMpService.getWxMpConfigStorage() == null) {
                return;
            }

            try {
                List<WxUserTag> wxUserTagList = wxMpService.getUserTagService().tagGet();

                memberImportTagComboBox.removeAllItems();
                userTagMap = new HashMap<>();

                for (WxUserTag wxUserTag : wxUserTagList) {
                    String item = wxUserTag.getName() + "/" + wxUserTag.getCount() + "用户";
                    memberImportTagComboBox.addItem(item);
                    userTagMap.put(item, wxUserTag.getId());
                }

            } catch (WxErrorException e1) {
                JOptionPane.showMessageDialog(App.mainFrame, "刷新失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
                e1.printStackTrace();
            }
        });

        // 公众号-导入选择的标签分组用户按钮事件(取并集)
        memberImportTagButton.addActionListener(e -> ThreadUtil.execute(() -> {
            PeopleEditForm peopleEditForm = PeopleEditForm.getInstance();
            JProgressBar progressBar = peopleEditForm.getMemberTabImportProgressBar();

            try {
                if (memberImportTagComboBox.getSelectedItem() != null
                        && StringUtils.isNotEmpty(memberImportTagComboBox.getSelectedItem().toString())) {

                    long selectedTagId = userTagMap.get(memberImportTagComboBox.getSelectedItem());
                    getMpUserListByTag(this, selectedTagId, false);
                    PeopleEditForm.initDataTable(peopleId);
                    JOptionPane.showMessageDialog(App.mainFrame, "导入完成！", "完成",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(App.mainFrame, "请先选择需要导入的标签！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (WxErrorException e1) {
                JOptionPane.showMessageDialog(App.mainFrame, "导入失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
            } finally {
                progressBar.setIndeterminate(false);
                progressBar.setValue(progressBar.getMaximum());
                progressBar.setVisible(false);
            }
        }));

        // 公众号-导入选择的标签分组用户按钮事件(取交集)
        memberImportTagRetainButton.addActionListener(e -> ThreadUtil.execute(() -> {
            PeopleEditForm peopleEditForm = PeopleEditForm.getInstance();
            JProgressBar progressBar = peopleEditForm.getMemberTabImportProgressBar();

            try {
                if (memberImportTagComboBox.getSelectedItem() != null
                        && StringUtils.isNotEmpty(memberImportTagComboBox.getSelectedItem().toString())) {

                    long selectedTagId = userTagMap.get(memberImportTagComboBox.getSelectedItem());
                    getMpUserListByTag(this, selectedTagId, true);
                    PeopleEditForm.initDataTable(peopleId);
                    JOptionPane.showMessageDialog(App.mainFrame, "导入完成！", "完成",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(App.mainFrame, "请先选择需要导入的标签！", "提示",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (WxErrorException e1) {
                JOptionPane.showMessageDialog(App.mainFrame, "导入失败！\n\n" + e1.getMessage(), "失败",
                        JOptionPane.ERROR_MESSAGE);
                logger.error(e1);
                e1.printStackTrace();
            } finally {
                progressBar.setIndeterminate(false);
                progressBar.setValue(progressBar.getMaximum());
                progressBar.setVisible(false);
            }
        }));

        // 公众号-清空本地缓存按钮事件
        clearDbCacheButton.addActionListener(e -> {
            int count = tWxMpUserMapper.deleteAll();
            JOptionPane.showMessageDialog(App.mainFrame, "清理完毕！\n\n共清理：" + count + "条本地数据", "提示",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    /**
     * 导入微信全员
     */
    public void importWxAll(ImportByWxMp dialog) {
        PeopleEditForm instance = PeopleEditForm.getInstance();
        JProgressBar progressBar = instance.getMemberTabImportProgressBar();
        instance.getImportButton().setEnabled(false);

        try {
            getMpUserList(dialog);
            PeopleEditForm.initDataTable(peopleId);
            progressBar.setVisible(false);
            JOptionPane.showMessageDialog(App.mainFrame, "导入完成！", "完成", JOptionPane.INFORMATION_MESSAGE);
        } catch (WxErrorException e1) {
            JOptionPane.showMessageDialog(App.mainFrame, "导入失败！\n\n" + e1.getMessage(), "失败",
                    JOptionPane.ERROR_MESSAGE);
            logger.error(e1);
            e1.printStackTrace();
        } finally {
            progressBar.setIndeterminate(false);
            progressBar.setVisible(false);
            instance.getImportButton().setEnabled(true);
        }
    }

    /**
     * 拉取公众平台用户列表
     */
    public void getMpUserList(ImportByWxMp dialog) throws WxErrorException {
        PeopleEditForm instance = PeopleEditForm.getInstance();
        JProgressBar progressBar = instance.getMemberTabImportProgressBar();
        JLabel memberCountLabel = instance.getMemberTabCountLabel();

        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        WxMpService wxMpService = WxMpTemplateMsgSender.getWxMpService(tPeople.getAccountId());
        if (wxMpService.getWxMpConfigStorage() == null) {
            return;
        }

        WxMpUserList wxMpUserList = wxMpService.getUserService().userList(null);

        logger.info("关注该公众账号的总用户数：" + wxMpUserList.getTotal());
        logger.info("拉取的OPENID个数：" + wxMpUserList.getCount());

        progressBar.setIndeterminate(false);
        progressBar.setMaximum((int) wxMpUserList.getTotal());
        int importedCount = 0;
        String now = SqliteUtil.nowDateForSqlite();
        String dataVersion = UUID.fastUUID().toString(true);

        // 保存导入配置
        TPeopleImportConfig beforePeopleImportConfig = peopleImportConfigMapper.selectByPeopleId(peopleId);

        TPeopleImportConfig tPeopleImportConfig = new TPeopleImportConfig();
        tPeopleImportConfig.setPeopleId(peopleId);
        tPeopleImportConfig.setLastWay(String.valueOf(PeopleImportWayEnum.BY_WX_MP_CODE));
        tPeopleImportConfig.setAppVersion(UiConsts.APP_VERSION);
        tPeopleImportConfig.setLastDataVersion(dataVersion);
        tPeopleImportConfig.setModifiedTime(now);

        WxMpImportConfig wxMpImportConfig = new WxMpImportConfig();
        wxMpImportConfig.setUserType(1);
        tPeopleImportConfig.setLastWayConfig(JSONUtil.toJsonStr(wxMpImportConfig));

        if (beforePeopleImportConfig != null) {
            tPeopleImportConfig.setId(beforePeopleImportConfig.getId());
            peopleImportConfigMapper.updateByPrimaryKeySelective(tPeopleImportConfig);
        } else {
            tPeopleImportConfig.setCreateTime(now);
            peopleImportConfigMapper.insert(tPeopleImportConfig);
        }

        if (wxMpUserList.getCount() == 0) {
            memberCountLabel.setText(String.valueOf(importedCount));
            progressBar.setValue(importedCount);
            return;
        }

        List<String> openIds = wxMpUserList.getOpenids();

        for (String openId : openIds) {
            List<String> varData = getVarDatas(dialog, openId);

            TPeopleData tPeopleData = new TPeopleData();
            tPeopleData.setPeopleId(peopleId);
            tPeopleData.setPin(openId);
            tPeopleData.setVarData(JSONUtil.toJsonStr(varData));
            tPeopleData.setAppVersion(UiConsts.APP_VERSION);
            tPeopleData.setDataVersion(dataVersion);
            tPeopleData.setCreateTime(now);
            tPeopleData.setModifiedTime(now);

            peopleDataMapper.insert(tPeopleData);

            importedCount++;
            memberCountLabel.setText(String.valueOf(importedCount));
            progressBar.setValue(importedCount);
        }

        while (StringUtils.isNotEmpty(wxMpUserList.getNextOpenid())) {
            wxMpUserList = wxMpService.getUserService().userList(wxMpUserList.getNextOpenid());

            logger.info("拉取的OPENID个数：" + wxMpUserList.getCount());

            if (wxMpUserList.getCount() == 0) {
                break;
            }
            openIds = wxMpUserList.getOpenids();
            for (String openId : openIds) {
                List<String> varData = getVarDatas(dialog, openId);

                TPeopleData tPeopleData = new TPeopleData();
                tPeopleData.setPeopleId(peopleId);
                tPeopleData.setPin(openId);
                tPeopleData.setVarData(JSONUtil.toJsonStr(varData));
                tPeopleData.setAppVersion(UiConsts.APP_VERSION);
                tPeopleData.setDataVersion(dataVersion);
                tPeopleData.setCreateTime(now);
                tPeopleData.setModifiedTime(now);

                peopleDataMapper.insert(tPeopleData);

                importedCount++;
                memberCountLabel.setText(String.valueOf(importedCount));
                progressBar.setValue(importedCount);
            }
        }

        progressBar.setValue((int) wxMpUserList.getTotal());
    }

    @NotNull
    private List<String> getVarDatas(ImportByWxMp dialog, String openId) {
        boolean needToGetInfoFromWeiXin = false;
        WxMpService wxMpService = WxMpTemplateMsgSender.getWxMpService(tPeople.getAccountId());

        if (dialog.getImportOptionBasicInfoCheckBox().isSelected() ||
                dialog.getImportOptionAvatarCheckBox().isSelected()) {
            needToGetInfoFromWeiXin = true;
        }

        List<String> varData = Lists.newArrayList();
        varData.add(openId);
        if (needToGetInfoFromWeiXin) {
            WxMpUser wxMpUser = null;
            TWxMpUser tWxMpUser = tWxMpUserMapper.selectByPrimaryKey(openId);
            if (tWxMpUser != null) {
                wxMpUser = new WxMpUser();
                BeanUtil.copyProperties(tWxMpUser, wxMpUser);
            } else {
                if (wxMpService != null) {
                    try {
                        wxMpUser = wxMpService.getUserService().userInfo(openId);
                        if (wxMpUser != null) {
                            tWxMpUser = new TWxMpUser();
                            BeanUtil.copyProperties(wxMpUser, tWxMpUser);
                            tWxMpUserMapper.insertSelective(tWxMpUser);
                        }
                    } catch (Exception e) {
                        logger.error(e);
                    }
                }
            }

            if (wxMpUser != null) {
                if (dialog.getImportOptionAvatarCheckBox().isSelected()) {
                    varData.add(wxMpUser.getHeadImgUrl());
                }
                if (dialog.getImportOptionBasicInfoCheckBox().isSelected()) {
                    varData.add(wxMpUser.getNickname());
                    varData.add("");
                    varData.add("");
                    varData.add(DateFormatUtils.format(wxMpUser.getSubscribeTime() * 1000, "yyyy-MM-dd HH:mm:ss"));
                }
            } else {
                if (dialog.getImportOptionAvatarCheckBox().isSelected()) {
                    varData.add("");
                }
                if (dialog.getImportOptionBasicInfoCheckBox().isSelected()) {
                    varData.add("");
                    varData.add("");
                    varData.add("");
                    varData.add("");
                }
            }
        }
        return varData;
    }

    /**
     * 按标签拉取公众平台用户列表
     *
     * @param tagId
     * @param retain 是否取交集
     * @throws WxErrorException
     */
    public void getMpUserListByTag(ImportByWxMp dialog, Long tagId, boolean retain) throws WxErrorException {
        PeopleEditForm instance = PeopleEditForm.getInstance();
        JProgressBar progressBar = instance.getMemberTabImportProgressBar();

        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);

        WxMpService wxMpService = WxMpTemplateMsgSender.getWxMpService(tPeople.getAccountId());
        if (wxMpService.getWxMpConfigStorage() == null) {
            return;
        }

        WxTagListUser wxTagListUser = wxMpService.getUserTagService().tagListUser(tagId, "");

        logger.info("拉取的OPENID个数：" + wxTagListUser.getCount());

        if (wxTagListUser.getCount() == 0) {
            return;
        }

        String now = SqliteUtil.nowDateForSqlite();
        String dataVersion = UUID.fastUUID().toString(true);

        // 保存导入配置
        TPeopleImportConfig beforePeopleImportConfig = peopleImportConfigMapper.selectByPeopleId(peopleId);

        TPeopleImportConfig tPeopleImportConfig = new TPeopleImportConfig();
        tPeopleImportConfig.setPeopleId(peopleId);
        tPeopleImportConfig.setLastWay(String.valueOf(PeopleImportWayEnum.BY_WX_MP_CODE));
        tPeopleImportConfig.setAppVersion(UiConsts.APP_VERSION);
        tPeopleImportConfig.setLastDataVersion(dataVersion);
        tPeopleImportConfig.setModifiedTime(now);

        WxMpImportConfig wxMpImportConfig = new WxMpImportConfig();
        wxMpImportConfig.setTagId(tagId);
        wxMpImportConfig.setUserType(retain ? 3 : 2);
        tPeopleImportConfig.setLastWayConfig(JSONUtil.toJsonStr(wxMpImportConfig));

        if (beforePeopleImportConfig != null) {
            tPeopleImportConfig.setId(beforePeopleImportConfig.getId());
            peopleImportConfigMapper.updateByPrimaryKeySelective(tPeopleImportConfig);
        } else {
            tPeopleImportConfig.setCreateTime(now);
            peopleImportConfigMapper.insert(tPeopleImportConfig);
        }

        List<String> openIds = wxTagListUser.getData().getOpenidList();

        if (tagUserSet == null) {
            tagUserSet = Collections.synchronizedSet(new HashSet<>());
            tagUserSet.addAll(openIds);
        }

        if (retain) {
            // 取交集
            tagUserSet.retainAll(openIds);
        } else {
            // 无重复并集
            openIds.removeAll(tagUserSet);
            tagUserSet.addAll(openIds);
        }

        while (StringUtils.isNotEmpty(wxTagListUser.getNextOpenid())) {
            wxTagListUser = wxMpService.getUserTagService().tagListUser(tagId, wxTagListUser.getNextOpenid());

            logger.info("拉取的OPENID个数：" + wxTagListUser.getCount());

            if (wxTagListUser.getCount() == 0) {
                break;
            }
            openIds = wxTagListUser.getData().getOpenidList();

            if (retain) {
                // 取交集
                tagUserSet.retainAll(openIds);
            } else {
                // 无重复并集
                openIds.removeAll(tagUserSet);
                tagUserSet.addAll(openIds);
            }
        }

        for (String openId : tagUserSet) {
            List<String> varData = getVarDatas(dialog, openId);

            TPeopleData tPeopleData = new TPeopleData();
            tPeopleData.setPeopleId(peopleId);
            tPeopleData.setPin(openId);
            tPeopleData.setVarData(JSONUtil.toJsonStr(varData));
            tPeopleData.setAppVersion(UiConsts.APP_VERSION);
            tPeopleData.setDataVersion(dataVersion);
            tPeopleData.setCreateTime(now);
            tPeopleData.setModifiedTime(now);

            peopleDataMapper.insert(tPeopleData);
        }

        progressBar.setIndeterminate(false);
        progressBar.setValue(progressBar.getMaximum());

    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        memberImportTagButton = new JButton();
        Font memberImportTagButtonFont = this.$$$getFont$$$(null, Font.PLAIN, -1, memberImportTagButton.getFont());
        if (memberImportTagButtonFont != null) memberImportTagButton.setFont(memberImportTagButtonFont);
        memberImportTagButton.setText("导入选择的标签分组-取并集");
        panel2.add(memberImportTagButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        memberImportTagRetainButton = new JButton();
        Font memberImportTagRetainButtonFont = this.$$$getFont$$$(null, Font.PLAIN, -1, memberImportTagRetainButton.getFont());
        if (memberImportTagRetainButtonFont != null)
            memberImportTagRetainButton.setFont(memberImportTagRetainButtonFont);
        memberImportTagRetainButton.setText("导入选择的标签分组-取交集");
        panel2.add(memberImportTagRetainButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        memberImportAllButton = new JButton();
        Font memberImportAllButtonFont = this.$$$getFont$$$(null, Font.PLAIN, -1, memberImportAllButton.getFont());
        if (memberImportAllButtonFont != null) memberImportAllButton.setFont(memberImportAllButtonFont);
        memberImportAllButton.setText("导入所有关注公众号的用户");
        panel2.add(memberImportAllButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        importOptionPanel = new JPanel();
        importOptionPanel.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(importOptionPanel, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        importOptionOpenIdCheckBox = new JCheckBox();
        importOptionOpenIdCheckBox.setEnabled(false);
        importOptionOpenIdCheckBox.setSelected(true);
        importOptionOpenIdCheckBox.setText("openId");
        importOptionPanel.add(importOptionOpenIdCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        importOptionPanel.add(spacer1, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        importOptionBasicInfoCheckBox = new JCheckBox();
        importOptionBasicInfoCheckBox.setText("昵称、性别等基本信息");
        importOptionBasicInfoCheckBox.setToolTipText("每获取一条信息会花费一次每日接口调用量");
        importOptionPanel.add(importOptionBasicInfoCheckBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        importOptionAvatarCheckBox = new JCheckBox();
        importOptionAvatarCheckBox.setText("头像");
        importOptionAvatarCheckBox.setToolTipText("勾选会导致左侧列表甚至WePush变卡哦");
        importOptionPanel.add(importOptionAvatarCheckBox, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        clearDbCacheButton = new JButton();
        clearDbCacheButton.setText("清空本地缓存");
        importOptionPanel.add(clearDbCacheButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        memberImportTagComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        memberImportTagComboBox.setModel(defaultComboBoxModel1);
        panel3.add(memberImportTagComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        memberImportTagFreshButton = new JButton();
        Font memberImportTagFreshButtonFont = this.$$$getFont$$$(null, Font.PLAIN, -1, memberImportTagFreshButton.getFont());
        if (memberImportTagFreshButtonFont != null) memberImportTagFreshButton.setFont(memberImportTagFreshButtonFont);
        memberImportTagFreshButton.setText("刷新");
        panel3.add(memberImportTagFreshButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("标签分组");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

    public void reImport() {
        TPeopleImportConfig peopleImportConfig = peopleImportConfigMapper.selectByPeopleId(peopleId);
        String lastWayConfig = peopleImportConfig.getLastWayConfig();
        WxMpImportConfig wxMpImportConfigBefore = JSONUtil.toBean(lastWayConfig, WxMpImportConfig.class);
        if (wxMpImportConfigBefore == null) {
            return;
        }

        try {
            //  1:全部，2：标签取并集，3:标签取交集
            if (wxMpImportConfigBefore.getUserType() == 1) {
                WxMpService wxMpService = WxMpTemplateMsgSender.getWxMpService(tPeople.getAccountId());
                if (wxMpService.getWxMpConfigStorage() == null) {
                    return;
                }

                WxMpUserList wxMpUserList = wxMpService.getUserService().userList(null);

                logger.info("关注该公众账号的总用户数：" + wxMpUserList.getTotal());
                logger.info("拉取的OPENID个数：" + wxMpUserList.getCount());

                int importedCount = 0;
                String now = SqliteUtil.nowDateForSqlite();
                String dataVersion = UUID.fastUUID().toString(true);

                // 保存导入配置
                TPeopleImportConfig beforePeopleImportConfig = peopleImportConfigMapper.selectByPeopleId(peopleId);

                TPeopleImportConfig tPeopleImportConfig = new TPeopleImportConfig();
                tPeopleImportConfig.setPeopleId(peopleId);
                tPeopleImportConfig.setLastWay(String.valueOf(PeopleImportWayEnum.BY_WX_MP_CODE));
                tPeopleImportConfig.setAppVersion(UiConsts.APP_VERSION);
                tPeopleImportConfig.setLastDataVersion(dataVersion);
                tPeopleImportConfig.setModifiedTime(now);

                WxMpImportConfig wxMpImportConfig = new WxMpImportConfig();
                wxMpImportConfig.setUserType(1);
                tPeopleImportConfig.setLastWayConfig(JSONUtil.toJsonStr(wxMpImportConfig));

                if (beforePeopleImportConfig != null) {
                    tPeopleImportConfig.setId(beforePeopleImportConfig.getId());
                    peopleImportConfigMapper.updateByPrimaryKeySelective(tPeopleImportConfig);
                } else {
                    tPeopleImportConfig.setCreateTime(now);
                    peopleImportConfigMapper.insert(tPeopleImportConfig);
                }

                if (wxMpUserList.getCount() == 0) {
                    return;
                }

                List<String> openIds = wxMpUserList.getOpenids();

                peopleDataMapper.deleteByPeopleId(peopleId);

                for (String openId : openIds) {
                    List<String> varData = getVarDatas(this, openId);

                    TPeopleData tPeopleData = new TPeopleData();
                    tPeopleData.setPeopleId(peopleId);
                    tPeopleData.setPin(openId);
                    tPeopleData.setVarData(JSONUtil.toJsonStr(varData));
                    tPeopleData.setAppVersion(UiConsts.APP_VERSION);
                    tPeopleData.setDataVersion(dataVersion);
                    tPeopleData.setCreateTime(now);
                    tPeopleData.setModifiedTime(now);

                    peopleDataMapper.insert(tPeopleData);

                    importedCount++;
                }

                while (StringUtils.isNotEmpty(wxMpUserList.getNextOpenid())) {
                    wxMpUserList = wxMpService.getUserService().userList(wxMpUserList.getNextOpenid());

                    logger.info("拉取的OPENID个数：" + wxMpUserList.getCount());

                    if (wxMpUserList.getCount() == 0) {
                        break;
                    }
                    openIds = wxMpUserList.getOpenids();
                    for (String openId : openIds) {
                        List<String> varData = getVarDatas(this, openId);

                        TPeopleData tPeopleData = new TPeopleData();
                        tPeopleData.setPeopleId(peopleId);
                        tPeopleData.setPin(openId);
                        tPeopleData.setVarData(JSONUtil.toJsonStr(varData));
                        tPeopleData.setAppVersion(UiConsts.APP_VERSION);
                        tPeopleData.setDataVersion(dataVersion);
                        tPeopleData.setCreateTime(now);
                        tPeopleData.setModifiedTime(now);

                        peopleDataMapper.insert(tPeopleData);

                        importedCount++;
                    }
                }
            } else if (wxMpImportConfigBefore.getUserType() == 2) {
                WxMpService wxMpService = WxMpTemplateMsgSender.getWxMpService(tPeople.getAccountId());
                if (wxMpService.getWxMpConfigStorage() == null) {
                    return;
                }

                WxTagListUser wxTagListUser = wxMpService.getUserTagService().tagListUser(wxMpImportConfigBefore.getTagId(), "");

                logger.info("拉取的OPENID个数：" + wxTagListUser.getCount());

                if (wxTagListUser.getCount() == 0) {
                    return;
                }

                String now = SqliteUtil.nowDateForSqlite();
                String dataVersion = UUID.fastUUID().toString(true);

                // 保存导入配置
                TPeopleImportConfig beforePeopleImportConfig = peopleImportConfigMapper.selectByPeopleId(peopleId);

                TPeopleImportConfig tPeopleImportConfig = new TPeopleImportConfig();
                tPeopleImportConfig.setPeopleId(peopleId);
                tPeopleImportConfig.setLastWay(String.valueOf(PeopleImportWayEnum.BY_WX_MP_CODE));
                tPeopleImportConfig.setAppVersion(UiConsts.APP_VERSION);
                tPeopleImportConfig.setLastDataVersion(dataVersion);
                tPeopleImportConfig.setModifiedTime(now);

                WxMpImportConfig wxMpImportConfig = new WxMpImportConfig();
                wxMpImportConfig.setTagId(wxMpImportConfigBefore.getTagId());
                wxMpImportConfig.setUserType(2);
                tPeopleImportConfig.setLastWayConfig(JSONUtil.toJsonStr(wxMpImportConfig));

                if (beforePeopleImportConfig != null) {
                    tPeopleImportConfig.setId(beforePeopleImportConfig.getId());
                    peopleImportConfigMapper.updateByPrimaryKeySelective(tPeopleImportConfig);
                } else {
                    tPeopleImportConfig.setCreateTime(now);
                    peopleImportConfigMapper.insert(tPeopleImportConfig);
                }

                List<String> openIds = wxTagListUser.getData().getOpenidList();

                if (tagUserSet == null) {
                    tagUserSet = Collections.synchronizedSet(new HashSet<>());
                    tagUserSet.addAll(openIds);
                }

                // 无重复并集
                openIds.removeAll(tagUserSet);
                tagUserSet.addAll(openIds);

                while (StringUtils.isNotEmpty(wxTagListUser.getNextOpenid())) {
                    wxTagListUser = wxMpService.getUserTagService().tagListUser(wxMpImportConfigBefore.getTagId(), wxTagListUser.getNextOpenid());

                    logger.info("拉取的OPENID个数：" + wxTagListUser.getCount());

                    if (wxTagListUser.getCount() == 0) {
                        break;
                    }
                    openIds = wxTagListUser.getData().getOpenidList();

                    // 无重复并集
                    openIds.removeAll(tagUserSet);
                    tagUserSet.addAll(openIds);
                }

                peopleDataMapper.deleteByPeopleId(peopleId);

                for (String openId : tagUserSet) {
                    List<String> varData = getVarDatas(this, openId);

                    TPeopleData tPeopleData = new TPeopleData();
                    tPeopleData.setPeopleId(peopleId);
                    tPeopleData.setPin(openId);
                    tPeopleData.setVarData(JSONUtil.toJsonStr(varData));
                    tPeopleData.setAppVersion(UiConsts.APP_VERSION);
                    tPeopleData.setDataVersion(dataVersion);
                    tPeopleData.setCreateTime(now);
                    tPeopleData.setModifiedTime(now);

                    peopleDataMapper.insert(tPeopleData);
                }
            } else if (wxMpImportConfigBefore.getUserType() == 3) {
                WxMpService wxMpService = WxMpTemplateMsgSender.getWxMpService(tPeople.getAccountId());
                if (wxMpService.getWxMpConfigStorage() == null) {
                    return;
                }

                WxTagListUser wxTagListUser = wxMpService.getUserTagService().tagListUser(wxMpImportConfigBefore.getTagId(), "");

                logger.info("拉取的OPENID个数：" + wxTagListUser.getCount());

                if (wxTagListUser.getCount() == 0) {
                    return;
                }

                String now = SqliteUtil.nowDateForSqlite();
                String dataVersion = UUID.fastUUID().toString(true);

                // 保存导入配置
                TPeopleImportConfig beforePeopleImportConfig = peopleImportConfigMapper.selectByPeopleId(peopleId);

                TPeopleImportConfig tPeopleImportConfig = new TPeopleImportConfig();
                tPeopleImportConfig.setPeopleId(peopleId);
                tPeopleImportConfig.setLastWay(String.valueOf(PeopleImportWayEnum.BY_WX_MP_CODE));
                tPeopleImportConfig.setAppVersion(UiConsts.APP_VERSION);
                tPeopleImportConfig.setLastDataVersion(dataVersion);
                tPeopleImportConfig.setModifiedTime(now);

                WxMpImportConfig wxMpImportConfig = new WxMpImportConfig();
                wxMpImportConfig.setTagId(wxMpImportConfigBefore.getTagId());
                wxMpImportConfig.setUserType(3);
                tPeopleImportConfig.setLastWayConfig(JSONUtil.toJsonStr(wxMpImportConfig));

                if (beforePeopleImportConfig != null) {
                    tPeopleImportConfig.setId(beforePeopleImportConfig.getId());
                    peopleImportConfigMapper.updateByPrimaryKeySelective(tPeopleImportConfig);
                } else {
                    tPeopleImportConfig.setCreateTime(now);
                    peopleImportConfigMapper.insert(tPeopleImportConfig);
                }

                List<String> openIds = wxTagListUser.getData().getOpenidList();

                if (tagUserSet == null) {
                    tagUserSet = Collections.synchronizedSet(new HashSet<>());
                    tagUserSet.addAll(openIds);
                }

                // 取交集
                tagUserSet.retainAll(openIds);

                while (StringUtils.isNotEmpty(wxTagListUser.getNextOpenid())) {
                    wxTagListUser = wxMpService.getUserTagService().tagListUser(wxMpImportConfigBefore.getTagId(), wxTagListUser.getNextOpenid());

                    logger.info("拉取的OPENID个数：" + wxTagListUser.getCount());

                    if (wxTagListUser.getCount() == 0) {
                        break;
                    }
                    openIds = wxTagListUser.getData().getOpenidList();

                    // 取交集
                    tagUserSet.retainAll(openIds);
                }

                peopleDataMapper.deleteByPeopleId(peopleId);

                for (String openId : tagUserSet) {
                    List<String> varData = getVarDatas(this, openId);

                    TPeopleData tPeopleData = new TPeopleData();
                    tPeopleData.setPeopleId(peopleId);
                    tPeopleData.setPin(openId);
                    tPeopleData.setVarData(JSONUtil.toJsonStr(varData));
                    tPeopleData.setAppVersion(UiConsts.APP_VERSION);
                    tPeopleData.setDataVersion(dataVersion);
                    tPeopleData.setCreateTime(now);
                    tPeopleData.setModifiedTime(now);

                    peopleDataMapper.insert(tPeopleData);
                }
            }
        } catch (Exception e) {
            logger.error(e);
        }

        dispose();
    }
}

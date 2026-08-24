package com.fangxuele.tool.push.logic.msgmaker;

import com.alibaba.fastjson.JSON;
import com.fangxuele.tool.push.bean.account.WxCpAccountConfig;
import com.fangxuele.tool.push.dao.TAccountMapper;
import com.fangxuele.tool.push.domain.TAccount;
import com.fangxuele.tool.push.domain.TMsg;
import com.fangxuele.tool.push.domain.TMsgWxCp;
import com.fangxuele.tool.push.domain.WxCpMiniProgramContentItem;
import com.fangxuele.tool.push.util.MybatisUtil;
import com.fangxuele.tool.push.util.TemplateUtil;
import com.fangxuele.tool.push.util.WxCpMiniProgramNoticeSupport;
import me.chanjar.weixin.cp.bean.article.NewArticle;
import me.chanjar.weixin.cp.bean.message.WxCpMessage;
import me.chanjar.weixin.cp.bean.messagebuilder.MiniProgramNoticeMsgBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * <pre>
 * 企业号消息加工器
 * </pre>
 *
 * @author <a href="https://github.com/rememberber">Zhou Bo</a>
 * @since 2019/6/29.
 */
public class WxCpMsgMaker extends BaseMsgMaker implements IMsgMaker {

    private String agentId;

    private String msgType;

    private String msgTitle;

    private String picUrl;

    private String desc;

    private String url;

    private String btnTxt;

    private String msgContent;

    private String miniProgramAppId;

    private String miniProgramPage;

    private Boolean miniProgramEmphasisFirstItem;

    private List<WxCpMiniProgramContentItem> miniProgramContentItems;

    private Boolean enableIdTrans;

    private Boolean enableDuplicateCheck;

    private Integer duplicateCheckInterval;

    private WxCpAccountConfig wxCpAccountConfig;

    private static TAccountMapper accountMapper = MybatisUtil.getSqlSession().getMapper(TAccountMapper.class);

    public WxCpMsgMaker(TMsg tMsg) {
        TMsgWxCp tMsgWxCp = JSON.parseObject(tMsg.getContent(), TMsgWxCp.class);

        TAccount tAccount = accountMapper.selectByPrimaryKey(tMsg.getAccountId());
        String accountConfig = tAccount.getAccountConfig();
        wxCpAccountConfig = JSON.parseObject(accountConfig, WxCpAccountConfig.class);
        agentId = wxCpAccountConfig.getAgentId();

        msgType = tMsgWxCp.getCpMsgType();
        msgTitle = tMsgWxCp.getTitle();
        picUrl = tMsgWxCp.getImgUrl();
        desc = tMsgWxCp.getDescribe();
        url = tMsgWxCp.getUrl();
        btnTxt = tMsgWxCp.getBtnTxt();
        msgContent = tMsgWxCp.getContent();
        miniProgramAppId = tMsgWxCp.getMiniProgramAppId();
        miniProgramPage = tMsgWxCp.getMiniProgramPage();
        miniProgramEmphasisFirstItem = tMsgWxCp.getMiniProgramEmphasisFirstItem();
        miniProgramContentItems = tMsgWxCp.getMiniProgramContentItems();
        enableIdTrans = tMsgWxCp.getEnableIdTrans();
        enableDuplicateCheck = tMsgWxCp.getEnableDuplicateCheck();
        duplicateCheckInterval = tMsgWxCp.getDuplicateCheckInterval();
    }

    /**
     * 组织消息-企业号
     *
     * @param msgData 消息数据
     * @return WxMpTemplateMessage
     */
    @Override
    public WxCpMessage makeMsg(String[] msgData) {

        WxCpMessage wxCpMessage = null;
        VelocityContext velocityContext = getVelocityContext(msgData);
        if ("图文消息".equals(msgType)) {
            NewArticle article = new NewArticle();

            // 标题
            String title = TemplateUtil.evaluate(msgTitle, velocityContext);
            article.setTitle(title);

            // 图片url
            article.setPicUrl(TemplateUtil.evaluate(picUrl, velocityContext));

            // 描述
            String description = TemplateUtil.evaluate(desc, velocityContext);
            article.setDescription(description);

            // 跳转url
            article.setUrl(TemplateUtil.evaluate(url, velocityContext));

            wxCpMessage = WxCpMessage.NEWS().addArticle(article).build();
        } else if ("文本消息".equals(msgType)) {
            String content = TemplateUtil.evaluate(msgContent, velocityContext);
            wxCpMessage = WxCpMessage.TEXT().agentId(Integer.valueOf(agentId)).toUser(msgData[0]).content(content).build();
        } else if ("markdown消息".equals(msgType)) {
            String content = TemplateUtil.evaluate(msgContent, velocityContext);
            wxCpMessage = WxCpMessage.MARKDOWN().agentId(Integer.valueOf(agentId)).toUser(msgData[0]).content(content).build();
        } else if ("文本卡片消息".equals(msgType)) {
            // 标题
            String title = TemplateUtil.evaluate(msgTitle, velocityContext);
            // 描述
            String description = TemplateUtil.evaluate(desc, velocityContext);
            // 跳转url
            String urlLink = TemplateUtil.evaluate(url, velocityContext);
            wxCpMessage = WxCpMessage.TEXTCARD().agentId(Integer.valueOf(agentId)).toUser(msgData[0]).title(title)
                    .description(description).url(urlLink).btnTxt(btnTxt).build();
        } else if (WxCpMiniProgramNoticeSupport.MESSAGE_TYPE.equals(msgType)) {
            WxCpMiniProgramNoticeSupport.validateRecipient(msgData.length == 0 ? "" : msgData[0]);
            String appId = TemplateUtil.evaluate(StringUtils.defaultString(miniProgramAppId), velocityContext).trim();
            String page = TemplateUtil.evaluate(StringUtils.defaultString(miniProgramPage), velocityContext).trim();
            String title = TemplateUtil.evaluate(StringUtils.defaultString(msgTitle), velocityContext).trim();
            String description = TemplateUtil.evaluate(StringUtils.defaultString(desc), velocityContext).trim();

            List<WxCpMiniProgramContentItem> renderedItems = new ArrayList<>();
            if (miniProgramContentItems != null) {
                for (WxCpMiniProgramContentItem item : miniProgramContentItems) {
                    if (item == null) {
                        continue;
                    }
                    renderedItems.add(new WxCpMiniProgramContentItem(
                            TemplateUtil.evaluate(StringUtils.defaultString(item.getKey()), velocityContext),
                            TemplateUtil.evaluate(StringUtils.defaultString(item.getValue()), velocityContext)));
                }
            }
            LinkedHashMap<String, String> contentItems = WxCpMiniProgramNoticeSupport.validateAndBuildContentItems(
                    appId, page, title, description, renderedItems, enableDuplicateCheck, duplicateCheckInterval);

            MiniProgramNoticeMsgBuilder builder = WxCpMessage.newMiniProgramNoticeBuilder()
                    .agentId(Integer.valueOf(agentId))
                    .toUser(msgData[0])
                    .appId(appId)
                    .title(title)
                    .contentItems(contentItems)
                    .emphasisFirstItem(Boolean.TRUE.equals(miniProgramEmphasisFirstItem));
            if (StringUtils.isNotBlank(page)) {
                builder.page(page);
            }
            if (StringUtils.isNotBlank(description)) {
                builder.description(description);
            }
            wxCpMessage = builder.build();
            wxCpMessage.setEnableIdTrans(Boolean.TRUE.equals(enableIdTrans));
            wxCpMessage.setEnableDuplicateCheck(Boolean.TRUE.equals(enableDuplicateCheck));
            if (Boolean.TRUE.equals(enableDuplicateCheck)) {
                wxCpMessage.setDuplicateCheckInterval(duplicateCheckInterval == null
                        ? WxCpMiniProgramNoticeSupport.DEFAULT_DUPLICATE_CHECK_INTERVAL : duplicateCheckInterval);
            }
        }

        return wxCpMessage;
    }
}

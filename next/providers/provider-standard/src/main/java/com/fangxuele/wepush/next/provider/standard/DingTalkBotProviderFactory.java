package com.fangxuele.wepush.next.provider.standard;

public final class DingTalkBotProviderFactory extends AbstractBotProviderFactory {
    public static final String PROVIDER_ID = "wepush.bot.dingtalk";
    public static final String VERSION = AbstractBotProviderFactory.VERSION;

    public DingTalkBotProviderFactory() { super(BotVendor.DINGTALK); }

    DingTalkBotProviderFactory(boolean allowTestEndpoints) { super(BotVendor.DINGTALK, allowTestEndpoints); }
}

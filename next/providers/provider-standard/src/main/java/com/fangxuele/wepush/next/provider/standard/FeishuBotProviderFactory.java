package com.fangxuele.wepush.next.provider.standard;

public final class FeishuBotProviderFactory extends AbstractBotProviderFactory {
    public static final String PROVIDER_ID = "wepush.bot.feishu";
    public static final String VERSION = AbstractBotProviderFactory.VERSION;

    public FeishuBotProviderFactory() { super(BotVendor.FEISHU); }

    FeishuBotProviderFactory(boolean allowTestEndpoints) { super(BotVendor.FEISHU, allowTestEndpoints); }
}

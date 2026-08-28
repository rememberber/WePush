package com.fangxuele.wepush.next.provider.standard;

public final class WeComBotProviderFactory extends AbstractBotProviderFactory {
    public static final String PROVIDER_ID = "wepush.bot.wecom";
    public static final String VERSION = AbstractBotProviderFactory.VERSION;

    public WeComBotProviderFactory() { super(BotVendor.WECOM); }

    WeComBotProviderFactory(boolean allowTestEndpoints) { super(BotVendor.WECOM, allowTestEndpoints); }
}

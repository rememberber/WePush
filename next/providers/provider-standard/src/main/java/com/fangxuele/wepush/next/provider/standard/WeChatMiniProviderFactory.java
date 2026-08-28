package com.fangxuele.wepush.next.provider.standard;

import java.net.URI;

public final class WeChatMiniProviderFactory extends AbstractWeChatProviderFactory {
    public static final String PROVIDER_ID = "wepush.wechat.mini";

    public WeChatMiniProviderFactory() { super(WeChatPlatform.MINI); }

    WeChatMiniProviderFactory(URI apiBase) { super(WeChatPlatform.MINI, apiBase); }
}

package com.fangxuele.wepush.next.provider.standard;

import java.net.URI;

public final class WeChatOfficialProviderFactory extends AbstractWeChatProviderFactory {
    public static final String PROVIDER_ID = "wepush.wechat.official";

    public WeChatOfficialProviderFactory() { super(WeChatPlatform.OFFICIAL); }

    WeChatOfficialProviderFactory(URI apiBase) { super(WeChatPlatform.OFFICIAL, apiBase); }
}

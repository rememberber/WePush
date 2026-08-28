package com.fangxuele.wepush.next.provider.standard;

import java.net.URI;

public final class WeComAppProviderFactory extends AbstractWeChatProviderFactory {
    public static final String PROVIDER_ID = "wepush.wecom.app";

    public WeComAppProviderFactory() { super(WeChatPlatform.WECOM_APP); }

    WeComAppProviderFactory(URI apiBase) { super(WeChatPlatform.WECOM_APP, apiBase); }
}

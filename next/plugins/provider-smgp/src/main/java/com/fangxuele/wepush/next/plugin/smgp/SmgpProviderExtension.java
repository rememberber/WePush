package com.fangxuele.wepush.next.plugin.smgp;

import com.fangxuele.wepush.next.agent.plugin.ProviderFactoryExtension;
import com.fangxuele.wepush.next.plugin.carriersms.CarrierProtocol;
import com.fangxuele.wepush.next.plugin.carriersms.CarrierSmsProviderFactory;
import com.fangxuele.wepush.next.provider.spi.ProviderFactory;
import org.pf4j.Extension;

@Extension
public final class SmgpProviderExtension implements ProviderFactoryExtension {
    @Override public ProviderFactory factory() { return new CarrierSmsProviderFactory(CarrierProtocol.SMGP); }
}

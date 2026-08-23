package com.fangxuele.wepush.next.agent.plugin;

import com.fangxuele.wepush.next.provider.spi.ProviderFactory;
import org.pf4j.ExtensionPoint;

/** PF4J boundary adapter; Core and Provider SPI remain independent from PF4J. */
public interface ProviderFactoryExtension extends ExtensionPoint {
    ProviderFactory factory();
}

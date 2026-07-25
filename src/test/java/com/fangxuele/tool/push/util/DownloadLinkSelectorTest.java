package com.fangxuele.tool.push.util;

import com.jayway.jsonpath.JsonPath;
import org.junit.Assert;
import org.junit.Test;

public class DownloadLinkSelectorTest {

    private static final String LINKS = """
            {
              "windows": "https://example.com/win.exe",
              "macSilicon": "https://example.com/mac-arm.dmg",
              "mac": "https://example.com/mac-intel.dmg",
              "linux": "https://example.com/linux.deb"
            }
            """;

    @Test
    public void selectWindows() {
        Assert.assertEquals(
                "https://example.com/win.exe",
                DownloadLinkSelector.select("Windows 10", "amd64", JsonPath.parse(LINKS)));
    }

    @Test
    public void selectMacAppleSilicon() {
        Assert.assertEquals(
                "https://example.com/mac-arm.dmg",
                DownloadLinkSelector.select("Mac OS X", "aarch64", JsonPath.parse(LINKS)));
    }

    @Test
    public void selectMacIntel() {
        Assert.assertEquals(
                "https://example.com/mac-intel.dmg",
                DownloadLinkSelector.select("Mac OS X", "x86_64", JsonPath.parse(LINKS)));
    }

    @Test
    public void selectLinux() {
        Assert.assertEquals(
                "https://example.com/linux.deb",
                DownloadLinkSelector.select("Linux", "amd64", JsonPath.parse(LINKS)));
    }
}

package com.fangxuele.tool.push.util;

import com.fangxuele.tool.push.bean.VersionSummary;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class UpgradeUtilTest {

    @Test
    public void versionChangesAfterUsesIndexOrder() {
        VersionSummary summary = new VersionSummary();
        summary.setCurrentVersion("v_5.0.2");
        summary.setVersionIndex("{\"v_5.0.0\":\"1\",\"v_5.0.1\":\"2\",\"v_5.0.2\":\"3\"}");

        VersionSummary.Version v1 = new VersionSummary.Version();
        v1.setVersion("v_5.0.1");
        v1.setTitle("one");
        v1.setLog("a");
        VersionSummary.Version v2 = new VersionSummary.Version();
        v2.setVersion("v_5.0.2");
        v2.setTitle("two");
        v2.setLog("b");
        VersionSummary.Version v0 = new VersionSummary.Version();
        v0.setVersion("v_5.0.0");
        v0.setTitle("zero");
        v0.setLog("z");
        summary.setVersionDetailList(List.of(v0, v2, v1));

        List<VersionSummary.Version> changes = UpgradeUtil.versionChangesAfter(summary, "v_5.0.0");
        Assert.assertEquals(2, changes.size());
        Assert.assertEquals("v_5.0.1", changes.get(0).getVersion());
        Assert.assertEquals("v_5.0.2", changes.get(1).getVersion());
    }

    @Test
    public void versionChangesAfterReturnsEmptyWhenAlreadyLatest() {
        VersionSummary summary = new VersionSummary();
        summary.setCurrentVersion("v_5.0.1");
        summary.setVersionIndex("{\"v_5.0.0\":\"1\",\"v_5.0.1\":\"2\"}");
        VersionSummary.Version v1 = new VersionSummary.Version();
        v1.setVersion("v_5.0.1");
        v1.setTitle("one");
        v1.setLog("a");
        summary.setVersionDetailList(List.of(v1));

        Assert.assertTrue(UpgradeUtil.versionChangesAfter(summary, "v_5.0.1").isEmpty());
    }
}

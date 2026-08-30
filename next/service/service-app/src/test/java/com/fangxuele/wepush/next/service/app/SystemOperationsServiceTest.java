package com.fangxuele.wepush.next.service.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SystemOperationsServiceTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void selectsNewestStableNextReleaseAndIgnoresClassicDraftsAndPrereleases() throws Exception {
        var selected = SystemOperationsService.selectNextStableRelease(json.readTree("""
                [
                  {"tag_name":"v3.0.0","html_url":"https://example/classic"},
                  {"tag_name":"next-v1.2.0","draft":true},
                  {"tag_name":"next-v1.1.1","prerelease":true},
                  {"tag_name":"next-v1.0.0","html_url":"https://example/1.0.0"},
                  {"tag_name":"next-v1.1.0","html_url":"https://example/1.1.0"}
                ]
                """));

        assertEquals("next-v1.1.0", selected.path("tag_name").asText());
        assertEquals("https://example/1.1.0", selected.path("html_url").asText());
    }

    @Test
    void returnsMissingNodeWhenNoStableNextReleaseExists() throws Exception {
        var selected = SystemOperationsService.selectNextStableRelease(json.readTree("""
                [{"tag_name":"v3.0.0"},{"tag_name":"next-v1.2.0","prerelease":true}]
                """));

        assertTrue(selected.isMissingNode());
    }
}

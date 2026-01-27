package com.github.lankalana.crawler4j.util;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.github.lankalana.crawler4j.crawler.CrawlConfig;
import com.github.lankalana.crawler4j.url.WebURL;

public class NetTest {

    private static final Net STANDARD = new Net(new CrawlConfig(), null);
    private static final Net ALLOW_SINGLE_LEVEL_DOMAIN;

    static {
        CrawlConfig config = new CrawlConfig();
        config.setAllowSingleLevelDomain(true);
        ALLOW_SINGLE_LEVEL_DOMAIN = new Net(config, null);
    }

    @Test
    public void testNoSchemeSpecified() {
        Set<WebURL> extracted = STANDARD.extractUrls("www.wikipedia.com");
        expectMatch(extracted, "http://www.wikipedia.com/");
    }

    @Test
    public void testLocalhost() {
        Set<WebURL> extracted = ALLOW_SINGLE_LEVEL_DOMAIN.extractUrls("http://localhost/page/1");
        expectMatch(extracted, "http://localhost/page/1");
    }

    @Test
    public void testNoUrlFound() {
        Set<WebURL> extracted = STANDARD.extractUrls("http://localhost");
        expectMatch(extracted);
    }

    @Test
    public void testMultipleUrls() {
        String unicodeDomain = "http://\u4f8b\u5b50.\u6d4b\u8bd5/";
        Set<WebURL> extracted = STANDARD.extractUrls(
                " hey com check out host.com/toodles and http://\u4f8b\u5b50.\u6d4b\u8bd5 real soon ");
        expectMatch(extracted, "http://host.com/toodles", unicodeDomain);
    }

    private void expectMatch(Set<WebURL> extractedUrls, String... expectedUrls) {
        Set<String> extracted = new HashSet<>();
        for (WebURL url : extractedUrls) {
            extracted.add(url.getURL());
        }

        Set<String> expected = new HashSet<>();
        for (String url : expectedUrls) {
            expected.add(url);
        }

        assertEquals(expected, extracted);
    }
}

package edu.uci.ics.crawler4j.url;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;

public class TLDListOnlineTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(new WireMockConfiguration().dynamicPort());

    @Test
    public void testDownloadTldFromUrl() throws Exception {
        stubFor(get(urlEqualTo("/tld-names.txt"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("fakeprovince.ca")));

        CrawlConfig config = new CrawlConfig();
        config.setOnlineTldListUpdate(true);
        config.setPublicSuffixSourceUrl("http://localhost:" + wireMockRule.port() + "/tld-names.txt");
        TLDList tldList = new TLDList(config);

        assertTrue(tldList.contains("fakeprovince.ca"));
        assertFalse(tldList.contains("on.ca"));
        verify(1, getRequestedFor(urlEqualTo("/tld-names.txt")));
    }
}

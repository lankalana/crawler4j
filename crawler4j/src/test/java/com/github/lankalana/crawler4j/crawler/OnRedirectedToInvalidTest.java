package com.github.lankalana.crawler4j.crawler;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.lankalana.crawler4j.fetcher.PageFetcher;
import com.github.lankalana.crawler4j.robotstxt.RobotstxtConfig;
import com.github.lankalana.crawler4j.robotstxt.RobotstxtServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class OnRedirectedToInvalidTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(new WireMockConfiguration().dynamicPort());

    @Test
    public void testInterceptRedirectToInvalidUrl() throws Exception {
        int[] redirectHttpCodes = new int[] {300, 301, 302, 303, 307, 308};
        for (int redirectHttpCode : redirectHttpCodes) {
            runRedirectTest(redirectHttpCode);
        }
    }

    private void runRedirectTest(int redirectHttpCode) throws Exception {
        WireMock.reset();

        String redirectToNothing = "asd://-invalid-/varybadlocation";

        stubFor(get(urlEqualTo("/some/index.html"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html>\n" +
                                "        <head>\n" +
                                "            <meta charset=\"UTF-8\">\n" +
                                "        </head>\n" +
                                "        <body> \n" +
                                "            <a href=\"/some/redirect.html\">link to a redirected page to nothing</a>\n" +
                                "        </body>\n" +
                                "       </html>")));

        stubFor(get(urlPathMatching("/some/redirect.html"))
                .willReturn(aResponse()
                        .withStatus(redirectHttpCode)
                        .withHeader("Content-Type", "text/html")
                        .withHeader("Location", redirectToNothing)
                        .withBody("<html>\n" +
                                "    <head>\n" +
                                "        <title>Moved</title>\n" +
                                "    </head>\n" +
                                "    <body>\n" +
                                "        <h1>Moved</h1>\n" +
                                "        <p>This page has moved to <a href=\"" + redirectToNothing + "\">Some invalid location</a>.</p>\n" +
                                "    </body>\n" +
                                "    </html>")));

        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(temp.getRoot().getAbsolutePath());
        config.setPolitenessDelay(0);
        config.setMaxConnectionsPerHost(1);
        config.setThreadShutdownDelaySeconds(1);
        config.setThreadMonitoringDelaySeconds(1);
        config.setCleanupDelaySeconds(1);

        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        robotstxtConfig.setEnabled(false);
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);
        controller.addSeed("http://localhost:" + wireMockRule.port() + "/some/index.html");

        HandleInvalidRedirectWebCrawler crawler = new HandleInvalidRedirectWebCrawler();
        controller.start(crawler);

        Assert.assertEquals("/some/redirect.html", crawler.invalidLocation);
    }

    public static class HandleInvalidRedirectWebCrawler extends WebCrawler {

        private String invalidLocation;

        @Override
        protected void onRedirectedToInvalidUrl(Page page) {
            super.onRedirectedToInvalidUrl(page);
            invalidLocation = page.getWebURL().getPath();
        }
    }
}

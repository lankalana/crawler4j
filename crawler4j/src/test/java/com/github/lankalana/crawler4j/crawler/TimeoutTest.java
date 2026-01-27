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
import com.github.lankalana.crawler4j.url.WebURL;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class TimeoutTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(new WireMockConfiguration().dynamicPort());

    @Test
    public void testInterceptSocketTimeoutException() throws Exception {
        stubFor(get(urlEqualTo("/some/index.html"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html>\n" +
                                "    <body> \n" +
                                "        <a href=\"/some/page1.html\">link to normal page</a>\n" +
                                "        <a href=\"/some/page2.html\">link to noindex page</a>\n" +
                                "    </body>\n" +
                                "   </html>")
                        .withFixedDelay(200)));

        stubFor(get(urlPathMatching("/robots.txt"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("User-agent: * \n  Allow: /\n")));

        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(temp.getRoot().getAbsolutePath());
        config.setPolitenessDelay(0);
        config.setMaxConnectionsPerHost(1);
        config.setThreadShutdownDelaySeconds(1);
        config.setThreadMonitoringDelaySeconds(1);
        config.setCleanupDelaySeconds(1);
        config.setConnectionTimeout(50);
        config.setSocketTimeout(50);

        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtServer robotstxtServer = new RobotstxtServer(new RobotstxtConfig(), pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);
        controller.addSeed("http://localhost:" + wireMockRule.port() + "/some/index.html");

        controller.start(VisitAllCrawler.class, 1);

        Page page = (Page) controller.getCrawlersLocalData().get(0);
        Assert.assertEquals(0, page.getStatusCode());
        Assert.assertEquals(0, page.getFetchResponseHeaders().length);
    }

    @Test
    public void testResponseCodeAndHeaderPresentWhenReadTimeout() throws Exception {
        stubFor(get(urlEqualTo("/some/index.html"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html>\n" +
                                "    <head>\n" +
                                "        <meta charset=\"UTF-8\">\n" +
                                "    </head>\n" +
                                "    <body> \n" +
                                "        <title>Hello, world!</title>\n" +
                                "    </body>\n" +
                                "   </html>")
                        .withChunkedDribbleDelay(5, 300)));

        stubFor(get(urlPathMatching("/robots.txt"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/plain")
                        .withBody("User-agent: * \n  Allow: /\n")));

        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(temp.getRoot().getAbsolutePath());
        config.setPolitenessDelay(0);
        config.setMaxConnectionsPerHost(1);
        config.setThreadShutdownDelaySeconds(1);
        config.setThreadMonitoringDelaySeconds(1);
        config.setCleanupDelaySeconds(1);
        config.setConnectionTimeout(100);
        config.setSocketTimeout(100);

        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtServer robotstxtServer = new RobotstxtServer(new RobotstxtConfig(), pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);
        controller.addSeed("http://localhost:" + wireMockRule.port() + "/some/index.html");

        controller.start(VisitAllCrawler.class, 1);

        Page page = (Page) controller.getCrawlersLocalData().get(0);
        Assert.assertEquals(200, page.getStatusCode());
        Assert.assertTrue(page.getFetchResponseHeaders().length > 1);
    }

    public static class VisitAllCrawler extends WebCrawler {

        private Page page;

        @Override
        protected boolean shouldFollowLinksIn(WebURL url) {
            return true;
        }

        @Override
        protected void onContentFetchError(Page page) {
            this.page = page;
        }

        @Override
        public Object getMyLocalData() {
            return page;
        }
    }
}

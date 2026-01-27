package com.github.lankalana.crawler4j.crawler;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.lankalana.crawler4j.fetcher.PageFetcher;
import com.github.lankalana.crawler4j.robotstxt.RobotstxtConfig;
import com.github.lankalana.crawler4j.robotstxt.RobotstxtServer;
import com.github.lankalana.crawler4j.url.WebURL;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class WebCrawlerTest {

    private static final String PAGE_WHICH_LINKS_MUST_NOT_BE_VISITED = "page2.html";
    private static final String PAGE_UNVISITED = "page4.html";

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(new WireMockConfiguration().dynamicPort());

    @Test
    public void testIgnoreLinksContainedInGivenPage() throws Exception {
        stubFor(get(urlEqualTo("/some/index.html"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html>\n" +
                                "    <head>\n" +
                                "        <meta charset=\"UTF-8\">\n" +
                                "    </head>\n" +
                                "    <body> \n" +
                                "        <a href=\"/some/page1.html\">a link</a>\n" +
                                "        <a href=\"/some/" + PAGE_WHICH_LINKS_MUST_NOT_BE_VISITED + "\">ignore links in this page</a>\n" +
                                "        <a href=\"/some/page3.html\">a link</a> \n" +
                                "    </body>\n" +
                                "   </html>")));
        stubFor(get(urlPathMatching("/some/page([1,3,4]*).html"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html>\n" +
                                "    <head>\n" +
                                "        <meta charset=\"UTF-8\">\n" +
                                "    </head>\n" +
                                "    <body>\n" +
                                "        <h1>title</h1>\n" +
                                "    </body>\n" +
                                "  </html>")));
        stubFor(get(urlPathMatching("/some/" + PAGE_WHICH_LINKS_MUST_NOT_BE_VISITED))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html>\n" +
                                "    <head>\n" +
                                "        <meta charset=\"UTF-8\">\n" +
                                "    </head>\n" +
                                "    <body>\n" +
                                "        <a href=\"/some/" + PAGE_UNVISITED + "\">should not visit this</a>\n" +
                                "    </body>\n" +
                                "  </html>")));

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

        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtServer robotstxtServer = new RobotstxtServer(new RobotstxtConfig(), pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);
        controller.addSeed("http://localhost:" + wireMockRule.port() + "/some/index.html");

        controller.start(ShouldNotVisitPageWebCrawler.class, 1);

        verify(1, getRequestedFor(urlEqualTo("/robots.txt")));
        verify(1, getRequestedFor(urlEqualTo("/some/page1.html")));
        verify(1, getRequestedFor(urlEqualTo("/some/" + PAGE_WHICH_LINKS_MUST_NOT_BE_VISITED)));
        verify(1, getRequestedFor(urlEqualTo("/some/page3.html")));
        verify(0, getRequestedFor(urlEqualTo("/some/" + PAGE_UNVISITED)));
    }

    public static class ShouldNotVisitPageWebCrawler extends WebCrawler {

        @Override
        protected boolean shouldFollowLinksIn(WebURL url) {
            return !url.getPath().endsWith(WebCrawlerTest.PAGE_WHICH_LINKS_MUST_NOT_BE_VISITED);
        }
    }
}

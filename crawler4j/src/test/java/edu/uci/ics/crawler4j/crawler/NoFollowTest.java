package edu.uci.ics.crawler4j.crawler;

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

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class NoFollowTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(new WireMockConfiguration().dynamicPort());

    @Test
    public void testIgnoreNofollowLinks() throws Exception {
        stubFor(get(urlEqualTo("/some/index.html"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html>\n" +
                                "    <head>\n" +
                                "      <meta charset=\"UTF-8\">\n" +
                                "    </head>\n" +
                                "    <body> \n" +
                                "        <a href=\"/some/page1.html\" rel=\"nofollow\">should not visit this</a>\n" +
                                "        <a href=\"/some/page2.html\">link to a nofollow page</a>\n" +
                                "    </body>\n" +
                                "   </html>")));
        stubFor(get(urlPathMatching("/some/page(1|3).html"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html>\n" +
                                "    <head>\n" +
                                "      <meta charset=\"UTF-8\">\n" +
                                "    </head>\n" +
                                "    <body>\n" +
                                "        <h1>title</h1>\n" +
                                "    </body>\n" +
                                "  </html>")));
        stubFor(get(urlPathMatching("/some/page2.html"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html>\n" +
                                "    <head>\n" +
                                "      <meta charset=\"UTF-8\">\n" +
                                "      <meta name=\"robots\" content=\"nofollow\">\n" +
                                "    </head>\n" +
                                "    <body>\n" +
                                "        <a href=\"/some/page3.html\">should not visit this</a>\n" +
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

        controller.start(WebCrawler.class, 1);

        verify(1, getRequestedFor(urlEqualTo("/robots.txt")));
        verify(0, getRequestedFor(urlEqualTo("/some/page1.html")));
        verify(1, getRequestedFor(urlEqualTo("/some/page2.html")));
        verify(0, getRequestedFor(urlEqualTo("/some/page3.html")));
    }
}

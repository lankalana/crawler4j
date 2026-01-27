package com.github.lankalana.crawler4j.crawler;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import com.github.lankalana.crawler4j.fetcher.PageFetcher;
import com.github.lankalana.crawler4j.robotstxt.RobotstxtConfig;
import com.github.lankalana.crawler4j.robotstxt.RobotstxtServer;

public class CrawlerWithJSTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(new WireMockConfiguration().dynamicPort());

    @Test
    public void testVisitJavascriptFiles() throws Exception {
        stubFor(get(urlEqualTo("/some/index.html"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html>\n" +
                                "    <head>\n" +
                                "        <meta charset=\"UTF-8\">\n" +
                                "        <script src=\"/js/app.js\"></script>\n" +
                                "    </head>\n" +
                                "    <body> \n" +
                                "        <a href=\"/some/page1.html\">a link</a>\n" +
                                "        <a href=\"/some/page2.html\">a link</a>\n" +
                                "    </body>\n" +
                                "   </html>")));

        stubFor(get(urlPathMatching("/some/page1.html"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html>\n" +
                                "    <head>\n" +
                                "        <meta charset=\"UTF-8\">\n" +
                                "        <script src=\"/js/app.js\"></script>\n" +
                                "    </head> \n" +
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
                                "        <meta charset=\"UTF-8\">\n" +
                                "        <script src=\"/js/app.js\"></script>\n" +
                                "    </head> \n" +
                                "    <body>\n" +
                                "        <h1>title</h1>\n" +
                                "        <script src=\"/js/module1.js\"></script>\n" +
                                "    </body>\n" +
                                "  </html>")));
        stubFor(get(urlPathMatching("/js/app.js"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/javascript")
                        .withBody("\n" +
                                "    function greetings() {\n" +
                                "        alert('Hello, world!');\n" +
                                "    }\n" +
                                "\n" +
                                "    greetings();\n")));
        stubFor(get(urlPathMatching("/js/module1.js"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/javascript")
                        .withBody("\n" +
                                "    // This is the source of the module\n")));

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

        controller.start(ShouldWebCrawler.class, 1);

        verify(1, getRequestedFor(urlEqualTo("/robots.txt")));
        verify(1, getRequestedFor(urlEqualTo("/js/app.js")));
        verify(1, getRequestedFor(urlEqualTo("/js/module1.js")));
    }

    public static class ShouldWebCrawler extends WebCrawler {
    }
}

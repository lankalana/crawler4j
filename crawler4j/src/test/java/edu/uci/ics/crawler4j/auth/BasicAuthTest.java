package edu.uci.ics.crawler4j.auth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import java.net.InetAddress;
import java.util.Collections;

import org.apache.http.impl.conn.InMemoryDnsResolver;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.crawler.authentication.BasicAuthInfo;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class BasicAuthTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(new WireMockConfiguration().dynamicPort());

    @Test
    public void testHttpBasicAuth() throws Exception {
        stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withHeader("Host", "first.com")
                        .withBody("<html>\n" +
                                "    <head>\n" +
                                "        <meta charset=\"UTF-8\">\n" +
                                "        <title>Landing page</title>\n" +
                                "    </head>\n" +
                                "    <body> \n" +
                                "        <h1>Title</h1>\n" +
                                "        <a href=\"/some/index.html\">a link!</a>\n" +
                                "    </body>\n" +
                                "   </html>")));
        stubFor(get(urlEqualTo("/some/index.html"))
                .withBasicAuth("user", "pass")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withHeader("Host", "first.com")
                        .withBody("<html>\n" +
                                "    <head>\n" +
                                "        <meta charset=\"UTF-8\">\n" +
                                "        <title>Hello, world!</title>\n" +
                                "    </head>\n" +
                                "    <body> \n" +
                                "        <h1>Sub page</h1>\n" +
                                "    </body>\n" +
                                "   </html>")));

        stubFor(get(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withHeader("Host", "first.com")
                        .withBody("<html>\n" +
                                "    <head>\n" +
                                "        <meta charset=\"UTF-8\">\n" +
                                "        <title>Landing page</title>\n" +
                                "    </head>\n" +
                                "    <body> \n" +
                                "        <h1>Title</h1>\n" +
                                "        <a href=\"/some/index.html\">a link!</a>\n" +
                                "    </body>\n" +
                                "   </html>")));
        stubFor(get(urlEqualTo("/some/index.html"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withHeader("Host", "first.com")
                        .withBody("<html>\n" +
                                "    <head>\n" +
                                "        <meta charset=\"UTF-8\">\n" +
                                "        <title>Hello, world!</title>\n" +
                                "    </head>\n" +
                                "    <body> \n" +
                                "        <h1>Sub page</h1>\n" +
                                "    </body>\n" +
                                "   </html>")));

        InMemoryDnsResolver inMemDnsResolver = new InMemoryDnsResolver();
        inMemDnsResolver.add("first.com", InetAddress.getByName("127.0.0.1"));
        inMemDnsResolver.add("second.com", InetAddress.getByName("127.0.0.1"));

        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(temp.newFolder().getAbsolutePath());
        config.setMaxPagesToFetch(10);
        config.setPolitenessDelay(0);
        config.setThreadShutdownDelaySeconds(1);
        config.setThreadMonitoringDelaySeconds(0);
        config.setCleanupDelaySeconds(0);
        BasicAuthInfo basicAuthInfo = new BasicAuthInfo(
                "user", "pass",
                "http://first.com:" + wireMockRule.port() + "/"
        );
        config.setDnsResolver(inMemDnsResolver);
        config.setAuthInfos(Collections.singletonList(basicAuthInfo));

        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        robotstxtConfig.setEnabled(false);
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

        controller.addSeed("http://first.com:" + wireMockRule.port() + "/");
        controller.addSeed("http://second.com:" + wireMockRule.port() + "/");
        controller.start(WebCrawler.class, 1);

        verify(1, getRequestedFor(urlEqualTo("/some/index.html"))
                .withBasicAuth(new BasicCredentials("user", "pass"))
                .withHeader("Host", new EqualToPattern("first.com:" + wireMockRule.port())));
        verify(1, getRequestedFor(urlEqualTo("/"))
                .withBasicAuth(new BasicCredentials("user", "pass"))
                .withHeader("Host", new EqualToPattern("first.com:" + wireMockRule.port())));

        verify(1, getRequestedFor(urlEqualTo("/some/index.html"))
                .withHeader("Host", new EqualToPattern("second.com:" + wireMockRule.port())));
        verify(1, getRequestedFor(urlEqualTo("/"))
                .withHeader("Host", new EqualToPattern("second.com:" + wireMockRule.port())));
    }
}

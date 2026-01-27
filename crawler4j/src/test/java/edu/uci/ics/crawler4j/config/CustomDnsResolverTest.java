package edu.uci.ics.crawler4j.config;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import java.net.InetAddress;

import org.apache.http.impl.conn.InMemoryDnsResolver;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class CustomDnsResolverTest {

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
                                "        <title>Hello, world!</title>\n" +
                                "    </head>\n" +
                                "    <body> \n" +
                                "        <h1>Title</h1>\n" +
                                "    </body>\n" +
                                "   </html>")));

        InMemoryDnsResolver inMemDnsResolver = new InMemoryDnsResolver();
        inMemDnsResolver.add("googhle.com", InetAddress.getByName("127.0.0.1"));

        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(temp.newFolder().getAbsolutePath());
        config.setMaxPagesToFetch(10);
        config.setPolitenessDelay(0);
        config.setThreadShutdownDelaySeconds(1);
        config.setThreadMonitoringDelaySeconds(0);
        config.setCleanupDelaySeconds(0);
        config.setDnsResolver(inMemDnsResolver);

        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        robotstxtConfig.setEnabled(false);
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

        controller.addSeed("http://googhle.com:" + wireMockRule.port() + "/some/index.html");
        controller.start(WebCrawler.class, 1);

        verify(1, getRequestedFor(urlEqualTo("/some/index.html")));
    }
}

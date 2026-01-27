package com.github.lankalana.crawler4j.crawler;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.lankalana.crawler4j.fetcher.PageFetcher;
import com.github.lankalana.crawler4j.robotstxt.RobotstxtConfig;
import com.github.lankalana.crawler4j.robotstxt.RobotstxtServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class NoIndexTest {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Rule
	public WireMockRule wireMockRule = new WireMockRule(new WireMockConfiguration().dynamicPort());

	@Test
	public void testIgnoreNoindexPages() throws Exception {
		stubFor(get(urlEqualTo("/some/index.html"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "text/html")
						.withBody("<html>\n" + "    <head>\n"
								+ "        <meta charset=\"UTF-8\">\n"
								+ "    </head>\n"
								+ "    <body> \n"
								+ "        <a href=\"/some/page1.html\">link to normal page</a>\n"
								+ "        <a href=\"/some/page2.html\">link to noindex page</a>\n"
								+ "    </body>\n"
								+ "   </html>")));
		stubFor(get(urlPathMatching("/some/page1.html"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "text/html")
						.withBody("<html>\n" + "    <head>\n"
								+ "        <meta charset=\"UTF-8\">\n"
								+ "    </head>\n"
								+ "    <body>\n"
								+ "        <h1>title</h1>\n"
								+ "    </body>\n"
								+ "  </html>")));
		stubFor(get(urlPathMatching("/some/page2.html"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "text/html")
						.withBody("<html>\n" + "    <head>\n"
								+ "      <meta charset=\"UTF-8\">\n"
								+ "      <meta name=\"robots\" content=\"noindex, nofollow\">\n"
								+ "    </head>\n"
								+ "    <body>\n"
								+ "        <p>This is a paragraph.</p>\n"
								+ "    </body>\n"
								+ "  </html>")));

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

		controller.start(NoIndexWebCrawler.class, 1);

		@SuppressWarnings("unchecked")
		Map<String, Page> visitedPages =
				(Map<String, Page>) controller.getCrawlersLocalData().get(0);
		Assert.assertTrue(visitedPages.containsKey("http://localhost:" + wireMockRule.port() + "/some/index.html"));
		Assert.assertTrue(visitedPages.containsKey("http://localhost:" + wireMockRule.port() + "/some/page1.html"));
		Assert.assertFalse(visitedPages.containsKey("http://localhost:" + wireMockRule.port() + "/some/page2.html"));
	}

	public static class NoIndexWebCrawler extends WebCrawler {

		private Map<String, Page> visitedPages;

		@Override
		public void init(int id, CrawlController crawlController)
				throws InstantiationException, IllegalAccessException {
			super.init(id, crawlController);
			visitedPages = new HashMap<>();
		}

		@Override
		public void visit(Page page) {
			visitedPages.put(page.getWebURL().toString(), page);
		}

		@Override
		public Object getMyLocalData() {
			return visitedPages;
		}
	}
}

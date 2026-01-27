package com.github.lankalana.crawler4j.crawler;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import java.util.ArrayList;
import java.util.List;

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

public class RedirectHandlerTest {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Rule
	public WireMockRule wireMockRule = new WireMockRule(new WireMockConfiguration().dynamicPort());

	@Test
	public void testFollowRedirects() throws Exception {
		int[] redirectStatuses = new int[] {301, 302};
		for (int redirectStatus : redirectStatuses) {
			runRedirectTest(redirectStatus);
		}
	}

	private void runRedirectTest(int redirectStatus) throws Exception {
		WireMock.reset();

		stubFor(get(urlEqualTo("/some/index.html"))
				.willReturn(aResponse().withStatus(redirectStatus).withHeader("Location", "/another/index.html")));

		stubFor(get(urlPathMatching("/another/index.html"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "text/html")
						.withBody("<html>\n" + "    <head>\n"
								+ "        <meta charset=\"UTF-8\">\n"
								+ "    </head>\n"
								+ "    <body>\n"
								+ "        <h1>Redirected here.</h1>\n"
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

		controller.start(HandleRedirectWebCrawler.class, 1);

		@SuppressWarnings("unchecked")
		List<Object> crawlerData =
				(List<Object>) controller.getCrawlersLocalData().get(0);
		Assert.assertEquals(1, crawlerData.get(0));
		Assert.assertEquals("http://localhost:" + wireMockRule.port() + "/another/index.html", crawlerData.get(1));

		verify(1, getRequestedFor(urlEqualTo("/some/index.html")));
		verify(1, getRequestedFor(urlEqualTo("/another/index.html")));
	}

	public static class HandleRedirectWebCrawler extends WebCrawler {

		private int onRedirectedCounter = 0;
		private final List<Object> data = new ArrayList<>();

		@Override
		protected void onRedirectedStatusCode(Page page) {
			data.add(0, ++onRedirectedCounter);
			data.add(1, page.getRedirectedToUrl());
		}

		@Override
		public Object getMyLocalData() {
			return data;
		}
	}
}

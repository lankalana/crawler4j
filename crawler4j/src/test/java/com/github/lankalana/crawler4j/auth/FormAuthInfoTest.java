package com.github.lankalana.crawler4j.auth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import java.util.Collections;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.impl.client.BasicCookieStore;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.lankalana.crawler4j.crawler.CrawlConfig;
import com.github.lankalana.crawler4j.crawler.CrawlController;
import com.github.lankalana.crawler4j.crawler.WebCrawler;
import com.github.lankalana.crawler4j.crawler.authentication.FormAuthInfo;
import com.github.lankalana.crawler4j.fetcher.PageFetcher;
import com.github.lankalana.crawler4j.robotstxt.RobotstxtConfig;
import com.github.lankalana.crawler4j.robotstxt.RobotstxtServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;

public class FormAuthInfoTest {

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
						.withHeader("Host", "localhost")
						.withBody("<html>\n" + "    <head>\n"
								+ "        <meta charset=\"UTF-8\">\n"
								+ "        <title>Home page</title>\n"
								+ "    </head>\n"
								+ "    <body>\n"
								+ "        <h1>Title</h1>\n"
								+ "        <a href=\"/login.php\">Login</a>\n"
								+ "        <a href=\"/profile.php\">Profile</a>\n"
								+ "    </body>\n"
								+ "   </html>")));

		stubFor(get(urlEqualTo("/login.php"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "text/html")
						.withHeader("Host", "localhost")
						.withBody("<html>\n" + "    <head>\n"
								+ "        <meta charset=\"UTF-8\">\n"
								+ "        <title>Landing page</title>\n"
								+ "    </head>\n"
								+ "    <body>\n"
								+ "        <h1>Title</h1>\n"
								+ "        <form action=\"/login.php\" method=\"POST\">\n"
								+ "            <input type=\"text\" title=\"username\" placeholder=\"username\" />\n"
								+ "            <input type=\"password\" title=\"password\" placeholder=\"password\" />\n"
								+ "            <button type=\"submit\" class=\"btn\">Login</button>\n"
								+ "        </form>\n"
								+ "    </body>\n"
								+ "   </html>")));
		stubFor(post(urlEqualTo("/login.php"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Set-Cookie", "secret=hash; Path=/")
						.withHeader("Host", "localhost")));
		stubFor(get(urlEqualTo("/profile.php"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "text/html")
						.withHeader("Host", "localhost")
						.withHeader("Cookie", "secret=hash")
						.withBody("<html>\n" + "    <head>\n"
								+ "        <meta charset=\"UTF-8\">\n"
								+ "        <title>Profile page</title>\n"
								+ "    </head>\n"
								+ "    <body>\n"
								+ "        <h1>Title</h1>\n"
								+ "    </body>\n"
								+ "   </html>")));

		CrawlConfig config = new CrawlConfig();
		config.setCrawlStorageFolder(temp.newFolder().getAbsolutePath());
		config.setMaxPagesToFetch(10);
		config.setPolitenessDelay(0);
		config.setThreadShutdownDelaySeconds(1);
		config.setThreadMonitoringDelaySeconds(0);
		config.setCleanupDelaySeconds(0);
		FormAuthInfo formAuthInfo = new FormAuthInfo(
				"foofy",
				"superS3cret",
				"http://localhost:" + wireMockRule.port() + "/login.php",
				"username",
				"password");
		config.setAuthInfos(Collections.singletonList(formAuthInfo));
		config.setCookieStore(new BasicCookieStore());
		config.setCookiePolicy(CookieSpecs.DEFAULT);

		PageFetcher pageFetcher = new PageFetcher(config);
		RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
		robotstxtConfig.setEnabled(false);
		RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
		CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

		controller.addSeed("http://localhost:" + wireMockRule.port() + "/");
		controller.start(WebCrawler.class, 1);

		verify(1, getRequestedFor(urlEqualTo("/")));
		verify(1, getRequestedFor(urlEqualTo("/login.php")));
		verify(1, postRequestedFor(urlEqualTo("/login.php")));
		verify(1, getRequestedFor(urlEqualTo("/profile.php")).withCookie("secret", new EqualToPattern("hash")));

		Assert.assertEquals(1, config.getCookieStore().getCookies().size());
		Assert.assertEquals(
				"secret", config.getCookieStore().getCookies().get(0).getName());
		Assert.assertEquals("hash", config.getCookieStore().getCookies().get(0).getValue());
	}
}

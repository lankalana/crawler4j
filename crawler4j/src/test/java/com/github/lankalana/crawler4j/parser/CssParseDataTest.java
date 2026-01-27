package com.github.lankalana.crawler4j.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.github.lankalana.crawler4j.url.WebURL;

public class CssParseDataTest {

	@Test
	public void testCssUrlsParsingQuotes() throws Exception {
		CssParseData parseData = new CssParseData();
		parseData.setTextContent(readResourceText("/css/quotes.css"));

		WebURL webUrl = new WebURL();
		webUrl.setURL("http://example.com/css.css");

		parseData.setOutgoingUrls(webUrl);
		Set<WebURL> urls = parseData.getOutgoingUrls();

		assertEquals(3, urls.size());
	}

	@Test
	public void testCssAbsoluteUrlsPaths() throws Exception {
		CssParseData parseData = new CssParseData();
		parseData.setTextContent(readResourceText("/css/absolute.css"));

		WebURL webUrl = new WebURL();
		webUrl.setURL("http://example.com/css.css");

		parseData.setOutgoingUrls(webUrl);
		Set<WebURL> urls = parseData.getOutgoingUrls();

		assertEquals(3, urls.size());

		List<String> mapped = new ArrayList<>();
		for (WebURL url : urls) {
			mapped.add(url.getURL());
		}

		assertTrue(mapped.contains("http://example.com/css/absolute_no_proto.png"));
		assertTrue(mapped.contains("http://example.com/css/absolute_path.png"));
		assertTrue(mapped.contains("http://example.com/css/absolute_with_domain.png"));
	}

	@Test
	public void testCssRelativeUrlsPaths() throws Exception {
		CssParseData parseData = new CssParseData();
		parseData.setTextContent(readResourceText("/css/relative.css"));

		WebURL webUrl = new WebURL();
		webUrl.setURL("http://example.com/asset/css/css.css");

		parseData.setOutgoingUrls(webUrl);
		Set<WebURL> urls = parseData.getOutgoingUrls();

		assertEquals(2, urls.size());

		List<String> mapped = new ArrayList<>();
		for (WebURL url : urls) {
			mapped.add(url.getURL());
		}

		assertTrue(mapped.contains("http://example.com/asset/images/backgound_one.jpg"));
		assertTrue(mapped.contains("http://example.com/backgound_two.jpg"));
	}

	private String readResourceText(String resourcePath) throws IOException {
		try (InputStream input = CssParseDataTest.class.getResourceAsStream(resourcePath)) {
			return IOUtils.toString(input, StandardCharsets.UTF_8);
		}
	}
}

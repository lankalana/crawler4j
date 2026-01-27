package com.github.lankalana.crawler4j.parser;

import java.io.File;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.junit.Test;

import com.github.lankalana.crawler4j.crawler.CrawlConfig;
import com.github.lankalana.crawler4j.crawler.Page;
import com.github.lankalana.crawler4j.url.WebURL;

public class HtmlParserTest {

	@Test
	public void testCanParseHtmlPage() throws Exception {
		JsoupHtmlParser parser = new JsoupHtmlParser(new CrawlConfig());
		WebURL url = new WebURL();
		url.setURL("http://wiki.c2.com/");
		File file = new File("src/test/resources/html/wiki.c2.com.html");
		ContentType contentType = ContentType.create("text/html", java.nio.charset.StandardCharsets.UTF_8);
		FileEntity entity = new FileEntity(file, contentType);
		Page page = new Page(url);
		page.load(entity, 1_000_000);

		parser.parse(page, url.getURL());
	}
}

package com.github.lankalana.crawler4j.crawler;

import static org.junit.Assert.assertEquals;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.junit.Test;

import com.github.lankalana.crawler4j.url.WebURL;

public class PageTest {

	@Test
	public void testDefaultCharsetFallback() throws Exception {
		BasicHttpEntity entity = new BasicHttpEntity();
		String content = "The content";
		entity.setContent(IOUtils.toInputStream(content, "UTF-8"));
		entity.setContentLength(content.length());
		entity.setContentType(new BasicHeader("Content-type", "text/html; charset=UNPARSABLE"));

		WebURL url = new WebURL();
		Page page = new Page(url);
		page.load(entity, 1024);

		assertEquals("UTF-8", page.getContentCharset());
	}
}

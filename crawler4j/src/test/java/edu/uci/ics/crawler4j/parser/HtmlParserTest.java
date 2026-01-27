package edu.uci.ics.crawler4j.parser;

import java.io.File;
import java.nio.charset.Charset;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.junit.Test;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.url.WebURL;

public class HtmlParserTest {

    @Test
    public void testCanParseHtmlPage() throws Exception {
        JsoupHtmlParser parser = new JsoupHtmlParser(new CrawlConfig(), null);
        WebURL url = new WebURL();
        url.setURL("http://wiki.c2.com/");
        File file = new File("src/test/resources/html/wiki.c2.com.html");
        ContentType contentType = ContentType.create("text/html", Charset.forName("UTF-8"));
        FileEntity entity = new FileEntity(file, contentType);
        Page page = new Page(url);
        page.load(entity, 1000000);

        parser.parse(page, url.getURL());
    }
}

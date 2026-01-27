package com.github.lankalana.crawler4j.parser;

import com.github.lankalana.crawler4j.crawler.Page;
import com.github.lankalana.crawler4j.crawler.exceptions.ParseException;

public interface HtmlParser {

    HtmlParseData parse(Page page, String contextURL) throws ParseException;

}

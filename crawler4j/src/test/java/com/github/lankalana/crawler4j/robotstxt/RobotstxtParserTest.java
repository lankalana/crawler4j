package com.github.lankalana.crawler4j.robotstxt;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class RobotstxtParserTest {

	@Test
	public void testNoNullExceptionWhenRobotsTxtEndsWithUserAgent() throws Exception {
		RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
		String body = FileUtils.readFileToString(
				new File(RobotstxtParserTest.class
						.getClassLoader()
						.getResource("robotstxt/he.wikipedia.org_robots.txt")
						.getFile()),
				"UTF-8");

		RobotstxtParser.parse(body, robotstxtConfig);
	}
}

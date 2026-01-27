package com.github.lankalana.crawler4j.url;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.github.lankalana.crawler4j.crawler.CrawlConfig;

public class PublicSuffixTest {

	private static final TLDList INTERNAL_TLD_LIST;
	private static final TLDList EXTERNAL_TLD_LIST;

	static {
		try {
			CrawlConfig internalConfig = new CrawlConfig();
			internalConfig.setOnlineTldListUpdate(false);
			INTERNAL_TLD_LIST = new TLDList(internalConfig);

			CrawlConfig externalConfig = new CrawlConfig();
			externalConfig.setOnlineTldListUpdate(true);
			externalConfig.setPublicSuffixLocalFile("src/test/resources/public_suffix_list.dat");
			EXTERNAL_TLD_LIST = new TLDList(externalConfig);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@Test
	public void testEtldDomainsInternalLookup() {
		assertEtldDomains(INTERNAL_TLD_LIST);
	}

	@Test
	public void testEtldDomainsExternalLookup() {
		assertEtldDomains(EXTERNAL_TLD_LIST);
	}

	private void assertEtldDomains(TLDList tldList) {
		Object[][] cases = new Object[][] {
			{"http://www.example.com", "example.com", "www"},
			{"http://dummy.edu.np", "dummy.edu.np", ""}
		};

		for (Object[] testCase : cases) {
			WebURL webUrl = new WebURL();
			webUrl.setTldList(tldList);
			webUrl.setURL((String) testCase[0]);
			assertEquals(testCase[1], webUrl.getDomain());
			assertEquals(testCase[2], webUrl.getSubDomain());
		}
	}
}

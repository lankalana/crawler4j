package com.github.lankalana.crawler4j.parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.lankalana.crawler4j.crawler.CrawlConfig;
import com.github.lankalana.crawler4j.crawler.Page;
import com.github.lankalana.crawler4j.crawler.exceptions.ParseException;
import com.github.lankalana.crawler4j.url.TLDList;
import com.github.lankalana.crawler4j.url.URLCanonicalizer;
import com.github.lankalana.crawler4j.url.WebURL;

public class JsoupHtmlParser implements com.github.lankalana.crawler4j.parser.HtmlParser {
    protected static final Logger logger = LoggerFactory.getLogger(JsoupHtmlParser.class);
    private static final int MAX_ANCHOR_LENGTH = 100;

    private final CrawlConfig config;
    private final TLDList tldList;

    public JsoupHtmlParser(CrawlConfig config, TLDList tldList) throws InstantiationException, IllegalAccessException {
        this.config = config;
        this.tldList = tldList;
    }

    public HtmlParseData parse(Page page, String contextURL) throws ParseException {
        HtmlParseData parsedData = new HtmlParseData();

        Document document;

        try (InputStream inputStream = new ByteArrayInputStream(page.getContentData())) {
            String charsetName = page.getContentCharset();
            if (charsetName != null && charsetName.isEmpty()) {
                charsetName = null;
            }
            document = Jsoup.parse(inputStream, charsetName, contextURL);
        } catch (Exception e) {
            logger.error("{}, while parsing: {}", e.getMessage(), page.getWebURL().getURL());
            throw new ParseException("could not parse [" + page.getWebURL().getURL() + "]", e);
        }

        String contentCharset = chooseEncoding(page, document);
        parsedData.setContentCharset(contentCharset);

        parsedData.setText(extractBodyText(document));
        parsedData.setTitle(document.title());
        parsedData.setMetaTags(extractMetaTags(document));

        try {
            List<ExtractedUrlAnchorPair> outgoingLinks = extractOutgoingLinks(document);
            String baseUrl = extractBaseUrl(document);
            if (baseUrl != null) {
                contextURL = baseUrl;
            }
            Set<WebURL> outgoingUrls = getOutgoingUrls(contextURL, outgoingLinks, contentCharset);
            parsedData.setOutgoingUrls(outgoingUrls);

            String htmlCharset = page.getContentCharset();
            if (htmlCharset == null || htmlCharset.isEmpty()) {
                if (contentCharset != null && !contentCharset.isEmpty()) {
                    parsedData.setHtml(new String(page.getContentData(), contentCharset));
                } else {
                    parsedData.setHtml(new String(page.getContentData()));
                }
            } else {
                parsedData.setHtml(new String(page.getContentData(), htmlCharset));
            }

            return parsedData;
        } catch (UnsupportedEncodingException e) {
            logger.error("error parsing the html: " + page.getWebURL().getURL(), e);
            throw new ParseException("could not parse [" + page.getWebURL().getURL() + "]", e);
        }

    }

    private Set<WebURL> getOutgoingUrls(String contextURL, List<ExtractedUrlAnchorPair> outgoingLinks,
                                        String contentCharset)
            throws UnsupportedEncodingException {
        Set<WebURL> outgoingUrls = new HashSet<>();

        int urlCount = 0;
        for (ExtractedUrlAnchorPair urlAnchorPair : outgoingLinks) {

            String href = urlAnchorPair.getHref();
            if ((href == null) || href.trim().isEmpty()) {
                continue;
            }

            String hrefLoweredCase = href.trim().toLowerCase();
            if (!hrefLoweredCase.contains("javascript:") &&
                    !hrefLoweredCase.contains("mailto:") && !hrefLoweredCase.contains("@")) {
                // Prefer page's content charset to encode href url
                Charset hrefCharset = ((contentCharset == null) || contentCharset.isEmpty()) ?
                        StandardCharsets.UTF_8 : Charset.forName(contentCharset);
                String url = URLCanonicalizer.getCanonicalURL(href, contextURL, hrefCharset);
                if (url != null) {
                    WebURL webURL = new WebURL();
                    webURL.setTldList(tldList);
                    webURL.setURL(url);
                    webURL.setTag(urlAnchorPair.getTag());
                    webURL.setAnchor(urlAnchorPair.getAnchor());
                    webURL.setAttributes(urlAnchorPair.getAttributes());
                    outgoingUrls.add(webURL);
                    urlCount++;
                    if (urlCount > config.getMaxOutgoingLinksToFollow()) {
                        break;
                    }
                }
            }
        }
        return outgoingUrls;
    }

    private String extractBodyText(Document document) {
        return document.body().text().trim();
    }

    private Map<String, String> extractMetaTags(Document document) {
        Map<String, String> metaTags = new HashMap<>();
        Elements metaElements = document.select("meta[http-equiv], meta[name]");
        for (Element meta : metaElements) {
            String key = meta.hasAttr("http-equiv") ? meta.attr("http-equiv") : meta.attr("name");
            if (key == null || key.isEmpty()) {
                continue;
            }
            String content = meta.attr("content");
            if (content == null || content.isEmpty()) {
                continue;
            }
            metaTags.put(key.toLowerCase(), content);
        }
        return metaTags;
    }

    private String extractBaseUrl(Document document) {
        Element base = document.selectFirst("base[href]");
        if (base == null) {
            return null;
        }
        String href = base.attr("href");
        return (href == null || href.isEmpty()) ? null : href;
    }

    private List<ExtractedUrlAnchorPair> extractOutgoingLinks(Document document) {
        List<ExtractedUrlAnchorPair> outgoing = new ArrayList<>();
        Elements hrefElements = document.select("a[href], area[href], link[href]");
        for (Element element : hrefElements) {
            String href = element.attr("href");
            if (href == null || href.isEmpty()) {
                continue;
            }
            ExtractedUrlAnchorPair pair = new ExtractedUrlAnchorPair();
            pair.setHref(href);
            pair.setTag(element.tagName());
            for (Attribute attribute : element.attributes()) {
                pair.setAttribute(attribute.getKey(), attribute.getValue());
            }
            String anchor = normalizeAnchorText(element.text());
            if (anchor.isEmpty()) {
                anchor = normalizeAnchorText(element.attr("title"));
            }
            if (anchor.isEmpty() && element.hasAttr("alt")) {
                anchor = normalizeAnchorText(element.attr("alt"));
            }
            if (!anchor.isEmpty()) {
                pair.setAnchor(anchor);
            }
            outgoing.add(pair);
        }

        Elements srcElements = document.select("img[src], iframe[src], frame[src], embed[src], script[src]");
        for (Element element : srcElements) {
            String src = element.attr("src");
            if (src == null || src.isEmpty()) {
                continue;
            }
            ExtractedUrlAnchorPair pair = new ExtractedUrlAnchorPair();
            pair.setHref(src);
            pair.setTag(element.tagName());
            outgoing.add(pair);
        }

        Elements metaElements = document.select("meta[http-equiv], meta[name]");
        boolean refreshSeen = false;
        boolean locationSeen = false;
        for (Element meta : metaElements) {
            String equiv = meta.hasAttr("http-equiv") ? meta.attr("http-equiv") : meta.attr("name");
            String content = meta.attr("content");
            if (equiv == null || equiv.isEmpty() || content == null || content.isEmpty()) {
                continue;
            }
            String equivLower = equiv.toLowerCase();
            if ("refresh".equals(equivLower) && !refreshSeen) {
                int pos = content.toLowerCase().indexOf("url=");
                if (pos != -1) {
                    String url = content.substring(pos + 4);
                    addMetaLink(outgoing, url, "meta");
                    refreshSeen = true;
                }
            } else if ("location".equals(equivLower) && !locationSeen) {
                addMetaLink(outgoing, content, "meta");
                locationSeen = true;
            }
        }

        return outgoing;
    }

    private void addMetaLink(List<ExtractedUrlAnchorPair> outgoing, String href, String tag) {
        if (href == null || href.isEmpty()) {
            return;
        }
        ExtractedUrlAnchorPair pair = new ExtractedUrlAnchorPair();
        pair.setHref(href);
        pair.setTag(tag);
        outgoing.add(pair);
    }

    private String normalizeAnchorText(String anchor) {
        if (anchor == null) {
            return "";
        }
        String normalized = anchor.replace('\n', ' ').replace('\t', ' ').trim();
        if (normalized.length() > MAX_ANCHOR_LENGTH) {
            normalized = normalized.substring(0, MAX_ANCHOR_LENGTH) + "...";
        }
        return normalized;
    }

    private String chooseEncoding(Page page, Document document) {
        String pageCharset = page.getContentCharset();
        if (pageCharset == null || pageCharset.isEmpty()) {
            if (document != null && document.charset() != null) {
                return document.charset().name();
            }
            return null;
        }
        return pageCharset;
    }
}

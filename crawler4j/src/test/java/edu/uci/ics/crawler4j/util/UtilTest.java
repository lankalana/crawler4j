package edu.uci.ics.crawler4j.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UtilTest {

    @Test
    public void testHasBinaryContent() {
        assertTrue(Util.hasBinaryContent("image/png"));
        assertTrue(Util.hasBinaryContent("application/pdf"));
        assertTrue(Util.hasBinaryContent("AuDiO/mpeg"));
        assertFalse(Util.hasBinaryContent("text/html"));
        assertFalse(Util.hasBinaryContent(null));
    }

    @Test
    public void testHasPlainTextContent() {
        assertTrue(Util.hasPlainTextContent("text/plain"));
        assertTrue(Util.hasPlainTextContent("text/css"));
        assertTrue(Util.hasPlainTextContent("TEXT/CSV"));
        assertFalse(Util.hasPlainTextContent("text/html; charset=UTF-8"));
        assertFalse(Util.hasPlainTextContent(null));
    }

    @Test
    public void testHasCssTextContent() {
        assertTrue(Util.hasCssTextContent("text/css"));
        assertTrue(Util.hasCssTextContent("application/css"));
        assertTrue(Util.hasCssTextContent("TEXT/CSS; charset=utf-8"));
        assertFalse(Util.hasCssTextContent("text/html"));
        assertFalse(Util.hasCssTextContent(null));
    }
}
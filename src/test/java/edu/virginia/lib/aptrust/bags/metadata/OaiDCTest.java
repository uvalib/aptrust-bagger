package edu.virginia.lib.aptrust.bags.metadata;

import edu.virginia.lib.aptrust.bags.metadata.OaiDC;
import edu.virginia.lib.aptrust.bags.metadata.annotations.APTrustIdentifier;
import edu.virginia.lib.aptrust.bags.metadata.annotations.APTrustTitle;
import edu.virginia.lib.aptrust.bags.metadata.annotations.AnnotationUtils;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public class OaiDCTest {

    @Test
    public void testParseSingleTitle() throws UnsupportedEncodingException, JAXBException {
        final String title = "title";
        final OaiDC dc = OaiDC.parse(new ByteArrayInputStream(generateDCRecordString(null, new String[]{title}).getBytes("UTF-8")));
        assertNotNull(dc.title);
        assertEquals(1, dc.title.length);
        assertEquals(title, dc.title[0]);
        assertEquals(title, dc.getFirstTitle());
    }

    @Test
    public void testParseMultipleTitle() throws UnsupportedEncodingException, JAXBException {
        final String title = "title";
        final String secondTitle = "secondTitle";
        final OaiDC dc = OaiDC.parse(new ByteArrayInputStream(generateDCRecordString(null, new String[]{title, secondTitle}).getBytes("UTF-8")));
        assertNotNull(dc.title);
        assertEquals(2, dc.title.length);
        assertEquals(title, dc.title[0]);
        assertEquals(title, dc.getFirstTitle());
        assertEquals(secondTitle, dc.title[1]);
    }

    @Test
    public void testParseMultipleIdentifiers() throws UnsupportedEncodingException, JAXBException {
        final String identifier = "id 1";
        final String secondIdentifier = "id 2";
        final OaiDC dc = OaiDC.parse(new ByteArrayInputStream(generateDCRecordString(new String[]{identifier, secondIdentifier}, null).getBytes("UTF-8")));
        assertNotNull(dc.identifier);
        assertEquals(2, dc.identifier.length);
        assertEquals(identifier, dc.identifier[0]);
        assertEquals(secondIdentifier, dc.identifier[1]);
    }

    @Test
    public void testIDAnnotation() throws UnsupportedEncodingException, JAXBException {
        final String identifier = "id";
        final OaiDC dc = OaiDC.parse(new ByteArrayInputStream(generateDCRecordString(new String[]{identifier}, null).getBytes("UTF-8")));
        Assert.assertEquals(identifier, AnnotationUtils.getAnnotationValue(APTrustIdentifier.class, dc));
    }

    @Test
    public void testTitleAnnotation() throws UnsupportedEncodingException, JAXBException {
        final String title = "title";
        final OaiDC dc = OaiDC.parse(new ByteArrayInputStream(generateDCRecordString(null, new String[]{title}).getBytes("UTF-8")));
        Assert.assertEquals(title, AnnotationUtils.getAnnotationValue(APTrustTitle.class, dc));
    }

    static String generateDCRecordString(String[] identifiers, String[] titles) {
        StringBuffer dc = new StringBuffer();
        dc.append("<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">");
        if (titles != null) {
            for (String title : titles) {
              dc.append("<dc:title xmlns:dc=\"http://purl.org/dc/elements/1.1/\">" + title + "</dc:title>");
            }
        }
        if (identifiers != null) {
            for (String identifier : identifiers) {
                dc.append("<dc:identifier xmlns:dc=\"http://purl.org/dc/elements/1.1/\">" + identifier + "</dc:identifier>");
            }
        }
        dc.append("</oai_dc:dc>");
        return dc.toString();
    }

}

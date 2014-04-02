package edu.virginia.lib.aptrust.bags.metadata;

import edu.virginia.lib.aptrust.bags.metadata.annotations.APTrustIdentifier;
import edu.virginia.lib.aptrust.bags.metadata.annotations.APTrustTitle;
import edu.virginia.lib.aptrust.bags.metadata.annotations.AnnotationUtils;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import static org.junit.Assert.assertEquals;

public class PBCoreTest {

    public String pbcoreString;

    @Before
    public void setUp() {
        pbcoreString = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<pbcoreDescriptionDocument xmlns=\"http://www.pbcore.org/PBCore/PBCoreNamespace.html\"\n" +
                "                           xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "                           xsi:schemaLocation=\"http://www.pbcore.org/PBCore/PBCoreNamespace.html http://www.pbcore.org/xsd/pbcore-2.0.xsd\">\n" +
                "   <pbcoreAssetType>clip</pbcoreAssetType>\n" +
                "   <pbcoreAssetDate>01/01/57</pbcoreAssetDate>\n" +
                "   <pbcoreIdentifier source=\"uva\">2022_1</pbcoreIdentifier>\n" +
                "   <pbcoreTitle>Georgia Tech beats Pittsburgh in Gator Bowl 21-14</pbcoreTitle>\n" +
                "   <pbcoreSubject source=\"LCSH\" subjectType=\"Place\">Jacksonville (Fla.)</pbcoreSubject>\n" +
                "   <pbcoreDescription descriptionType=\"abstract\">Thirty-eight thousand fans watch Georgia Tech play against Pittsburgh in the Gator Bowl; Georgia Tech wins with a score of 21-14.</pbcoreDescription>\n" +
                "   <pbcoreInstantiation>\n" +
                "      <instantiationIdentifier source=\"uva\">2022_1</instantiationIdentifier>\n" +
                "      <instantiationDuration>1:38</instantiationDuration>\n" +
                "      <instantiationColors>BW</instantiationColors>\n" +
                "      <instantiationAnnotation>silent</instantiationAnnotation>\n" +
                "   </pbcoreInstantiation>\n" +
                "</pbcoreDescriptionDocument>";
    }

    @Test
    public void testParseIdentifier() throws UnsupportedEncodingException, JAXBException {
        PBCore pbcore = PBCore.parse(new ByteArrayInputStream(pbcoreString.getBytes("UTF-8")));
        assertEquals("2022_1", pbcore.pbcoreIdentifier);
    }

    @Test
    public void testParseTitle() throws UnsupportedEncodingException, JAXBException {
        PBCore pbcore = PBCore.parse(new ByteArrayInputStream(pbcoreString.getBytes("UTF-8")));
        assertEquals("Georgia Tech beats Pittsburgh in Gator Bowl 21-14", pbcore.pbcoreTitle);
    }

    @Test
    public void testAnnotations() throws UnsupportedEncodingException, JAXBException {
        PBCore pbcore = PBCore.parse(new ByteArrayInputStream(pbcoreString.getBytes("UTF-8")));
        assertEquals("Georgia Tech beats Pittsburgh in Gator Bowl 21-14", AnnotationUtils.getAnnotationValue(APTrustTitle.class, pbcore));
        assertEquals("2022_1", AnnotationUtils.getAnnotationValue(APTrustIdentifier.class, pbcore));
    }
}

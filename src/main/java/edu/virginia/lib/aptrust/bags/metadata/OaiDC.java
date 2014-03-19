package edu.virginia.lib.aptrust.bags.metadata;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.InputStream;

/**
 * An extremely simple container object with JAXB annotations allowing it to parse
 * a small subset of dublin core fields from XML.
 */
@XmlRootElement(namespace = "http://www.openarchives.org/OAI/2.0/oai_dc/", name="dc")
public class OaiDC {

    @XmlElement(namespace = "http://purl.org/dc/elements/1.1/")
    public String[] identifier;

    @XmlElement(namespace = "http://purl.org/dc/elements/1.1/")
    public String[] title;

    public String getFirstTitle() {
        return title == null ? null : title[0];
    }

    public static OaiDC parse(final InputStream stream) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(OaiDC.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        return (OaiDC) unmarshaller.unmarshal(stream);
    }
}

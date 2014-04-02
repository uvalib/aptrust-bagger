package edu.virginia.lib.aptrust.bags.metadata;

import edu.virginia.lib.aptrust.bags.metadata.annotations.APTrustIdentifier;
import edu.virginia.lib.aptrust.bags.metadata.annotations.APTrustTitle;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.InputStream;

@XmlRootElement(namespace=PBCore.PBCORE_NS, name="pbcoreDescriptionDocument")
public class PBCore {

    public static final String PBCORE_NS = "http://www.pbcore.org/PBCore/PBCoreNamespace.html";

    @APTrustIdentifier
    @XmlElement(namespace = PBCORE_NS)
    public String pbcoreIdentifier;

    @APTrustTitle
    @XmlElement(namespace = PBCORE_NS)
    public String pbcoreTitle;

    public static PBCore parse(final InputStream stream) throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(PBCore.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        return (PBCore) unmarshaller.unmarshal(stream);
    }

}

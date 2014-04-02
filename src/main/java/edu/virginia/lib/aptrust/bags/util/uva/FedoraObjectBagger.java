package edu.virginia.lib.aptrust.bags.util.uva;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.FedoraClientException;
import edu.virginia.lib.aptrust.bags.APTrustBag;
import edu.virginia.lib.aptrust.bags.APTrustInfo;
import edu.virginia.lib.aptrust.bags.BagInfo;
import edu.virginia.lib.aptrust.bags.fedora.FedoraHelper;
import edu.virginia.lib.aptrust.bags.fedora.FedoraObjectBag;
import edu.virginia.lib.aptrust.bags.metadata.OaiDC;
import edu.virginia.lib.aptrust.bags.metadata.annotations.APTrustIdentifier;
import edu.virginia.lib.aptrust.bags.metadata.annotations.APTrustTitle;
import edu.virginia.lib.aptrust.bags.metadata.annotations.AnnotationUtils;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 *   A utility class that encapsualtes the logic necessary to compile all the parts
 *   of a UVA fedora-based bag.  This logic uses information such as well-known
 *   naming conventions, fedora relationship ontology, datastream names, as well as
 *   other conventions that likely only apply to the University of Virginia.
 * </p>
 * <p>
 *   Only sensitive or variable information is expected to be contained in configuration
 *   files while the bulk of the logic is hard-coded here for ease of readability.
 * </p>
 */
public class FedoraObjectBagger {

    protected final File archiveStoreRoot;

    private final String defaultRights;

    private final String sourceOrganization;

    protected final FedoraClient fc;

    /**
     * Instantiates a FedoraObjectBagger.  For this method to be successful, a properties
     * file must be on the classpath with the name "uva.properties" and must define several
     * properties:
     * <ul>
     *     <li>
     *         archive-store-root: the root directory of the archival store (where master
     *         files may be found)
     *     </li>
     *     <li>
     *         default-rights: the default rights restrictions for created bags
     *     </li>
     *     <li>
     *         source-organization: a string that will populate the source-organization field
     *         in the bag.
     *     </li>
     * </ul>
     * @param fc a fedora client to access content directly from fedora.
     */
    public FedoraObjectBagger(FedoraClient fc) throws IOException {
        this.fc = fc;

        final Properties p = new Properties();
        final InputStream is = this.getClass().getClassLoader().getResourceAsStream("uva.properties");
        if (is == null) {
            throw new IllegalStateException("This class requires a \"uva.properties\" to be on the classpath!");
        }
        try {
            p.load(is);
        } finally {
            is.close();
        }
        archiveStoreRoot = new File(p.getProperty("archive-store-root"));
        defaultRights = p.getProperty("default-rights");
        sourceOrganization = p.getProperty("source-organization");
    }

    /**
     * Returns an object (expected to be marked up with APTrustIdentifier and APTrustTitle annotations)
     * representing the main metadata for the given pid.
     */
    protected Object getMetadata(String pid, List<String> contentModelPids) throws FedoraClientException, JAXBException, IOException {
        final InputStream dcInputStream = FedoraClient.getDatastreamDissemination(pid, "DC").execute(fc).getEntityInputStream();
        try {
            return OaiDC.parse(dcInputStream);
        } finally {
            dcInputStream.close();
        }
    }

    public APTrustBag getObjectBag(String pid) throws Exception {
        final List<String> contentModelPids = FedoraHelper.getContentModelPIDs(fc, pid);

        Object metadata = getMetadata(pid, contentModelPids);

        // build the APTrustInfo
        final APTrustInfo aptrustInfo = getAPTrustInfo(metadata);

        // build the BagInfo
        final BagInfo bagInfo = getBagInfo(metadata);

        // locate the master file
        File masterFile = locateMasterFile(metadata);

        return new FedoraObjectBag(bagInfo, aptrustInfo, fc, pid, masterFile, getDSIDsToPreserveForContentModels(contentModelPids));
    }

    public Iterator<APTrustBag> getSubgraphBag(String rootPid) throws Exception {
        return new ChildIterator(rootPid);
    }

    protected APTrustInfo getAPTrustInfo(Object metadata) {
        return new APTrustInfo(AnnotationUtils.getAnnotationValue(APTrustTitle.class, metadata), defaultRights);
    }

    protected BagInfo getBagInfo(Object metadata) {
        final BagInfo bagInfo = new BagInfo();
        bagInfo.sourceOrganization(sourceOrganization);
        return bagInfo;
    }

    protected File locateMasterFile(Object metadata) {
        for (String identifier : AnnotationUtils.getAnnotationValues(APTrustIdentifier.class, metadata)) {
            if (identifier.endsWith(".tif")) {
                final String masterFileName = identifier;
                Matcher m = Pattern.compile("(\\d+)_\\d+\\.tif").matcher(masterFileName);
                if (!m.matches()) {
                    return null;
                }
                final File masterFile = new File(new File(archiveStoreRoot, m.group(1)), masterFileName);
                if (masterFile.exists()) {
                    return masterFile;
                } else {
                    throw new RuntimeException("Master file not found!");
                }
            }
        }
        return null;
    }

    protected String[] getDSIDsToPreserveForContentModels(List<String> contentModels) {
        if (contentModels.contains("djatoka:jp2CModel")) {
            return new String[] { "DC", "descMetadata", "technicalMetadata", "RELS-EXT" };
        } else {
            return new String[] { "DC", "descMetadata", "MARC", "RELS-EXT" };
        }
    }

    protected List<String> getOrderedChildPids(String pid, List<String> contentModelPids) throws Exception {
        if (contentModelPids.contains("djatoka:jp2CModel")) {
            return Collections.emptyList();
        } else {
            return FedoraHelper.getOrderedParts(fc, pid,
                    "http://fedora.lib.virginia.edu/relationships#hasCatalogRecordIn",
                    "http://fedora.lib.virginia.edu/relationships#hasPreceedingPage");
        }
    }

    private class ChildIterator implements Iterator<APTrustBag> {

        private List<String> contentModelPids;

        private Iterator<String> childPids;

        public ChildIterator(String parentPid) throws Exception {
            contentModelPids = FedoraHelper.getContentModelPIDs(fc, parentPid);
            ArrayList<String> pids = new ArrayList<String>();
            pids.add(parentPid);
            pids.addAll(getOrderedChildPids(parentPid, contentModelPids));
            childPids = pids.iterator();
        }

        @Override
        public boolean hasNext() {
            return childPids.hasNext();
        }

        @Override
        public APTrustBag next() {
            try {
                return getObjectBag(childPids.next());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void remove() {
            childPids.remove();
        }
    }
}

package edu.virginia.lib.aptrust.bags.util.uva;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.FedoraClientException;
import edu.virginia.lib.aptrust.bags.fedora.FedoraHelper;
import edu.virginia.lib.aptrust.bags.metadata.OaiDC;
import edu.virginia.lib.aptrust.bags.metadata.PBCore;
import edu.virginia.lib.aptrust.bags.metadata.annotations.APTrustIdentifier;
import edu.virginia.lib.aptrust.bags.metadata.annotations.AnnotationUtils;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WSLSFedoraObjectBagger extends FedoraObjectBagger {

    protected PBCore pbcore;

    /**
     * {@inheritDoc}
     */
    public WSLSFedoraObjectBagger(FedoraClient fc) throws IOException {
        super(fc);
    }

    @Override
    protected Object getMetadata(String pid, List<String> contentModelPids) throws FedoraClientException, JAXBException, IOException {
        if (contentModelPids.contains("uva-lib:pbcore2CModel")) {
            final InputStream pbcoreInputStream = FedoraClient.getDatastreamDissemination(pid, "metadata").execute(fc).getEntityInputStream();
            try {
                return PBCore.parse(pbcoreInputStream);
            } finally {
                pbcoreInputStream.close();
            }
        } else {
            return super.getMetadata(pid, contentModelPids);
        }
    }

    /**
     * Overrides the superclass to reflect the alternate file organization for
     * WSLS materials that aren't managed by the tracking system.
     */
    @Override
    protected File locateMasterFile(Object metadata) {
        final String id = AnnotationUtils.getAnnotationValue(APTrustIdentifier.class, metadata);
        final DecimalFormat format = new DecimalFormat("000");
        for (int i = 1; i <= 20; i ++) {
            final File ltoTapePath = new File(archiveStoreRoot, "WSLS_LTO-" + format.format(i));
            final File master = new File(ltoTapePath, id + ".mov");
            if (master.exists()) {
                return master;
            }
        }
        return null;
    }

    @Override
    protected String[] getDSIDsToPreserveForContentModels(List<String> contentModels) {
        ArrayList<String> dsids = new ArrayList<String>();
        dsids.add("DC");
        dsids.add("RELS-EXT");
        if (contentModels.contains("uva-lib:pbcore2CModel")) {
            dsids.add("metadata");
        }
        if (contentModels.contains("uva-lib:wslsScriptCModel")) {
            dsids.add("scriptPDF");
            dsids.add("scriptTXT");
        }
        return dsids.toArray(new String[0]);
    }

    @Override
    protected List<String> getOrderedChildPids(String pid, List<String> contentModelPids) throws Exception {
        if (contentModelPids.contains("uva-lib:eadComponentCModel")
                || contentModelPids.contains("uva-lib:eadCollectionCModel")) {
            return FedoraHelper.getOrderedParts(fc, pid,
                    "info:fedora/fedora-system:def/relations-external#isPartOf",
                    "http://fedora.lib.virginia.edu/relationships#follows");
        } else if (contentModelPids.contains("uva-lib:pbcore2CModel")) {
            return FedoraHelper.getSubjectPids(fc, pid,
                    "http://fedora.lib.virginia.edu/wsls/relationships#isAnchorScriptFor");
        } else {
            return Collections.emptyList();
        }
    }
}

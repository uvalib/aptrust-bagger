package edu.virginia.lib.aptrust.bags.fedora;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.FedoraClientException;
import edu.virginia.lib.aptrust.bags.APTrustBag;
import edu.virginia.lib.aptrust.bags.APTrustInfo;
import edu.virginia.lib.aptrust.bags.BagInfo;
import org.apache.commons.compress.utils.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An extension of APTrustBag that handles typical Fedora content where
 * the master file isn't stored in fedora and only a subset of the
 * datastreams need to be included.
 */
public class FedoraObjectBag extends APTrustBag {

    private String pid;

    private FedoraClient fc;

    private String[] dsIdsToPreserve;

    private File master;

    /**
     * A constructor for a bag of a fedora object that has no master file (such as
     * an object that serves only to organize other objects).
     */
    public FedoraObjectBag(BagInfo bagInfo, APTrustInfo apTrustInfo, FedoraClient client, String pid, String ... dsIdsToPreserve) {
        this(bagInfo, apTrustInfo, client, pid, null, dsIdsToPreserve);
    }

    /**
     * A constructor for a typical fedora object whose master file is on a file
     * system rather than ingested into fedora.
     */
    public FedoraObjectBag(BagInfo bagInfo, APTrustInfo apTrustInfo, FedoraClient client, String pid, File masterFile, String ... dsIdsToPreserve) {
        super(bagInfo, apTrustInfo);
        this.pid = pid;
        fc = client;
        this.dsIdsToPreserve = dsIdsToPreserve;
        this.master = masterFile;
    }

    @Override
    protected String getItemId() {
        return pid.replace(':', '_');
    }

    protected List<File> getPayloadFiles() throws IOException, FedoraClientException {
        final List<File> payload = new ArrayList<File>();
        for (String dsId : dsIdsToPreserve) {
            payload.add(downloadAndCacheDatastreamFile(dsId));
        }
        if (master != null) {
            payload.add(master);
        }
        return payload;
    }

    private File downloadAndCacheDatastreamFile(final String dsId) throws IOException, FedoraClientException {
        final File f = File.createTempFile("datastream-" + dsId, ".binary");
        final FileOutputStream fos = new FileOutputStream(f);
        try {
            IOUtils.copy(FedoraClient.getDatastreamDissemination(pid, dsId).execute(fc).getEntityInputStream(), fos);
            return f;
        } finally {
            fos.close();
        }
    }
}

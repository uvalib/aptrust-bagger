package edu.virginia.lib.aptrust.bags;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


/**
 * A class that can be used to build an APTrust bag from content that is
 * stored as accessible files.  All of the contents and metadata are expected
 * to be supplied to the constructor.
 */
public class FileBag extends APTrustBag {

    private String id;

    private List<PendingPayloadFile> files;

    public FileBag(final String institutionId, BagInfo bagInfo, APTrustInfo apTrustInfo, String id, Collection<File> files) {
        super(institutionId, bagInfo, apTrustInfo);
        this.id = id;
        this.files = new ArrayList<PendingPayloadFile>();
        for (File f : files) {
            this.files.add(new PendingPayloadFile(f));
        }
    }

    public FileBag(final String institutionId, BagInfo bagInfo, APTrustInfo apTrustInfo, String id, File ... files) {
        super(institutionId, bagInfo, apTrustInfo);
        this.id = id;
        this.files = new ArrayList<PendingPayloadFile>();
        for (File f : files) {
            this.files.add(new PendingPayloadFile(f));
        }
    }

    public FileBag(final String institutionId, BagInfo bagInfo, APTrustInfo apTrustInfo, String id, PendingPayloadFile ... files) {
        super(institutionId, bagInfo, apTrustInfo);
        this.id = id;
        this.files = Arrays.asList(files);
    }

    @Override
    protected String getItemId() {
        return id;
    }

    @Override
    protected List<PendingPayloadFile> getPayloadFiles() throws Exception {
        return files;
    }

    @Override
    protected void freePayloadFile(PendingPayloadFile f) throws Exception {
        // does nothing
    }
}

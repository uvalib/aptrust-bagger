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

    private List<File> files;

    public FileBag(BagInfo bagInfo, APTrustInfo apTrustInfo, String id, Collection<File> files) {
        super(bagInfo, apTrustInfo);
        this.id = id;
        this.files = new ArrayList<File>(files);
    }

    public FileBag(BagInfo bagInfo, APTrustInfo apTrustInfo, String id, File ... files) {
        super(bagInfo, apTrustInfo);
        this.id = id;
        this.files = Arrays.asList(files);
    }

    @Override
    protected String getItemId() {
        return id;
    }

    @Override
    protected List<File> getPayloadFiles() throws Exception {
        return files;
    }
}

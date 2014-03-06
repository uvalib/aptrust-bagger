package edu.virginia.lib.aptrust.bags;

import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.BagInfoTxtWriter;
import gov.loc.repository.bagit.BagItTxtWriter;
import gov.loc.repository.bagit.Manifest;
import gov.loc.repository.bagit.ManifestWriter;
import gov.loc.repository.bagit.impl.StringBagFile;
import gov.loc.repository.bagit.utilities.MessageDigestHelper;
import gov.loc.repository.bagit.writer.impl.FileSystemWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * An abstract class that encapsualtes the requirements for creating and serializing
 * a Bagit bag meeting the specifications for submission materials for AP Trust.
 */
public abstract class APTrustBag {

    private BagInfo bagInfo;

    private APTrustInfo aptrustInfo;

    public APTrustBag(BagInfo bagInfo, APTrustInfo aptrustInfo) {
        this.bagInfo = bagInfo;
        this.aptrustInfo = aptrustInfo;
    }

    /**
     * Creates an AP Trust compliant bag
     * @param destinationDir
     * @throws Exception
     */
    public void serializeAPTrustBag(File destinationDir) throws Exception {
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        } else {
            if (!destinationDir.isDirectory()) {
                throw new IllegalArgumentException(destinationDir + " is not a directory!");
            }
        }
        File file = new File(destinationDir, getAptrustBagName());
        BagFactory f = new BagFactory();
        final Bag b = f.createBag();
        final Bag.BagPartFactory partFactory = f.getBagPartFactory();

        // write the bagit.txt
        b.putBagFile(partFactory.createBagItTxt());

        // write the manifest
        final File manifestFile = new File("manifest-md5.txt");
        final FileOutputStream manifestOS = new FileOutputStream(manifestFile);
        try {
            final ManifestWriter manifestWriter = f.getBagPartFactory().createManifestWriter(manifestOS);
            for (File payloadFile : getPayloadFiles()) {
                manifestWriter.write("data/" + payloadFile.getName(), MessageDigestHelper.generateFixity(payloadFile, Manifest.Algorithm.MD5));
            }
            manifestWriter.close();
            b.addFileAsTag(manifestFile);
        } finally {
            manifestOS.close();
        }

        // write the bag-info.txt
        final File bagInfoFile = new File("bag-info.txt");
        final FileOutputStream bagInfoOS = new FileOutputStream(bagInfoFile);
        try {
            final BagInfoTxtWriter bagInfoWriter = f.getBagPartFactory().createBagInfoTxtWriter(bagInfoOS, "UTF-8");
            bagInfo.write(bagInfoWriter);
            bagInfoOS.close();
            b.addFileAsTag(bagInfoFile);
        } finally {
            bagInfoOS.close();
        }

        // write the aptrust-info.txt
        final File aptrustInfoFile = new File("aptrust-info.txt");
        final FileOutputStream aptrustInfoOS = new FileOutputStream(aptrustInfoFile);
        try {
            final BagItTxtWriter aptrustInfoWriter = f.getBagPartFactory().createBagItTxtWriter(aptrustInfoOS, "UTF-8");
            aptrustInfoWriter.write("Title", aptrustInfo.getTitle());
            aptrustInfoWriter.write("Rights", aptrustInfo.getRights());
            aptrustInfoOS.close();
            b.addFileAsTag(aptrustInfoFile);
        } finally {
            aptrustInfoOS.close();
        }


        b.addFilesToPayload(getPayloadFiles());
        b.write(new FileSystemWriter(f), file);

        manifestFile.delete();
        aptrustInfoFile.delete();
        bagInfoFile.delete();
    }

    private String getAptrustBagName() {
        return getInstitutionalId() + "." + getItemId();
    }

    protected String getInstitutionalId() {
        // this should be provided by the build environment
        String id = System.getProperty("bagger.source-organization-id");
        return id == null ? "test" : id;
    }

    protected abstract String getItemId();

    protected abstract List<File> getPayloadFiles() throws Exception;
}

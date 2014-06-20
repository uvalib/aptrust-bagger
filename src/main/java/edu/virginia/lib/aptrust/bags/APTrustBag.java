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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * An abstract class that encapsualtes the requirements for creating and serializing
 * a Bagit bag meeting the specifications for submission materials for AP Trust.
 *
 * The specs are available online here:
 * https://sites.google.com/a/aptrust.org/aptrust-wiki/technical-documentation/processing-ingest/aptrust-bagit-profile
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
     * @param destinationDir the directory into which the bag will be serialized
     * @param tar if true, a tar file will be the ultimate output, if false a simple
     *            directory structure (note: the current implementation writes out
     *            a directory structure first and tars it second)
     * @return a BagSummary referencing the the file that represents the root of the bag
     * (either the tar file or the bag directory) and a checksum if the bag is a tar file
     * @throws Exception
     */
    public BagSummary serializeAPTrustBag(File destinationDir, boolean tar) throws Exception {
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

        final List<File> payload = getPayloadFiles();

        // write the bagit.txt
        b.putBagFile(partFactory.createBagItTxt());

        // write the manifest
        final File manifestFile = new File("manifest-md5.txt");
        final FileOutputStream manifestOS = new FileOutputStream(manifestFile);
        try {
            final ManifestWriter manifestWriter = f.getBagPartFactory().createManifestWriter(manifestOS);
            for (File payloadFile : payload) {
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
            aptrustInfoWriter.write("Access", aptrustInfo.getAccess());
            aptrustInfoOS.close();
            b.addFileAsTag(aptrustInfoFile);
        } finally {
            aptrustInfoOS.close();
        }

        b.addFilesToPayload(payload);
        b.write(new FileSystemWriter(f), file);

        for (File payloadFile : payload) {
            freePayloadFile(payloadFile);
        }
        manifestFile.delete();
        aptrustInfoFile.delete();
        bagInfoFile.delete();

        if (tar) {
            final BagSummary result = tarDirectory(file);
            FileUtils.deleteDirectory(file);
            return result;
        } else {
            return new BagSummary(file, null);
        }
    }

    private BagSummary tarDirectory(final File file) throws IOException {
        final File tarFile = new File(file.getAbsolutePath() + ".tar");
        final HashOutputStream dest = new HashOutputStream(new FileOutputStream(tarFile));
        TarOutputStream out = new TarOutputStream(new BufferedOutputStream(dest));
        for(File f : getFilesWithinDir(file, new ArrayList<File>())){
            out.putNextEntry(new TarEntry(f, f.getAbsolutePath().substring(file.getParentFile().getAbsolutePath().length())));
            FileInputStream fis = new FileInputStream(f);
            try {
                IOUtils.copy(fis, out);
            } finally {
                fis.close();
            }
        }
        out.close();
        return new BagSummary(tarFile, dest.getMD5Hash());
    }

    public List<File> getFilesWithinDir(File dir, List<File> result) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                getFilesWithinDir(f, result);
            } else {
                result.add(f);
            }
        }
        return result;
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

    /**
     * Will be called once the payload file (returned by getPayloadFiles())
     * has been used and will not be needed.  This allows temporary files
     * to be cleaned up by implementing classes.
     */
    protected abstract void freePayloadFile(File f) throws Exception;

    /**
     * An OutputStream that computes a hash of the content passed through it.
     */
    public static class HashOutputStream extends OutputStream {

        private MessageDigest digest;

        private OutputStream pipe;

        public HashOutputStream(OutputStream os) {
            pipe = os;
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException ex) {
                // can't happen because MD5 is supported by all JVMs
                assert false;
            }
        }

        public byte[] getMD5Hash() {
            return digest.digest();
        }

        public void write(int b) throws IOException {
            digest.update(new byte[] { (byte) b });
            pipe.write(b);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            digest.update(b, off, len);
            pipe.write(b, off, len);
        }

        public void write(byte[] b) throws IOException {
            digest.update(b);
            pipe.write(b);
        }

        public static byte[] getMD5Hash(String value) throws IOException {
            HashOutputStream os = new HashOutputStream(new ByteArrayOutputStream());
            os.write(value.getBytes("UTF-8"));
            os.close();
            return os.getMD5Hash();
        }
    }
}

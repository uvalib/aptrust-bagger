package edu.virginia.lib.aptrust.bags;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import edu.virginia.lib.aptrust.bags.util.OutputDrainerThread;
import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.BagInfoTxtWriter;
import gov.loc.repository.bagit.BagItTxtWriter;
import gov.loc.repository.bagit.Manifest;
import gov.loc.repository.bagit.ManifestWriter;
import gov.loc.repository.bagit.utilities.MessageDigestHelper;
import gov.loc.repository.bagit.writer.impl.FileSystemWriter;

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
    	long payloadSize = 0;
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
        final StringBuffer manifestCopy = new StringBuffer();
        final File manifestFile = new File("manifest-md5.txt");
        final FileOutputStream manifestOS = new FileOutputStream(manifestFile);
        try {
            final ManifestWriter manifestWriter = f.getBagPartFactory().createManifestWriter(manifestOS);
            for (File payloadFile : payload) {
            	payloadSize += payloadFile.length();
            	final String filename = "data/" + payloadFile.getName();
            	final String fixity = MessageDigestHelper.generateFixity(payloadFile, Manifest.Algorithm.MD5);
            	manifestCopy.append(fixity + "  " + filename + "\n");
                manifestWriter.write(filename, fixity);
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
            final BagSummary result = tarDirectory(file, manifestCopy.toString(), payloadSize);
            FileUtils.deleteDirectory(file);
            return result;
        } else {
            return new BagSummary(file, null, manifestCopy.toString(), payloadSize);
        }
    }

    private BagSummary tarDirectory(final File file, String manifestCopy, long payloadSize) throws IOException, InterruptedException {
        final File tarFile = new File(file.getAbsolutePath() + ".tar");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ProcessBuilder pb = new ProcessBuilder("tar", "-cvf", tarFile.getAbsolutePath(), file.getName());
        pb.directory(file.getParentFile());
        Process p = pb.start();
        
        new Thread(new OutputDrainerThread(p.getInputStream(), baos)).start();
        new Thread(new OutputDrainerThread(p.getErrorStream(), baos)).start();
        int returnCode = p.waitFor();
        if (returnCode != 0) {
            throw new RuntimeException("Invalid return code for process! " + new String(baos.toByteArray(), "UTF-8"));
        }
        final HashOutputStream dest = new HashOutputStream();
        FileInputStream fis = new FileInputStream(tarFile);
        IOUtils.copy(fis, dest);
        fis.close();
        
        return new BagSummary(tarFile, dest.getMD5Hash(), manifestCopy, payloadSize);
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

        public HashOutputStream() {
            this(null);
        }
        
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
            if (pipe != null) {
                pipe.write(b);
            }
        }

        public void write(byte[] b, int off, int len) throws IOException {
            digest.update(b, off, len);
            if (pipe != null) {
                pipe.write(b, off, len);
            }
        }

        public void write(byte[] b) throws IOException {
            digest.update(b);
            if (pipe != null) {
                pipe.write(b);
            }
        }

        public static byte[] getMD5Hash(String value) throws IOException {
            HashOutputStream os = new HashOutputStream(new ByteArrayOutputStream());
            os.write(value.getBytes("UTF-8"));
            os.close();
            return os.getMD5Hash();
        }
    }
}

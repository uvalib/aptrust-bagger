package edu.virginia.lib.aptrust.bags;

import edu.virginia.lib.aptrust.bags.util.OutputDrainerThread;
import gov.loc.repository.bagit.creator.CreatePayloadManifestsVistor;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.Manifest;
import gov.loc.repository.bagit.domain.Version;
import gov.loc.repository.bagit.hash.Hasher;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import gov.loc.repository.bagit.hash.SupportedAlgorithm;
import gov.loc.repository.bagit.writer.BagitFileWriter;
import gov.loc.repository.bagit.writer.ManifestWriter;
import gov.loc.repository.bagit.writer.MetadataWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An abstract class that encapsualtes the requirements for creating and serializing
 * a Bagit bag meeting the specifications for submission materials for AP Trust.
 *
 * The specs are available online here:
 * https://sites.google.com/a/aptrust.org/aptrust-wiki/technical-documentation/processing-ingest/aptrust-bagit-profile
 */
public abstract class APTrustBag {

    final private static Logger LOGGER = LoggerFactory.getLogger(APTrustBag.class);

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
        File bagOutputFile = new File(destinationDir, getAptrustBagName());
        bagOutputFile.mkdirs();

        final Bag b = new Bag(new Version(0, 97));
        b.setRootDir(bagOutputFile.toPath());
        Charset charset = b.getFileEncoding();

        final List<PendingPayloadFile> payload = getPayloadFiles();

        // write the bagit.txt
        BagitFileWriter.writeBagitFile(b.getVersion(), b.getFileEncoding(), b.getRootDir());

        // bring in the payload
        File dataDir = new File(b.getRootDir().toFile(), "data");
        for (PendingPayloadFile payloadFile : payload) {
            payloadSize += payloadFile.getFile().length();
            final Path destination = new File(dataDir, payloadFile.getPathWithinPayload()).toPath();
            destination.getParent().toFile().mkdirs();
            try {
                Files.createLink(destination, payloadFile.getFile().toPath());
            } catch (FileSystemException e) {
                LOGGER.info("Exception hard-linking bag payload! (performing aopy)", e);
                Files.copy(payloadFile.getFile().toPath(), destination);
            }
            freePayloadFile(payloadFile);
        }

        // write the payload manifest
        SupportedAlgorithm algorithm = StandardSupportedAlgorithms.SHA256;
        //Manifest manifest = new Manifest(algorithm);
        MessageDigest messageDigest = MessageDigest.getInstance(algorithm.getMessageDigestName());
        final Map<Manifest, MessageDigest> manifestToMessageDigestMap = Hasher.createManifestToMessageDigestMap(Collections.singleton(algorithm));
        //manifestToMessageDigestMap.put(manifest, messageDigest);
        CreatePayloadManifestsVistor visitor = new CreatePayloadManifestsVistor(manifestToMessageDigestMap, false);
        Files.walkFileTree(Paths.get(dataDir.toURI()), visitor);
        b.getPayLoadManifests().addAll(manifestToMessageDigestMap.keySet());
        ManifestWriter.writePayloadManifests(b.getPayLoadManifests(), b.getRootDir(), b.getRootDir(), b.getFileEncoding());
        final String manifestCopy = FileUtils.readFileToString(new File(b.getRootDir().toFile(), "manifest-sha256.txt"));

        // write bag-info.txt
        bagInfo.addToMetadata(b);
        MetadataWriter.writeBagMetadata(b.getMetadata(), b.getVersion(), b.getRootDir(), b.getFileEncoding());

        // write the aptrust-info.txt
        final Path aptrustInfoPath = new File(b.getRootDir().toFile(), "aptrust-info.txt").toPath();
        Files.write(aptrustInfoPath, ("Title : " + aptrustInfo.getTitle() + System.lineSeparator()).getBytes(charset),
                StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        Files.write(aptrustInfoPath, ("Access : " + aptrustInfo.getAccess() + System.lineSeparator()).getBytes(charset),
                StandardOpenOption.APPEND, StandardOpenOption.CREATE);

        // write the tag manifest
        Manifest tagManifest = new Manifest(algorithm);
        tagManifest.getFileToChecksumMap().put(aptrustInfoPath, Hasher.hash(aptrustInfoPath, messageDigest));
        final File bagInfoFile = new File(b.getRootDir().toFile(), "bag-info.txt");
        tagManifest.getFileToChecksumMap().put(bagInfoFile.toPath(), Hasher.hash(bagInfoFile.toPath(), messageDigest));
        b.getTagManifests().add(tagManifest);
        ManifestWriter.writeTagManifests(b.getTagManifests(), b.getRootDir(), b.getRootDir(), b.getFileEncoding());

        if (tar) {
            final BagSummary result = tarDirectory(bagOutputFile, manifestCopy.toString(), payloadSize);
            FileUtils.deleteDirectory(bagOutputFile);
            return result;
        } else {
            return new BagSummary(bagOutputFile, null, manifestCopy.toString(), payloadSize);
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

    protected abstract List<PendingPayloadFile> getPayloadFiles() throws Exception;

    /**
     * Will be called once the payload file (returned by getPayloadFiles())
     * has been used and will not be needed.  This allows temporary files
     * to be cleaned up by implementing classes.
     */
    protected abstract void freePayloadFile(PendingPayloadFile f) throws Exception;

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

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
import java.util.ArrayList;
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
     * @param destinationDir the directory into which the bag will be serialized
     * @param tar if true, a tar file will be the ultimate output, if false a simple
     *            directory structure (note: the current implementation writes out
     *            a directory structure first and tars it second)
     * @return the file that represents the root of the bag (either the tar file or
     * the bag directory)
     * @throws Exception
     */
    public File serializeAPTrustBag(File destinationDir, boolean tar) throws Exception {
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

        if (tar) {
            final File tarFile = tarDirectory(file);
            FileUtils.deleteDirectory(file);
            return tarFile;
        } else {
            return file;
        }
    }

    private File tarDirectory(final File file) throws IOException {
        final File tarFile = new File(file.getAbsolutePath() + ".tar");
        final FileOutputStream dest = new FileOutputStream(tarFile);
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
        return tarFile;
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
}

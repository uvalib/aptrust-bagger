package edu.virginia.lib.aptrust.bags;

import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.exceptions.CorruptChecksumException;
import gov.loc.repository.bagit.reader.BagReader;
import gov.loc.repository.bagit.verify.BagVerifier;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

public class FileBagTest extends APTrustBagTest {

    private File outputDir;

    @Before
    public void createTestBag() {
        outputDir = new File("target/output");
    }

    @After
    public void deleteTestBag() {

    }

    @Test
    public void testCollectionConstructor() throws Exception {
        String id = UUID.randomUUID().toString();
        File f1 = new File(UUID.randomUUID().toString());
        File f2 = new File(UUID.randomUUID().toString());
        FileBag b = new FileBag("test", null, null, id, Arrays.asList(new File[]{f1, f2}));
        Assert.assertEquals("ID must be preserved!", id, b.getItemId());
        Assert.assertEquals("Passed files must be preserved!", f1, b.getPayloadFiles().get(0).getFile());
        Assert.assertEquals("Passed files must be preserved!", f2, b.getPayloadFiles().get(1).getFile());
    }

    @Test
    public void testVariableLengthArgumentsConstructor() throws Exception {
        String id = UUID.randomUUID().toString();
        File f1 = new File(UUID.randomUUID().toString());
        File f2 = new File(UUID.randomUUID().toString());
        FileBag b = new FileBag("test", null, null, id, f1, f2);
        Assert.assertEquals("ID must be preserved!", id, b.getItemId());
        Assert.assertEquals("Passed files must be preserved!", f1, b.getPayloadFiles().get(0).getFile());
        Assert.assertEquals("Passed files must be preserved!", f2, b.getPayloadFiles().get(1).getFile());
    }

    @Test
    public void testBagSerialization() throws Exception {
        String id = UUID.randomUUID().toString();
        File f1 = createDummyFile(500);
        File f2 = createDummyFile(800);

        BagInfo bagInfo = new BagInfo();
        APTrustInfo aptrustInfo = new APTrustInfo("Title", APTrustInfo.CONSORTIA);

        FileBag b = new FileBag("test", bagInfo, aptrustInfo, id, new PendingPayloadFile(f1, "f1.random"), new PendingPayloadFile(f2, "subdir/f2.random"));
        b.serializeAPTrustBag(outputDir, false);

        BagReader r = new BagReader();
        final Bag bag = r.read(new File(outputDir, b.getInstitutionalId() + "." + b.getItemId()).toPath());
        try {
            BagVerifier v = new BagVerifier();
            v.isValid(bag, false);
        } catch (Throwable t) {
            t.printStackTrace();
            Assert.fail("Bag should be valid! " + t.getMessage());
        }
    }

    @Test(expected=CorruptChecksumException.class)
    public void testCorruptedBagSerialization() throws Exception {
        String id = UUID.randomUUID().toString();
        File f1 = createDummyFile(500);
        File f2 = createDummyFile(800);

        BagInfo bagInfo = new BagInfo();
        APTrustInfo aptrustInfo = new APTrustInfo("Title", APTrustInfo.CONSORTIA);

        FileBag b = new FileBag("test", bagInfo, aptrustInfo, id, f1, f2);
        b.serializeAPTrustBag(outputDir, false);

        File bagRoot = new File(outputDir, b.getInstitutionalId() + "." + b.getItemId());
        File f1InBag = new File(bagRoot, "data" + File.separator + f1.getName());
        FileOutputStream fos = new FileOutputStream(f1InBag, true);
        try {
            fos.write('x');
        } finally {
            fos.close();
        }

        BagReader r = new BagReader();
        final Bag bag = r.read(bagRoot.toPath());
        BagVerifier v = new BagVerifier();
        v.isValid(bag, false);
    }

    private File createDummyFile(long length) throws IOException {
        File dummyFile = File.createTempFile("dummy-file", ".dummy");
        FileOutputStream fos = new FileOutputStream(dummyFile);
        try {
            Random r = new Random();
            byte[] buffer = new byte[1024];
            for (long i = 0; i < length; i += 1024) {
                r.nextBytes(buffer);
                fos.write(buffer, 0, Math.min(1024, (int) (length - i)));
            }
        } finally {
            fos.close();
        }
        return dummyFile;
    }
}

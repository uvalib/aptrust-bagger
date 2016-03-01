package edu.virginia.lib.aptrust.bags;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public abstract class APTrustBagTest {

    @Test 
    public void testTar() throws Exception {
        File tempFile = File.createTempFile("prefix", "suffix");
        FileUtils.writeStringToFile(tempFile, "THIS IS THE CONTENT OF THE FILE");
        APTrustBag b = new FileBag(new BagInfo(), new APTrustInfo("Title", "Consortia"), "test", tempFile);
        b.serializeAPTrustBag(new File("target"), true);
    }
    
}

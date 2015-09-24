package edu.virginia.lib.aptrust.bags;

import org.apache.commons.codec.DecoderException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BagSummaryTest {

    private BagSummary example;

    @Before
    public void setUp() throws NoSuchAlgorithmException {
        final MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update("test".getBytes());
        example = new BagSummary(new File("test"), digest.digest(), "", 0);
    }

    @Test
    public void testMD5Conversion() throws DecoderException {
        String base16 = "098f6bcd4621d373cade4e832627b4f6";
        String base64 = "CY9rzUYh03PK3k6DJie09g==";
        Assert.assertEquals(base16, example.getHexChecksum());
        Assert.assertEquals(base64, example.getBase64Checksum());
    }
}

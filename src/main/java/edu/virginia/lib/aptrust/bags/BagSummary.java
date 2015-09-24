package edu.virginia.lib.aptrust.bags;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import java.io.File;
import java.io.UnsupportedEncodingException;

public class BagSummary {

    private File file;

    private byte[] checksum;
    
    private String manifestCopy;
    
    private long payloadSize;

    public BagSummary(File file, byte[] checksum, String manifestCopy, long payloadSize) {
        this.file = file;
        this.checksum = checksum;
        this.manifestCopy = manifestCopy;
        this.payloadSize = payloadSize;
    }

    public File getFile() {
        return this.file;
    }

    public byte[] getChecksumBytes() {
        return this.checksum;
    }
    
    public String getManifestCopy() {
    	return this.manifestCopy;
    }
    
    public long getBagPayloadSize() {
    	return this.payloadSize;
    }

    public String getBase64Checksum() {
        if (checksum == null) {
            return null;
        } else {
            try {
                return new String(Base64.encodeBase64(checksum), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getHexChecksum() {
        if (checksum == null) {
            return null;
        } else {
            return String.valueOf(Hex.encodeHex(checksum));
        }
    }

}

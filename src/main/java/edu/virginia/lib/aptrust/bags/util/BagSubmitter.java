package edu.virginia.lib.aptrust.bags.util;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import edu.virginia.lib.aptrust.bags.BagSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *  A general purpose utility to submit bags to AP Trust.
 */
public class BagSubmitter {

    final private static Logger LOGGER = LoggerFactory.getLogger(BagSubmitter.class);

    private AmazonS3Client s3Client;

    private String bucketName;

    private long chunkSize = (5 * 1024 * 1025 * 1024);

    public BagSubmitter(AmazonS3Client s3Client, final String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    /**
     * Transfers the bag and returns a summary of the operation.
     * @param bagSummary info about the the file (bag) to transfer
     */
    public TransferSummary transferBag(BagSummary bagSummary, final boolean overwrite) {
        TransferSummary t = new TransferSummary();
        t.startTime = System.currentTimeMillis();
        File f = bagSummary.getFile();
        t.size = f.length();
        try {
            final S3ObjectSummary o = getObjectSummary(f.getName());
            if (o != null) {
                if (overwrite) {
                    // delete the bag
                    s3Client.deleteObject(bucketName, f.getName());
                    t.deletedExistingBag = true;
                    final long duration = System.currentTimeMillis() - t.startTime;
                    LOGGER.info(f.getName() + "," + new Date() + ",deleted existing bag," + o.getSize() + ",,"
                            + duration + ",deleted existing object (created " + o.getLastModified()
                            + ") on S3 before transfer");
                } else {
                    return TransferSummary.WOULD_NOT_OVERWRITE();
                }
            }
            putFile(f, bagSummary.getBase64Checksum(), t);
            t.endTime = System.currentTimeMillis();
            return t;
        } catch (Throwable thr) {
            LOGGER.error("Error transferring file " + f.getName() + "!", thr);
            t.transferred = false;
            t.endTime = System.currentTimeMillis();
            t.message = thr.getClass().getSimpleName() + ": " + thr.getMessage();
            return t;
        }
    }

    private void putFile(File f, String checksum64, TransferSummary t) throws Throwable {
        t.localBagChecksum = checksum64;
        if (f.length() > chunkSize) {
            putLargeFile(f, checksum64, t);
        } else {
            putSmallFile(f, checksum64, t);
        }
    }

    private void putSmallFile(File f, String checksum64, TransferSummary t) {
        final long start = System.currentTimeMillis();
        final PutObjectResult result = s3Client.putObject(bucketName, f.getName(), f);
        t.transferred = true;
        t.amazonBagChecksum = result.getContentMd5();
        final long duration = System.currentTimeMillis() - start;
        LOGGER.info(f.getName() + "," + new Date() + ",transferred to S3," + f.length() + "," + t.amazonBagChecksum + ","
                + duration + "," + (!t.amazonBagChecksum.equalsIgnoreCase(checksum64) ? "CHECKSUM MISMATCH" : ""));
    }

    private void putLargeFile(File f, String checksum64, TransferSummary t) throws Throwable {
        final long start = System.currentTimeMillis();
        final InitiateMultipartUploadRequest req = new InitiateMultipartUploadRequest(bucketName, f.getName());
        InitiateMultipartUploadResult multipartUploadResult = s3Client.initiateMultipartUpload(req);
        try {
            List<PartETag> partETags = new ArrayList<PartETag>();
            int partNumber = 1;
            for (long offset = 0; offset < f.length(); offset += chunkSize) {
                final UploadPartRequest partRequest = new UploadPartRequest()
                        .withUploadId(multipartUploadResult.getUploadId())
                        .withPartNumber(partNumber++)
                        .withPartSize(Math.min(chunkSize, f.length() - offset))
                        .withBucketName(bucketName)
                        .withKey(f.getName())
                        .withFile(f)
                        .withFileOffset(offset);
                partRequest.setLastPart(chunkSize >= f.length() - offset);
                final UploadPartResult result = s3Client.uploadPart(partRequest);
                partETags.add(result.getPartETag());
            }
            CompleteMultipartUploadResult r = s3Client.completeMultipartUpload(new CompleteMultipartUploadRequest(bucketName, f.getName(), multipartUploadResult.getUploadId(), partETags));
            final long duration = System.currentTimeMillis() - start;
            t.amazonBagChecksum = "checksum not yet available";
            LOGGER.info(f.getName() + "," + new Date() + ",transferred to S3," + f.length() + "," + t.amazonBagChecksum + ","
                    + duration + ",");
            t.transferred = true;
        } catch (Throwable thr) {
            s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, f.getName(), multipartUploadResult.getUploadId()));
            LOGGER.error("ERROR with multipart upload!", thr);
            t.transferred = false;
            t.message = "ERROR with multipart upload!  " + thr.getClass().getSimpleName() + ": " + t.getMessage();
            throw thr;
        }
    }

    private S3ObjectSummary getObjectSummary(String key) {
        final ObjectListing l = s3Client.listObjects(bucketName, key);
        final List<S3ObjectSummary> objects = l.getObjectSummaries();
        if (objects != null) {
            for (S3ObjectSummary o : objects) {
                if (key.equals(o.getKey())) {
                    return o;
                }
            }
        }
        return null;
    }

    public static class TransferSummary {

        private boolean transferred;

        private String amazonBagChecksum;

        private String localBagChecksum;

        private long size;

        private boolean deletedExistingBag;

        private long startTime;

        private long endTime;

        private String message;

        public static TransferSummary WOULD_NOT_OVERWRITE() {
            TransferSummary t = new TransferSummary();
            t.transferred = false;
            t.message = "Bag exists, did not overwrite!";
            return t;
        }

        public String getMessage() {
            return message;
        }

        public boolean wasTransferred() {
            return this.transferred;
        }

        public String getLocalBagChecksum() {
            return localBagChecksum;
        }

        public String getAmazonBagChecksum() {
            return amazonBagChecksum;
        }

        public long getBagSize() {
            return size;
        }

        public boolean deletedExistingBag() {
            return deletedExistingBag;
        }

        public long getDuration() {
            return this.endTime - this.startTime;
        }

    }

}

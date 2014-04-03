package edu.virginia.lib.aptrust.bags.util.uva;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
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
import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.FedoraCredentials;
import edu.virginia.lib.aptrust.bags.APTrustBag;
import edu.virginia.lib.aptrust.bags.BagSummary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * A special program whose purpose is to prepare, bag and submit a
 * sample of content for the AP Trust 2014 trial.
 */
public class TrialSubmissionBagger {

    public static void main(String[] args) throws Exception {

        FedoraClient fc = new FedoraClient(new FedoraCredentials(System.getProperty("fedora-url"),
                System.getProperty("fedora-username"), System.getProperty("fedora-password")));

        File bagDir = new File("target/bags");
        bagDir.mkdirs();

        TrialSubmissionBagger bagger = new TrialSubmissionBagger(bagDir, fc);
        for (String arg : args) {
            if ("-r".equals(arg) || "--retain-bags".equals(arg)) {
                bagger.retainBags();
            } else if ("-t".equals(arg) || "--transfer".equals(arg)) {
                bagger.transfer();
            } else if ("-o".equals(arg) || "--overwrite".equals(arg)) {
                bagger.overwrite();
            } else if (arg.startsWith("-c=")) {
                final String size = arg.substring("-c=".length());
                if (size.endsWith("G") || size.endsWith("g")) {
                    bagger.chunkSize = Long.parseLong(size) * (1024 * 1024 * 1024);
                } else if (size.endsWith("M") || size.endsWith("m")) {
                    bagger.chunkSize = Long.parseLong(size) * (1024 * 1024);
                } else {
                    bagger.chunkSize = Long.parseLong(size);
                }
            } else {
                throw new IllegalArgumentException("Unexpected argument: " + arg);
            }
        }

        // 6 WSLS clips, 6 WSLS scripts
        bagger.bagWSLSRecord("uva-lib:2271004");
        bagger.bagWSLSRecord("uva-lib:2274763");
        bagger.bagWSLSRecord("uva-lib:2221258");
        bagger.bagWSLSRecord("uva-lib:2274765");
        bagger.bagWSLSRecord("uva-lib:2274641");
        bagger.bagWSLSRecord("uva-lib:2238632"); // this one is over 5GB

        // A few Tracksys DL Content that happen to be available on mid-tier storage
        bagger.bagTracksysOriginatingPid("uva-lib:710281"); // a simple map
        bagger.bagTracksysOriginatingPid("uva-lib:2278801"); // a lengthy digitized book
        bagger.bagTracksysOriginatingPid("uva-lib:2141114"); // a short piece of sheet music

        bagger.report.println("TOTAL: " + bagger.bytes + "bytes");
        bagger.report.close();

    }

    /**
     * A PrintWriter whose output is a CSV with the following columns:
     * id
     * date
     * event (genereated, transferred)
     * size
     * md5
     * time elapsed
     * note
     */
    private PrintWriter report;

    private long bytes;

    private File bagCreationDir;

    private FedoraClient fedora;

    private boolean transfer;

    private AmazonS3Client s3Client;

    private String bucketName;

    private boolean retainBags;

    private boolean overwrite;

    private long chunkSize = (5 * 1024 * 1025 * 1024);

    public TrialSubmissionBagger(File bagCreationDir, FedoraClient fc) throws FileNotFoundException {
        this.bagCreationDir = bagCreationDir;
        this.fedora = fc;
        bytes = 0;
        transfer = false;
        retainBags = false;
        report = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new SimpleDateFormat("yyyy-MM-dd HH:mm ss.SSS").format(new Date()) + "-bag-report.csv")));
        report.println("id,date,event,size,md5,time elapsed,note");
    }

    public void retainBags() {
        retainBags = true;
    }

    public void overwrite() {
        overwrite = true;
    }

    /**
     * Sets up this bagger to transfer content.  This method expects there to be a "aws-credentials.properties" file
     * in the classpath with the 'accessKey' and 'secretKey' set.
     */
    public void transfer() throws IOException {
        Properties p = new Properties();
        p.load(getClass().getClassLoader().getResourceAsStream("aws-credentials.properties"));
        AWSCredentials credentials = new BasicAWSCredentials(p.getProperty("accessKey"), p.getProperty("secretKey"));
        s3Client = new AmazonS3Client(credentials);
        this.bucketName = p.getProperty("bucketName");
        transfer = true;
    }

    public void bagWSLSRecord(String pid) throws Exception {
        WSLSFedoraObjectBagger b = new WSLSFedoraObjectBagger(fedora);
        writeBagsAndGenerateReport(b.getSubgraphBag(pid));
    }

    public void bagTracksysOriginatingPid(String pid) throws Exception {
        FedoraObjectBagger b = new FedoraObjectBagger(fedora);
        writeBagsAndGenerateReport(b.getSubgraphBag(pid));
    }

    private void writeBagsAndGenerateReport(Iterator<APTrustBag> bagIt) throws Exception {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm ss.SSS");
        while (bagIt.hasNext()) {
            APTrustBag bag = bagIt.next();
            final long start = System.currentTimeMillis();
            final BagSummary bagSummary = bag.serializeAPTrustBag(bagCreationDir, true);
            final long duration = System.currentTimeMillis() - start;
            final File bagFile = bagSummary.getFile();
            report.println(bagFile.getName() + "," + new Date() + ",created," + bagFile.length() + ","
                    + bagSummary.getBase64Checksum() + "," + duration + ",");
            report.flush();
            bytes += bagFile.length();
            if (s3Client != null) {
                transferFile(bagSummary);
            }
            if (!retainBags) {
                bagFile.delete();
            }
        }
    }

    /**
     * Transfers the bag and returns its checksum.
     * @param bagSummary info about the the file (bag) to transfer
     */
    private boolean transferFile(BagSummary bagSummary) {
        File f = bagSummary.getFile();
        try {
            final S3ObjectSummary o = getObjectSummary(f.getName());
            if (o != null) {
                if (overwrite) {
                    // delete the bag
                    final long start = System.currentTimeMillis();
                    s3Client.deleteObject(bucketName, f.getName());
                    final long duration = System.currentTimeMillis() - start;
                    report.println(f.getName() + "," + new Date() + ",deleted existing bag," + o.getSize() + ",,"
                            + duration + ",deleted existing object (created " + o.getLastModified()
                            + ") on S3 before transfer");
                    report.flush();
                } else {
                    return false;
                }
            }

            putFile(f, bagSummary.getBase64Checksum());
            report.flush();
            return true;
        } catch (Throwable t) {
            report.println("Error transferring file " + f.getName() + "!");
            t.printStackTrace(report);
            return false;
        }
    }

    private void putFile(File f, String checksum64) {
        if (f.length() > chunkSize) {
            try {
                putLargeFile(f, checksum64);
            } catch (IOException e) {
                report.println("ERROR");
                e.printStackTrace(report);
                report.flush();
            }
        } else {
            putSmallFile(f, checksum64);
        }
    }

    private void putSmallFile(File f, String checksum64) {
        final long start = System.currentTimeMillis();
        final PutObjectResult result = s3Client.putObject(bucketName, f.getName(), f);
        final String amazonMD5 = result.getContentMd5();
        final long duration = System.currentTimeMillis() - start;
        report.println(f.getName() + "," + new Date() + ",transferred to S3," + f.length() + "," + amazonMD5 + ","
                + duration + "," + (!amazonMD5.equalsIgnoreCase(checksum64) ? "CHECKSUM MISMATCH" : ""));
        report.flush();
    }

    private void putLargeFile(File f, String checksum64) throws IOException {
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
            final String amazonMD5 = "checksum not yet available";
            report.println(f.getName() + "," + new Date() + ",transferred to S3," + f.length() + "," + amazonMD5 + ","
                    + duration + ",");
            report.flush();
        } catch (Throwable t) {
            s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, f.getName(), multipartUploadResult.getUploadId()));
            report.println("ERROR with multipart upload!");
            t.printStackTrace(report);
            report.flush();
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

}

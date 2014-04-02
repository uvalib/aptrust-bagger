package edu.virginia.lib.aptrust.bags.util.uva;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.FedoraCredentials;
import edu.virginia.lib.aptrust.bags.APTrustBag;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

    private PrintWriter report;

    private long bytes;

    private File bagCreationDir;

    private FedoraClient fedora;

    private boolean transfer;

    private AmazonS3Client s3Client;

    private String bucketName;

    private boolean retainBags;

    public TrialSubmissionBagger(File bagCreationDir, FedoraClient fc) throws FileNotFoundException {
        this.bagCreationDir = bagCreationDir;
        this.fedora = fc;
        bytes = 0;
        transfer = false;
        retainBags = false;
        report = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new SimpleDateFormat("yyyy-MM-dd HH:mm ss.SSS").format(new Date()) + "-bag-report.txt")));
    }

    public void retainBags() {
        retainBags = true;
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
            File bagFile = bag.serializeAPTrustBag(bagCreationDir, true);
            report.println(df.format(new Date()) + " " + bagFile.getName() + " " + bagFile.length() + "bytes");
            report.flush();
            bytes += bagFile.length();
            if (s3Client != null) {
                transferFileIfNotPresent(bagFile);
            }
            if (!retainBags) {
                bagFile.delete();
            }
        }
    }

    private boolean transferFileIfNotPresent(File f) {
        try {
            final S3ObjectSummary o = getObjectSummary(f.getName());
            if (o != null) {
                report.println(o.getKey() + " already exists: " + o.getSize() + "bytes last modified "
                        + o.getLastModified());
                return false;
            }


            final long start = System.currentTimeMillis();
            s3Client.putObject(bucketName, f.getName(), f);
            final long time = System.currentTimeMillis() - start;
            report.println(f.getName() + " transferred to S3 in " + time + "ms.");
            return true;
        } catch (Throwable t) {
            report.println("Error transferring file " + f.getName() + "!");
            t.printStackTrace(report);
            return false;
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

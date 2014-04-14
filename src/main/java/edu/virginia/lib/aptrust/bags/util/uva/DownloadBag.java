package edu.virginia.lib.aptrust.bags.util.uva;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class DownloadBag {

    public static void main(String [] args) throws Exception {
        File downloadDir = new File("download");
        downloadDir.mkdirs();

        final Properties p = new Properties();
        p.load(DownloadBag.class.getClassLoader().getResourceAsStream("aws-credentials.properties"));
        AWSCredentials credentials = new BasicAWSCredentials(p.getProperty("accessKey"), p.getProperty("secretKey"));
        final AmazonS3Client s3Client = new AmazonS3Client(credentials);
        final String bucketName = p.getProperty("bucketName");

        List<S3ObjectSummary> objects = s3Client.listObjects(bucketName).getObjectSummaries();
        System.out.println(objects.size() + " bags found.");
        for (S3ObjectSummary o : objects) {
            final File f = new File(downloadDir, o.getKey());
            final FileOutputStream fos = new FileOutputStream(f);
            final InputStream is = s3Client.getObject(bucketName, o.getKey()).getObjectContent();
            IOUtils.copy(is, fos);
            is.close();
            fos.close();
            break;
        }
    }
}

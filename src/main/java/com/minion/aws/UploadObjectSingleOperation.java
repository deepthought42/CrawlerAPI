package com.minion.aws;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

/**
 * Handles uploading and reading files from Amazon S3
 */
public class UploadObjectSingleOperation {
	private static Logger log = LoggerFactory.getLogger(UploadObjectSingleOperation.class);

	private static String bucketName     = "qanairy";
	
	public static String saveImageToS3(BufferedImage image, String domain, String page_key) {
		AWSCredentials credentials = new BasicAWSCredentials("AKIAIYBDBXPUQPKLDDXA","NUOCJBgqo943B784dTjjF6JC5PyK9lWg9hh73Mk2");;
		String filepath = null;
		// credentials=new ProfileCredentialsProvider().getCredentials();
        AmazonS3 s3client = new AmazonS3Client(credentials);
        try {
        	ByteArrayOutputStream os = new ByteArrayOutputStream();
        	ImageIO.write(image, "png", os);
        	byte[] buffer = os.toByteArray();
        	InputStream is = new ByteArrayInputStream(buffer);
        	ObjectMetadata meta = new ObjectMetadata();
        	meta.setContentLength(buffer.length);
            log.debug("Uploading a new object to S3 from a file: "+ image);
            s3client.putObject(new PutObjectRequest(
             		                 bucketName, domain+"/"+page_key+".png", is, meta).withCannedAcl(CannedAccessControlList.PublicRead));
            
            filepath = "https://s3-us-west-2.amazonaws.com/qanairy/"+domain+"/"+page_key+".png";
         } catch (AmazonServiceException ase) {
            log.error("Caught an AmazonServiceException, which " +
            		"means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            log.error("Error Message:    " + ase.getMessage());
            log.error("HTTP Status Code: " + ase.getStatusCode());
            log.error("AWS Error Code:   " + ase.getErrorCode());
            log.error("Error Type:       " + ase.getErrorType());
            log.error("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            log.error("Caught an AmazonClientException, which " +
            		"means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            log.error("Error Message: " + ace.getMessage());
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return filepath;
    }
	
	public static InputStream getImageFromS3(String url){
		AWSCredentials credentials = new BasicAWSCredentials("AKIAIYBDBXPUQPKLDDXA","NUOCJBgqo943B784dTjjF6JC5PyK9lWg9hh73Mk2");;

		// credentials=new ProfileCredentialsProvider().getCredentials();
        AmazonS3 s3client = new AmazonS3Client(credentials);
        
        try {
            log.debug("Uploading a new object to S3 from a filen: "+url);
            S3Object object = s3client.getObject(new GetObjectRequest(bucketName, url));
            InputStream objectData = object.getObjectContent();
            
            return objectData;
         } catch (AmazonServiceException ase) {
            log.error("Caught an AmazonServiceException, which " +
            		"means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            log.error("Error Message:    " + ase.getMessage());
            log.error("HTTP Status Code: " + ase.getStatusCode());
            log.error("AWS Error Code:   " + ase.getErrorCode());
            log.error("Error Type:       " + ase.getErrorType());
            log.error("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            log.error("Caught an AmazonClientException, which " +
            		"means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            log.error("Error Message: " + ace.getMessage());
        }
        
        return null;
	}
}
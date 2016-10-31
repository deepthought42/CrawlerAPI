package com.minion.aws;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.openqa.selenium.Point;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

public class UploadObjectSingleOperation {
	private static String bucketName     = "qanairy";
	
	public static String saveImageToS3(File image, String domain, String url) throws IOException {
		AWSCredentials credentials = new BasicAWSCredentials("AKIAIYBDBXPUQPKLDDXA","NUOCJBgqo943B784dTjjF6JC5PyK9lWg9hh73Mk2");;

		// credentials=new ProfileCredentialsProvider().getCredentials();
        AmazonS3 s3client = new AmazonS3Client(credentials);
        try {
        	
            System.out.println("Uploading a new object to S3 from a filen: "+domain+"/"+image.getName() + " " + image);
            s3client.putObject(new PutObjectRequest(
            		                 bucketName, domain+""+url+".png", image).withCannedAcl(CannedAccessControlList.PublicRead));
            return "https://s3-us-west-2.amazonaws.com/qanairy/"+domain+""+url+".png";
         } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which " +
            		"means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which " +
            		"means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
        
        return null;
    }
	
	public static InputStream getImageFromS3(String url){
		AWSCredentials credentials = new BasicAWSCredentials("AKIAIYBDBXPUQPKLDDXA","NUOCJBgqo943B784dTjjF6JC5PyK9lWg9hh73Mk2");;

		// credentials=new ProfileCredentialsProvider().getCredentials();
        AmazonS3 s3client = new AmazonS3Client(credentials);
        
        try {
            System.out.println("Uploading a new object to S3 from a filen: "+url);
            S3Object object = s3client.getObject(new GetObjectRequest(bucketName, url));
            InputStream objectData = object.getObjectContent();
            
            return objectData;
         } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which " +
            		"means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which " +
            		"means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
        
        return null;
	}
}
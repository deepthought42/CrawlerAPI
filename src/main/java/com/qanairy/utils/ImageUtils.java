package com.qanairy.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

/**
 * A collection of methods for working with Images
 */
public class ImageUtils {

	public static String getFileChecksum(URL url) throws IOException
	{
		
		//InputStream is = new URL(url).openStream(); 
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BufferedImage img = ImageIO.read(url);
		/*
		//turn image to grayscale
		//get image width and height
		int width = img.getWidth();
		int height = img.getHeight();
		*/
		/*
		for(int y = 0; y < height; y++){
		  for(int x = 0; x < width; x++){
			  int p = img.getRGB(x,y);
			  int a = (p>>24)&0xff;
			  int r = (p>>16)&0xff;
			  int g = (p>>8)&0xff;
			  int b = p&0xff;
			  
			  int avg = (r+g+b)/3;
			  p = (a<<24) | (avg<<16) | (avg<<8) | avg;
			  img.setRGB(x, y, p);
		  }
		}
	*/
		/*
		// creates output image
		BufferedImage outputImage = null;
        // creates output image
        if(height/10 > 100 && width/10 > 100){
        	outputImage = new BufferedImage(width/10, height/10, img.getType());
 
	        // scales the input image to the output image
	        Graphics2D g2d = outputImage.createGraphics();
	        g2d.drawImage(img, 0, 0, width/10, height/10, null);
	        g2d.dispose();
	 
        }
        else{
        	outputImage = img;
        }
		boolean foundWriter = ImageIO.write(outputImage, "png", baos);
		assert foundWriter; // Not sure about this... with jpg it may work but other formats ?
    
		String image_hex = Hex.encodeHexString(baos.toByteArray());
		StringBuilder sb = new StringBuilder();
		for(int idx = 0; idx<image_hex.length(); idx+=97){
			if(sb.toString().length() > 512){
				break;
			}
			sb.append(image_hex.charAt(idx));			
		}
		
	    return sb.toString();
		
		
		*/
		boolean foundWriter = ImageIO.write(img, "png", baos);
		assert foundWriter; // Not sure about this... with jpg it may work but other formats ?

		 try {
			
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			byte[] thedigest = sha.digest(baos.toByteArray());
	        return DatatypeConverter.printHexBinary(thedigest);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	    return "";
	    
	    /*
	    //Create byte array to read data in chunks
	    byte[] byteArray = new byte[1024];
	    int bytesCount = 0;
	      
	    //Read file data and update in message digest
	    while ((bytesCount = is.read(byteArray)) != -1) {
	        digest.update(byteArray, 0, bytesCount);
	    };
	     
	    //close the stream; We don't need it now.
	    is.close();
	     
	    //Get the hash's bytes
	    byte[] bytes = digest.digest();
	     
	    //This bytes[] has bytes in decimal format;
	    //Convert it to hexadecimal format
	    StringBuilder sb = new StringBuilder();
	    for(int i=0; i< bytes.length ;i++)
	    {
	        sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
	    }
	     
	   //return complete hash
	   return sb.toString();
	   */
	}
	
	public static String getFileChecksum(BufferedImage buff_img) throws IOException
	{		
		/*
		BufferedImage img = new BufferedImage(buff_img.getWidth(), buff_img.getHeight(), buff_img.getType());
	    Graphics graphics = img.getGraphics();
	    graphics.drawImage(buff_img, 0, 0, null);
	    graphics.dispose();
	    //InputStream is = new URL(url).openStream(); 
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		//turn image to grayscale
		//get image width and height
		int width = img.getWidth();
		int height = img.getHeight();
		*/
/*
        for(int y = 0; y < height; y++){
		  for(int x = 0; x < width; x++){
			  int p = img.getRGB(x,y);
			  int a = (p>>24)&0xff;
			  int r = (p>>16)&0xff;
			  int g = (p>>8)&0xff;
			  int b = p&0xff;
			  
			  int avg = (r+g+b)/3;
			  p = (a<<24) | (avg<<16) | (avg<<8) | avg;
			  img.setRGB(x, y, p);
		  }
		}
  */      
		/*
        BufferedImage outputImage = null;
        // creates output image
        if(height/10 > 100 && width/10 > 100){
	        outputImage = new BufferedImage(width/10, height/10, img.getType());
	 
	        // scales the input image to the output image
	        Graphics2D g2d = outputImage.createGraphics();
	        g2d.drawImage(outputImage, 0, 0, width/10, height/10, null);
	        g2d.dispose();
        }
        else{
        	outputImage = img;
        }		
		boolean foundWriter = ImageIO.write(outputImage, "png", baos);
		assert foundWriter; // Not sure about this... with jpg it may work but other formats ?

		String image_hex = Hex.encodeHexString(baos.toByteArray());

		StringBuilder sb = new StringBuilder();

		for(int idx = 0; idx<image_hex.length(); idx+=97){
			if(sb.toString().length() > 512){
				break;
			}
			sb.append(image_hex.charAt(idx));
			
		}
        return sb.toString();
		*/
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		boolean foundWriter = ImageIO.write(buff_img, "png", baos);
		assert foundWriter; // Not sure about this... with jpg it may work but other formats ?
	    //Get file input stream for reading the file content
	    
	    try {
			MessageDigest sha = MessageDigest.getInstance("SHA-256");
			byte[] thedigest = sha.digest(baos.toByteArray());
			baos.close();
	        return DatatypeConverter.printHexBinary(thedigest);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	    return "";
	    
	}

}

package com.looksee.gcp;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Block;
import com.google.cloud.vision.v1.ColorInfo;
import com.google.cloud.vision.v1.DominantColorsAnnotation;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.FaceAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.LocationInfo;
import com.google.cloud.vision.v1.Page;
import com.google.cloud.vision.v1.Paragraph;
import com.google.cloud.vision.v1.Symbol;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.cloud.vision.v1.WebDetection;
import com.google.cloud.vision.v1.WebDetection.WebEntity;
import com.google.cloud.vision.v1.WebDetection.WebImage;
import com.google.cloud.vision.v1.WebDetection.WebLabel;
import com.google.cloud.vision.v1.WebDetection.WebPage;
import com.google.cloud.vision.v1.Word;
import com.google.protobuf.ByteString;
import com.looksee.models.audit.ColorUsageStat;
import com.looksee.utils.ImageUtils;

/**
 * Contains methods for analyzing analyzing images using the Google Cloud Vision API
 */
public class CloudVisionUtils {
	private static Logger log = LoggerFactory.getLogger(CloudVisionUtils.class);

    /**
	 * Detects image properties such as color frequency from the specified local image.
	 * 
	 * @param image_url
	 * @throws IOException
	 */
	public static List<String> extractImageText(BufferedImage buffered_image) throws IOException {
		List<String> text_values = new ArrayList<>();
		 
	    List<AnnotateImageRequest> requests = new ArrayList<>();
	    buffered_image = ImageUtils.resize(buffered_image, 768, 1024);
	    ByteArrayOutputStream os = new ByteArrayOutputStream();
	    ImageIO.write(buffered_image, "png", os);
	    InputStream input_stream = new ByteArrayInputStream(os.toByteArray());
	    
	    ByteString imgBytes = ByteString.readFrom(input_stream);
	
	    Image img = Image.newBuilder().setContent(imgBytes).build();
	    Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
	    AnnotateImageRequest request =
	        AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
	    requests.add(request);
	
	    // Initialize client that will be used to send requests. This client only needs to be created
	    // once, and can be reused for multiple requests. After completing all of your requests, call
	    // the "close" method on the client to safely clean up any remaining background resources.
	    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
	    	BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
	    	List<AnnotateImageResponse> responses = response.getResponsesList();
	    	
	    	for (AnnotateImageResponse res : responses) {
	      		/*
    			log.warn("response label annotations :: " +res.getLabelAnnotationsList());
	      		log.warn("Full Text annotation :: " +res.getFullTextAnnotation());
		        log.warn("text annotations :   "+res.getTextAnnotationsList());
*/
    			
		        if (res.hasError()) {
		        	log.warn("Error: %s%n", res.getError().getMessage());
		        	return new ArrayList<>();
		        }
		        
		        // For full list of available annotations, see http://g.co/cloud/vision/docs
		        for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
		        	text_values.add(annotation.getDescription());
		        }
	      	}
	    }
        return text_values;
	}
	
	 /**
		 * Detects image properties such as color frequency from the specified local image.
		 * 
		 * @param image_url
		 * @throws IOException
		 */
		public static List<String> extractImageParagraphs(BufferedImage buffered_image) throws IOException {
			List<String> text_values = new ArrayList<>();
			 
		    List<AnnotateImageRequest> requests = new ArrayList<>();
		    buffered_image = ImageUtils.resize(buffered_image, 768, 1024);
		    ByteArrayOutputStream os = new ByteArrayOutputStream();
		    ImageIO.write(buffered_image, "png", os);
		    InputStream input_stream = new ByteArrayInputStream(os.toByteArray());
		    
		    ByteString imgBytes = ByteString.readFrom(input_stream);
		
		    Image img = Image.newBuilder().setContent(imgBytes).build();
		    Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
		    AnnotateImageRequest request =
		        AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
		    requests.add(request);
		
		    // Initialize client that will be used to send requests. This client only needs to be created
		    // once, and can be reused for multiple requests. After completing all of your requests, call
		    // the "close" method on the client to safely clean up any remaining background resources.
		    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
		    	BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
		    	List<AnnotateImageResponse> responses = response.getResponsesList();
		    	    	
	    		for (AnnotateImageResponse res : responses) {
		      		/*
	    			log.warn("response label annotations :: " +res.getLabelAnnotationsList());
		      		log.warn("Full Text annotation :: " +res.getFullTextAnnotation());
			        log.warn("text annotations :   "+res.getTextAnnotationsList());
	*/
	    			
			        if (res.hasError()) {
			          System.out.format("Error: %s%n", res.getError().getMessage());
			          return new ArrayList<>();
			        }
			        TextAnnotation annotation = res.getFullTextAnnotation();
			        for (Page page : annotation.getPagesList()) {
			          String pageText = "";
			          for (Block block : page.getBlocksList()) {
			            String blockText = "";
			            for (Paragraph para : block.getParagraphsList()) {
			              String paraText = "";
			              
			              for (Word word : para.getWordsList()) {
			                String wordText = "";
			                for (Symbol symbol : word.getSymbolsList()) {
			                  wordText = wordText + symbol.getText();
			                 /*
			                  System.out.format(
			                      "Symbol text: %s (confidence: %f)%n",
			                      symbol.getText(), symbol.getConfidence());
			                      */
			                }
			                /*
			                System.out.format(
			                    "Word text: %s (confidence: %f)%n%n", wordText, word.getConfidence());
			                    */
			                paraText = String.format("%s %s", paraText, wordText);
			              }
			              
			              // Output Example using Paragraph:
			              System.out.println("%nParagraph: %n" + paraText);
			              System.out.format("Paragraph Confidence: %f%n", para.getConfidence());
			              text_values.add(paraText);

			              blockText = blockText + paraText;
			            }
			            pageText = pageText + blockText;
			          }
			        }
			        
			          
			        // For full list of available annotations, see http://g.co/cloud/vision/docs
			        /*
			        for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
			        	System.out.format("Text: %s%n", annotation.getDescription());
			        	System.out.format("Position : %s%n", annotation.getBoundingPoly());
			        	text_values.add(annotation.getDescription());
			        }
			        */
		      	}
		    }
	        return text_values;
		}
		
	/**
	 * Detects image properties such as color frequency from the specified local image.
	 * 
	 * @param image_url
	 * @throws IOException
	 */
	public static void extractImageLabels(BufferedImage buffered_image) throws IOException {
	    List<AnnotateImageRequest> requests = new ArrayList<>();
	    //InputStream url_input_stream = new URL(image_url).openStream();
	    ByteArrayOutputStream os = new ByteArrayOutputStream();
	    ImageIO.write(buffered_image, "jpeg", os);                          // Passing: ​(RenderedImage im, String formatName, OutputStream output)
	    InputStream input_stream = new ByteArrayInputStream(os.toByteArray());
	    
	    ByteString imgBytes = ByteString.readFrom(input_stream);
	
	    Image img = Image.newBuilder().setContent(imgBytes).build();
	    Feature feat = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
	    AnnotateImageRequest request =
	        AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
	    requests.add(request);
	
	    // Initialize client that will be used to send requests. This client only needs to be created
	    // once, and can be reused for multiple requests. After completing all of your requests, call
	    // the "close" method on the client to safely clean up any remaining background resources.
	    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
	    	BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
	    	List<AnnotateImageResponse> responses = response.getResponsesList();
	
	      	for (AnnotateImageResponse res : responses) {
		        if (res.hasError()) {
		          System.out.format("Error: %s%n", res.getError().getMessage());
		          return;
		        }
		
		        // For full list of available annotations, see http://g.co/cloud/vision/docs
		        log.warn("-----------------------Label Annotation list ----------------------------");
		        for (EntityAnnotation annotation : res.getLabelAnnotationsList()) {
		          annotation
		              .getAllFields()
		              .forEach((k, v) -> System.out.format("%s : %s%n", k, v.toString()));
		        }
	      	}
	    }
	}
	
	/**
	 * Detects image properties such as color frequency from the specified local image.
	 * 
	 * @param image_url
	 * @throws IOException
	 */
	public static void extractImageLandmarks(BufferedImage buffered_image) throws IOException {
	    List<AnnotateImageRequest> requests = new ArrayList<>();
	    //InputStream url_input_stream = new URL(image_url).openStream();
	    ByteArrayOutputStream os = new ByteArrayOutputStream();
	    ImageIO.write(buffered_image, "jpeg", os);                          // Passing: ​(RenderedImage im, String formatName, OutputStream output)
	    InputStream input_stream = new ByteArrayInputStream(os.toByteArray());
	    
	    ByteString imgBytes = ByteString.readFrom(input_stream);
	
	    Image img = Image.newBuilder().setContent(imgBytes).build();
	    Feature feat = Feature.newBuilder().setType(Feature.Type.LANDMARK_DETECTION).build();
	    AnnotateImageRequest request =
	        AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
	    requests.add(request);
	
	    // Initialize client that will be used to send requests. This client only needs to be created
	    // once, and can be reused for multiple requests. After completing all of your requests, call
	    // the "close" method on the client to safely clean up any remaining background resources.
	    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
	    	BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
	    	List<AnnotateImageResponse> responses = response.getResponsesList();
	
	      	for (AnnotateImageResponse res : responses) {
		        if (res.hasError()) {
		          System.out.format("Error: %s%n", res.getError().getMessage());
		          return;
		        }
		
		        // For full list of available annotations, see http://g.co/cloud/vision/docs
		        for (EntityAnnotation annotation : res.getLandmarkAnnotationsList()) {
		          LocationInfo info = annotation.getLocationsList().listIterator().next();
		          System.out.format("Landmark: %s%n %s%n", annotation.getDescription(), info.getLatLng());
		        }
	      	}
	    }
	}
	
	/**
	 * Detects image properties such as color frequency from the specified local image.
	 * 
	 * @param image_url
	 * @throws IOException
	 */
	public static void extractImageFaces(BufferedImage buffered_image) throws IOException {
	    List<AnnotateImageRequest> requests = new ArrayList<>();
	    //InputStream url_input_stream = new URL(image_url).openStream();
	    ByteArrayOutputStream os = new ByteArrayOutputStream();
	    ImageIO.write(buffered_image, "jpeg", os);                          // Passing: ​(RenderedImage im, String formatName, OutputStream output)
	    InputStream input_stream = new ByteArrayInputStream(os.toByteArray());
	    
	    ByteString imgBytes = ByteString.readFrom(input_stream);
	
	    Image img = Image.newBuilder().setContent(imgBytes).build();
	    Feature feat = Feature.newBuilder().setType(Feature.Type.FACE_DETECTION).build();
	    AnnotateImageRequest request =
	        AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
	    requests.add(request);
	
	    // Initialize client that will be used to send requests. This client only needs to be created
	    // once, and can be reused for multiple requests. After completing all of your requests, call
	    // the "close" method on the client to safely clean up any remaining background resources.
	    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
	    	BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
	    	List<AnnotateImageResponse> responses = response.getResponsesList();
	
	      	for (AnnotateImageResponse res : responses) {
		        if (res.hasError()) {
		          System.out.format("Error: %s%n", res.getError().getMessage());
		          return;
		        }
		
		        // For full list of available annotations, see http://g.co/cloud/vision/docs
		        for (FaceAnnotation annotation : res.getFaceAnnotationsList()) {
		          System.out.format(
		              "anger: %s%njoy: %s%nsurprise: %s%nposition: %s",
		              annotation.getAngerLikelihood(),
		              annotation.getJoyLikelihood(),
		              annotation.getSurpriseLikelihood(),
		              annotation.getBoundingPoly());
		        }
	      	}
	    }
	}
	
	/**
	 * Detects image properties such as color frequency from the specified local image.
	 * 
	 * @param image_url
	 * @throws IOException
	 */
	public static void extractImageLogos(BufferedImage buffered_image) throws IOException {
	    List<AnnotateImageRequest> requests = new ArrayList<>();
	    //InputStream url_input_stream = new URL(image_url).openStream();
	    ByteArrayOutputStream os = new ByteArrayOutputStream();
	    ImageIO.write(buffered_image, "jpeg", os);                          // Passing: ​(RenderedImage im, String formatName, OutputStream output)
	    InputStream input_stream = new ByteArrayInputStream(os.toByteArray());
	    
	    ByteString imgBytes = ByteString.readFrom(input_stream);
	
	    Image img = Image.newBuilder().setContent(imgBytes).build();
	    Feature feat = Feature.newBuilder().setType(Feature.Type.LOGO_DETECTION).build();
	    AnnotateImageRequest request =
	        AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
	    requests.add(request);
	
	    // Initialize client that will be used to send requests. This client only needs to be created
	    // once, and can be reused for multiple requests. After completing all of your requests, call
	    // the "close" method on the client to safely clean up any remaining background resources.
	    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
	    	BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
	    	List<AnnotateImageResponse> responses = response.getResponsesList();
	
	      	for (AnnotateImageResponse res : responses) {
		        if (res.hasError()) {
		          System.out.format("Error: %s%n", res.getError().getMessage());
		          return;
		        }
		
		        // For full list of available annotations, see http://g.co/cloud/vision/docs
		        for (EntityAnnotation annotation : res.getLogoAnnotationsList()) {
		          System.out.println(annotation.getDescription());
		        }
	      	}
	    }
	}
	
	
	/**
	 * Detects image properties such as color frequency from the specified local image.
	 * 
	 * @param image_url
	 * @throws IOException
	 */
	public static void searchWebForImageUsage(BufferedImage buffered_image) throws IOException {
	    List<AnnotateImageRequest> requests = new ArrayList<>();
	    //InputStream url_input_stream = new URL(image_url).openStream();
	    ByteArrayOutputStream os = new ByteArrayOutputStream();
	    ImageIO.write(buffered_image, "jpeg", os);                          // Passing: ​(RenderedImage im, String formatName, OutputStream output)
	    InputStream input_stream = new ByteArrayInputStream(os.toByteArray());
	    
	    ByteString imgBytes = ByteString.readFrom(input_stream);
	
	    Image img = Image.newBuilder().setContent(imgBytes).build();
	    Feature feat = Feature.newBuilder().setType(Feature.Type.WEB_DETECTION).build();
	    AnnotateImageRequest request =
	        AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
	    requests.add(request);
	
	    // Initialize client that will be used to send requests. This client only needs to be created
	    // once, and can be reused for multiple requests. After completing all of your requests, call
	    // the "close" method on the client to safely clean up any remaining background resources.
	    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
	    	BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
	    	List<AnnotateImageResponse> responses = response.getResponsesList();
	
	      	for (AnnotateImageResponse res : responses) {
		        if (res.hasError()) {
		          System.out.format("Error: %s%n", res.getError().getMessage());
		          return;
		        }
		
		        // Search the web for usages of the image. You could use these signals later
		        // for user input moderation or linking external references.
		        // For a full list of available annotations, see http://g.co/cloud/vision/docs
		        WebDetection annotation = res.getWebDetection();
		        System.out.println("Entity:Id:Score");
		        System.out.println("===============");
		        for (WebEntity entity : annotation.getWebEntitiesList()) {
		          System.out.println(
		              entity.getDescription() + " : " + entity.getEntityId() + " : " + entity.getScore());
		        }
		        for (WebLabel label : annotation.getBestGuessLabelsList()) {
		          System.out.format("%nBest guess label: %s", label.getLabel());
		        }
		        System.out.println("%nPages with matching images: Score%n==");
		        for (WebPage page : annotation.getPagesWithMatchingImagesList()) {
		          System.out.println(page.getUrl() + " : " + page.getScore());
		        }
		        System.out.println("%nPages with partially matching images: Score%n==");
		        for (WebImage image : annotation.getPartialMatchingImagesList()) {
		          System.out.println(image.getUrl() + " : " + image.getScore());
		        }
		        System.out.println("%nPages with fully matching images: Score%n==");
		        for (WebImage image : annotation.getFullMatchingImagesList()) {
		          System.out.println(image.getUrl() + " : " + image.getScore());
		        }
		        System.out.println("%nPages with visually similar images: Score%n==");
		        for (WebImage image : annotation.getVisuallySimilarImagesList()) {
		          System.out.println(image.getUrl() + " : " + image.getScore());
		        }
	      	}
	    }
	}
	
	/**
	 * Detects image properties such as color frequency from the specified local image.
	 * 
	 * @param image_url
	 * @throws IOException
	 */
	public static List<ColorUsageStat> extractImageProperties(BufferedImage buffered_image) throws IOException {
		List<ColorUsageStat> color_usage_stats = new ArrayList<>();

	    List<AnnotateImageRequest> requests = new ArrayList<>();
	    //InputStream url_input_stream = new URL(image_url).openStream();
	    ByteArrayOutputStream os = new ByteArrayOutputStream();
	    ImageIO.write(buffered_image, "jpeg", os);                          // Passing: ​(RenderedImage im, String formatName, OutputStream output)
	    InputStream input_stream = new ByteArrayInputStream(os.toByteArray());
	    
	    ByteString imgBytes = ByteString.readFrom(input_stream);
	
	    Image img = Image.newBuilder().setContent(imgBytes).build();
	    Feature feat = Feature.newBuilder().setType(Feature.Type.IMAGE_PROPERTIES).build();
	    AnnotateImageRequest request =
	        AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
	    requests.add(request);
	
	    // Initialize client that will be used to send requests. This client only needs to be created
	    // once, and can be reused for multiple requests. After completing all of your requests, call
	    // the "close" method on the client to safely clean up any remaining background resources.
	    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
	    	BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
	    	List<AnnotateImageResponse> responses = response.getResponsesList();
	
	      	for (AnnotateImageResponse res : responses) {
		        if (res.hasError()) {
		          System.out.format("Error: %s%n", res.getError().getMessage());
		          return color_usage_stats;
		        }
		
		        // For full list of available annotations, see http://g.co/cloud/vision/docs
		        DominantColorsAnnotation colors = res.getImagePropertiesAnnotation().getDominantColors();
		        for (ColorInfo color : colors.getColorsList()) {
		          System.out.format(
		              "fraction: %f%nr: %f, g: %f, b: %f, score: %f%n",
		              color.getPixelFraction(),
		              color.getColor().getRed(),
		              color.getColor().getGreen(),
		              color.getColor().getBlue(),
		          	  color.getScore());
		          ColorUsageStat color_stat = new ColorUsageStat(color.getColor().getRed(), color.getColor().getGreen(), color.getColor().getBlue(), color.getPixelFraction(), color.getScore());
		          color_usage_stats.add(color_stat);
		        }
	      	}
	    }
	    
	    return color_usage_stats;
	}
}

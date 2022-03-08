package com.looksee.actors;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.gcp.CloudVisionUtils;
import com.looksee.models.ElementState;
import com.looksee.models.ImageElementState;
import com.looksee.models.ImageFaceAnnotation;
import com.looksee.models.ImageLandmarkInfo;
import com.looksee.models.message.ElementExtractionError;
import com.looksee.models.message.ElementExtractionMessage;
import com.looksee.models.message.ElementProgressMessage;
import com.looksee.services.BrowserService;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.ImageUtils;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

import com.looksee.models.Label;
import com.looksee.models.Logo;
import com.looksee.models.audit.ColorData;
import com.looksee.models.ImageSearchAnnotation;


@Component
@Scope("prototype")
public class ElementStateExtractor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(ElementStateExtractor.class);
	private Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	private BrowserService browser_service;
	
	//subscribe to cluster changes
	@Override
	public void preStart() {
		cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
				MemberEvent.class, UnreachableMember.class);
	}

	//re-subscribe when restart
	@Override
    public void postStop() {
	  cluster.unsubscribe(getSelf());
    }

	/**
	 * {@inheritDoc}
	 *
	 * NOTE: Do not change the order of the checks for instance of below. These are in this order because ExploratoryPath
	 * 		 is also a Test and thus if the order is reversed, then the ExploratoryPath code never runs when it should
	 * @throws NullPointerException
	 * @throws IOException
	 * @throws NoSuchElementException
	 */
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(ElementExtractionMessage.class, message-> {
					try {
						URL full_page_screenshot_url = new URL(message.getPageState().getFullPageScreenshotUrlOnload());
						BufferedImage page_screenshot = ImageUtils.readImageFromURL(full_page_screenshot_url);
						URL page_url = new URL(BrowserUtils.sanitizeUrl(message.getPageState().getUrl(),
								 						 message.getPageState().isSecure()));
						List<ElementState> element_states = browser_service.buildPageElements(	message.getPageState(), 
																								message.getXpaths(),
																								message.getAuditRecordId(),
																								page_url,
																								page_screenshot.getHeight());
						
					
						//ENRICHMENT : BACKGROUND COLORS
						element_states = element_states.parallelStream()
								   .filter(element -> element != null)
								   .filter(element -> !element.getOwnedText().isEmpty())
								   .map(element -> {
										try {
											ColorData font_color = new ColorData(element.getRenderedCssValues().get("color"));				
											//extract opacity color
											ColorData bkg_color = null;
											if(element.getScreenshotUrl().trim().isEmpty()) {
												bkg_color = new ColorData(element.getRenderedCssValues().get("background-color"));
											}
											else {
												//log.warn("extracting background color");
												bkg_color = ImageUtils.extractBackgroundColor( new URL(element.getScreenshotUrl()),
																							   font_color);
												
												//log.warn("done extracting background color");
											}
											String bg_color = bkg_color.rgb();	
											
											//Identify background color by getting largest color used in picture
											//ColorData background_color_data = ImageUtils.extractBackgroundColor(new URL(element.getScreenshotUrl()));
											ColorData background_color = new ColorData(bg_color);
											element.setBackgroundColor(background_color.rgb());
											element.setForegroundColor(font_color.rgb());
											
											double contrast = ColorData.computeContrast(background_color, font_color);
											log.warn("contrast :: "+contrast);
											element.setTextContrast(contrast);
											return element;
										}
										catch (Exception e) {
											log.warn("element screenshot url  :: "+element.getScreenshotUrl());
											e.printStackTrace();
										}
										return element;
								})
								.collect(Collectors.toList());
						
						//Enrich data using NLP and computer vision to add labels to elements
						/*
						Set<Label> image_labels = new HashSet<>();
						Set<ImageLandmarkInfo> image_landmarks = new HashSet<>();
						Set<Logo> image_logos = new HashSet<>();
						Set<ImageFaceAnnotation> image_face_annotation = new HashSet<>();
						Set<ImageSearchAnnotation> reverse_image_search = new HashSet<>();
						
						List<ImageElementState> image_elements = BrowserUtils.getImageElements(element_states);
						for(ImageElementState image : image_elements) {
							BufferedImage buffered_img = ImageUtils.readImageFromURL(new URL(image.getScreenshotUrl()));
							image_labels.addAll( CloudVisionUtils.extractImageLabels( buffered_img ));
							image_landmarks.addAll( CloudVisionUtils.extractImageLandmarks( buffered_img ));
							image_logos.addAll( CloudVisionUtils.extractImageLogos( buffered_img ));
							image_face_annotation.addAll( CloudVisionUtils.extractImageFaces( buffered_img ));
							reverse_image_search.addAll( CloudVisionUtils.searchWebForImageUsage( buffered_img ));
						}
						
						for(Label label: image_labels) {
							log.warn("labels :: " + label.getDescription());
						}
						
						for(ImageLandmarkInfo landmark : image_landmarks) {
							log.warn("landmarks :: "+landmark.getDescription());
							log.warn("lat/lng   :: "+landmark.getLatLngSet());
						}
						
						for(Logo logo: image_logos) {
							log.warn("Logo  :  "+logo.getDescription());
						}
						
						for(ImageFaceAnnotation face : image_face_annotation) {
							log.warn("ANGER      	  :  "+face.getAngerLikelihood());
							log.warn("BLURRED    	  :  "+face.getBlurredLikelihood());
							log.warn("JOY 	     	  :  "+face.getJoyLikelihood());
							log.warn("SORROW    	  :  "+face.getSorrowLikelihood());
							log.warn("SURPRISE        :  "+face.getSurpriseLikelihood());
							log.warn("UNDER EXPOSED   :  "+face.getUnderExposedLikelihood());
							log.warn("HEADWEAR        :  "+face.getHeadwearLikelihood());

						}
						
						for(ImageSearchAnnotation img_search : reverse_image_search) {
							log.warn("FULL MATCHING IMAGES      	  :  "+img_search.getFullMatchingImages());
							log.warn("SIMILAR IMAGES    		  	  :  "+img_search.getSimilarImages());
							log.warn("BEST GUESS LABEL 	     		  :  "+img_search.getBestGuessLabel());
							log.warn("LABELS    	  			  	  :  "+img_search.getLabels());
							log.warn("SCORE           			  	  :  "+img_search.getScore());

						}
						*/
						
						//tell page state builder of element states
						ElementProgressMessage element_message = new ElementProgressMessage(message.getAccountId(), 
																							message.getAuditRecordId(),
																							message.getPageState().getId(), 
																							message.getXpaths(),
																							element_states,
																							0L,
																							0L, 
																							message.getPageState().getUrl(),
																							message.getDomainId());
						
						getContext().parent().tell(element_message, getSelf());
					} catch(Exception e) {
						//tell page state builder of element states
						ElementExtractionError element_message = new ElementExtractionError(message.getAccountId(), 
																							message.getAuditRecordId(), 
																							message.getDomainId(),
																							message.getPageState().getId(), 
																							message.getPageState().getUrl(),
																							e.getMessage());
						
						getContext().parent().tell(element_message, getSelf());
						
						e.printStackTrace();
					}
				})
				.match(MemberUp.class, mUp -> {
					log.info("Member is Up: {}", mUp.member());
				})
				.match(UnreachableMember.class, mUnreachable -> {
					log.info("Member detected as unreachable: {}", mUnreachable.member());
				})
				.match(MemberRemoved.class, mRemoved -> {
					log.info("Member is Removed: {}", mRemoved.member());
				})
				.matchAny(o -> {
					log.info("received unknown message of type :: "+o.getClass().getName());
				})
				.build();
	}
}

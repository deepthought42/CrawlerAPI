package com.minion.actors;

import java.awt.image.BufferedImage;
import java.net.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.aws.UploadObjectSingleOperation;
import com.qanairy.models.PageElement;
import com.qanairy.models.repository.PageElementRepository;

import akka.actor.Props;
import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;


@Component
@Scope("prototype")
public class AwsS3ScreenshotUploader extends AbstractActor{
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	Cluster cluster = Cluster.get(getContext().getSystem());
	
	@Autowired
	private PageElementRepository page_element_repo;
	
	public static Props props() {
	  return Props.create(AwsS3ScreenshotUploader.class);
	}
	
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

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(ElementScreenshotUpload.class, screenshot_upload -> {
					String viewport_screenshot_url = UploadObjectSingleOperation.saveImageToS3(screenshot_upload.screenshot, screenshot_upload.page_url.getHost(), screenshot_upload.page_elem_key, "element_screenshot");

					System.err.println("Screenshot uploaded to :: "+viewport_screenshot_url);
					/*ScreenshotSet screenshot_set = new ScreenshotSet(viewport_screenshot_url, screenshot_upload.browser_name);
					
					ScreenshotSet screenshot_record = screenshot_set_repo.findByKey(screenshot_set.getKey());
					if(screenshot_record != null){
						screenshot_set = screenshot_record;
					}
					else{
						screenshot_set = screenshot_set_repo.save(screenshot_set);
					}
					
					HashSet<ScreenshotSet> screenshots = new HashSet<ScreenshotSet>();
					screenshots.add(screenshot_set);
					*/
					PageElement page_elem_record = page_element_repo.findByKey(screenshot_upload.page_elem_key);
					page_elem_record.setScreenshot(viewport_screenshot_url);
					page_elem_record = page_element_repo.save(page_elem_record);
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
					System.err.println("o class :: "+o.getClass().getName());
					log.info("received unknown message");
				})
				.build();
	}
	
	public static final class ElementScreenshotUpload{
		
		private BufferedImage screenshot;
		private URL page_url;
		private String page_elem_key;
		
		public ElementScreenshotUpload(BufferedImage screenshot, URL url, String page_elem_key, String browser_name){
			this.screenshot = screenshot;
			this.page_elem_key = page_elem_key;
			this.page_url = url;
		}
	}
}

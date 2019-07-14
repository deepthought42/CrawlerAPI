package com.minion.actors;

import java.net.URL;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.minion.aws.UploadObjectSingleOperation;
import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.message.ElementScreenshotUpload;
import com.qanairy.services.ElementStateService;

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
	private ElementStateService page_element_service;
	
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
					String viewport_screenshot_url = UploadObjectSingleOperation.saveImageToS3(screenshot_upload.getScreenshot(), screenshot_upload.getPageUrl().getHost(), screenshot_upload.getPageElemKey(), screenshot_upload.getBrowserName());					
					ElementState page_elem_record = page_element_service.findByKey(screenshot_upload.getPageElemKey());
					page_elem_record.setScreenshot(viewport_screenshot_url);
					page_elem_record.setScreenshotChecksum(PageState.getFileChecksum(ImageIO.read(new URL(viewport_screenshot_url))));
					page_element_service.save(page_elem_record);
					
					//tell requester what response is
					postStop();
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
					log.info("o class :: "+o.getClass().getName());
					log.info("received unknown message");
				})
				.build();
	}
}

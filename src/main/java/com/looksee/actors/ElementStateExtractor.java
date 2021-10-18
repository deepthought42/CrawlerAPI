package com.looksee.actors;


import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.NoSuchElementException;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.models.ElementState;
import com.looksee.models.enums.BrowserType;
import com.looksee.models.message.ElementExtractionMessage;
import com.looksee.models.message.ElementProgressMessage;
import com.looksee.services.BrowserService;
import com.looksee.services.PageStateService;
import com.looksee.utils.BrowserUtils;
import com.looksee.utils.ImageUtils;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

@Component
@Scope("prototype")
public class ElementStateExtractor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(ElementStateExtractor.class);
	private Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private PageStateService page_state_service;
	
	@Autowired
	private ActorSystem actor_system;
	
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
					log.warn("Extracting element states from page");
					try {
						URL sanitized_url = new URL(BrowserUtils.sanitizeUserUrl(message.getPageState().getUrl() ));
						BufferedImage page_screenshot = ImageIO.read(new URL(message.getPageState().getFullPageScreenshotUrlOnload()));
						List<ElementState> element_states = browser_service.buildPageElements(message.getPageState(), 
								message.getXpaths(),
								message.getAuditRecordId(), 
								sanitized_url,
								page_screenshot.getHeight());
						
						//BufferedImage onload_screenshot = ImageIO.read(new URL(message.getPageState().getFullPageScreenshotUrlOnload()));
						//String composite_img_url = ImageUtils.createComposite(onload_screenshot, element_states, message.getPageState(), BrowserType.CHROME);
						
						log.warn("completed element state extraction for "+message.getXpaths().size() + "  xpaths");
						ElementProgressMessage element_message = new ElementProgressMessage(message.getAuditRecordId(), 
								message.getPageState().getId(), 
								message.getXpaths());
						
						//page_state_service.updateCompositeImageUrl(message.getPageState().getId(), message.getPageState().getFullPageScreenshotUrlOnload());
//						message.getPageState().setFullPageScreenshotUrlComposite(message.getPageState().getFullPageScreenshotUrlOnload());
//						page_state_service.save(message.getPageState());
						getSender().tell(element_message, getSelf());
					} catch(Exception e) {
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

package com.looksee.actors;

import static com.looksee.config.SpringExtension.SpringExtProvider;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.gcp.CloudVisionUtils;
import com.looksee.models.PageState;
import com.looksee.models.audit.ColorData;
import com.looksee.models.audit.ColorPaletteUtils;
import com.looksee.models.audit.ColorUsageStat;
import com.looksee.models.competitiveanalysis.brand.Brand;
import com.looksee.models.competitiveanalysis.brand.BrandService;
import com.looksee.models.designsystem.PaletteColor;
import com.looksee.models.enums.CrawlAction;
import com.looksee.models.message.CompetitorMessage;
import com.looksee.models.message.CrawlActionMessage;
import com.looksee.models.message.PageCandidateFound;
import com.looksee.services.BrowserService;
import com.looksee.services.CompetitorService;
import com.looksee.utils.BrowserUtils;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

@Component
@Scope("prototype")
public class CompetitiveAnalysisActor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(CompetitiveAnalysisActor.class);
	private Cluster cluster = Cluster.get(getContext().getSystem());
	
	@Autowired
	private ActorSystem actor_system;
	
	@Autowired
	private CompetitorService competitor_service;
	
	@Autowired
	private BrowserService browser_service;
	
	@Autowired
	private BrandService brand_service;
	
	//subscribe to cluster changes
	@Override
	public void preStart() {
		cluster.subscribe(getSelf(), ClusterEvent.initialStateAsEvents(),
				MemberEvent.class, UnreachableMember.class);
	}

	private int page_count = 0;
	private long competitor_id;
	
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
				.match(CompetitorMessage.class, message-> {
					this.competitor_id = message.getCompetitorId();
					URL sanitized_url = new URL(BrowserUtils.sanitizeUserUrl(message.getCompetitor().getUrl()));
					CrawlActionMessage crawl_action = new CrawlActionMessage(CrawlAction.START, 
							 -1L,
							 message.getAccountId(),
							 -1, 
							 false, 
							 sanitized_url,
							 message.getCompetitor().getUrl());
					//Save competitor to local variable
					ActorRef web_crawl_actor = getContext().actorOf(SpringExtProvider.get(actor_system)
							.props("webCrawlerActor"), "webCrawlerActor"+UUID.randomUUID());
					
					web_crawl_actor.tell(crawl_action, getSelf());
				})
				.match(PageCandidateFound.class, message -> {
					//review 20 PAGES OF THE COMPETITOR WEBSITE TO EXTRACT BRAND INFO
					if(this.page_count < 20) {				
						this.page_count++;

						PageState page = browser_service.buildPageState(message.getUrl());
						BufferedImage full_page_screenshot = ImageIO.read( new URL( page.getFullPageScreenshotUrlOnload()));
								
						List<ColorUsageStat> color_stats = CloudVisionUtils.extractImageProperties(full_page_screenshot);
						
						log.warn("Colors found on page ... "+color_stats.size());	
						List<ColorData> color_data_list = color_stats.parallelStream()
																.distinct()
																.map(x -> new ColorData(x))
																.collect(Collectors.toList());
						
						List<PaletteColor> palette_colors = ColorPaletteUtils.extractPalette(color_data_list);
						log.warn("palette colors extracted :: "+palette_colors.size());
						List<String> colors = palette_colors.parallelStream()
															.map(x -> x.getPrimaryColor())
															.collect(Collectors.toList());
						
						//review all 
						//Create a Brand object
						Brand brand = brand_service.save(new Brand(colors));
						//save brand as part of a {@link CompetitiveAnalysis} object and associate the competitive analysis with the appropriate domain {@link Competitor}
						competitor_service.addBrand(competitor_id, brand.getId());
						//tell user about competitor update using pusher
					}
					
				})
				.match(MemberUp.class, mUp -> {
					log.info("Member is Up: {}", mUp.member());
				})
				.match(UnreachableMember.class, mUnreachable -> {
					log.warn("Member detected as unreachable: {}", mUnreachable.member());
				})
				.match(MemberRemoved.class, mRemoved -> {
					log.warn("Member is Removed: {}", mRemoved.member());
				})
				.matchAny(o -> {
					log.warn("received unknown message of type :: "+o.getClass().getName());
				})
				.build();
	}

}

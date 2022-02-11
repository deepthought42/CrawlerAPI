package com.looksee.actors;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.gcp.CloudVisionUtils;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.ColorData;
import com.looksee.models.audit.ColorPaletteUtils;
import com.looksee.models.audit.ColorUsageStat;
import com.looksee.models.audit.aesthetics.ColorPaletteAudit;
import com.looksee.models.designsystem.DesignSystem;
import com.looksee.models.designsystem.PaletteColor;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.message.AuditError;
import com.looksee.models.message.AuditProgressUpdate;
import com.looksee.models.message.DomainAuditRecordMessage;
import com.looksee.models.message.PageAuditRecordMessage;
import com.looksee.services.AuditRecordService;
import com.looksee.services.DesignSystemService;
import com.looksee.services.DomainService;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

/**
 * Extracts {@link DesignSystem} from either a {@link PageStateAuditRecord} or {@link DomainAuditRecord}
 */
@Component
@Scope("prototype")
public class DesignSystemExtractor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(DesignSystemExtractor.class);
	private Cluster cluster = Cluster.get(getContext().getSystem());
	
	@Autowired
	private DomainService domain_service;
	
	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private DesignSystemService design_system_service;
	
	@Autowired
	private ColorPaletteAudit color_palette_auditor;
	
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
				.match(PageAuditRecordMessage.class, message-> {
					log.warn("++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
					log.warn("Page Audit record received by design system extraction");
					try {
						//extract design system from page
						PageState page_state = audit_record_service.getPageStateForAuditRecord( message.getPageAuditId());
						//List<ElementState> elements = page_state_service.getElementStates(page_state.getId());
						List<ColorUsageStat> color_usage_list = new ArrayList<>();
						
						URL url = new URL(page_state.getFullPageScreenshotUrlOnload());
						//color_usage_list.addAll(ColorUtils.extractColorsFromScreenshot(url, elements));
						color_usage_list.addAll(CloudVisionUtils.extractImageProperties(ImageIO.read(url)));

						//extract declared css color properties
						/*
						List<ColorData> colors_declared = new ArrayList<>();
						List<String> raw_stylesheets = Browser.extractStylesheets(page_state.getSrc()); 
						
						//open stylesheet
						for(String stylesheet : raw_stylesheets) {
							colors_declared.addAll(BrowserUtils.extractColorsFromStylesheet(stylesheet));
						}
						*/
						Map<String, ColorData> color_map = new HashMap<>();
						for(ColorUsageStat stat : color_usage_list) {
							ColorData color = new ColorData(stat);
							if(color.getUsagePercent() < 0.0001) {
								continue;
							}
							color.setUsagePercent(stat.getPixelPercent());
							log.warn("Color :: " + color.rgb() + "  :  " + color.getUsagePercent());
							
							color_map.put(color.rgb().trim(), color);
						}
						
						//generate palette, identify color scheme and score how well palette conforms to color scheme
						List<ColorData> colors = new ArrayList<ColorData>(color_map.values());
						Set<PaletteColor> palette_colors = new HashSet<>();
						palette_colors.addAll( ColorPaletteUtils.extractColors(colors) );
						//ColorScheme color_scheme = ColorPaletteUtils.getColorScheme(palette_colors);
						log.warn("Palette colors found :: "+palette_colors );
						List<String> color_palette = new ArrayList<>();
						for(PaletteColor color : palette_colors) {
							color_palette.add(color.getPrimaryColor());
						}
						log.warn("color palette :: "+color_palette);
						AuditRecord audit_record = audit_record_service.findById( message.getAuditRecordId() ).get(); 
						audit_record.setColors(color_palette);
						audit_record_service.save(audit_record, message.getAccountId(), message.getDomainId());
						
						
						Optional<DesignSystem> design_system_opt = domain_service.getDesignSystem(message.getDomainId());
						DesignSystem design_system = null;
						
						if(!design_system_opt.isPresent()) {
							log.warn("design system couldn't be found for domain :: "+message.getDomainId());
							design_system = design_system_service.save( new DesignSystem() );
							domain_service.addDesignSystem(message.getDomainId(), design_system.getId());
						}
						else {
							design_system = design_system_opt.get();
						}
						
						
						try {
							Audit color_paelette_audit = color_palette_auditor.execute(page_state, audit_record, design_system);
							if( color_paelette_audit != null ) {
							
								AuditProgressUpdate audit_update3 = new AuditProgressUpdate(
																			message.getAccountId(),
																			audit_record.getId(),
																			1.0,
																			"Completed review of color palette",
																			AuditCategory.AESTHETICS,
																			AuditLevel.PAGE, 
																			color_paelette_audit, 
																			message.getDomainId());
	
								getContext().getParent().tell(audit_update3, getSelf());
							}
						}
						catch(Exception e) {
							AuditError audit_err = new AuditError(message.getDomainId(), 
																  message.getAccountId(), 
																  message.getAuditRecordId(), 
																  "An error occurred while performing non-text audit", 
																  AuditCategory.AESTHETICS, 
																  1.0);
							getContext().getParent().tell(audit_err, getSelf());
							e.printStackTrace();
						}
					} catch(Exception e) {
						
						e.printStackTrace();
					}
				})
				.match(DomainAuditRecordMessage.class, message-> {
					try {
						//extract design system from all {@link PageAuditRecord}s for this domain audit record
						
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

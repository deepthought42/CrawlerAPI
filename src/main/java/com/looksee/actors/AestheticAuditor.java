package com.looksee.actors;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.models.Account;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.aesthetics.NonTextColorContrastAudit;
import com.looksee.models.audit.aesthetics.TextColorContrastAudit;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.message.AuditProgressUpdate;
import com.looksee.services.AuditRecordService;
import com.looksee.services.AuditService;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

/**
 * Responsible for performing audits for {@link PageVersion}s and {@link Domain}s
 * 
 */
@Component
@Scope("prototype")
public class AestheticAuditor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(AestheticAuditor.class.getName());

	private Cluster cluster = Cluster.get(getContext().getSystem());
	
	@Autowired
	private TextColorContrastAudit text_contrast_auditor;

	@Autowired
	private NonTextColorContrastAudit non_text_contrast_auditor;
	
	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private AuditRecordService audit_record_service;
	
	private Account account;

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
				.match(PageAuditRecord.class, page_audit_record_msg -> {
					try {
					   	//generate audit report
					   	//Set<Audit> audits = new HashSet<>();
						AuditRecord audit_record = audit_record_service.findById(page_audit_record_msg.getId()).get();
					   	//PageState page = audit_record_service.getPageStateForAuditRecord(page_audit_record_msg.getId());
					   	PageState page = page_audit_record_msg.getPageState();
					   	//check if page state already
			   			//perform audit and return audit result
					   
					   	//Audit color_palette_audit = color_palette_auditor.execute(page);
						//audits.add(color_palette_audit);

					   	AuditProgressUpdate audit_update = new AuditProgressUpdate(
																	page_audit_record_msg.getId(),
																	(1.0/3.0),
																	"Reviewing text contrast",
																	AuditCategory.AESTHETICS,
																	AuditLevel.PAGE,
																	null);

					   	getSender().tell(audit_update, getSelf());
					   	
						Audit text_contrast_audit = text_contrast_auditor.execute(page, audit_record);
						
						/*
						Audit padding_audits = padding_auditor.execute(page);
						audits.add(padding_audits);
	
						Audit margin_audits = margin_auditor.execute(page);
						audits.add(margin_audits);
						 */

						AuditProgressUpdate audit_update2 = new AuditProgressUpdate(
																	page_audit_record_msg.getId(),
																	(2.0/3.0),
																	"Reviewing non-text contrast for WCAG compliance",
																	AuditCategory.AESTHETICS,
																	AuditLevel.PAGE,
																	text_contrast_audit);
						
						getSender().tell(audit_update2, getSelf());
						
						Audit non_text_contrast_audit = non_text_contrast_auditor.execute(page, audit_record);
						
						AuditProgressUpdate audit_update3 = new AuditProgressUpdate(
																	page_audit_record_msg.getId(),
																	1.0,
																	"Completed review of non-text contrast",
																	AuditCategory.AESTHETICS,
																	AuditLevel.PAGE,
																	non_text_contrast_audit);

						getSender().tell(audit_update3, getSelf());
						
			   			//send message to either user or page channel containing reference to audits
	
						//NOTE: SEND DATA TO AUDIT MANAGER
					}catch(Exception e) {
						log.warn("exception caught during aesthetic audit");
						e.printStackTrace();
						log.warn("-------------------------------------------------------------");
						log.warn("-------------------------------------------------------------");
						log.warn("THERE WAS AN ISSUE DURING AESTHETICS AUDIT");
						log.warn("-------------------------------------------------------------");
						log.warn("-------------------------------------------------------------");

						
						
						AuditProgressUpdate audit_update3 = new AuditProgressUpdate(
								page_audit_record_msg.getId(),
								1.0,
								"Completed review of non-text contrast",
								AuditCategory.AESTHETICS,
								AuditLevel.PAGE,
								null);

						getSender().tell(audit_update3, getSelf());
						
					}
				})
				.match(MemberUp.class, mUp -> {
					log.debug("Member is Up: {}", mUp.member());
				})
				.match(UnreachableMember.class, mUnreachable -> {
					log.debug("Member detected as unreachable: {}", mUnreachable.member());
				})
				.match(MemberRemoved.class, mRemoved -> {
					log.debug("Member is Removed: {}", mRemoved.member());
				})
				.matchAny(o -> {
					log.debug("received unknown message of type :: "+o.getClass().getName());
				})
				.build();
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}
	
	
}

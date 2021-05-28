package com.looksee.actors;

import java.io.IOException;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.models.Account;
import com.looksee.models.PageState;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.ImageAltTextAudit;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.ParagraphingAudit;
import com.looksee.models.audit.ReadabilityAudit;
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
public class ContentAuditor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(ContentAuditor.class.getName());

	private Cluster cluster = Cluster.get(getContext().getSystem());
	
	@Autowired
	private ImageAltTextAudit image_alt_text_auditor;
	
	@Autowired
	private ParagraphingAudit paragraph_auditor;
	
	@Autowired
	private ReadabilityAudit readability_auditor;
	
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
				   	//generate audit report
				   	Set<Audit> audits = new HashSet<>();
				   	PageState page = page_audit_record_msg.getPageState();
				   	//check if page state already
		   			//perform audit and return audit result
				   	/*
				   	log.warn("?????????????????????????????????????????????????????????????????????");
				   	log.warn("?????????????????????????????????????????????????????????????????????");
				   	log.warn("?????????????????????????????????????????????????????????????????????");

			   		log.warn("requesting performance audit from performance auditor....");
			   		ActorRef performance_insight_actor = actor_system.actorOf(SpringExtProvider.get(actor_system)
							.props("performanceAuditor"), "performanceAuditor"+UUID.randomUUID());
					performance_insight_actor.tell(page_state, getSelf());
					*/
				   	/* NOTE typeface audit is incomplete and currently commented out
					 
					Audit typeface_audit = typeface_auditor.execute(page);
					audits.add(typeface_audit);
					 */
				   	log.warn("page audit record recieved :: "+page_audit_record_msg.getId());
					Audit alt_text_audit = image_alt_text_auditor.execute(page);
					audits.add(alt_text_audit);
					
					page_audit_record_msg = (PageAuditRecord)audit_record_service.findById(page_audit_record_msg.getId()).get();
					page_audit_record_msg.setContentAuditProgress( (1.0/3.0) );
					page_audit_record_msg.setContentAuditMsg("Reviewing content for readability");
					page_audit_record_msg = (PageAuditRecord)audit_record_service.save(page_audit_record_msg);		
					
					Audit readability_audit = readability_auditor.execute(page);
					audits.add(readability_audit);
					
					
					page_audit_record_msg = (PageAuditRecord)audit_record_service.findById(page_audit_record_msg.getId()).get();
					page_audit_record_msg.setContentAuditProgress( (2.0/3.0) );
					page_audit_record_msg.setContentAuditMsg("Reviewing paragraph structure");
					page_audit_record_msg = (PageAuditRecord)audit_record_service.save(page_audit_record_msg);

					//Audit font_audit = font_auditor.execute(page);
					//audits.add(font_audit);
					
					Audit paragraph_audit = paragraph_auditor.execute(page);
					audits.add(paragraph_audit);	
					
					page_audit_record_msg = (PageAuditRecord)audit_record_service.findById(page_audit_record_msg.getId()).get();
					page_audit_record_msg.setContentAuditMsg("Finished content audit");
					page_audit_record_msg.setContentAuditProgress( (3.0/3.0) ); 
					page_audit_record_msg = (PageAuditRecord)audit_record_service.save(page_audit_record_msg);		

					
					log.warn("content audits complete :: "+audits.size());
					for(Audit audit : audits) {						
						audit = audit_service.save(audit);
						audit_record_service.addAudit( page_audit_record_msg.getId(), audit.getId() );
						((PageAuditRecord)page_audit_record_msg).addAudit(audit);
					}
				  
		   			//send message to either user or page channel containing reference to audits
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

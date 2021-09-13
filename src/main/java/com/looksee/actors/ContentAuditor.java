package com.looksee.actors;

import java.util.HashSet;
import java.util.Set;

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
import com.looksee.models.audit.ParagraphingAudit;
import com.looksee.models.audit.content.ImageAltTextAudit;
import com.looksee.models.audit.content.ReadabilityAudit;
import com.looksee.services.AccountService;
import com.looksee.services.AuditRecordService;
import com.looksee.services.AuditService;
import com.looksee.services.SendGridMailService;
import com.looksee.utils.AuditUtils;

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
	
	@Autowired
	private AccountService account_service;
	
	@Autowired
	private SendGridMailService email_service;
	
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
	 */
	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(PageAuditRecord.class, page_audit_record_msg -> {
				   	//generate audit report
				   	Set<Audit> audits = new HashSet<>();
				   	PageState page = audit_record_service.getPageStateForAuditRecord(page_audit_record_msg.getId());
				  
				   	AuditRecord page_audit_record = audit_record_service.findById(page_audit_record_msg.getId()).get();
					page_audit_record.setContentAuditProgress( (1.0/4.0) );
					page_audit_record.setContentAuditMsg("checking image alt text...");
					audit_record_service.save(page_audit_record);	
					
				   	log.warn("page audit record recieved :: "+page_audit_record_msg.getId());
					Audit alt_text_audit = image_alt_text_auditor.execute(page, page_audit_record);
					audits.add(alt_text_audit);
					
					page_audit_record = audit_record_service.findById(page_audit_record_msg.getId()).get();
					page_audit_record.setContentAuditProgress( (2.0/4.0) );
					page_audit_record.setContentAuditMsg("Reviewing content for readability...");
					audit_record_service.save(page_audit_record);		
					
					Audit readability_audit = readability_auditor.execute(page, page_audit_record);
					audits.add(readability_audit);
					
					
					page_audit_record = audit_record_service.findById(page_audit_record_msg.getId()).get();
					page_audit_record.setContentAuditProgress( (3.0/4.0) );
					page_audit_record.setContentAuditMsg("Reviewing paragraph structure...");
					audit_record_service.save(page_audit_record);

					//Audit font_audit = font_auditor.execute(page);
					//audits.add(font_audit);
					
					Audit paragraph_audit = paragraph_auditor.execute(page, page_audit_record);
					audits.add(paragraph_audit);	
					
					page_audit_record = audit_record_service.findById(page_audit_record_msg.getId()).get();
					page_audit_record.setContentAuditMsg("Done!");
					page_audit_record.setContentAuditProgress( (4.0/4.0) ); 
					page_audit_record = audit_record_service.save(page_audit_record);		

					
					log.warn("content audits complete :: "+audits.size());
					for(Audit audit : audits) {						
						audit = audit_service.save(audit);
						audit_record_service.addAudit( page_audit_record_msg.getId(), audit.getId() );
						((PageAuditRecord)page_audit_record_msg).addAudit(audit);
					}
				  
					boolean is_audit_complete = AuditUtils.isPageAuditComplete(page_audit_record);
					if(is_audit_complete) {
						
						Set<Account> accounts = account_service.findForAuditRecord(page_audit_record.getId());
						for(Account account: accounts) {
							email_service.sendPageAuditCompleteEmail(account.getEmail(), page.getUrl(), page_audit_record.getId());
						}
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

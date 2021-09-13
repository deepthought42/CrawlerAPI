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
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.audit.informationarchitecture.LinksAudit;
import com.looksee.models.audit.informationarchitecture.MetadataAudit;
import com.looksee.models.audit.informationarchitecture.SecurityAudit;
import com.looksee.models.audit.informationarchitecture.TitleAndHeaderAudit;
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
public class InformationArchitectureAuditor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(InformationArchitectureAuditor.class.getName());

	private Cluster cluster = Cluster.get(getContext().getSystem());
	
	@Autowired
	private LinksAudit links_auditor;
	
	@Autowired
	private MetadataAudit metadata_auditor;
	
	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private TitleAndHeaderAudit title_and_header_auditor;

	@Autowired
	private SecurityAudit security_audit;
	
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
					PageState page = audit_record_service.getPageStateForAuditRecord(page_audit_record_msg.getId());
				   	Set<Audit> audits = new HashSet<>();
				   	
				   	AuditRecord page_audit_record = audit_record_service.findById(page_audit_record_msg.getId()).get();
					page_audit_record.setInfoArchAuditProgress( (1.0/5.0) ); 
					page_audit_record.setInfoArchMsg("Reviewing links...");
					audit_record_service.save(page_audit_record);
					
				   	Audit link_audit = links_auditor.execute(page, null);
					audits.add(link_audit);
					
					page_audit_record = audit_record_service.findById(page_audit_record_msg.getId()).get();
					page_audit_record.setInfoArchAuditProgress( (2.0/5.0) ); 
					page_audit_record.setInfoArchMsg("Reviewing title and header...");
					audit_record_service.save(page_audit_record);
					
					Audit title_and_headers = title_and_header_auditor.execute(page, null);
					audits.add(title_and_headers);
					
					page_audit_record = audit_record_service.findById(page_audit_record_msg.getId()).get();
					page_audit_record.setInfoArchAuditProgress( (3.0/5.0) ); 
					page_audit_record.setInfoArchMsg("Checking security...");
					audit_record_service.save(page_audit_record);
					
					Audit security = security_audit.execute(page, null);
					audits.add(security);
					
					page_audit_record = audit_record_service.findById(page_audit_record_msg.getId()).get();
					page_audit_record.setInfoArchAuditProgress( (4.0/5.0) ); 
					page_audit_record.setInfoArchMsg("Reviewing SEO");
					page_audit_record = audit_record_service.save(page_audit_record);
					
					Audit metadata = metadata_auditor.execute(page, null);
					audits.add(metadata);
					
					page_audit_record = audit_record_service.findById(page_audit_record_msg.getId()).get();
					page_audit_record.setInfoArchAuditProgress( (5.0/5.0) ); 
					page_audit_record.setInfoArchMsg("Done!");
					page_audit_record = audit_record_service.save(page_audit_record);
					
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

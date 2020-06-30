package com.minion.actors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.qanairy.models.Account;
import com.qanairy.models.PageState;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.AuditFactory;
import com.qanairy.models.audit.AuditRecord;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.message.AccountRequest;
import com.qanairy.models.message.AuditRecordSet;
import com.qanairy.services.AuditRecordService;
import com.qanairy.services.AuditService;
import com.qanairy.services.PageStateService;

import akka.actor.AbstractActor;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.ClusterEvent.MemberEvent;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.MemberUp;
import akka.cluster.ClusterEvent.UnreachableMember;

/**
 * 
 * 
 */
@Component
@Scope("prototype")
public class Auditor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(Auditor.class.getName());

	private Cluster cluster = Cluster.get(getContext().getSystem());

	@Autowired
	private PageStateService page_state_service;

	
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
				.match(PageState.class, page_state-> {
					log.warn("performing audits on page state...");
					//retrieve all audits that the customer requested
					Map<PageState, List<Audit>> page_audit_map = new HashMap<PageState, List<Audit>>();
					List<AuditRecord> audit_records = new ArrayList<AuditRecord>();
					
				   	//generate audit report
				   	List<Audit> audits = new ArrayList<>();
				   	
				   	for(AuditCategory audit_category : AuditCategory.values()) {
			   			//perform audit and return audit result
			   			List<Audit> audits_executed = AuditFactory.executePageAudit(audit_category, page_state, "Look-See-admin");
			   			
			   			audits_executed = audit_service.saveAll(audits_executed);
			   			audits.addAll(audits_executed);
						page_audit_map.put(page_state, audits);
			   		}
		   			
					AuditRecord audit_record = new AuditRecord(audits);
		   			audit_record = audit_record_service.save(audit_record);
		   			audit_records.add(audit_record);
		   			
		   			page_state.addAuditRecord(audit_record);
		   			page_state_service.save(page_state);
		   			getSender().tell(audit_record, getSelf());
		   			//send message to either user or page channel containing reference to audits
		   			log.warn("Completed audits for page state ... "+page_state.getUrl());
		   			postStop();
				})
				.match(AuditRecordSet.class, msg -> {
					log.warn("audit record set message received...");
					//TODO Audit record analysis for domain
				   	for(AuditCategory audit_category : AuditCategory.values()) {
				   	//perform audit and return audit result
				   		log.warn("Executing domain audit for "+audit_category);
			   			List<Audit> audits_executed = AuditFactory.executeDomainAudit(audit_category, msg.getAuditRecords(), "Look-See-admin");
			   			
			   			audits_executed = audit_service.saveAll(audits_executed);
			   			//TODO send message to sender with domain audits result
				   	}
					postStop();
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

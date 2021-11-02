package com.looksee.actors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.looksee.models.Account;
import com.looksee.models.audit.Audit;
import com.looksee.models.audit.AuditFactory;
import com.looksee.models.audit.AuditRecord;
import com.looksee.models.audit.PageAuditRecord;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.ExecutionStatus;
import com.looksee.models.message.AuditSet;
import com.looksee.models.message.DomainAuditMessage;
import com.looksee.models.message.PageAuditRecordMessage;
import com.looksee.models.message.PageStateAuditComplete;
import com.looksee.models.message.PageStateMessage;
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
public class Auditor extends AbstractActor{
	private static Logger log = LoggerFactory.getLogger(Auditor.class.getName());

	private Cluster cluster = Cluster.get(getContext().getSystem());
	
	@Autowired
	private AuditService audit_service;
	
	@Autowired
	private AuditRecordService audit_record_service;
	
	@Autowired
	private AuditFactory audit_factory;
	
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
				.match(PageStateMessage.class, page_state_msg -> {
				   	//generate audit report
				   	Set<Audit> audits = new HashSet<>();
				   	
				   	//check if page state has valid status code
				   	if(page_state_msg.getPageState().getHttpStatus() == 404) {
				   		return;
				   	}
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
				   	for(AuditCategory audit_category : AuditCategory.values()) {
						log.warn("performing all other audits");
						
			   			List<Audit> rendered_audits_executed = audit_factory.executePageAudits( audit_category, 
			   																					page_state_msg.getPageState());
			   			rendered_audits_executed = audit_service.saveAll(rendered_audits_executed);
			   			audits.addAll(rendered_audits_executed);
			   		}
		   			
					PageStateAuditComplete audit_complete = new PageStateAuditComplete(page_state_msg.getDomainId(), 
																					   page_state_msg.getAccountId(), 
																					   page_state_msg.getAuditRecordId(), 
																					   page_state_msg.getPageState());
		   			getSender().tell(audit_complete, getSelf());
		   			
		   			AuditRecord audit_record = new PageAuditRecord(ExecutionStatus.IN_PROGRESS, audits, page_state_msg.getPageState(), true);
		   			audit_record = audit_record_service.save(audit_record);
		   			
		   			PageAuditRecordMessage page_audit_msg = new PageAuditRecordMessage(
		   																(PageAuditRecord)audit_record, 
		   																page_state_msg.getDomainId(), 
		   																page_state_msg.getAccountId(), 
		   																page_state_msg.getAuditRecordId());
		   			getSender().tell( page_audit_msg, getSelf() );
		   			//send message to either user or page channel containing reference to audits
				})
				.match(DomainAuditMessage.class, domain_msg -> {
					log.warn("audit record set message received...");
					List<Audit> audits_executed = new ArrayList<>();
				   	for(AuditCategory audit_category : AuditCategory.values()) {
				   		//perform audit and return audit result
				   		getSender().tell(new AuditSet(audits_executed, "http://"+domain_msg.getDomain().getUrl()), getSelf());
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

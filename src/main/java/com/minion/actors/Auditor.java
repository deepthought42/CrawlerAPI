package com.minion.actors;

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

import com.qanairy.models.Account;
import com.qanairy.models.PageState;
import com.qanairy.models.PageVersion;
import com.qanairy.models.audit.Audit;
import com.qanairy.models.audit.AuditFactory;
import com.qanairy.models.audit.PageAuditRecord;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.ExecutionStatus;
import com.qanairy.models.message.AuditSet;
import com.qanairy.models.message.DomainAuditMessage;
import com.qanairy.models.message.PageStateAuditComplete;
import com.qanairy.services.AuditService;

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
				.match(PageVersion.class, page_version-> {
				   	//generate audit report
				   	List<Audit> audits = new ArrayList<>();
				   	
				   	for(AuditCategory audit_category : AuditCategory.values()) {
				   		//check if page state already
			   			//perform audit and return audit result
			   			List<Audit> audits_executed = audit_factory.executePrerenderPageAudits(audit_category, page_version);

			   			audits.addAll(  audit_service.saveAll(audits_executed) );
			   		}
		   			
					//PageAuditComplete audit_complete = new PageAuditComplete(page_state);
		   			//getSender().tell(audit_complete, getSelf());
		   			getSender().tell(new AuditSet(audits, page_version.getUrl()), getSelf());
		   			//send message to either user or page channel containing reference to audits
				})
				.match(PageState.class, page_state-> {
				   	//generate audit report
				   	Set<Audit> audits = new HashSet<>();
				   	
				   	for(AuditCategory audit_category : AuditCategory.values()) {
				   		//check if page state already
			   			//perform audit and return audit result
			   			List<Audit> rendered_audits_executed = audit_factory.executePostRenderPageAudits(audit_category, page_state);
    
			   			rendered_audits_executed = audit_service.saveAll(rendered_audits_executed);

			   			audits.addAll(rendered_audits_executed);
			   		}
		   			
					PageStateAuditComplete audit_complete = new PageStateAuditComplete(page_state);
		   			getSender().tell(audit_complete, getSelf());
		   			getSender().tell( new PageAuditRecord(ExecutionStatus.IN_PROGRESS, audits, page_state), getSelf() );
		   			//send message to either user or page channel containing reference to audits
				})
				.match(DomainAuditMessage.class, domain_msg -> {
					log.warn("audit record set message received...");
				   	for(AuditCategory audit_category : AuditCategory.values()) {
				   		//perform audit and return audit result
				   		List<Audit> audits_executed = new ArrayList<>();
			   			audits_executed.addAll(audit_factory.executePrerenderDomainAudit(audit_category, domain_msg.getDomain()));
			   			audits_executed.addAll(audit_factory.executePostRenderDomainAudit(audit_category, domain_msg.getDomain()));
				   		
			   			audits_executed = audit_service.saveAll(audits_executed);
			   			getSender().tell(new AuditSet(audits_executed, "http://"+domain_msg.getDomain().getHost()), getSelf());
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

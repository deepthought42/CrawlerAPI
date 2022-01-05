package services;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.boot.test.context.SpringBootTest;

import com.looksee.models.enums.SubscriptionPlan;
import com.looksee.services.SubscriptionService;

@SpringBootTest
public class SubscriptionServiceTests {

	@InjectMocks
	private SubscriptionService subscription_service;
	
	
	@Before
	public void start(){
        MockitoAnnotations.initMocks(this);
    }
	
	//@Test
	/*
	public void belowLimitTestRunsOnFreePlan() throws StripeException{
		when(account.getEmail()).thenReturn("test@test.com");
		when(account_service.getTestCountByMonth(anyString(), anyInt())).thenReturn(199);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.FREE);
		assertFalse(has_exceeded);
	}
	
	//@Test
	public void reachingLimitTestRunsOnFreePlan() throws StripeException {
		when(account.getEmail()).thenReturn("test@test.com");
		when(account_service.getTestCountByMonth(anyString(), anyInt())).thenReturn(200);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.FREE);
		assertFalse(has_exceeded);
	}
	
	//@Test
	public void exceedingLimitTestRunsOnFreePlan() throws StripeException {
		when(account.getEmail()).thenReturn("test@test.com");
		when(account_service.getTestCountByMonth(anyString(), anyInt())).thenReturn(201);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.FREE);
		assertTrue(has_exceeded);
	}
	

	//@Test
	public void lessThanLimitDiscoveredTestsOnFreePlan() throws StripeException {
		when(account.getEmail()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		records.add(record);
		
		when(record.getTestCount()).thenReturn(99);
		when(record.getStartTime()).thenReturn(new Date());
		when(account_service.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.FREE);
		assertFalse(has_exceeded);
	}
	
	//@Test
	public void reachingLimitDiscoveredTestsOnFreePlan() throws StripeException {
		when(account.getEmail()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		when(record.getTestCount()).thenReturn(100);
		when(record.getStartTime()).thenReturn(new Date());
		records.add(record);
		when(account_service.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.FREE);
		assertFalse(has_exceeded);
	}
	
	//@Test
	public void exceedingDiscoveredTestsOnFreePlan() throws StripeException {
		when(account.getEmail()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		when(record.getTestCount()).thenReturn(101);
		when(record.getStartTime()).thenReturn(new Date());
		records.add(record);
		when(account_service.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.FREE);
		assertTrue(has_exceeded);
	}

	//@Test
	public void belowLimitTestRunsOnProPlan() throws StripeException {
		when(account.getEmail()).thenReturn("test@test.com");
		when(account_service.getTestCountByMonth(anyString(), anyInt())).thenReturn(1999);
			
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.COMPANY_PRO);
		assertFalse(has_exceeded);
	}
	
	//@Test
	public void reachingLimitTestRunsOnProPlan() throws StripeException {
		when(account.getEmail()).thenReturn("test@test.com");
		when(account_service.getTestCountByMonth(anyString(), anyInt())).thenReturn(2000);
			
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.COMPANY_PRO);
		assertFalse(has_exceeded);
	}
	
	//@Test
	public void exceedingLimitTestRunsOnProPlan() throws StripeException {
		when(account.getEmail()).thenReturn("test@test.com");
		when(account_service.getTestCountByMonth(anyString(), anyInt())).thenReturn(2001);
			
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.COMPANY_PRO);
		assertTrue(has_exceeded);
	}
	
	
	//@Test
	public void lessThanLimitDiscoveredTestsOnProPlan() throws StripeException {
		when(account.getEmail()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		when(record.getTestCount()).thenReturn(249);
		when(record.getStartTime()).thenReturn(new Date());
		records.add(record);
		when(account_service.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.COMPANY_PRO);
		assertFalse(has_exceeded);
	}
	
	//@Test
	public void reachingLimitDiscoveredTestsOnPROPlan() throws StripeException {
		when(account.getEmail()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		when(record.getTestCount()).thenReturn(250);
		when(record.getStartTime()).thenReturn(new Date());
		records.add(record);
		when(account_service.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.COMPANY_PRO);
		assertFalse(has_exceeded);
	}
	
	//@Test
	public void exceedingDiscoveredTestsOnProPlan() throws StripeException {
		when(account.getEmail()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		when(record.getTestCount()).thenReturn(251);
		when(record.getStartTime()).thenReturn(new Date());
		records.add(record);
		when(account_service.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.COMPANY_PRO);
		assertTrue(has_exceeded);
	}
	*/
	
	@Test
	public void hasExceededDomainPageAuditLimit()  {
		boolean has_exceeded = subscription_service.hasExceededDomainPageAuditLimit(SubscriptionPlan.FREE, 5);
		assertFalse(has_exceeded);
		
		has_exceeded = subscription_service.hasExceededDomainPageAuditLimit(SubscriptionPlan.FREE, 6);
		assertTrue(has_exceeded);
		
		has_exceeded = subscription_service.hasExceededDomainPageAuditLimit(SubscriptionPlan.COMPANY_PRO, 50);
		assertFalse(has_exceeded);
		
		has_exceeded = subscription_service.hasExceededDomainPageAuditLimit(SubscriptionPlan.COMPANY_PRO, 51);
		assertTrue(has_exceeded);
		
		has_exceeded = subscription_service.hasExceededDomainPageAuditLimit(SubscriptionPlan.COMPANY_PREMIUM, 200);
		assertFalse(has_exceeded);
		
		has_exceeded = subscription_service.hasExceededDomainPageAuditLimit(SubscriptionPlan.COMPANY_PREMIUM, 201);
		assertTrue(has_exceeded);
		
		has_exceeded = subscription_service.hasExceededDomainPageAuditLimit(SubscriptionPlan.AGENCY_PRO, 50);
		assertFalse(has_exceeded);
		
		has_exceeded = subscription_service.hasExceededDomainPageAuditLimit(SubscriptionPlan.AGENCY_PRO, 51);
		assertTrue(has_exceeded);
		
		has_exceeded = subscription_service.hasExceededDomainPageAuditLimit(SubscriptionPlan.AGENCY_PREMIUM, 400);
		assertFalse(has_exceeded);
		
		has_exceeded = subscription_service.hasExceededDomainPageAuditLimit(SubscriptionPlan.AGENCY_PREMIUM, 401);
		assertTrue(has_exceeded);
	}
	
	@Test
	public void hasExceededSinglePageAuditLimit()  {
		boolean has_exceeded = subscription_service.hasExceededSinglePageAuditLimit(SubscriptionPlan.FREE, 5);
		assertFalse(has_exceeded);
		
		has_exceeded = subscription_service.hasExceededSinglePageAuditLimit(SubscriptionPlan.FREE, 6);
		assertTrue(has_exceeded);
		
		has_exceeded = subscription_service.hasExceededSinglePageAuditLimit(SubscriptionPlan.COMPANY_PRO, 50);
		assertFalse(has_exceeded);
		
		has_exceeded = subscription_service.hasExceededSinglePageAuditLimit(SubscriptionPlan.COMPANY_PRO, 51);
		assertTrue(has_exceeded);
		
		has_exceeded = subscription_service.hasExceededSinglePageAuditLimit(SubscriptionPlan.COMPANY_PREMIUM, 100);
		assertFalse(has_exceeded);
		
		has_exceeded = subscription_service.hasExceededSinglePageAuditLimit(SubscriptionPlan.COMPANY_PREMIUM, 101);
		assertTrue(has_exceeded);
		
		has_exceeded = subscription_service.hasExceededSinglePageAuditLimit(SubscriptionPlan.AGENCY_PRO, 200);
		assertFalse(has_exceeded);
		
		has_exceeded = subscription_service.hasExceededSinglePageAuditLimit(SubscriptionPlan.AGENCY_PRO, 201);
		assertTrue(has_exceeded);
		
		has_exceeded = subscription_service.hasExceededSinglePageAuditLimit(SubscriptionPlan.AGENCY_PREMIUM, 500);
		assertFalse(has_exceeded);
		
		has_exceeded = subscription_service.hasExceededSinglePageAuditLimit(SubscriptionPlan.AGENCY_PREMIUM, 501);
		assertTrue(has_exceeded);
	}
	
	/*
	public void freePlanWithExistingSubscription() throws Exception{
		subscription_service.changeSubscription(account_spy, SubscriptionPlan.FREE);
	}
	*/
}

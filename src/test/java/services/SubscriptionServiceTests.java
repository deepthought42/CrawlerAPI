package services;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.boot.test.context.SpringBootTest;

import com.qanairy.models.Account;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.enums.SubscriptionPlan;
import com.qanairy.services.AccountService;
import com.qanairy.services.SubscriptionService;
import com.stripe.exception.StripeException;

@SpringBootTest
public class SubscriptionServiceTests {

	@InjectMocks
	private SubscriptionService subscription_service;
	
	@Mock
	private Account account;
	
	@Spy
	private Account account_spy;
	
	@Mock
	private AccountService account_service;

	@Mock
	private DiscoveryRecord record;
	
	
	@Before
	public void start(){
        MockitoAnnotations.initMocks(this);
    }
	
	@Test
	public void belowLimitTestRunsOnFreePlan() throws StripeException{
		when(account.getUsername()).thenReturn("test@test.com");
		when(account_service.getTestCountByMonth(anyString(), anyInt())).thenReturn(399);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.FREE);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void reachingLimitTestRunsOnFreePlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		when(account_service.getTestCountByMonth(anyString(), anyInt())).thenReturn(400);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.FREE);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void exceedingLimitTestRunsOnFreePlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		when(account_service.getTestCountByMonth(anyString(), anyInt())).thenReturn(401);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.FREE);
		assertTrue(has_exceeded);
	}
	

	@Test
	public void lessThanLimitDiscoveredTestsOnFreePlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		records.add(record);
		
		when(record.getTestCount()).thenReturn(199);
		when(record.getStartTime()).thenReturn(new Date());
		when(account_service.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.FREE);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void reachingLimitDiscoveredTestsOnFreePlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		when(record.getTestCount()).thenReturn(200);
		when(record.getStartTime()).thenReturn(new Date());
		records.add(record);
		when(account_service.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.FREE);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void exceedingDiscoveredTestsOnFreePlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		when(record.getTestCount()).thenReturn(201);
		when(record.getStartTime()).thenReturn(new Date());
		records.add(record);
		when(account_service.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.FREE);
		assertTrue(has_exceeded);
	}

	@Test
	public void belowLimitTestRunsOnProPlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		when(account_service.getTestCountByMonth(anyString(), anyInt())).thenReturn(1999);
			
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.PRO);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void reachingLimitTestRunsOnProPlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		when(account_service.getTestCountByMonth(anyString(), anyInt())).thenReturn(2000);
			
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.PRO);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void exceedingLimitTestRunsOnProPlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		when(account_service.getTestCountByMonth(anyString(), anyInt())).thenReturn(2001);
			
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.PRO);
		assertTrue(has_exceeded);
	}
	
	
	@Test
	public void lessThanLimitDiscoveredTestsOnProPlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		when(record.getTestCount()).thenReturn(999);
		when(record.getStartTime()).thenReturn(new Date());
		records.add(record);
		when(account_service.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.PRO);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void reachingLimitDiscoveredTestsOnPROPlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		when(record.getTestCount()).thenReturn(1000);
		when(record.getStartTime()).thenReturn(new Date());
		records.add(record);
		when(account_service.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.PRO);
		assertFalse(has_exceeded);
	}
	
	//@Test
	public void exceedingDiscoveredTestsOnProPlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		when(record.getTestCount()).thenReturn(1001);
		when(record.getStartTime()).thenReturn(new Date());
		records.add(record);
		when(account_service.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.PRO);
		assertTrue(has_exceeded);
	}
	
	public void freePlanWithExistingSubscription() throws Exception{
		subscription_service.changeSubscription(account_spy, SubscriptionPlan.FREE, "paymentToken");
	}
}

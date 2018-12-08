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
import com.qanairy.models.repository.AccountRepository;
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
	private AccountRepository account_repo;

	@Mock
	private DiscoveryRecord record;
	
	
	@Before
	public void start(){
        MockitoAnnotations.initMocks(this);
    }
	
	@Test
	public void belowLimitTestRunsOnFreePlan() throws StripeException{
		when(account.getUsername()).thenReturn("test@test.com");
		when(account_repo.getTestCountByMonth(anyString(), anyInt())).thenReturn(99);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.FREE);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void reachingLimitTestRunsOnFreePlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		when(account_repo.getTestCountByMonth(anyString(), anyInt())).thenReturn(100);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.FREE);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void exceedingLimitTestRunsOnFreePlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		when(account_repo.getTestCountByMonth(anyString(), anyInt())).thenReturn(101);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.FREE);
		assertTrue(has_exceeded);
	}
	

	@Test
	public void lessThanLimitDiscoveredTestsOnFreePlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		records.add(record);
		
		when(record.getTestCount()).thenReturn(49);
		when(record.getStartTime()).thenReturn(new Date());
		when(account_repo.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.FREE);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void reachingLimitDiscoveredTestsOnFreePlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		when(record.getTestCount()).thenReturn(50);
		when(record.getStartTime()).thenReturn(new Date());
		records.add(record);
		when(account_repo.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.FREE);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void exceedingDiscoveredTestsOnFreePlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		when(record.getTestCount()).thenReturn(51);
		when(record.getStartTime()).thenReturn(new Date());
		records.add(record);
		when(account_repo.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.FREE);
		assertTrue(has_exceeded);
	}

	@Test
	public void belowLimitTestRunsOnProPlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		when(account_repo.getTestCountByMonth(anyString(), anyInt())).thenReturn(499);
			
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.PRO);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void reachingLimitTestRunsOnProPlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		when(account_repo.getTestCountByMonth(anyString(), anyInt())).thenReturn(500);
			
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.PRO);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void exceedingLimitTestRunsOnProPlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		when(account_repo.getTestCountByMonth(anyString(), anyInt())).thenReturn(501);
			
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.PRO);
		assertTrue(has_exceeded);
	}
	
	
	@Test
	public void lessThanLimitDiscoveredTestsOnProPlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		when(record.getTestCount()).thenReturn(249);
		when(record.getStartTime()).thenReturn(new Date());
		records.add(record);
		when(account_repo.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.PRO);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void reachingLimitDiscoveredTestsOnPROPlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		when(record.getTestCount()).thenReturn(250);
		when(record.getStartTime()).thenReturn(new Date());
		records.add(record);
		when(account_repo.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.PRO);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void exceedingDiscoveredTestsOnProPlan() throws StripeException {
		when(account.getUsername()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		when(record.getTestCount()).thenReturn(251);
		when(record.getStartTime()).thenReturn(new Date());
		records.add(record);
		when(account_repo.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.PRO);
		assertTrue(has_exceeded);
	}
	
	public void freePlanWithExistingSubscription() throws Exception{
		subscription_service.changeSubscription(account_spy, SubscriptionPlan.FREE, "paymentToken");
	}
}

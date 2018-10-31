package services;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import com.qanairy.models.Account;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.enums.SubscriptionPlan;
import com.qanairy.models.repository.AccountRepository;
import com.qanairy.services.SubscriptionService;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;

@SpringBootTest
public class SubscriptionServiceTests {

	@InjectMocks
	private SubscriptionService subscription_service;
	
	@Mock
	private Account account;
	
	@Mock
	private AccountRepository account_repo;

	
	
	@Before
	public void start(){
        MockitoAnnotations.initMocks(this);
    }
	
	@Test
	public void belowLimitTestRunsOnFreePlan() throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
		when(account.getUsername()).thenReturn("test@test.com");
		when(account_repo.getTestCountByMonth(anyString(), anyInt())).thenReturn(99);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.FREE);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void reachingLimitTestRunsOnFreePlan() throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
		when(account.getUsername()).thenReturn("test@test.com");
		when(account_repo.getTestCountByMonth(anyString(), anyInt())).thenReturn(100);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.FREE);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void ExceedingLimitTestRunsOnFreePlan() throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
		when(account.getUsername()).thenReturn("test@test.com");
		when(account_repo.getTestCountByMonth(anyString(), anyInt())).thenReturn(101);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.FREE);
		assertTrue(has_exceeded);
	}
	

	@Test
	public void lessThanLimitDiscoveredTestsOnFreePlan() throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
		when(account.getUsername()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		DiscoveryRecord record = new DiscoveryRecord();
		record.setTestCount(49);
		records.add(record);
		when(account_repo.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.FREE);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void reachingLimitDiscoveredTestsOnFreePlan() throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
		when(account.getUsername()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		DiscoveryRecord record = new DiscoveryRecord();
		record.setTestCount(50);
		records.add(record);
		when(account_repo.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.FREE);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void exceedingDiscoveredTestsOnFreePlan() throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
		when(account.getUsername()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		DiscoveryRecord record = new DiscoveryRecord();
		record.setTestCount(51);
		records.add(record);
		when(account_repo.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.FREE);
		assertTrue(has_exceeded);
	}
	

	@Test
	public void belowLimitTestRunsOnProPlan() throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
		when(account.getUsername()).thenReturn("test@test.com");
		when(account_repo.getTestCountByMonth(anyString(), anyInt())).thenReturn(4999);
			
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.PRO);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void reachingLimitTestRunsOnProPlan() throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
		when(account.getUsername()).thenReturn("test@test.com");
		when(account_repo.getTestCountByMonth(anyString(), anyInt())).thenReturn(5000);
			
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.PRO);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void ExceedingLimitTestRunsOnProPlan() throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
		when(account.getUsername()).thenReturn("test@test.com");
		when(account_repo.getTestCountByMonth(anyString(), anyInt())).thenReturn(5001);
			
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account, SubscriptionPlan.PRO);
		assertTrue(has_exceeded);
	}
	
	
	@Test
	public void lessThanLimitDiscoveredTestsOnProPlan() throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
		when(account.getUsername()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		DiscoveryRecord record = new DiscoveryRecord();
		record.setTestCount(249);
		records.add(record);
		when(account_repo.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.PRO);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void reachingLimitDiscoveredTestsOnPROPlan() throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
		when(account.getUsername()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		DiscoveryRecord record = new DiscoveryRecord();
		record.setTestCount(250);
		records.add(record);
		when(account_repo.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.PRO);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void exceedingDiscoveredTestsOnProPlan() throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
		when(account.getUsername()).thenReturn("test@test.com");
		Set<DiscoveryRecord> records = new HashSet<DiscoveryRecord>();
		DiscoveryRecord record = new DiscoveryRecord();
		record.setTestCount(251);
		records.add(record);
		when(account_repo.getDiscoveryRecordsByMonth(anyString(), anyInt())).thenReturn(records);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionDiscoveredLimit(account, SubscriptionPlan.PRO);
		assertTrue(has_exceeded);
	}
}

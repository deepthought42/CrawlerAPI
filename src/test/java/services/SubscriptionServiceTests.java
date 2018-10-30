package services;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.matchers.Any;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.minion.actors.GeneralFormTestDiscoveryActor;
import com.qanairy.models.Account;
import com.qanairy.models.DiscoveryRecord;
import com.qanairy.models.Domain;
import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.SubscriptionPlan;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.models.repository.AccountRepository;
import com.qanairy.models.repository.DomainRepository;
import com.qanairy.services.SubscriptionService;
import com.stripe.exception.APIConnectionException;
import com.stripe.exception.APIException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.CardException;
import com.stripe.exception.InvalidRequestException;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class SubscriptionServiceTests {
	@Mock
	private Account account;
	
	@Mock
	private AccountRepository account_repo;
	
	@Mock
	private DomainRepository domain_repo;
	
	@Mock
	SubscriptionService subscription_service;
	
	@Before
	public void start(){
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void assertFalseForLessThan20TestRunsOnFreePlan() throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
		System.err.println("Subscription_service  :  "+subscription_service);
		when(account.getSubscriptionToken()).thenReturn("subscription_token");
		when(subscription_service.getSubscriptionPlanName(any(Account.class))).thenReturn(SubscriptionPlan.FREE);
		
		Set<Domain> records = new HashSet<Domain>();
		Domain record = new Domain("http", "staging-marketing.qanairy.com", "chrome", null);
		records.add(record);
		when(account_repo.getDomains(any(String.class))).thenReturn(records);
		
		Set<TestRecord> tests = new HashSet<TestRecord>();
		for(int i=0; i<20; i++){
			TestRecord test = new TestRecord(new Date(), TestStatus.PASSING, "chrome", null, 100);
			tests.add(test);
		}
		when(domain_repo.getTestsByMonth(any(String.class), any(Integer.class))).thenReturn(tests);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void assertFalseFor20TestRunsOnFreePlan() throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
		when(subscription_service.getSubscriptionPlanName(any(Account.class))).thenReturn(SubscriptionPlan.FREE);
		
		Set<Domain> records = new HashSet<Domain>();
		Domain record = new Domain("http", "staging-marketing.qanairy.com", "chrome", null);
		records.add(record);
		when(account_repo.getDomains(any(String.class))).thenReturn(records);
		
		Set<TestRecord> tests = new HashSet<TestRecord>();
		for(int i=0; i<21; i++){
			TestRecord test = new TestRecord(new Date(), TestStatus.PASSING, "chrome", null, 100);
			tests.add(test);
		}
		when(domain_repo.getTestsByMonth(any(String.class), any(Integer.class))).thenReturn(tests);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account);
		assertFalse(has_exceeded);
	}
	
	@Test
	public void assertTrueFor21TestRunsOnFreePlan() throws AuthenticationException, InvalidRequestException, APIConnectionException, CardException, APIException{
		when(subscription_service.getSubscriptionPlanName(any(Account.class))).thenReturn(SubscriptionPlan.FREE);
		
		Set<Domain> records = new HashSet<Domain>();
		Domain record = new Domain("http", "staging-marketing.qanairy.com", "chrome", null);
		records.add(record);
		when(account_repo.getDomains(any(String.class))).thenReturn(records);
		
		Set<TestRecord> tests = new HashSet<TestRecord>();
		for(int i=0; i<22; i++){
			TestRecord test = new TestRecord(new Date(), TestStatus.PASSING, "chrome", null, 100);
			tests.add(test);
		}
		when(domain_repo.getTestsByMonth(any(String.class), any(Integer.class))).thenReturn(tests);
		
		boolean has_exceeded = subscription_service.hasExceededSubscriptionTestRunsLimit(account);
		assertFalse(has_exceeded);
	}
}

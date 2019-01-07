package models;

import static org.junit.Assert.*;
import static org.mockito.Mockito.framework;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import com.minion.api.EntryPoint;
import com.qanairy.models.PageState;
import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.models.repository.TestRecordRepository;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EntryPoint.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TestRecordTests {

	@Autowired
	TestRecordRepository test_record_repo;
	
	@Mock
	PageState page_state;
	
	
	@Test
	public void testRecordUpdateVerification(){
		when(page_state.getKey()).thenReturn("pagestate:fakeKey");
		TestRecord fake_record = new TestRecord(new Date(), TestStatus.PASSING, "chrome", page_state, 100000L);
		
		//save record
		System.err.println("key :: " + fake_record.getKey());
		TestRecord copy = test_record_repo.findByKey("testrecord::561712743pagestate:fakeKey");
		//TestRecord record = test_record_repo.save(fake_record);
		
		//if(copy != null){
		//	record = copy;
		//}
		
		TestStatus status = TestStatus.FAILING;
		TestRecord record = test_record_repo.updateStatus(copy.getKey(), status.toString());
		
		copy = test_record_repo.findByKey(record.getKey());
		
		assertTrue(copy.getStatus().equals(TestStatus.FAILING));
		
	}
}

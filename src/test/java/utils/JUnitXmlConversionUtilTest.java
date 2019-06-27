package utils;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.qanairy.models.Test;
import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.TestStatus;
import com.qanairy.utils.JUnitXmlConversionUtil;

/**
 * Unit tests for {@link JUnitXmlConversionUtil}
 * @author brand
 *
 */
public class JUnitXmlConversionUtilTest {

	@Mock 
	private Test test;
	
	@Mock
	private TestRecord record;
	
	@Before
	public void setup(){
		MockitoAnnotations.initMocks(this);
	}
	
	@org.junit.Test
	public void convertToJUnitXmlTestWith1FailingRecord(){
		List<TestRecord> records = new ArrayList<TestRecord>();
		records.add(record);
		
		when(record.getTest()).thenReturn(test);
		when(record.getRunTime()).thenReturn(10000L);
		when(test.getName()).thenReturn("Practice test #1");
		when(record.getStatus()).thenReturn(TestStatus.FAILING);

		String xml = JUnitXmlConversionUtil.convertToJUnitXml(records, 1, 250, new Date());
		System.err.println("OUTPUT ::   "+xml);
	}
}

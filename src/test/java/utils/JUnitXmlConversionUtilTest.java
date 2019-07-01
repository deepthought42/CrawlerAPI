package utils;

import static org.junit.Assert.assertTrue;
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
	public void setUp(){
		MockitoAnnotations.initMocks(this);
	}
	
	public void convertToJUnitXmlTestWith1FailingRecord(){
		List<TestRecord> records = new ArrayList<TestRecord>();
		records.add(record);
		
		when(record.getRunTime()).thenReturn(10000L);
		when(test.getName()).thenReturn("Practice test #1");
		when(record.getStatus()).thenReturn(TestStatus.FAILING);

		String xml = JUnitXmlConversionUtil.convertToJUnitXml(records, 1, 250, new Date());
		System.err.println("OUTPUT ::   "+xml);
		//assertTrue(xml.equals("<testsuites id='' name='' tests='1' failures='1' time='250'>\n<testsuite id='' name='' skipped='' tests='1' failures='1' time='250' timestamp='Thu Jun 27 00:20:51 EDT 2019'><testcase id='' name='Practice test #1' time='10000'>\n<failure message='' type='WARNING' >\nERROR MESSAGE HERE. (THE ABILITY TO TRACK ERROR MESSAGES IS NOT CURRENTLY SUPPORTED)\n</failure>\n</testcase>\n</testsuite>\n</testsuites>"));
	}
}

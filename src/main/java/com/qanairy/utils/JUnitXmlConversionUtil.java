package com.qanairy.utils;

import java.util.Date;
import java.util.List;

import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.TestStatus;

public class JUnitXmlConversionUtil {

	public static String convertToJUnitXml(List<TestRecord> test_record_list, long time_in_sec, Date date){
		StringBuffer str_buf = new StringBuffer();
		
		str_buf.append("<testsuites>\n");
		str_buf.append("<testsuite errors='' skipped='' tests='" +test_record_list.size()+ "' failures='' time='" + time_in_sec + "' timestamp='" + date.toString() + "'>\n");
		
		for(TestRecord record : test_record_list){
			str_buf.append("<testcase name='" + record.getTest().getName()+ "' time='" + record.getRunTime() + "'>\n");
			
			if(record.getStatus().equals(TestStatus.FAILING)){
				str_buf.append("<failure message='' type='WARNING' >\n");
				str_buf.append("ERROR MESSAGE HERE. (THE ABILITY TO TRACK ERROR MESSAGES IS NOT CURRENTLY SUPPORTED)");
				str_buf.append("</failure>\n");
			}
			
			str_buf.append("</testcase>\n");
		}
		
		str_buf.append("</testsuite>\n");
		str_buf.append("</testsuites>\n");
		return str_buf.toString();
	}
}

package com.qanairy.utils;

import java.util.List;

import com.qanairy.models.TestRecord;
import com.qanairy.models.enums.TestStatus;

public class JUnitXmlConversionUtil {

	public String convertToXml(List<TestRecord> test_record_list){
		StringBuffer str_buf = new StringBuffer();
		
		str_buf.append("<testsuites>");
		str_buf.append("<testsuite errors='' skipped='' tests='" +test_record_list.size()+ "' failures='' time='' timestamp=''>");
		
		for(TestRecord record : test_record_list){
			str_buf.append("<testcase name='" + record.getTest().getName()+ "' time='" + record.getRunTime() + "'> ");
			
			if(record.getPassing().equals(TestStatus.FAILING)){
				str_buf.append("<failure message='' type='WARNING' >");
				str_buf.append("ERROR MESSAGE HERE");
				str_buf.append("</failure>");
			}
			
			str_buf.append("</testcase>");
		}
		
		str_buf.append("</testsuite>");
		str_buf.append("</testsuites>");
		return str_buf.toString();
	}
}

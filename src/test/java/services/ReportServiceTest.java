package services;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import com.looksee.models.audit.UXIssueMessage;
import com.looksee.models.audit.recommend.Recommendation;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;
import com.looksee.services.ReportService;

public class ReportServiceTest {
	
	@Test
	public void testGenerateExcelReport() throws FileNotFoundException, IOException {
		//create list of messages
		List<UXIssueMessage> messages = new ArrayList<>();
		Set<Recommendation> recommendations = new HashSet<>();

		UXIssueMessage issue_msg_1 = new UXIssueMessage(Priority.HIGH,
				"38 icons do not meet the minimum required color contrast ratio level of 3:1",
				ObservationType.COLOR_CONTRAST,
				AuditCategory.AESTHETICS,
				"buttons should have a minimum color contrast ratio of 3:1 with the background surrounding the button",
				new HashSet<>(),
				null, 
				null, 
				0, 
				1, 
				recommendations,
				"");
		
		messages.add(issue_msg_1);
		
		UXIssueMessage issue_msg_2 = new UXIssueMessage(Priority.LOW,
				"3 grammatical errors found",
				ObservationType.ELEMENT,
				AuditCategory.CONTENT,
				"The grammar should be consistent with respect to the language used\n" + 
				"(English UK / US).",
				new HashSet<>(),
				null, 
				null, 
				0, 
				1, 
				recommendations,
				"");
		
		messages.add(issue_msg_2);
		
		UXIssueMessage issue_msg_3 = new UXIssueMessage(Priority.MEDIUM,
				"3 grammatical errors found",
				ObservationType.ELEMENT,
				AuditCategory.INFORMATION_ARCHITECTURE,
				"meta title exceeds the recommended 70 character limit",
				new HashSet<>(),
				null, 
				null, 
				0, 
				1, 
				recommendations,
				"");
		
		messages.add(issue_msg_3);
		
		UXIssueMessage issue_msg_4 = new UXIssueMessage(Priority.MEDIUM,
				"3 grammatical errors found",
				ObservationType.ELEMENT,
				AuditCategory.AESTHETICS,
				"meta title exceeds the recommended 70 character limit",
				new HashSet<>(),
				null, 
				null, 
				0, 
				1, 
				recommendations,
				"");
		
		messages.add(issue_msg_4);
		
		
		URL url = new URL("https://www.look-see.com");
		XSSFWorkbook workbook = ReportService.generateExcelSpreadsheet(messages, url);
        
        try (FileOutputStream outputStream = new FileOutputStream("JavaBooks.xlsx")) {
        	System.out.println("writing to output stream ... ");
            workbook.write(outputStream);
        }
		System.out.println("workbook ... "+workbook.getAllNames());
		System.out.println("workbook sheets... "+workbook.getNumberOfSheets());
		System.out.println("workbook sheets... "+workbook.getActiveSheetIndex());
		System.out.println("workbook sheets... "+workbook.getSpreadsheetVersion());

	}
}

package services;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import com.looksee.models.audit.UXIssueMessage;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.ObservationType;
import com.looksee.models.enums.Priority;
import com.looksee.services.ReportService;

public class ReportServiceTest {
	
	@Test
	public void testGenerateExcelReport() throws FileNotFoundException, IOException {
		//create list of messages
		List<UXIssueMessage> messages = new ArrayList<>();
		UXIssueMessage issue_msg_1 = new UXIssueMessage("The buttons on the home page have a low visibility due to the video in the\n" + 
				"background. Consider changing the background or color of the buttons.\n" + 
				"\n" + 
				"Graphical objects & user interface components should have a color contrast of\n" + 
				"at least 3:1 with the background colors.",
				Priority.HIGH,
				"38 icons do not meet the minimum required color contrast ratio level of 3:1",
				ObservationType.COLOR_CONTRAST,
				AuditCategory.AESTHETICS,
				"buttons should have a minimum color contrast ratio of 3:1 with the background surrounding the button",
				new HashSet<>(), 
				null, 
				null, 
				0, 
				1);
		
		messages.add(issue_msg_1);
		
		UXIssueMessage issue_msg_2 = new UXIssueMessage("Overall, the Miso website uses an active voice and scored well on our\n" + 
				"passive voice index. However, we identified over 50 grammatical errors on\n" + 
				"your website, out of which more than 80% were from ‘Press Release’\n" + 
				"pages. Proper grammar communicates professionalism to your users, so it\n" + 
				"is best to ensure no errors in this category.",
				Priority.LOW,
				"3 grammatical errors found",
				ObservationType.ELEMENT,
				AuditCategory.CONTENT,
				"The grammar should be consistent with respect to the language used\n" + 
				"(English UK / US).",
				new HashSet<>(), 
				null, 
				null, 
				0, 
				1);
		
		messages.add(issue_msg_2);
		
		UXIssueMessage issue_msg_3 = new UXIssueMessage("The meta-title and meta-description provide the user with a concise, easy to digest summary of what your page is about. A user is more likely to click on your website and visit your page if they are attracted by a title and\n" + 
				"description that is clear, concise, and not overwhelming.",
				Priority.MEDIUM,
				"3 grammatical errors found",
				ObservationType.ELEMENT,
				AuditCategory.INFORMATION_ARCHITECTURE,
				"meta title exceeds the recommended 70 character limit",
				new HashSet<>(), 
				null, 
				null, 
				0, 
				1);
		
		messages.add(issue_msg_3);
		
		UXIssueMessage issue_msg_4 = new UXIssueMessage("Clean typography, with the use of only 1 to 2 typefaces, invites users to the text on your website. It plays an important role in how clear, distinct and legible the textual content is. Sticking to 1-2 typefaces keeps your website clean, making users more likely to read it.",
				Priority.MEDIUM,
				"3 grammatical errors found",
				ObservationType.ELEMENT,
				AuditCategory.AESTHETICS,
				"meta title exceeds the recommended 70 character limit",
				new HashSet<>(), 
				null, 
				null, 
				0, 
				1);
		
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

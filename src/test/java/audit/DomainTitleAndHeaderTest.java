package audit;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

import com.qanairy.models.PageState;
import com.qanairy.models.audit.TitleAndHeaderAudit;
import com.qanairy.models.enums.BrowserType;


public class DomainTitleAndHeaderTest {

	@Test
	public void verifyHasFavicon(){
		String src_example = "<html><head><link rel='icon' href='http://nourl.com'/></head><body></body></html>";
		PageState page = new PageState("", new ArrayList<>(), src_example, true, 0, 0, 1000, 1000, BrowserType.CHROME, "", "example.com");
		TitleAndHeaderAudit audit = new TitleAndHeaderAudit();
		assertTrue(audit.hasFavicon(page));
	}
}

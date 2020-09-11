package audit;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

import com.qanairy.models.PageVersion;
import com.qanairy.models.audit.domain.DomainTitleAndHeaderAudit;


public class DomainTitleAndHeaderTest {

	@Test
	public void verifyHasFavicon(){
		String src_example = "<html><head><link rel='icon' href='http://nourl.com'/></head><body></body></html>";
		PageVersion page = new PageVersion(new ArrayList<>(), src_example, "","", "");
		DomainTitleAndHeaderAudit audit = new DomainTitleAndHeaderAudit();
		assertTrue(audit.hasFavicon(page));
	}
}

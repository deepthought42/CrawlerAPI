package utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.services.pagespeedonline.v5.model.LighthouseAuditResultV5;
import com.google.api.services.pagespeedonline.v5.model.PagespeedApiPagespeedResponseV5;
import com.crawlerApi.gcp.PageSpeedInsightUtils;
import com.crawlerApi.models.audit.UXIssueMessage;
import com.crawlerApi.utils.BrowserUtils;

public class PageSpeedInsightUtilTest {
	private static Logger log = LoggerFactory.getLogger(PageSpeedInsightUtilTest.class.getName());

	public void testPageSpeedInsights() throws IOException, GeneralSecurityException {
		PagespeedApiPagespeedResponseV5 page_speed_response = PageSpeedInsightUtils.getPageInsights(BrowserUtils.sanitizeUrl("https://wave.webaim.org", false));
		System.out.println("page speed response length :: " + page_speed_response.toPrettyString().length());
		assertTrue(page_speed_response.toPrettyString().length() > 0);
		//List<UXIssueMessage> ux_issues = extractInsights(page_speed_response);
		
	    Map<String, LighthouseAuditResultV5> audit_map = page_speed_response.getLighthouseResult().getAudits();

	    List<UXIssueMessage> ux_issues = PageSpeedInsightUtils.extractIssues(page_speed_response);
		
	}
}

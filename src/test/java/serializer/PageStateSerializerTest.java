package serializer;

import static org.testng.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qanairy.models.PageState;
import com.qanairy.models.ScreenshotSet;

public class PageStateSerializerTest {

	@Test
	public void verifyPageStateSerializer() throws JsonProcessingException{
		PageState page_state = new PageState();
		page_state.setKey("temp_key");
		Set<ScreenshotSet> scrn_set = new HashSet<ScreenshotSet>();
		scrn_set.add(new ScreenshotSet("temp_screenshot.png", "chrome"));
		page_state.setBrowserScreenshots(scrn_set);
		page_state.setUrl("staging-marketing.qanairy.com");
		page_state.setType("PageState");
		String serialized = new ObjectMapper().writeValueAsString(page_state);
		
		assertTrue(serialized.contains("\"key\":\"temp_key\""));
		assertTrue(serialized.contains("\"type\":\"PageState\""));
		assertTrue(serialized.contains("\"url\":\"staging-marketing.qanairy.com\""));
		assertTrue(serialized.contains("\"browserScreenshots\":["));
		assertTrue(serialized.contains("\"key\":\"screenshot::e2c267e549a1c640779dad386583e5fca4f8bd1bc4b5079b889093e8b07cba6263561c1edc956b82a2bb14d1bddf1f9820d5fe27f80ddada64b6712e20e9e4da\""));
		assertTrue(serialized.contains("\"viewportScreenshot\":\"temp_screenshot.png\""));
		assertTrue(serialized.contains("\"browser\":\"chrome\""));
	}
}

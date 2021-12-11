package utils;

import org.junit.Test;

import com.looksee.models.audit.ColorData;
import com.looksee.models.audit.recommend.ColorContrastRecommendation;
import com.looksee.utils.ColorUtils;

/**
 * 
 * 
 */
public class ColorUtilsTest {
	
	@Test
	public void verifyFindCompliantBackgroundColor(){
		ColorData text_color = new ColorData("#C4C4C4");
		ColorData bg_color = new ColorData("rgb(255,255,255)");
		
		ColorContrastRecommendation recommendation = 
				ColorUtils.findCompliantBackgroundColor(text_color, 
														bg_color, 
														false, 
														14, 
														true);
		
		assert(recommendation == null);

		ColorData text_color2 = new ColorData("#757575");
		ColorData bg_color2 = new ColorData("#C4C4C4");
		
		ColorContrastRecommendation recommendation2 = 
				ColorUtils.findCompliantBackgroundColor(text_color2, 
														bg_color2, 
														false, 
														14, 
														true);
		
		assert(recommendation2.getColor1Rgb().contentEquals("117,117,117"));
		assert(recommendation2.getColor2Rgb().contentEquals("253,253,253"));
		
		ColorData text_color3 = new ColorData("#4F4F4F");
		ColorData bg_color3 = new ColorData("#C4C4C4");
		
		ColorContrastRecommendation recommendation3 = 
				ColorUtils.findCompliantBackgroundColor(text_color3, 
														bg_color3, 
														false, 
														14, 
														true);
		
		assert(recommendation3.getColor1Rgb().contentEquals("79,79,79"));
		assert(recommendation3.getColor2Rgb().contentEquals("196,196,196"));
	}
	
	@Test
	public void verifyFindCompliantFontColor(){
		ColorData text_color = new ColorData("#C4C4C4");
		ColorData bg_color = new ColorData("rgb(255,255,255)");
		
		ColorContrastRecommendation recommendation = 
				ColorUtils.findCompliantFontColor(text_color, 
													bg_color, 
													false, 
													14, 
													true);
		
		assert(recommendation.getColor1Rgb().contentEquals("118,118,118"));
		assert(recommendation.getColor2Rgb().contentEquals("255,255,255"));
		
		ColorData text_color2 = new ColorData("#757575");
		ColorData bg_color2 = new ColorData("#C4C4C4");
		
		ColorContrastRecommendation recommendation2 = 
				ColorUtils.findCompliantFontColor(text_color2, 
													bg_color2, 
													false, 
													14, 
													true);
		
		assert(recommendation2.getColor1Rgb().contentEquals("81,81,81"));
		assert(recommendation2.getColor2Rgb().contentEquals("196,196,196"));
		
		ColorData text_color3 = new ColorData("#4F4F4F");
		ColorData bg_color3 = new ColorData("#C4C4C4");
		
		ColorContrastRecommendation recommendation3 = 
				ColorUtils.findCompliantFontColor(text_color3, 
													bg_color3, 
													false, 
													14, 
													true);
		
		assert(recommendation3.getColor1Rgb().contentEquals("79,79,79"));
		assert(recommendation3.getColor2Rgb().contentEquals("196,196,196"));
		
		ColorData text_color4 = new ColorData("#000000");
		ColorData bg_color4 = new ColorData("#111111");
		
		ColorContrastRecommendation recommendation4 = 
				ColorUtils.findCompliantFontColor(text_color4, 
													bg_color4, 
													false, 
													14, 
													true);
		
		assert(recommendation4 == null);
	}
}

package utils;

import org.junit.Assert;
import org.junit.Test;

import com.looksee.models.audit.ColorData;
import com.looksee.utils.ImageUtils;

public class ImageUtilsTests {
	@Test
	public void testCalculateDeltaE() {
		ColorData color1 = new ColorData("32,34,2"); //less black
		ColorData color2 = new ColorData("32,2,0"); // black
		
		float diff = ImageUtils.calculateDeltaE(color1, color2);
		System.out.println("black and less black delta e diff :: "+diff);
		Assert.assertTrue(diff == 27.587776f);
		
		ColorData color3 = new ColorData("16,9,129"); // blue
		ColorData color4 = new ColorData("16,98,4"); // green
		diff = ImageUtils.calculateDeltaE(color3, color4);
		System.out.println("blue and green delta e diff :: "+diff);
		Assert.assertTrue(diff == 72.1287f);
		
		
		
		ColorData color5 = new ColorData("255,0,80"); //red
		ColorData color6 = new ColorData("32,2,0"); //black
		diff = ImageUtils.calculateDeltaE(color5, color6);
		System.out.println("red and black delta e diff :: "+diff);
		Assert.assertTrue(diff == 100.18136f);

		ColorData color7 = new ColorData("16,9,129"); // blue
		ColorData color8 = new ColorData("32,3,0"); // green
		diff = ImageUtils.calculateDeltaE(color7, color8);
		System.out.println("blue and black delta e diff :: "+diff);
		Assert.assertTrue(diff == 35.121536f);

		ColorData color9 = new ColorData("16,9,129"); // blue
		ColorData color0 = new ColorData("17,73,105"); // green
		diff = ImageUtils.calculateDeltaE(color9, color0);
		System.out.println("blue and light blue delta e diff :: "+diff);
		Assert.assertTrue(diff == 39.37754f);
	}
	
}

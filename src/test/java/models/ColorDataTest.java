package models;

import org.junit.Assert;
import org.junit.Test;

import com.looksee.models.audit.ColorData;

public class ColorDataTest {

	@Test
	public void computeContrast() {
		ColorData color1 = new ColorData("rgb(231,238,231)");
		ColorData color2 = new ColorData("rgba(0,0,0, 0.5)");
		color2.alphaBlend(color1);
		
		double contrast = ColorData.computeContrast(color1, color2);
				
		Assert.assertTrue(3.85276408388746 == contrast);
	}
	
	@Test
	public void computeContrastWhiteAndBlack() {
		ColorData color1 = new ColorData("rgb(255, 255, 255)");
		ColorData color2 = new ColorData("rgba(0,0,0)");
		color2.alphaBlend(color1);
		
		double contrast = ColorData.computeContrast(color1, color2);
		Assert.assertTrue(21.0 == contrast);
		
		ColorData color3 = new ColorData("rgb(35, 31, 32)");
		ColorData color4 = new ColorData("rgba(235, 248, 255)");
		color4.alphaBlend(color3);
		
		double contrast2 = ColorData.computeContrast(color3, color4);
		System.out.println("contrast 2 :: "+contrast2);
		Assert.assertTrue(15.061046459808948 == contrast2);
	}
}

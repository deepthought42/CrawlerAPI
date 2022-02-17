package models;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import com.looksee.models.audit.ColorData;

public class ColorDataTest {

	@Test
	public void constructorTest() {
		ColorData color1 = new ColorData("rgb(35, 82, 251)");
		assertTrue(color1.getHue() == 227);
		assertTrue(color1.getSaturation() == 86);
		assertTrue(color1.getBrightness() == 98);
		assertTrue(color1.getLuminosity() == 0.13356946167941947);
	}
	
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
	
	@Test
	public void isSimilarHueTest() {
		ColorData color1 = new ColorData("rgb(35, 82, 251)");
		ColorData color2 = new ColorData("rgb(200, 211, 250)");
		
		assertTrue(color1.isSimilarHue(color2));
		
		color2 = new ColorData("rgb(4, 8, 26)");
		
		assertTrue(color1.isSimilarHue(color2));
		
		color2 = new ColorData("rgb(35, 99, 250)");
		
		assertTrue(color1.isSimilarHue(color2));
		
		color2 = new ColorData("rgb(35, 103, 250)");
		
		assertFalse(color1.isSimilarHue(color2));
		
		color2 = new ColorData("rgb(35, 64, 250)");
		
		assertTrue(color1.isSimilarHue(color2));
		
		color2 = new ColorData("rgb(35, 60, 250)");
		
		assertFalse(color1.isSimilarHue(color2));
		
	}
}

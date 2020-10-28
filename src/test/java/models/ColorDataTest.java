package models;

import org.junit.Assert;
import org.junit.Test;

import com.qanairy.models.audit.ColorData;

public class ColorDataTest {

	@Test
	public void computeContrast() {
		ColorData color1 = new ColorData("rgb(231,238,231)");
		ColorData color2 = new ColorData("rgba(0,0,0, 0.5)");
		color2.alphaBlend(color1);
		
		double contrast = ColorData.computeContrast(color1, color2);
				
		Assert.assertTrue(3.85276408388746 == contrast);
	}
}

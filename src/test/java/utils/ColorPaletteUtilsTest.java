package utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.qanairy.models.audit.ColorData;
import com.qanairy.models.audit.ColorPaletteUtils;


public class ColorPaletteUtilsTest {
	@Test
	public void groupColorTest() {
		List<ColorData> colors = new ArrayList<>();
		colors.add(new ColorData("231,238,231"));
		colors.add(new ColorData("0,0,80"));
		colors.add(new ColorData("53,60,53"));
		colors.add(new ColorData("rgb(116,119,116)"));

		Set<Set<ColorData>> color_sets = ColorPaletteUtils.groupColors(colors);
		
		System.out.println("colors :: "+color_sets);
		for(Set<ColorData> color_set : color_sets) {
			System.out.println("-----------------------------");
			for(ColorData color : color_set){
				System.out.println("color ::  "+color.rgb());
			}
		}
	}
	
	@Test
	public void getMaxRGBTest() {
		ColorData color = new ColorData("rgb( 231,238,231 )");
		int max = ColorPaletteUtils.getMax(color);
		assertTrue(max == 238);
		
		int min = ColorPaletteUtils.getMin(color);
		assertTrue(min == 231);
		
		boolean is_gray = ColorPaletteUtils.isGrayScale(color);
		assertTrue(is_gray);
		
		color = new ColorData("rgb( 227,238,231 )");

		is_gray = ColorPaletteUtils.isGrayScale(color);
		assertTrue(!is_gray);
		
		color = new ColorData("rgb( 228,238,231 )");

		is_gray = ColorPaletteUtils.isGrayScale(color);
		assertTrue(is_gray);
	}
	
	@Test
	public void isSimilarTest() {
		ColorData color1 = new ColorData("rgb( 231,238,231 )");
		color1.setUsagePercent(0.1f);
		ColorData color2 = new ColorData("rgb( 53,60,53 )");
		color2.setUsagePercent(1f);
		
		assertFalse(ColorPaletteUtils.isSimilar(color1, color2));
	}
	
	@Test
	public void isSimilarTestBlackAndWhite() {
		ColorData color1 = new ColorData("rgb( 255, 255, 255 )");
		color1.setUsagePercent(0.1f);
		ColorData color2 = new ColorData("rgb( 0, 0, 0 )");
		color2.setUsagePercent(1f);
		
		assertFalse(ColorPaletteUtils.isSimilar(color1, color2));
	}
	
	
	@Test
	public void identifyPrimaryColorsTest() {
		ColorData color = new ColorData("rgb(116,119,116)");
		color.setUsagePercent(0.5f);
		ColorData color1 = new ColorData("rgb( 231,255,231 )");
		color1.setUsagePercent(0.1f);

		ColorData color2 = new ColorData("rgb( 53, 10,53 )");
		color2.setUsagePercent(1f);

		
		List<ColorData> colors = new ArrayList<>();
		colors.add(color);
		colors.add(color1);
		colors.add(color2);
		
		Set<ColorData> color_set = ColorPaletteUtils.identifyPrimaryColors(colors);
		for(ColorData primary : color_set) {
			System.out.println(primary.rgb());
		}
	}
	
	/*
	Primary Color : 0,0,0    Percentage :: 7%
	Primary Color : 255,0,80    Percentage :: 13%
	Primary Color : 249,191,8    Percentage :: 0%
	Primary Color : 255,255,255    Percentage :: 25%
	Primary Color : 2,6,22    Percentage :: 0%
	Primary Color : 247,248,251    Percentage :: 22%
	Primary Color : 35,31,32    Percentage :: 1%

	 */
	@Test
	public void identifyPrimaryColorsTestLookseeColors() {
		ColorData color = new ColorData("rgb(0,0,0)");
		color.setUsagePercent(0.5f);
		ColorData color1 = new ColorData("rgb( 255,0,80 )");
		color1.setUsagePercent(0.1f);

		ColorData color2 = new ColorData("rgb( 249,191,8 )");
		color2.setUsagePercent(1f);

		ColorData color3 = new ColorData("255,255,255");
		color3.setUsagePercent(2f);
		
		ColorData color4 = new ColorData("rgb( 2,6,22 )");
		color4.setUsagePercent(0.1f);

		ColorData color5 = new ColorData("rgb( 247,248,251 )");
		color5.setUsagePercent(1f);

		ColorData color6 = new ColorData("35,31,32");
		color6.setUsagePercent(2f);
		
		List<ColorData> colors = new ArrayList<>();
		colors.add(color);
		colors.add(color1);
		colors.add(color2);
		colors.add(color3);
		colors.add(color4);
		colors.add(color5);
		colors.add(color6);
		
		Set<ColorData> color_set = ColorPaletteUtils.identifyPrimaryColors(colors);
		for(ColorData primary : color_set) {
			System.out.println(primary.rgb());
		}
	}
}

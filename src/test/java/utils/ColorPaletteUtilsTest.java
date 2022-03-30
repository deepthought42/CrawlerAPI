package utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.looksee.models.audit.ColorData;
import com.looksee.models.audit.ColorPaletteUtils;
import com.looksee.models.designsystem.PaletteColor;


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
		
		color = new ColorData("rgb( 212,238,231 )");

		is_gray = ColorPaletteUtils.isGrayScale(color);
		assertTrue(is_gray);
		
		color = new ColorData("rgb( 213,238,231 )");

		is_gray = ColorPaletteUtils.isGrayScale(color);
		assertTrue(is_gray);
		
		color = new ColorData("rgb( 1,6,22 )");

		is_gray = ColorPaletteUtils.isGrayScale(color);
		assertTrue(is_gray);
		
		color = new ColorData("rgb( 255,255,255 )");

		is_gray = ColorPaletteUtils.isGrayScale(color);
		assertTrue(is_gray);
		
		color = new ColorData("rgb( 193,193,193 )");

		is_gray = ColorPaletteUtils.isGrayScale(color);
		assertTrue(is_gray);
		
		color = new ColorData("rgb(  2,6,22 )");

		is_gray = ColorPaletteUtils.isGrayScale(color);
		assertTrue(is_gray);
		
		color = new ColorData("rgb( 54,58,65)");

		is_gray = ColorPaletteUtils.isGrayScale(color);
		assertFalse(is_gray);
		
		color = new ColorData("rgb( 53,60,53)");

		is_gray = ColorPaletteUtils.isGrayScale(color);
		assertTrue(is_gray);
		
		color = new ColorData("rgb( 95,88,80)");

		is_gray = ColorPaletteUtils.isGrayScale(color);
		assertFalse(is_gray);
		
		color = new ColorData("rgb( 252,229,221)");

		is_gray = ColorPaletteUtils.isGrayScale(color);
		assertTrue(is_gray);
		
		color = new ColorData("rgb(  99,104,113)");

		is_gray = ColorPaletteUtils.isGrayScale(color);
		assertTrue(is_gray);
		
		color = new ColorData("rgb( 34,41,53)");

		is_gray = ColorPaletteUtils.isGrayScale(color);
		assertTrue(is_gray);
		
		color = new ColorData("rgb(129,136,129)");

		is_gray = ColorPaletteUtils.isGrayScale(color);
		assertTrue(is_gray);
		
		color = new ColorData("rgb(115, 135, 115)");

		is_gray = ColorPaletteUtils.isGrayScale(color);
		assertFalse(is_gray);
		
		color = new ColorData("rgb(124, 135, 124)");

		is_gray = ColorPaletteUtils.isGrayScale(color);
		assertTrue(is_gray);
		
		color = new ColorData("rgb(47, 51, 47)");

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
		
		Set<ColorData> color_set = ColorPaletteUtils.identifyColorSet(colors);
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
		
		Set<ColorData> color_set = ColorPaletteUtils.identifyColorSet(colors);
		for(ColorData primary : color_set) {
			System.out.println(primary.rgb());
		}
	}
	
	@Test
	public void extractPaletteTest() {
		ColorData color = new ColorData("rgb( 231,238,231)");
		color.setUsagePercent(0.71f);
		ColorData color1 = new ColorData("rgb(136,170,137)");
		color1.setUsagePercent(0.001f);

		ColorData color2 = new ColorData("rgb(53,60,53)");
		color2.setUsagePercent(0.14f);

		ColorData color3 = new ColorData("0,0,0");
		color3.setUsagePercent(0.08f);
		
		ColorData color4 = new ColorData("rgb( 36,36,36)");
		color4.setUsagePercent(0.02f);

		ColorData color5 = new ColorData("255,255,255");
		color5.setUsagePercent(0.08f);
		
		ColorData color6 = new ColorData("129,136,129");
		color6.setUsagePercent(0.001f);
		
		List<ColorData> colors = new ArrayList<>();
		colors.add(color);
		colors.add(color1);
		colors.add(color2);
		colors.add(color3);
		colors.add(color4);
		colors.add(color5);
		colors.add(color6);

		List<PaletteColor> color_set = ColorPaletteUtils.extractPalette(colors);
		for(PaletteColor primary : color_set) {
			System.out.println(primary.getPrimaryColor());
		}
		//assertTrue(color_set.size() == 3);
	}
	
	@Test
	public void extractPaletteTestLookSeeColors() {
		ColorData color = new ColorData("rgb( 35,31,32)");
		color.setUsagePercent(0.71f);
		
		ColorData color1 = new ColorData("rgb(193,193,193)");
		color1.setUsagePercent(0.001f);

		ColorData color2 = new ColorData("rgb(255,0,81)");
		color2.setUsagePercent(0.14f);

		ColorData color3 = new ColorData("35,27,24");
		color3.setUsagePercent(0.08f);
		
		ColorData color4 = new ColorData("55,17,24");
		color4.setUsagePercent(0.08f);
		
		ColorData color5 = new ColorData("105,17,204");
		color5.setUsagePercent(0.08f);
		
		ColorData color6 = new ColorData("145,127,204");
		color6.setUsagePercent(0.06f);
		
		ColorData color7 = new ColorData("15,17,204");
		color7.setUsagePercent(0.05f);
		
		List<ColorData> colors = new ArrayList<>();
		colors.add(color);
		colors.add(color1);
		colors.add(color2);
		colors.add(color3);
		colors.add(color4);
		colors.add(color5);
		colors.add(color6);
		colors.add(color7);
		
		List<PaletteColor> color_set = ColorPaletteUtils.extractPalette(colors);
		for(PaletteColor primary : color_set) {
			System.out.println(primary.getPrimaryColor());
		}
		
		System.out.println("primary colors : "+color_set.size());
		assertTrue(color_set.size() == 5);
	}

}

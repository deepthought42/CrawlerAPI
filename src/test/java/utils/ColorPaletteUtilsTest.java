package utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.qanairy.models.audit.ColorData;
import com.qanairy.models.audit.ColorPaletteUtils;


public class ColorPaletteUtilsTest {
	@Test
	public void groupColorTest() {
		List<ColorData> colors = new ArrayList<>();
		colors.add(new ColorData("32,34,2"));
		colors.add(new ColorData("255,0,80"));
		colors.add(new ColorData("25,137,105"));
		colors.add(new ColorData("253,151,31"));
		colors.add(new ColorData("17,73,105"));
		colors.add(new ColorData("32,2,0"));

		colors.add(new ColorData("174,129,255"));
		colors.add(new ColorData("152,16,129"));
		colors.add(new ColorData("189,0,0"));
		colors.add(new ColorData("35,31,32"));
		colors.add(new ColorData("16,9,129"));

		Set<Set<ColorData>> color_sets = ColorPaletteUtils.groupColors(colors);
		
		System.out.println("colors :: "+color_sets);
		
	}
}

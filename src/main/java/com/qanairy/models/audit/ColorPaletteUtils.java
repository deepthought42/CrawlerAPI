package com.qanairy.models.audit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.models.enums.ColorScheme;

/**
 * 
 */
public class ColorPaletteUtils {
	private static Logger log = LoggerFactory.getLogger(ColorPaletteUtils.class);

	/**
	 * Scores site color palette based on the color scheme it most resembles
	 * @param palette
	 * @param scheme
	 * @return
	 * 
	 * @pre palette != null
	 * @pre scheme != null
	 */
	public static double getPaletteScore(Map<ColorData, Set<ColorData>> palette, ColorScheme scheme) {
		assert palette != null;
		assert scheme != null;
		
		//if palette has exactly 1 color set and that color set has more than 1 color, then monochromatic
		double score = 0;
		if(ColorScheme.GRAYSCALE.equals(scheme)) {
			score = 3;
		}
		//check if monochromatic
		else if(ColorScheme.MONOCHROMATIC.equals(scheme)) {
			score = getMonochromaticScore(palette);
		}
		//check if complimentary
		else if( ColorScheme.COMPLEMENTARY.equals(scheme)) {
			score = getComplementaryScore(palette);
		}
		//analogous and triadic both have 3 colors
		else if(ColorScheme.ANALOGOUS.equals(scheme)) {
			score = 3;
		}
		else if(ColorScheme.SPLIT_COMPLIMENTARY.equals(scheme)) {
			log.warn("Color scheme is SPLIT COMPLIMENTARY!!!");
			score = 3;
		}
		else if(ColorScheme.TRIADIC.equals(scheme)) {
			//check if triadic
			//if hues are nearly equal in differences then return triadic
			log.warn("Color scheme is TRIADIC!!!");
			score = 3;
		}
		else if(ColorScheme.TETRADIC.equals(scheme)) {
			log.warn("Color scheme is Tetradic!!!");

			//check if outer points are equal in distances
			score = 2;
		}
		else {
			//unknown color scheme
			score = 0;
		}
		
		return score;
	}

	/**
	 * // NOTE:: we consider black and white as one color and the shades of gray as shades of 1 extreme meaning that grayscale is 1 color(gray) with many shades.
	 * @param palette
	 * @return
	 * 
	 * @pre palette != null
	 */
	public static ColorScheme getColorScheme(Map<ColorData, Set<ColorData>> palette) {
		assert palette != null;
		
		//if palette has exactly 1 color set and that color set has more than 1 color, then monochromatic
		if(palette.isEmpty()) {
			return ColorScheme.GRAYSCALE;
		}
		//check if monochromatic
		else if(palette.size() == 1 && palette.get(palette.keySet().iterator().next()).size() > 1) {
			log.warn("COLOR IS MONOCHROMATIC!!!!!!!");
			return ColorScheme.MONOCHROMATIC;
		}
		
		//check if complimentary
		else if( palette.size() == 2 ) {
			return ColorScheme.COMPLEMENTARY;
		}
		//analogous and triadic both have 3 colors
		else if(palette.size() == 3) {
			//check if analogous
			//if difference in hue is less than 0.40 for min and max hues then return analogous
			double min_hue = 1.0;
			double max_hue = 0.0;
			for(ColorData color : palette.keySet()) {
				log.warn("primary color :: rgb : "+color.rgb() + " ;      hsv    :  "+color.hsb());
				log.warn("primary color has subcolors    ::   "+palette.get(color).size());
				for(ColorData sub_color : palette.get(color)) {
					if(sub_color == null) {
						continue;
					}
					log.warn("Sub-color  ::   "+sub_color.rgb());
				}
				
				if(color.getHue() > max_hue) {
					max_hue = color.getHue();
				}
				if(color.getHue() < min_hue) {
					min_hue = color.getHue();
				}
			}
			
			if((max_hue-min_hue) < 0.16) {
				log.warn("Color scheme is ANALOGOUS");
				return ColorScheme.ANALOGOUS;
			}
			else {
				//if all hues are roughly the same distance apart, then TRIADIC
				if(areEquidistantColors(palette.keySet())) {
					return ColorScheme.TRIADIC;
				}
				else {
					return ColorScheme.SPLIT_COMPLIMENTARY;
				}
			}
		}
		else if(palette.size() == 4) {
			log.warn("Color scheme is Tetradic!!!");
			//check if hues are equal in differences
			return ColorScheme.TETRADIC;
		}
		else {
			return ColorScheme.UNKNOWN;
		}
	}

	/**
	 * TODO Needs testing
	 * Checks if all colors are equidistant on the color wheel
	 * 
	 * @param colors
	 * @return
	 * 
	 * @pre colors != null;
	 */
	private static boolean areEquidistantColors(Set<ColorData> colors) {
		assert colors != null;
		
		List<ColorData> color_list = new ArrayList<>(colors);
		List<Double> distances = new ArrayList<>();
		for(int a=0; a < color_list.size()-1; a++) {
			for(int b=a+1; b < color_list.size(); b++) {
				//TODO AN ACTUAL DISTANCE METHOD HERE WOULD BE GREAT!!!!
				distances.add(
						Math.sqrt( Math.pow((color_list.get(b).getHue() - color_list.get(a).getHue()), 2) 
						+ Math.pow((color_list.get(b).getSaturation() - color_list.get(a).getSaturation()), 2) 
						+ Math.pow((color_list.get(b).getLuminosity() - color_list.get(a).getLuminosity()), 2)));
			}	
		}
		
		for(int a=0; a < distances.size()-1; a++) {
			for(int b=a+1; b < distances.size(); b++) {
				if( Math.abs(distances.get(a) - distances.get(b)) > .05 ){
					return false;
				}
			}	
		}
		
		return true;
	}

	/**
	 * Calculates a score for how well a palette adheres to a complimentary color palette
	 * @param palette
	 * @return
	 * 
	 * @pre palette != null
	 */
	private static double getComplementaryScore(Map<ColorData, Set<ColorData>> palette) {
		assert palette != null;
		
		//complimentary colors should add up to 255, 255, 255 with a margin of error of 2%
		double total_red = 0;
		double total_green = 0;
		double total_blue = 0;
		
		//if both color sets have only 1
		for(ColorData color : palette.keySet()) {
			total_red+= color.getRed();
			total_green += color.getGreen();
			total_blue += color.getBlue();
		}
		
		int red_score = getComplimentaryColorScore(total_red);
		int blue_score = getComplimentaryColorScore(total_blue);
		int green_score = getComplimentaryColorScore(total_green);

		return (red_score + blue_score + green_score)/ 3;
	}

	private static int getComplimentaryColorScore(double color_val) {
		//test if each color is within a margin of error that is acceptable for complimentary colors
		int score = 0;		
		if(color_val > 250 && color_val < 260) {
			score = 3;
		}
		else if(color_val > 245 && color_val < 265) {
			score = 2;
		}
		else if(color_val > 230 && color_val < 280) {
			score = 1;
		}
		return score;
	}

	/**
	 * Scores palette based on how well it adheres to a monochromatic color set
	 * @param palette
	 * @return
	 * 
	 * @pre palette != null
	 */
	private static int getMonochromaticScore(Map<ColorData, Set<ColorData>> palette) {
		assert palette != null;
		
		int score = 0;
		if(palette.get(palette.keySet().iterator().next()).size() == 2) {
			score = 3;
		}
		else if(palette.get(palette.keySet().iterator().next()).size() <= 1) {
			score = 1;
		}
		else if(palette.get(palette.keySet().iterator().next()).size() >= 3) {
			score = 2;
		}
		return score;
	}

	public static Map<ColorData, Set<ColorData>> extractPalette(Set<String> color_strings) {
		List<ColorData> colors = new ArrayList<ColorData>();
		
		for(String color : color_strings) {
			colors.add(new ColorData(color.trim()));
		}
		
		//identify colors that are a shade/tint of another color in the colors list and group them together in a set
		Set<Set<ColorData>> color_sets = groupColors(colors);
		
		//identify primary colors using saturation. Higher saturation indicates purity or intensity of the color
		Map<ColorData, Set<ColorData>> palette = new HashMap<>();
		for(Set<ColorData> color_set : color_sets) {
			if(color_set.size() == 1 ) {
				palette.put(color_set.iterator().next(), new HashSet<>());
			}
			else if(color_set.size() > 1) {
				double max_saturation = -1.0;
				ColorData primary_color = null;
				Set<ColorData> seconday_colors = new HashSet<>();
				for(ColorData color : color_set) {
					if(color.getSaturation() > max_saturation) {
						max_saturation = color.getSaturation();
						seconday_colors.add(primary_color);
						primary_color = color;
					}
					else {
						seconday_colors.add(color);
					}
				}
				palette.put(primary_color, seconday_colors);
			}
		}
		
		return palette;
	}

	private static Set<Set<ColorData>> groupColors(List<ColorData> colors) {
		Set<Set<ColorData>> color_sets = new HashSet<>();
		while(!colors.isEmpty()) {
			ColorData color = colors.get(0);
			Set<ColorData> similar_colors = new HashSet<>();
			for(int idx=0; idx < colors.size(); idx++) {
				ColorData color2 = colors.get(idx);

				if(!color2.equals(color)) {
					//if the difference between the 2 hues is less 3 degrees  
					if(Math.abs(color.getHue() - color2.getHue()) < 0.09 ) {	
						log.warn("Colors are similar in hue!!!!!");
						if(similar_colors.isEmpty()) {
							similar_colors.add(color);
						}
						similar_colors.add( color2 );
					}
				}
			}
			if(similar_colors.isEmpty()) {
				similar_colors.add(color);
			}
			color_sets.add(similar_colors);
			
			//filter similar colors and primary color from colors set
			for(ColorData similar_color : similar_colors) {
				colors.remove(similar_color);
			}
		}
		
		return color_sets;
	}
}

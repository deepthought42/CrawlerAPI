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
	public static Score getPaletteScore(List<PaletteColor> palette, ColorScheme scheme) {
		assert palette != null;
		assert scheme != null;
		
		//if palette has exactly 1 color set and that color set has more than 1 color, then monochromatic
		int score = 0;
		int max_points = 3;
				
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
			log.debug("Color scheme is SPLIT COMPLIMENTARY!!!");
			score = 3;
		}
		else if(ColorScheme.TRIADIC.equals(scheme)) {
			//check if triadic
			//if hues are nearly equal in differences then return triadic
			log.debug("Color scheme is TRIADIC!!!");
			score = 3;
		}
		else if(ColorScheme.TETRADIC.equals(scheme)) {
			log.debug("Color scheme is Tetradic!!!");

			//check if outer points are equal in distances
			score = 2;
		}
		else {
			//unknown color scheme
			score = 0;
		}
		
		return new Score(score, max_points, new HashSet<>());
	}

	/**
	 * // NOTE:: we consider black and white as one color and the shades of gray as shades of 1 extreme meaning that grayscale is 1 color(gray) with many shades.
	 * @param palette
	 * @return
	 * 
	 * @pre palette != null
	 */
	public static ColorScheme getColorScheme(List<PaletteColor> palette) {
		assert palette != null;
		
		//if palette has exactly 1 color set and that color set has more than 1 color, then monochromatic
		if(palette.isEmpty()) {
			return ColorScheme.GRAYSCALE;
		}
		//check if monochromatic
		else if(palette.size() == 1 && palette.iterator().next().getTintsShadesTones().size() > 1) {
			log.debug("COLOR IS MONOCHROMATIC!!!!!!!");
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
			for(PaletteColor palette_color : palette) {
				ColorData color = new ColorData(palette_color.getPrimaryColor());
				if(color.getHue() > max_hue) {
					max_hue = color.getHue();
				}
				if(color.getHue() < min_hue) {
					min_hue = color.getHue();
				}
			}
			
			if((max_hue-min_hue) < 0.16) {
				log.debug("Color scheme is ANALOGOUS");
				return ColorScheme.ANALOGOUS;
			}
			else {
				//if all hues are roughly the same distance apart, then TRIADIC
				if(areEquidistantColors(palette)) {
					return ColorScheme.TRIADIC;
				}
				else {
					return ColorScheme.SPLIT_COMPLIMENTARY;
				}
			}
		}
		else if(palette.size() == 4) {
			log.debug("Color scheme is Tetradic!!!");
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
	private static boolean areEquidistantColors(List<PaletteColor> colors) {
		assert colors != null;
		
		List<PaletteColor> color_list = new ArrayList<>(colors);
		List<Double> distances = new ArrayList<>();
		for(int a=0; a < color_list.size()-1; a++) {
			ColorData color_a = new ColorData(color_list.get(a).getPrimaryColor());
			for(int b=a+1; b < color_list.size(); b++) {
				ColorData color_b = new ColorData(color_list.get(b).getPrimaryColor());

				//TODO AN ACTUAL DISTANCE METHOD HERE WOULD BE GREAT!!!!
				distances.add(
						Math.sqrt( Math.pow((color_b.getHue() - color_a.getHue()), 2) 
						+ Math.pow((color_b.getSaturation() - color_a.getSaturation()), 2) 
						+ Math.pow((color_b.getLuminosity() - color_a.getLuminosity()), 2)));
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
	private static int getComplementaryScore(List<PaletteColor> palette) {
		assert palette != null;
		
		//complimentary colors should add up to 255, 255, 255 with a margin of error of 2%
		double total_red = 0;
		double total_green = 0;
		double total_blue = 0;
		
		//if both color sets have only 1
		for(PaletteColor color : palette) {
			ColorData color_data = new ColorData(color.getPrimaryColor());
			total_red+= color_data.getRed();
			total_green += color_data.getGreen();
			total_blue += color_data.getBlue();
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
	private static int getMonochromaticScore(List<PaletteColor> palette) {
		assert palette != null;
		
		int tint_shade_tone_size = palette.get(0).getTintsShadesTones().size();
		int score = 0;
		if(tint_shade_tone_size == 2) {
			score = 3;
		}
		else if(tint_shade_tone_size <= 1) {
			score = 1;
		}
		else if(tint_shade_tone_size >= 3) {
			score = 2;
		}
		return score;
	}

	/**
	 * Extracts set of {@link PaletteColor colors} that define a palette based on a set of rgb strings
	 * 
	 * @param color_strings
	 * @return
	 */
	public static List<PaletteColor> extractPalette(List<String> color_strings) {
		assert color_strings != null;
		
		List<ColorData> colors = new ArrayList<>();
		for(String color : color_strings) {
			colors.add(new ColorData(color));
		}
		
		List<PaletteColor> palette_colors = new ArrayList<>();
		
		//identify colors that are a shade/tint of another color in the colors list and group them together in a set
		Set<Set<ColorData>> color_sets = groupColors(colors);
		
		//identify primary colors using saturation. Higher saturation indicates purity or intensity of the color
		for(Set<ColorData> color_set : color_sets) {
			if(color_set.size() == 1 ) {
				ColorData primary_color = color_set.iterator().next();
				PaletteColor palette_color = new PaletteColor(
						primary_color.rgb(), 
						primary_color.getUsagePercent(), 
						new HashMap<>());
				palette_colors.add(palette_color);
			}
			else if(color_set.size() > 1) {
				double max_saturation = -1.0;
				ColorData primary_color = null;
				Map<String, String> secondary_colors = new HashMap<>();
				
				for(ColorData color : color_set) {
					if(color.getSaturation() > max_saturation) {
						max_saturation = color.getSaturation();
						if(primary_color != null) {
							secondary_colors.put(primary_color.rgb(), primary_color.getUsagePercent()+"");
						}
						primary_color = color;
					}
					else {
						secondary_colors.put(color.rgb(), color.getUsagePercent()+"");
					}
				}
				PaletteColor palette_color = new PaletteColor(
													primary_color.rgb(), 
													primary_color.getUsagePercent(), 
													secondary_colors);
				palette_colors.add(palette_color);
			}
		}
		
		return palette_colors;
	}

	private static Set<Set<ColorData>> groupColors(List<ColorData> colors) {
		assert colors != null;
		
		Set<Set<ColorData>> color_sets = new HashSet<>();
		while(!colors.isEmpty()) {
			String color_rgb = colors.get(0).toString();
			
			ColorData color = colors.get(0);
			Set<ColorData> similar_colors = new HashSet<>();
			for(int idx=0; idx < colors.size(); idx++) {
				ColorData color2 = colors.get(idx);

				if(!color2.equals(color)) {
					//if the difference between the 2 hues is less 3 degrees  
					if(Math.abs(color.getHue() - color2.getHue()) < 0.09 ) {	
						log.debug("Colors are similar in hue!!!!!");
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
	
	/**
	 * Converts a map representing primary and secondary colors within a palette from using {@link ColorData} to {@link String}
	 * @param palette
	 * @return
	 */
	public static Map<String, Set<String>> convertPaletteToStringRepresentation(Map<ColorData, Set<ColorData>> palette) {
		assert palette != null;
		
		Map<String, Set<String>> stringified_map = new HashMap<>();
		for(ColorData primary : palette.keySet()) {
			Set<String> secondary_colors = new HashSet<>();
			for(ColorData secondary : palette.get(primary)) {
				if(secondary == null) {
					continue;
				}
				secondary_colors.add(secondary.rgb());
			}
			stringified_map.put(primary.rgb(), secondary_colors);
		}
		return stringified_map;
	}
}

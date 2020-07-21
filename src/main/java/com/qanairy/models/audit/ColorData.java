package com.qanairy.models.audit;

import java.awt.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.models.LookseeObject;


/**
 * Represents an both rgb and hsb and luminosity values
 *
 */
public class ColorData extends LookseeObject{
	private static Logger log = LoggerFactory.getLogger(ColorData.class);

	private int red;
	private int green;
	private int blue;
	
	private double transparency;
	private double brightness;
	private double hue;
	private double saturation;
	private double luminosity;
	
	/**
	 * 
	 * @param rgba_string
	 * 
	 * @pre rgba_string != null
	 * @pre !rgba_string.isEmpty()
	 */
	public ColorData(String rgba_string) {
		assert rgba_string != null;
		assert !rgba_string.isEmpty();
		
		if(rgba_string.startsWith("#")) {
			Color color = Color.decode(rgba_string);
			this.red = color.getRed();
			this.green = color.getGreen();
			this.blue = color.getBlue();
		}
		else {
			//extract r,g,b,a from color_str
			String tmp_color_str = rgba_string.replace(")", "");
			tmp_color_str = tmp_color_str.replace("rgba(", "");
			tmp_color_str = tmp_color_str.replace("rgb(", "");
			tmp_color_str = tmp_color_str.replaceAll(" ", "");
			String[] rgba = tmp_color_str.split(",");
			
			this.red = Integer.parseInt(rgba[0]);
			this.green = Integer.parseInt(rgba[1]);
			this.blue = Integer.parseInt(rgba[2]);
			if(rgba.length == 4) {
				setTransparency(Double.parseDouble(rgba[3]));
			}
		}
		
		
		//convert rgb to hsl, store all as Color object
		float[] hsb = Color.RGBtoHSB(red, green, blue, null);
		this.hue = hsb[0];
		this.saturation = hsb[1];
		this.brightness = hsb[2];
		
		this.setLuminosity(calculateLuminosity(red, green, blue));
		
	}

	/**
	 * Calculates luminosity from rgb color
	 * 
	 * @param red
	 * @param green
	 * @param blue
	 * @return
	 */
	private double calculateLuminosity(int red, int green, int blue) {
		//calculate luminosity
		//For the sRGB colorspace, the relative luminance of a color is defined as
		//where R, G and B are defined as:
		double RsRGB = red/255;
		double GsRGB = green/255;
		double BsRGB = blue/255;

		double R, G, B;
		if(RsRGB <= 0.03928) {
			R = RsRGB/12.92;
		}
		else {
			R = Math.pow(((RsRGB+0.055)/1.055), 2.4);
		}
		if(GsRGB <= 0.03928) {
			G = GsRGB/12.92;
		}
		else {
			G = Math.pow(((GsRGB+0.055)/1.055), 2.4);
		}
		
		if(BsRGB <= 0.03928) {
			B = BsRGB/12.92;
		}
		else {
			B = Math.pow(((BsRGB+0.055)/1.055), 2.4);
		}
		
		return 0.2126 * R + 0.7152 * G + 0.0722 * B;
	}

	/**
	 * Returns a string with rgb values separated by a comma. For example 0,0,0
	 * 
	 * @return
	 */
	public String rgb() {
		return red+","+green+","+blue;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
        if (!(obj instanceof ColorData)) return false;
        
		ColorData color = (ColorData)obj;
		return color.blue == this.blue && color.red == this.red && color.blue == this.blue;
	}

	public String hsb() {
		return hue+" , "+saturation+" , "+brightness;
	}

	public int getRed() {
		return red;
	}
	
	public int getGreen() {
		return green;
	}
	
	public int getBlue() {
		return blue;
	}
	
	public double getTransparency() {
		return transparency;
	}

	private void setTransparency(double transparency) {
		this.transparency = transparency;
	}

	public double getLuminosity() {
		return luminosity;
	}

	private void setLuminosity(double luminosity) {
		this.luminosity = luminosity;
	}
	

	public double getHue() {
		return hue;
	}

	public double getSaturation() {
		return saturation;
	}
	
	public double getBrightness() {
		return brightness;
	}

	/**
	 * Computes the contrast of the 2 colors provided
	 * 
	 * @param color_data_1
	 * @param color_data_2
	 * 
	 * @return contrast value
	 * 
	 * @pre color_data_1 != null
	 * @pre color_data_2 != null
	 */
	public static double computeContrast(ColorData color_data_1, ColorData color_data_2) {
		assert color_data_1 != null;
		assert color_data_2 != null;
		
		double max_luminosity = 0.0;
		double min_luminosity = 0.0;
		
		if(color_data_1.getLuminosity() > color_data_2.getLuminosity()) {
			min_luminosity = color_data_2.getLuminosity();
			max_luminosity = color_data_1.getLuminosity();
		}
		else {
			min_luminosity = color_data_1.getLuminosity();
			max_luminosity = color_data_2.getLuminosity();
		}
		return (max_luminosity + 0.05) / (min_luminosity + 0.05);
	}

	@Override
	public String generateKey() {
		return rgb();
	}
}
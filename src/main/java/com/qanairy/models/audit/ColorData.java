package com.qanairy.models.audit;

/**
 * Represents an both rgb and hsb setting simulatenously
 *
 */
class ColorData{
	int red;
	int green;
	int blue;
	
	double transparency;
	double brightness;
	double hue;
	double saturation;
	
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
			transparency = Double.parseDouble(rgba[3]);
		}
		this.hue = 0.0;
		this.saturation = 0.0;
		this.brightness = 0.0;
	}
}
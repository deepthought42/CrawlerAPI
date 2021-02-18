package com.qanairy.utils;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.openimaj.image.analysis.colour.CIEDE2000;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.qanairy.models.ElementState;
import com.qanairy.models.audit.ColorData;
import com.qanairy.models.audit.ColorUsageStat;

public class ImageUtils {
	private static Logger log = LoggerFactory.getLogger(ImageUtils.class);

	 public static BufferedImage resize(BufferedImage img, int height, int width) {
        Image tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, img.getType());
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resized;
    }
	 
	/**
	 * Calculate the colour difference value between two colours in lab space.
	 * @param lab1 first colour
	 * @param lab2 second colour
	 * @return the CIE 2000 colour difference
	 */
	public static float calculateDeltaE(float [] lab1, float[] lab2) {
		return (float) CIEDE2000.calculateDeltaE(lab1[0],lab1[1],lab1[2],lab2[0],lab2[1],lab2[2]);
	}
	
	/**
	 * Calculate the colour difference value between two colours in lab space.
	 * @param lab1 first colour
	 * @param lab2 second colour
	 * @return the CIE 2000 colour difference
	 */
	public static float calculateDeltaE(ColorData color1, ColorData color2) {
		int[] lab1 = rgb2lab(color1.getRed(), color1.getGreen(), color1.getBlue());
		int[] lab2 = rgb2lab(color2.getRed(), color2.getGreen(), color2.getBlue());

		return (float) CIEDE2000.calculateDeltaE(lab1[0],lab1[1],lab1[2],lab2[0],lab2[1],lab2[2]);
	}
	
	public static int[] rgb2lab(int R, int G, int B) {
	    //http://www.brucelindbloom.com

	    float r, g, b, X, Y, Z, fx, fy, fz, xr, yr, zr;
	    float Ls, as, bs;
	    float eps = 216.f/24389.f;
	    float k = 24389.f/27.f;

	    float Xr = 0.964221f;  // reference white D50
	    float Yr = 1.0f;
	    float Zr = 0.825211f;

	    // RGB to XYZ
	    r = R/255.f; //R 0..1
	    g = G/255.f; //G 0..1
	    b = B/255.f; //B 0..1

	    // assuming sRGB (D65)
	    if (r <= 0.04045)
	        r = r/12;
	    else
	        r = (float) Math.pow((r+0.055)/1.055,2.4);

	    if (g <= 0.04045)
	        g = g/12;
	    else
	        g = (float) Math.pow((g+0.055)/1.055,2.4);

	    if (b <= 0.04045)
	        b = b/12;
	    else
	        b = (float) Math.pow((b+0.055)/1.055,2.4);


	    X =  0.436052025f*r     + 0.385081593f*g + 0.143087414f *b;
	    Y =  0.222491598f*r     + 0.71688606f *g + 0.060621486f *b;
	    Z =  0.013929122f*r     + 0.097097002f*g + 0.71418547f  *b;

	    // XYZ to Lab
	    xr = X/Xr;
	    yr = Y/Yr;
	    zr = Z/Zr;

	    if ( xr > eps )
	        fx =  (float) Math.pow(xr, 1/3.);
	    else
	        fx = (float) ((k * xr + 16.) / 116.);

	    if ( yr > eps )
	        fy =  (float) Math.pow(yr, 1/3.);
	    else
	    fy = (float) ((k * yr + 16.) / 116.);

	    if ( zr > eps )
	        fz =  (float) Math.pow(zr, 1/3.);
	    else
	        fz = (float) ((k * zr + 16.) / 116);

	    Ls = ( 116 * fy ) - 16;
	    as = 500*(fx-fy);
	    bs = 200*(fy-fz);
	    int[] lab = new int[3];
	    lab[0] = (int) (2.55*Ls + .5);
	    lab[1] = (int) (as + .5); 
	    lab[2] = (int) (bs + .5);       
	    
	    return lab;
	}

	/**
	 * Detects image properties such as color frequency from the specified local image.
	 * 
	 * @param image_url
	 * @throws IOException
	 */
	public static List<ColorUsageStat> extractImageProperties(BufferedImage buffered_image) throws IOException {
		List<ColorUsageStat> color_usage_stats = new ArrayList<>();
		
		int w = buffered_image.getWidth();
		int h = buffered_image.getHeight();
		//BufferedImage after = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		//AffineTransform at = new AffineTransform();
		//at.scale(0.5, 0.5);
		/*AffineTransformOp scaleOp = 
		   new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
		after = scaleOp.filter(buffered_image, after);
		*/
		Map<String, Integer> colors = new HashMap<>();
		//extract colors
		// Getting pixel color by position x and y
		for(int x=0; x < buffered_image.getWidth(); x++) {
			for(int y=0; y < buffered_image.getHeight(); y++) {
				 int clr = buffered_image.getRGB(x, y);
		        int red =   (clr & 0x00ff0000) >> 16;
		        int green = (clr & 0x0000ff00) >> 8;
		        int blue =   clr & 0x000000ff;
		        String rgb = red+","+green+","+blue;
		        if(colors.containsKey(rgb)) {
		        	colors.put(rgb, colors.get(rgb)+1); 
		        	
		        }else {
		        	colors.put(rgb, 1);
		        }
			}
		}
       
		for(String color_str: colors.keySet()) {
			ColorData color = new ColorData(color_str);
			float percent = colors.get(color_str) / (float) ( w * h );
			//log.warn(color_str+"     :     "+percent);
			ColorUsageStat color_stat = new ColorUsageStat(color.getRed(), color.getGreen(), color.getBlue(), percent, 0);
			color_usage_stats.add(color_stat);
		}
        
	    return color_usage_stats;
	}

	/**
	 * Extracts background color from element screenshot by identifying the most prevalent color and returning that color
	 * @param element
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public static ColorData extractBackgroundColor(ElementState element) throws MalformedURLException, IOException {
		List<ColorUsageStat> color_data_list = new ArrayList<>();
		log.warn("------------------------------------------------------");
		log.warn("element screenshot url : "+element.getScreenshotUrl());
		log.warn("------------------------------------------------------");
		
		color_data_list.addAll( extractImageProperties(ImageIO.read(new URL(element.getScreenshotUrl()))) );

		color_data_list.sort((ColorUsageStat h1, ColorUsageStat h2) -> Float.compare(h1.getPixelPercent(), h2.getPixelPercent()));

		//ColorUsageStat background_usage = color_data_list.get(color_data_list.size()-1);
		//ColorUsageStat foreground_usage = color_data_list.get(color_data_list.size()-2);
		//ColorData text_color = new ColorData("rgb("+ foreground_usage.getRed()+","+foreground_usage.getGreen()+","+foreground_usage.getBlue()+")");
		float largest_pixel_percent = -1f;
	    ColorUsageStat largest_color = null;
		//extract background colors
		for(ColorUsageStat color_stat : color_data_list) {
			//get color most used for background color
			if(color_stat.getPixelPercent() > largest_pixel_percent) {
				largest_pixel_percent = color_stat.getPixelPercent();
				largest_color = color_stat;
			}
		}
		return new ColorData("rgb("+ largest_color.getRed()+","+largest_color.getGreen()+","+largest_color.getBlue()+")");
		
	}
	
}

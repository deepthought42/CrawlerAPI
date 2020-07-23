package com.qanairy.models.audit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.qanairy.models.ElementState;
import com.qanairy.models.PageState;
import com.qanairy.models.enums.AuditCategory;
import com.qanairy.models.enums.AuditLevel;
import com.qanairy.models.enums.AuditSubcategory;

import cz.vutbr.web.css.CSSException;
import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.CombinedSelector;
import cz.vutbr.web.css.Declaration;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.css.StyleSheet;
import cz.vutbr.web.csskit.RuleFontFaceImpl;
import cz.vutbr.web.csskit.RuleKeyframesImpl;
import cz.vutbr.web.csskit.RuleMediaImpl;


/**
 * Responsible for executing an audit on the hyperlinks on a page for the information architecture audit category
 */
@Component
public class MarginAudit implements IExecutablePageStateAudit {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(MarginAudit.class);

	private String[] size_units = {"px", "pt", "%", "em", "rem", "ex", "vh", "vw", "vmax", "vmin", "mm", "cm", "in", "pc"};
	
	
	public MarginAudit() {	}
	
	private static String getAuditDescription() {
		return "";
	}

	private static List<String> buildBestPractices() {
		List<String> best_practices = new ArrayList<>();
		best_practices.add("");
		
		return best_practices;
	}
	
	private static String getAdaDescription() {
		return "None";
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * Identifies colors used on page, the color scheme type used, and the ultimately the score for how the colors used conform to scheme
	 *  
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 */
	@Override
	public Audit execute(PageState page_state) {
		assert page_state != null;
		
		Document doc = Jsoup.parse(page_state.getSrc());	
		List<String> raw_stylesheets = new ArrayList<>();
		
		Elements stylesheets = doc.select("link");
		for(Element stylesheet : stylesheets) {
			if("text/css".equalsIgnoreCase(stylesheet.attr("type"))) {
				String stylesheet_url = stylesheet.absUrl("href");
				//parse the style sheet
				try {
					String raw_sheet = URLReader(new URL(stylesheet_url));
					StyleSheet sheet = CSSFactory.parse(raw_sheet);
					raw_stylesheets.add(URLReader(new URL(stylesheet_url)));
				} catch (MalformedURLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (CSSException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		List<String> margin_values = new ArrayList<>();
		
		for(String raw_stylesheet : raw_stylesheets) {
			//parse the style sheet
			try {
				StyleSheet sheet = CSSFactory.parseString(raw_stylesheet, new URL(page_state.getUrl()));
				for(int idx = 0; idx < sheet.size(); idx++) {
					if(sheet.get(idx) instanceof RuleFontFaceImpl 
							|| sheet.get(idx) instanceof RuleMediaImpl
							|| sheet.get(idx) instanceof RuleKeyframesImpl) {
						continue;
					}
					
					//access the rules and declarations
					RuleSet rule = (RuleSet) sheet.get(idx);       //get the first rule
					for(CombinedSelector selector : rule.getSelectors()) {
	
						//if selector name is a class or id then clean selector name 
						//if selector name is used in the page source then
						//extract any margin property values
						
						//if selector name is a tagname then check if tag name is used in document
						//if selector is used in document then 
						//extract any margin property values

						String selector_str = selector.toString();
						if(selector_str.startsWith(".")
							|| selector_str.startsWith("#")) 
						{
							selector_str = selector_str.substring(1);
						}

						if(page_state.getSrc().contains(selector_str)) {
							//TODO look for margin and add it to the document
							for(Declaration declaration : rule) {
								if(declaration.getProperty().contains("margin")) {
									String raw_property_value = declaration.toString();
									raw_property_value = raw_property_value.replace("margin:", "");
									raw_property_value = raw_property_value.replace("margin-top:", "");
									raw_property_value = raw_property_value.replace("margin-bottom:", "");
									raw_property_value = raw_property_value.replace("margin-right:", "");
									raw_property_value = raw_property_value.replace("margin-left:", "");
									raw_property_value = raw_property_value.replace(";", "");
									
									String[] separated_values = raw_property_value.split(" ");
									for(String value : separated_values) {											
										margin_values.add(value);
									}
								}
							}
						}
					}
				}
				//or even print the entire style sheet (formatted)
			} catch (MalformedURLException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (CSSException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		}
		
		List<Observation> observations = new ArrayList<>();
		observations.add(new Observation("Margin values are not consistent in their differences. It's best to use margin values that have a common multiple") {
			
			@Override
			public String generateKey() {
				return org.apache.commons.codec.digest.DigestUtils.sha256Hex(this.getDescription());
			}
		});
		//calculate margin size score
		int margin_size_score = scoreMarginSizes(margin_values);
		
		//calculate score for question "Is margin used as margin?" NOTE: The expected calculation expects that margins are not used as padding
		int margin_as_padding_score = scoreMarginAsPadding(page_state.getElements());
		
		int score = (margin_size_score + margin_as_padding_score);
		log.warn("MARGIN SIZE SCORE  :::   "+margin_size_score);	
		log.warn("MARGIN AS PADDING SCORE  :::   "+margin_as_padding_score);	
		log.warn("MARGIN SCORE  :::   "+score);	

		return new Audit(AuditCategory.INFORMATION_ARCHITECTURE, AuditSubcategory.PADDING, score, observations, AuditLevel.PAGE, 2);
 
		
		
		
		/*
		
		//THE FOLLOWING WORKS TO GET RENDERED CSS VALUES FOR EACH ELEMENT THAT ACTUALLY HAS CSS
		Tidy tidy = new Tidy(); // obtain a new Tidy instance
		tidy.setXHTML(true); // set desired config options using tidy setters 
		                          // (equivalent to command line options)

		org.w3c.dom.Document w3c_document = tidy.parseDOM(new ByteArrayInputStream(doc.outerHtml().getBytes()), null);
		
		List<String> margin = new ArrayList<>();

		//count all elements with non 0 px values that aren't decimals
		MediaSpec media = new MediaSpecAll(); //use styles for all media
		StyleMap map = null;
		try {
			map = CSSFactory.assignDOM(w3c_document, "UTF-8", new URL(page_state.getUrl()), media, true);
			
			log.warn("css dom map ::   "+map.size());
			for(ElementState element : page_state.getElements()) {
	
				//create the style map
	
				XPath xPath = XPathFactory.newInstance().newXPath();
				try {
					Node node = (Node)xPath.compile(element.getXpath()).evaluate(w3c_document, XPathConstants.NODE);
					NodeData style = map.get((org.w3c.dom.Element)node); //get the style map for the element
					log.warn("element node ::   "+node);
					log.warn("Element styling  ::  "+style);
					if(style != null) {
						log.warn("Element styling  ::  "+style.getProperty("margin-top"));
					}
					
					//StyleSheet sheet = CSSFactory.parseString(raw_stylesheet, new URL(page_state.getUrl()));
				} catch (XPathExpressionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				String margin_top = element.getCssValues().get("margin-top");
				String margin_left = element.getCssValues().get("margin-left");
				String margin_right = element.getCssValues().get("margin-right");
				String margin_bottom = element.getCssValues().get("margin-bottom");
	
				margin.add(margin_top);
				margin.add(margin_bottom);			
				margin.add(margin_left);
				margin.add(margin_right);
			}
			margin.remove(null);
			double score = scoreMarginUsage(margin);
			return new Audit(AuditCategory.INFORMATION_ARCHITECTURE, buildBestPractices(), getAdaDescription(), getAuditDescription(), AuditSubcategory.PADDING, score, new ArrayList<>(), AuditLevel.PAGE);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return null;
		}
		*/
	}

	/**
	 * Identifies elements that are using margin when they should be using padding
	 * @param elements
	 * @return
	 */
	private int scoreMarginAsPadding(List<ElementState> elements) {
		for(ElementState element : elements) {
			//identify situations of margin collapse by finding elements that are 
				//positioned vertically where 1 is above the other and both have margins
				//element is empty and has margins set
				//first and last child margin collapse when their parent has margin set
			
			
		}
		
		return 0;
		
	}

	public static String URLReader(URL url) throws IOException {
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        
        log.warn("Content encoding for URL connection ::  " + con.getContentEncoding());
        if(con.getContentEncoding() != null && con.getContentEncoding().equalsIgnoreCase("gzip")) {
        	return readGzipStream(con.getInputStream());
        }
        else {
        	return readStream(con.getInputStream());
        }
	}
	
	private static String readGzipStream(InputStream inputStream) {
		 StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream( inputStream )));) {
            String nextLine = "";
            while ((nextLine = reader.readLine()) != null) {
                sb.append(nextLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
	}

	private static String readStream(InputStream in) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in));) {
            String nextLine = "";
            while ((nextLine = reader.readLine()) != null) {
                sb.append(nextLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
	
	/**
	 * Generate score for margin usage
	 * 
	 * @param margin_set
	 * @return
	 */
	private int scoreMarginSizes(List<String> margin_set) {
		int score = 0;
		int total_possible_score = 0;
		//sort margin values into em, percent and px measure types
		Map<String, List<Double>> converted_unit_buckets = sortSizeUnits(margin_set);
		//reduce lists in map to unique values;
		log.warn("converted bucket list  "+converted_unit_buckets);
		
		//SCORING 1 - Check if all values have a similar multiple
		for(String unit : converted_unit_buckets.keySet()) {
			List<Double> margin_values = converted_unit_buckets.get(unit);
		
			//sort margin values and make them unique
			margin_values = sortAndMakeDistinct(margin_values);
			
			Double smallest_value = margin_values.get(0);
			
			for(int idx = 1; idx < margin_values.size(); idx++) {
				if(margin_values.get(idx) % smallest_value == 0) {
					score += 3;
				}
				else {
					score += 1;
				}
				total_possible_score+= 3;
			}
			
		}
		//SCORING 1a - Check if all values have the same difference
		
		
		//CALCULATE OVERALL SCORE :: 
		log.warn("Margin score :: "+(score/total_possible_score));
		if(score == 0.0) {
			return score;
		}
		return score / total_possible_score;
	}

	private Map<String, List<String>> categorizeSizeUnits(Map<String, List<String>> converted_unit_buckets) {
		Map<String, List<String>> unit_categories = new HashMap<>();
		
		for(String unit : converted_unit_buckets.keySet()) {
			List<String> units = new ArrayList<String>();
			if("rem".equals(unit) || "em".equals(unit) || "%".equals(unit)) {
				if(unit_categories.containsKey("scalable-high")) {
					units = unit_categories.get("scalable-high");
				}
				units.add(unit);
				unit_categories.put("scalable-high", units);
			}
			else if("vh".equals(unit) || "vw".equals(unit) || "vmin".equals(unit) || "vmax".equals(unit)) {
				if(unit_categories.containsKey("scalable-low")) {
					units = unit_categories.get("scalable-low");
				}
				units.add(unit);
				unit_categories.put("scalable-low", units);
			}
			else if("px".equals(unit) || "ex".equals(unit) || "pt".equals(unit) || "cm".equals(unit) || "mm".equals(unit) || "in".equals(unit) || "pc".equals(unit)) {
				if(unit_categories.containsKey("constant")) {
					units = unit_categories.get("constant");
				}
				units.add(unit);
				unit_categories.put("constant", units);
			}
		}
		
		return unit_categories;
	}

	/**
	 * Sort units into buckets by mapping unit type to margin sizes
	 * 
	 * @param margin_set
	 * @return
	 */
	private Map<String, List<Double>> sortSizeUnits(List<String> margin_set) {
		Map<String, List<Double>> sorted_margins = new HashMap<>();
		//replace all px values with em for values that contain decimals
		
		for(String margin_value : margin_set) {
			if(margin_value == null 
					|| "0".equals(margin_value.trim()) 
					|| margin_value.contains("auto")) {
				continue;
			}
			
			for(String unit : size_units) {
				if(margin_value != null && margin_value.contains(unit)) {
					List<Double> values = new ArrayList<Double>();

					if(sorted_margins.containsKey(unit)) {
						values = sorted_margins.get(unit);
					}
					
					String value = cleanSizeUnits(margin_value);
					
					//values = cleanSizeUnits(values);
					//List<Double> converted_values = convertList(values, s -> Double.parseDouble(s));
					values.add(Double.parseDouble(value));
					sorted_margins.put(unit, values);
				}
			}
		}
		return sorted_margins;
	}
	
	public static List<Double> sortAndMakeDistinct(List<Double> from){
		return from.stream().filter(n -> n != 0.0).distinct().sorted().collect(Collectors.toList());
	}
	
	public static List<String> cleanSizeUnits(List<String> from){
		return from.stream()
				.map(line -> line.replaceAll("px", ""))
				.map(line -> line.replaceAll("%", ""))
				.map(line -> line.replaceAll("em", ""))
				.map(line -> line.replaceAll("rem", ""))
				.map(line -> line.replaceAll("pt", ""))
				.map(line -> line.replaceAll("ex", ""))
				.map(line -> line.replaceAll("vm", ""))
				.map(line -> line.replaceAll("vh", ""))
				.map(line -> line.replaceAll("cm", ""))
				.map(line -> line.replaceAll("mm", ""))
				.map(line -> line.replaceAll("in", ""))
				.map(line -> line.replaceAll("pc", ""))
				.map(line -> line.indexOf(".") > -1 ? line.substring(0, line.indexOf(".")) : line)
				.collect(Collectors.toList());
	}
	
	public static String cleanSizeUnits(String value){
		return value.replaceAll("px", "")
					.replaceAll("%", "")
					.replaceAll("em", "")
					.replaceAll("rem", "")
					.replaceAll("pt", "")
					.replaceAll("ex", "")
					.replaceAll("vm", "")
					.replaceAll("vh", "")
					.replaceAll("cm", "")
					.replaceAll("mm", "")
					.replaceAll("in", "")
					.replaceAll("pc", "");
	}
	
	public static List<Integer> removeZeroValues(List<Integer> from){
		return from.stream().filter(n -> n != 0).collect(Collectors.toList());
	}
	
	//for lists
	public static <T, U> List<U> convertList(List<T> from, Function<T, U> func) {
	    return from.stream().map(func).collect(Collectors.toList());
	}
	
	/* * Java method to find GCD of two number using Euclid's method * @return GDC of two numbers in Java */ 
	private static int findGCD(int number1, int number2) { 
		//base case 
		if(number2 == 0){ 
			return number1; 
		} 
		return findGCD(number2, number1%number2);
	}
}
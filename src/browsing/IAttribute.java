package browsing;

import com.tinkerpop.frames.Property;

public interface IAttribute {
	@Property("name")
	public String getName();
	
	@Property("name")
	public String setName();
	
	@Property("vals")
	public String[] getVals();
	
	@Property("vals")
	public String[] setVals();
}

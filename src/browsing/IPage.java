package browsing;

import java.net.URL;
import java.util.Iterator;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;

/**
 * Tinkerpop/frames interface implementation of {@link Page}
 * @author brandon
 *
 */
public interface IPage {
	@Property("landable")
	public boolean isLandable();
	
	@Property("landable")
	public void setLandable(boolean isLandable);
	
	@Property("screenshot")
	public URL getScreenshot();
	
	@Property("screenshot")
	public void setScreenshot(URL screenshotUrl);
	
	@Property("src")
	public String getSrc();

	@Property("src")
	public void setSrc(String src);
	
	@Property("url")
	public URL getUrl();
	
	@Property("url")
	public URL setUrl(URL url);
	
	@Adjacency(label="contains")
	public Iterator<PageElement> getElements();
	
	@Adjacency(label="contains")
	public void setElements();
}

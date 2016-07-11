package persistence;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

import browsing.Page;

/**
 * Tinkerpop/frames interface implementation of {@link Page}
 * @author brandon
 *
 */
@TypeValue("Page") public interface IPage extends IPathObject, IPersistable<IPage>{
	@Property("key")
	public String getKey();
	
	@Property("key")
	public void setKey(String key);
	
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
	public Iterator<IPageElement> getElements();
	
	@Adjacency(label="contains")
	public void setElements(List<IPageElement> elements);
}

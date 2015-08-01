package actors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import browsing.Page;


public class PageMonitor{
	ConcurrentHashMap<String, List<Page>> hostPageMap = null;
	
	/**
	 * 
	 */
	public PageMonitor(){
		hostPageMap = new ConcurrentHashMap<String, List<Page>>();
	}

	/**
	 * 
	 * @param page
	 * @return
	 */
	public Page findPage(String pageSrc, String host){		
		List<Page> pagesForHost = hostPageMap.get(host);
		
		if(pagesForHost != null){
			for(Page hostPage: pagesForHost){
				if(hostPage.getSrc().equals(pageSrc)){
					return hostPage;
				}
			}
		}
		else{
			hostPageMap.put(host, new ArrayList<Page>());
		}
		return null;		
	}
	
	/**
	 * Adds {@link Page} to hash of host pages
	 * 
	 * @param page
	 * @return true if page added successfully, otherwise false
	 */
	public boolean addPage(Page page){
		String host = page.getUrl().getHost();
		
		return hostPageMap.get(host).add(page);
	}
}

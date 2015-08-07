package actors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import structs.ConcurrentNode;
import browsing.Page;


public class NodeMonitor{
	ConcurrentHashMap<String, List<ConcurrentNode<?>>> hostNodeMap = null;
	
	/**
	 * 
	 */
	public NodeMonitor(){
		hostNodeMap = new ConcurrentHashMap<String, List<ConcurrentNode<?>>>();
	}

	/**
	 * 
	 * @param page
	 * @return
	 */
	public ConcurrentNode<?> findNode(ConcurrentNode<?> node, String host){
			//String pageSrc, String host){		
		List<ConcurrentNode<?>> nodesForHost = hostNodeMap.get(host);
		
		if(nodesForHost != null){
			for(ConcurrentNode<?> hostNode: nodesForHost){
				if(hostNode.getData().equals(node.getData())){
					return hostNode;
				}
			}
		}
		else{
			hostNodeMap.put(host, new ArrayList<ConcurrentNode<?>>());
		}
		return null;		
	}
	
	/**
	 * Adds {@link Page} to hash of host pages
	 * 
	 * @param page
	 * @return true if page added successfully, otherwise false
	 */
	public boolean addPage(ConcurrentNode<?> pageNode){
		assert(pageNode.getClass().equals(Page.class));
		Page page = ((Page)pageNode.getData());
		String host = page.getUrl().getHost();
		
		return hostNodeMap.get(host).add(pageNode);
	}
	
	/**
	 * Adds {@link Page} to hash of host pages
	 * 
	 * @param page
	 * @param host host name of website (eg. google.com) used as key
	 * @return true if page added successfully, otherwise false
	 */
	public boolean addNode(ConcurrentNode<?> node, String host){
		return hostNodeMap.get(host).add(node);
	}
}

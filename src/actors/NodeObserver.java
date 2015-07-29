package actors;

import java.util.Observable;
import java.util.Observer;

import observableStructs.ObservableQueue;
import browsing.ConcurrentNode;
import browsing.Page;


public class NodeObserver implements Observer{
	ObservableQueue<ConcurrentNode<Page>> nodeList = null;
	
	public NodeObserver(){
		nodeList = new ObservableQueue<ConcurrentNode<Page>>();
		nodeList.addObserver(this);
	}

	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub
		
	}
}

package com.qanairy.persistence.edges;

import com.qanairy.persistence.Account;
import com.qanairy.persistence.Domain;
import com.syncleus.ferma.AbstractEdgeFrame;
import com.syncleus.ferma.annotations.InVertex;
import com.syncleus.ferma.annotations.OutVertex;

public abstract class HasDomain extends AbstractEdgeFrame {
	@InVertex
	public abstract Domain getIn();

	@OutVertex
	public abstract Account getOut();
}

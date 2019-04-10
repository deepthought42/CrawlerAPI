package com.minion.browsing.table;

import java.util.List;
import com.minion.browsing.ElementNode;
import com.qanairy.models.ElementState;

/**
 *	Contains the {@link ElementNode}s that make up a row in a [@link Table} within a {@link Page}. 
 */
public class Row {
	private List<ElementNode<ElementState>> row_cells;
	
	public Row(){}
	
	public Row(List<ElementNode<ElementState>> table_row){
		this.setRowCells(table_row);
	}

	public List<ElementNode<ElementState>> getRowCells() {
		return row_cells;
	}

	public void setRowCells(List<ElementNode<ElementState>> row_cells) {
		this.row_cells = row_cells;
	}
}

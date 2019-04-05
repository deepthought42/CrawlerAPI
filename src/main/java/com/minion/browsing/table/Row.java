package com.minion.browsing.table;

import java.util.List;
import com.minion.browsing.PageElementNode;
import com.qanairy.models.PageElementState;

/**
 *	Contains the {@link PageElementNode}s that make up a row in a [@link Table} within a {@link Page}. 
 */
public class Row {
	private List<PageElementNode<PageElementState>> row_cells;
	
	public Row(){}
	
	public Row(List<PageElementNode<PageElementState>> table_row){
		this.setRowCells(table_row);
	}

	public List<PageElementNode<PageElementState>> getRowCells() {
		return row_cells;
	}

	public void setRowCells(List<PageElementNode<PageElementState>> row_cells) {
		this.row_cells = row_cells;
	}
}

package com.minion.browsing.Table;

import java.util.List;

import com.minion.browsing.PageElementNode;
import com.qanairy.models.PageElement;

/**
 *	Contains the {@link PageElementNode}s that make up a row in a [@link Table} within a {@link Page}. 
 */
public class Row {
	private List<PageElementNode<PageElement>> row_cells;
	
	public Row(){}
	
	public Row(List<PageElementNode<PageElement>> table_row){
		this.setRowCells(table_row);
	}

	public List<PageElementNode<PageElement>> getRowCells() {
		return row_cells;
	}

	public void setRowCells(List<PageElementNode<PageElement>> row_cells) {
		this.row_cells = row_cells;
	}
}

package com.qanairy.models.experience;

import org.neo4j.ogm.annotation.NodeEntity;

import com.qanairy.models.Persistable;
import com.qanairy.models.enums.ItemType;

/**
 * 
 * 
 */
@NodeEntity
public class TableHeader implements Persistable{
	
	private String label; // also referred to as text in some
	private String key; // also referred to as name
	private String item_type; // also referred to as valueType
	private String display_unit; 
	
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public ItemType getItemType() {
		return ItemType.create(item_type);
	}
	public void setItemType( ItemType item_type) {
		this.item_type = item_type.getShortName();
	}
	public Double getGranularity() {
		return granularity;
	}
	public void setGranularity(Double granularity) {
		this.granularity = granularity;
	}
	public String getDisplayUnit() {
		return display_unit;
	}
	public void setDisplayUnit(String display_unit) {
		this.display_unit = display_unit;
	}
	private Double granularity;

	@Override
	public String generateKey() {
		return key;
	}
	
}

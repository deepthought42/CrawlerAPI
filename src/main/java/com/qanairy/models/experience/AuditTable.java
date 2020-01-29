package com.qanairy.models.experience;

import java.util.List;

/**
 * Contains details information for Google PageSpeed API Performance audits
 */
public class AuditTable extends AuditDetail {
	List<TableHeader> header;
	List<TableRow> items;
}

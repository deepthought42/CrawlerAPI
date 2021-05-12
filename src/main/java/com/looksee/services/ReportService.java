package com.looksee.services;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.looksee.models.audit.UXIssueMessage;

public class ReportService {

	public static XSSFWorkbook generateExcelSpreadsheet( 
			List<UXIssueMessage> audit_messages, 
			URL url
	) throws FileNotFoundException, IOException {
		assert audit_messages != null;
		assert url != null;
		
		XSSFWorkbook workbook = new XSSFWorkbook();

		String workbook_name = url.getHost();
        Sheet sheet = workbook.createSheet(workbook_name);
        int rowCount = 0;
        
        //write header
        XSSFCellStyle header_cell_style = workbook.createCellStyle();
        header_cell_style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        header_cell_style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row header_row = sheet.createRow(rowCount);
        header_row.setRowStyle(header_cell_style);
        //write object to cells in order of category, issue, why it matters, recommendations, ada compliance, and priority
        Cell category_header_cell = header_row.createCell(0);
        category_header_cell.setCellValue("Category");
        
        Cell description_header_cell = header_row.createCell(1);
        description_header_cell.setCellValue("Description");
        
        Cell why_it_matters_header_cell = header_row.createCell(2);
        why_it_matters_header_cell.setCellValue("Why it matters");
        
        Cell recommendation_header_cell = header_row.createCell(3);
        recommendation_header_cell.setCellValue("Recommendation");
        
        Cell wcag_header_cell = header_row.createCell(4);
        wcag_header_cell.setCellValue("WCAG/ADA compliance");
        
        Cell priority_header_cell = header_row.createCell(5);
        priority_header_cell.setCellValue("Priority");
        
        sheet.setColumnWidth(0, 15 * 256);
        sheet.setColumnWidth(1, 25 * 256);
        sheet.setColumnWidth(2, 35 * 256);
        sheet.setColumnWidth(3, 50 * 256);
        sheet.setColumnWidth(4, 40 * 256);

        
        
        XSSFCellStyle wrapped_text_cell_style = workbook.createCellStyle();
        wrapped_text_cell_style.setWrapText(true);

        for(UXIssueMessage msg : audit_messages) {
            Row row = sheet.createRow(++rowCount);
            row.setRowStyle(wrapped_text_cell_style);
            row.setHeight((short)2000);
        
            //write object to cells in order of category, issue, why it matters, recommendations, ada compliance, and priority
            Cell category_cell = row.createCell(0);
            category_cell.setCellValue(msg.getCategory().toString());
            
            Cell description_cell = row.createCell(1);
            description_cell.setCellValue(msg.getDescription());
            
            Cell why_it_matters_cell = row.createCell(2);
            why_it_matters_cell.setCellValue("why it matters text goes here");
            why_it_matters_cell.setCellStyle(wrapped_text_cell_style);
            
            Cell recommendation_cell = row.createCell(3);
            recommendation_cell.setCellValue(msg.getRecommendation());
            
            Cell wcag_cell = row.createCell(4);
            wcag_cell.setCellValue(msg.getWcagCompliance());
            
            Cell priority_cell = row.createCell(5);
            priority_cell.setCellValue(msg.getPriority().toString());
        
        }
        
        return workbook;
	}
}

package com.minion.api;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import com.qanairy.config.WebSecurityConfig;
import com.qanairy.models.Action;

/**
 *	API for interacting with {@link User} data
 */
@RestController
@RequestMapping("/reports")
public class ReportController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
    @Autowired
    protected WebSecurityConfig appConfig;
    
    /**
     * Retrieves {@link Action account} with a given key
     * 
     * @param key account key
     * @return {@link Action account}
     * @throws IOException 
     */
    @RequestMapping(method = RequestMethod.GET)
    public XSSFWorkbook getExcelReport() throws IOException {
    	
    	XSSFWorkbook workbook = new XSSFWorkbook();
        //CellStyle cellStyle = workbook.createCellStyle();

        XSSFSheet sheet = workbook.createSheet("Java Books");
        Object[][] bookData = {
                {"Head First Java", "Kathy Serria", 79},
                {"Effective Java", "Joshua Bloch", 36},
                {"Clean Code", "Robert martin", 42},
                {"Thinking in Java", "Bruce Eckel", 35},
        };
 
        int rowCount = 0;
        
        for (Object[] aBook : bookData) {
            Row row = sheet.createRow(++rowCount);
             
            int columnCount = 0;
             
            for (Object field : aBook) {
                Cell cell = row.createCell(++columnCount);
                if (field instanceof String) {
                    cell.setCellValue((String) field);
                } else if (field instanceof Integer) {
                    cell.setCellValue((Integer) field);
                }
            }
             
        }
         
         
        try (FileOutputStream outputStream = new FileOutputStream("JavaBooks.xlsx")) {
            workbook.write(outputStream);
        }
        
        return workbook;
        
    }
}
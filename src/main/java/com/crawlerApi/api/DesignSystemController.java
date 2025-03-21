package com.crawlerApi.api;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.crawlerApi.browsing.Crawler;
import com.crawlerApi.models.designsystem.DesignSystem;
import com.crawlerApi.services.AuditRecordService;
import com.crawlerApi.services.AuditService;
import com.crawlerApi.services.DesignSystemService;

/**
 *	API for interacting with {@link User} data
 */
@RestController
@RequestMapping("/designsystem")
public class DesignSystemController {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    @Autowired
    protected AuditRecordService audit_record_service;
    
    @Autowired
    protected Crawler crawler;
    
    @Autowired
    protected AuditService audit_service;
	
	@Autowired
	private DesignSystemService design_system_service;
	
	/**
     * 
     * @param request
     * @param page
     * @return
     * @throws Exception
     */
	@RequestMapping(path="/{id}/color", method = RequestMethod.POST)
	public @ResponseBody void updateColors(
			HttpServletRequest request,
			@PathVariable(value="id", required=true) long id,
			@RequestBody(required=true) List<String> colors
	) throws Exception {		
		Optional<DesignSystem> design_system_opt = design_system_service.findById(id);
		if(design_system_opt.isPresent()){
			DesignSystem design_system = design_system_opt.get();
			design_system.setColorPalette(colors);
			design_system_service.save(design_system);
		}
	}
}
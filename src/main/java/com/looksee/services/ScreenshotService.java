package com.looksee.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.looksee.models.Screenshot;
import com.looksee.models.repository.ScreenshotRepository;

@Service
public class ScreenshotService {

	@Autowired
	private ScreenshotRepository screenshot_repo;
	
	public Screenshot save(Screenshot screenshot){
		Screenshot screenshot_record = findByKey(screenshot.getKey());
		if(screenshot_record == null){
			screenshot_record = screenshot_repo.save(screenshot);
		}
		return screenshot_record;
	}
	
	public Screenshot findByKey(String key){
		return screenshot_repo.findByKey(key);
	}
}

package com.qanairy.models.dao.impl;

import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qanairy.models.dao.ScreenshotSetDao;
import com.qanairy.persistence.OrientConnectionFactory;
import com.qanairy.persistence.ScreenshotSet;

public class ScreenshotSetDaoImpl implements ScreenshotSetDao {
	private static Logger log = LoggerFactory.getLogger(ScreenshotSetDaoImpl.class);
	
	public String generateKey(ScreenshotSet obj) {
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(obj.getFullScreenshot())+":"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(obj.getViewportScreenshot());
	}
	
	@Override
	public ScreenshotSet save(ScreenshotSet screenshot) {
		assert(screenshot != null);
		
		screenshot.setKey(generateKey(screenshot));		
		ScreenshotSet screenshot_record = find(screenshot.getKey());
		
		OrientConnectionFactory connection = new OrientConnectionFactory();
		if(screenshot_record == null){
			screenshot_record = connection.getTransaction().addFramedVertex(ScreenshotSet.class);
			screenshot_record.setKey(screenshot.getKey());
			screenshot_record.setBrowser(screenshot.getBrowser());
			screenshot_record.setFullScreenshot(screenshot.getFullScreenshot());
			screenshot_record.setViewportScreenshot(screenshot.getViewportScreenshot());
		}

		return screenshot_record;
	}

	@Override
	public ScreenshotSet find(String key) {
		ScreenshotSet screenshot = null;
		OrientConnectionFactory connection = new OrientConnectionFactory();

		try{
			screenshot = connection.getTransaction().getFramedVertices("key", key, ScreenshotSet.class).next();
		}catch(NoSuchElementException e){
			log.error("Error requesting screenshot set record from database");
		}
		connection.close();
		return screenshot;
	}

}

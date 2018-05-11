package com.qanairy.models.dto;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import com.qanairy.models.ScreenshotSet;
import com.qanairy.persistence.DataAccessObject;
import com.qanairy.persistence.IPersistable;
import com.qanairy.persistence.IScreenshotSet;
import com.qanairy.persistence.OrientConnectionFactory;

/**
 * 
 */
public class ScreenshotSetRepository implements IPersistable<ScreenshotSet, IScreenshotSet> {

	@Override
	public String generateKey(ScreenshotSet obj) {
		return org.apache.commons.codec.digest.DigestUtils.sha256Hex(obj.getFullScreenshot())+":"+org.apache.commons.codec.digest.DigestUtils.sha256Hex(obj.getViewportScreenshot());
	}

	@Override
	public IScreenshotSet save(OrientConnectionFactory connection, ScreenshotSet screenshot_set) {
		assert(screenshot_set != null);
		
		if(screenshot_set.getKey() == null || screenshot_set.getKey().isEmpty()){
			screenshot_set.setKey(generateKey(screenshot_set));
		}
		
		@SuppressWarnings("unchecked")
		Iterator<IScreenshotSet> screenshot_set_iter = ((Iterable<IScreenshotSet>) DataAccessObject.findByKey(screenshot_set.getKey(), connection, IScreenshotSet.class)).iterator();
		
		IScreenshotSet screenshot_set_record = null;
		
		if(screenshot_set_iter.hasNext()){
			screenshot_set_record = screenshot_set_iter.next();
		}
		else{
			screenshot_set_record = connection.getTransaction().addVertex("class:"+IScreenshotSet.class.getSimpleName()+","+UUID.randomUUID(), IScreenshotSet.class);
			screenshot_set_record.setKey(screenshot_set.getKey());
			screenshot_set_record.setBrowser(screenshot_set.getBrowserName());
			screenshot_set_record.setFullScreenshot(screenshot_set.getFullScreenshot());
			screenshot_set_record.setViewportScreenshot(screenshot_set.getViewportScreenshot());
		}

		return screenshot_set_record;
	}

	@Override
	public ScreenshotSet load(IScreenshotSet screenshotSet) {
		return new ScreenshotSet(screenshotSet.getFullScreenshot(), screenshotSet.getViewportScreenshot(), screenshotSet.getBrowser());
	}


	@Override
	public ScreenshotSet find(OrientConnectionFactory connection, String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ScreenshotSet> findAll(OrientConnectionFactory connection) {
		// TODO Auto-generated method stub
		return null;
	}

}

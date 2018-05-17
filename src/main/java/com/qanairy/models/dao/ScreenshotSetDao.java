package com.qanairy.models.dao;

import com.qanairy.persistence.ScreenshotSet;

/**
 * 
 */
public interface ScreenshotSetDao {
	public ScreenshotSet save(ScreenshotSet screenshot);
	public ScreenshotSet find(String key);
}

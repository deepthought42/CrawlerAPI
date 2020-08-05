package com.qanairy.services;

import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.springframework.stereotype.Service;

import com.minion.aws.UploadObjectSingleOperation;
import com.qanairy.models.enums.BrowserType;

@Service
public class ScreenshotUploadService {

	public static Future<String> uploadPageStateScreenshot(BufferedImage image, String host, String checksum, BrowserType browser, String user_id) {
		return CompletableFuture.supplyAsync(() -> UploadObjectSingleOperation.saveImageToS3ForUser(image, host, checksum, browser, user_id));
	}
}
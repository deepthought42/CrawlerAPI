package com.qanairy.services;

import java.awt.image.BufferedImage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.springframework.stereotype.Service;

import com.minion.aws.UploadObjectSingleOperation;

@Service
public class ScreenshotUploadService {

	public static Future<String> uploadPageStateScreenshot(BufferedImage image, String host, String checksum) {
		return CompletableFuture.supplyAsync(() -> UploadObjectSingleOperation.saveImageToS3(image, host, checksum));
	}
}
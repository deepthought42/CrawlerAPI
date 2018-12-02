package utils;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import com.qanairy.utils.ImageUtils;

public class ImageUtilsTests {
	
	@Test
	public void getImageChecksumFromUrlMatchesExpected() throws IOException{
		URL url = new File("C:\\Users\\brand\\workspace\\WebTestVisualizer\\src\\test\\resources\\sample_screenshot.png").toURI().toURL();
		String checksum = ImageUtils.getFileChecksum(url);
		assertEquals("B8FF1D86B6A9AA33117F9682D0481E5A0AF96264A110EE1F9D33F3FF710F434B", checksum);
	}
}

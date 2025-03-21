package com.crawlerApi.utils;

public class TimingUtils {
	public static void pauseThread(long time){
		try{
			Thread.sleep(time);
		}catch(Exception e){}
	}
}

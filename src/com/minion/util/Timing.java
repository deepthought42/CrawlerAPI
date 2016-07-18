package com.minion.util;

public class Timing {
	public static void pauseThread(long time){
		try{
			Thread.sleep(time);
		}catch(Exception e){}
	}
}

package util;

public class Timing {
	public static void pauseThread(long time){
		try{
			Thread.sleep(time*1000);
		}catch(Exception e){}
	}
}

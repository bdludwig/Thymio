package thymio;

public class ThymioStopThread extends Thread {
	private long stopAfterMsecs;
	private Thymio myThymio;
	
	public ThymioStopThread(Thymio t, long millis) {
		stopAfterMsecs = millis;
		myThymio = t;
	}
	
	public void run() {
		try {
			Thread.sleep(stopAfterMsecs);
			myThymio.setSpeed((short)0, (short)0);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
}

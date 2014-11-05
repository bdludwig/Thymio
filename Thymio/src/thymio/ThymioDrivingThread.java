package thymio;

public class ThymioDrivingThread extends Thread {
	public static final int UPDATE_INTERVAL = 100;
	private Thymio myThymio;
	
	public ThymioDrivingThread(Thymio t) {
		myThymio = t;
	}
	
	public void run() {
		while (true) {
			try {
				Thread.sleep(UPDATE_INTERVAL);
				myThymio.updatePose(System.currentTimeMillis());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

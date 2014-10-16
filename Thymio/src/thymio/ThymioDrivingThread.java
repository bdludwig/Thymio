package thymio;

public class ThymioDrivingThread extends Thread {
	private Thymio myThymio;
	
	public ThymioDrivingThread(Thymio t) {
		myThymio = t;
	}
	
	public void run() {
		while (true) {
			try {
				Thread.sleep(200);
				myThymio.updatePose(System.currentTimeMillis());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}

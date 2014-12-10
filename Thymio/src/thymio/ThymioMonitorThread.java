package thymio;

public class ThymioMonitorThread extends Thread {
	private Thymio myThymio;
	
	public ThymioMonitorThread(Thymio t) {
		myThymio = t;
	}

	public void run() {
		try {
			while (true) {
				if (myThymio.isPaused()) {
					synchronized (myThymio) {
						while (myThymio.isPaused()) {
							System.out.println("wait for driving operation to complete.");
							myThymio.wait();
						}					
					}
				}
				/*
				else if (myThymio.getTimerThread() != null) {
					synchronized (myThymio.getTimerThread()) {
						myThymio.getTimerThread().join();
					}
				}*/
				
				myThymio.updatePose(System.currentTimeMillis());
				Thread.sleep(Thymio.UPDATE_INTERVAL);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
}

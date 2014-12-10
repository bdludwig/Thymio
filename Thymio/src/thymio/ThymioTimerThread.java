package thymio;

public class ThymioTimerThread extends Thread {
	private Thymio myThymio;
	private long delay;
	
	public ThymioTimerThread(long dt, Thymio t) {
		delay = dt;
		myThymio = t;
	}

	public void run() {
		try {
			Thread.sleep(delay);

			synchronized (myThymio) {
				myThymio.setDriving(false);
				myThymio.setStopped();
				myThymio.setSpeed((short)0, (short)0, true);
				myThymio.notifyAll();
			}

			System.out.println("timer thread terminated.");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
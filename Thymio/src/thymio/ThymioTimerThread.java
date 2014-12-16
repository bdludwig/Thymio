package thymio;

public class ThymioTimerThread extends Thread {
	private Thymio myThymio;
	private long delay;
	private int state;
	
	public ThymioTimerThread(long dt, Thymio t, int s) {
		delay = dt;
		myThymio = t;
		state = s;
	}

	public void run() {
		try {
			Thread.sleep(delay);
			
			myThymio.setDriving(false);
			System.out.println("timer thread stops Thymio.");
			myThymio.setSpeed((short)0, (short)0, true);
			myThymio.setStopped();

			synchronized (myThymio) {
				myThymio.setTimerThread(null);
				myThymio.notifyAll();
			}

			System.out.println("timer thread terminated.");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

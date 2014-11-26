package thymio;

public class ThymioTimerThread extends Thread {
	private Thymio myThymio;
	private long delay;
	private short vleft, vright;
	
	public ThymioTimerThread(Thymio t, long d, short vleft, short vright) {
		myThymio = t;
		delay = d;
		this.vleft = vleft;
		this.vright = vright;
	}

	public void run() {
		try {
			long t;
			
			synchronized (myThymio) {
				myThymio.setDriving(true);
				myThymio.setSpeed(vleft, vright, true);
			}

			myThymio.updatePose(System.currentTimeMillis());
			Thread.sleep(delay);
			myThymio.updatePose(System.currentTimeMillis());

			myThymio.setDriving(false);
			myThymio.setSpeed((short)0, (short)0, true);

			myThymio.setStopped();

			synchronized (myThymio) {
				myThymio.notifyAll();				
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
}

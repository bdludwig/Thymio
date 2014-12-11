package thymio;

import java.util.ArrayList;

import observer.MapPanel;
import context.MapElement;

public class ThymioNavigatingThread extends Thread {
	public final static int WHITE = 0;
	public final static int BLACK = 1;

	private ArrayList<MapElement> controls;
	private Thymio myThymio;
	private MapPanel myPanel;
	
	public ThymioNavigatingThread(Thymio t, MapPanel p, ArrayList<MapElement> c) {
		controls = c;
		myThymio = t;
		myPanel = p;
	}

	private double [] attractiveForcesOfPath(int pos) {
		int i = pos;
		MapElement m1 = null, m2 = null, m3;
		double [] res = new double[2];
		
		res[0] = res[1] = 0;
		
		while (i < controls.size() - 2) {
			m1 = controls.get(i);
			m2 = controls.get(i+1);
			m3 = controls.get(i+2);

			if ((m2.getPosX() - m1.getPosX()) != (m3.getPosX() - m2.getPosX()) && (m2.getPosY() - m1.getPosY()) != (m3.getPosY() - m2.getPosY())) break;
			
			i++;
		}
		
		m1 = controls.get(pos);

		if (m2 == null) {
			m2 = controls.get(pos + 1);
		}
		
		res[0] = (m2.getPosX() - m1.getPosX())*MapPanel.LENGTH_EDGE_CM*0.33;
		res[1] = (m2.getPosY() - m1.getPosY())*MapPanel.LENGTH_EDGE_CM*0.33;
		
		return res;
	}
	
	public void run() {
		MapElement p;
		MapElement s;

		double dX, dY;
		double [] attr;
		int i = 0;

		while (i < controls.size() - 1) {
			p = controls.get(i);
			s = controls.get(i+1);

			System.out.println("starting at: " + p + " to: " + s);
			dX = s.getPosX() - p.getPosX();
			dY = s.getPosY() - p.getPosY();
			s.setGoal(true);
			
			attr = attractiveForcesOfPath(i);
			
			if (dX + dY > 0) {
				int color;
				
				//myThymio.drive(16.5);
				if (s.getColor().getRed() == 255 && s.getColor().getGreen() == 255 && s.getColor().getBlue() == 255)
					color = WHITE;
				else 
					color = BLACK;

				driveAheadUntil(color, color, attr, p, s);
				System.out.println("ahead complete. Go to next field.");
				
				s.setGoal(false);
				i++;
			}
			else if (dX + dY < 0) {
				int color;

				if (s.getColor().getRed() == 255 && s.getColor().getGreen() == 255 && s.getColor().getBlue() == 255)
					color = WHITE;
				else 
					color = BLACK;
				
				//myThymio.drive(-16.5);
				driveBackUntil(color, color, attr, p, s);
				System.out.println("backwards complete. Go to next field.");

				s.setGoal(false);
				i++;
			}			
		}
		
		for (i = 0; i < controls.size(); i++) controls.get(i).setOnPath(false);
	}
	
	private void driveUntil(int expectedColorLeft, int expectedColorRight, int direction, double [] attr, MapElement p, MapElement s) {
		MapElement c = myPanel.getCurrentPos();
		double [] repVector, corrVector = new double[2];
		double length;
		double rotationUpdate;
		boolean poseUpdated;

		System.out.println("NEXT COLORS: " + expectedColorLeft + "," + expectedColorRight + " " + attr[0] + "/" + attr[1]);

		length = Math.sqrt(attr[0]*attr[0]+attr[1]*attr[1]);

		rotationUpdate = Math.atan2(attr[1]/length, attr[0]/length);

		if (Math.abs(rotationUpdate-myPanel.getOrientation()) >= Math.PI/180*5) {
			System.out.println("turn into right direction ...");
			try {
				Thread.sleep(15*Thymio.UPDATE_INTERVAL);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (!myThymio.isPaused()) myThymio.rotate(rotationUpdate);
		}
	
		do {
			if (myThymio.isUpdating()) {
				synchronized (myThymio.getMonitorThread()) {
					while (myThymio.isUpdating())
						try {
							myThymio.getMonitorThread().wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}				
				}
			}

			if (myThymio.isRotating()) {
				synchronized (myThymio.getTimerThread()) {
					try {
						myThymio.getTimerThread().wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
			if (myThymio.isPaused()) {
				synchronized (myThymio) {
					try {
						myThymio.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
			poseUpdated = false;
			
			if (c.getID() == s.getID()) System.out.println("GOAL REACHED ...");
			else if (!myThymio.isPaused() && myThymio.isStopped()) {
				if (poseUpdated = myThymio.poseUpdated()) {
					System.out.println("pose updated. readjust navigation ...");
					repVector = myThymio.computeRepulsiveForces();

					corrVector[0] = attr[0] + repVector[0];
					corrVector[1] = attr[1] + repVector[1];

					length = Math.sqrt(corrVector[0]*corrVector[0]+corrVector[1]*corrVector[1]);

					rotationUpdate = Math.atan2(corrVector[1]/length, corrVector[0]/length);

					if (Math.abs(rotationUpdate-myPanel.getOrientation()) >= Math.PI/180*2) {
						System.out.println("FORCED ORIENTATION : " + Math.atan2(corrVector[1]/length, corrVector[0]/length));
						System.out.println("FORCE: " + corrVector[0] + "," + corrVector[1]);
						System.out.println("CURRENT ORIENTATION: " + myPanel.getOrientation());

						if (!myThymio.isPaused()) myThymio.rotate(rotationUpdate);
					}
					else if (!myThymio.isPaused()) myThymio.drive(direction == 1);
				}
				else if (!myThymio.isPaused()) myThymio.drive(direction == 1);
			}

			if (!myThymio.isStopped()) {
				repVector = myThymio.computeRepulsiveForces();
				if ((repVector[0] != 0) || (repVector[1] != 0)) {
					double angle;
					corrVector[0] = repVector[0];
					corrVector[1] = repVector[1];

					length = Math.sqrt(corrVector[0]*corrVector[0]+corrVector[1]*corrVector[1]);
					rotationUpdate = Math.atan2(corrVector[1]/length, corrVector[0]/length);
					angle = myPanel.getOrientation() - Math.PI/36*Math.signum(rotationUpdate + myPanel.getOrientation());
					
					if (Math.abs(angle) >= Math.PI/180*2) {
						System.out.println("AVOIDING obstacle: " + rotationUpdate + " at sensor: " + myPanel.getMinSensorId());
						System.out.println("FORCE: " + corrVector[0]/length + "," + corrVector[1]/length);

						myThymio.setSpeed((short)0, (short)0, true);
						myThymio.setStopped();
						myThymio.setDriving(false);
						
						if (!myThymio.isPaused()) myThymio.rotate(angle);
					}
				}
			}
			else if (myThymio.isRotating()) System.out.println("STILL ROTATIING TO CORRECT POSITION");
			else if (!myThymio.isPaused() && !myThymio.isRotating() && !poseUpdated) {
				System.out.println("GO AHEAD, NO OBSTACLE: " + myPanel.getOrientation());
				myThymio.drive(direction == 1);
			}

			c = myPanel.getCurrentPos();
		}
		while (c.getID() != s.getID());
	}

	public void driveAheadUntil(int expectedColorLeft, int expectedColorRight, double [] attr, MapElement p, MapElement s) {
		driveUntil(expectedColorLeft, expectedColorRight, 1, attr, p, s);
	}

	public void driveBackUntil(int expectedColorLeft, int expectedColorRight, double [] attr, MapElement p, MapElement s) {
		driveUntil(expectedColorLeft, expectedColorRight, -1, attr, p, s);	
	}
}

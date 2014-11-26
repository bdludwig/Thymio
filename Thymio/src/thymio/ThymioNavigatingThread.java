package thymio;

import java.awt.Color;
import java.util.ArrayList;

import observer.MapPanel;
import observer.ThymioInterface;

import context.MapElement;

import main.Pathfinder;

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

	/*
	public void run() {
		synchronized (myThymio) {
			while (myThymio.isUpdating() || myThymio.isPaused() || myThymio.isDriving()) {
				System.out.println("wait for pose update to complete.");
				try {
					myThymio.wait();
				} catch (InterruptedException e) {
					continue;
				}
			}
			
			myThymio.rotate(Math.PI/2);
		}
	}
	
	*/

	public void run() {
		MapElement p;
		MapElement s;

		double dX, dY;
		double intendedTheta;
		double currentTheta;
		int i = 0;
		
		while (i < controls.size() - 1) {
			p = controls.get(i);
			s = controls.get(i+1);

			System.out.println("starting at: " + p + " to: " + s);
			dX = s.getPosX() - p.getPosX();
			dY = s.getPosY() - p.getPosY();
			s.setGoal(true);
			intendedTheta = Math.atan2(dY,dX);
			currentTheta = myPanel.getOrientation();
			
			if (dX + dY > 0) {
				int color;
				
				//myThymio.drive(16.5);
				System.out.println("ahead: " + currentTheta + "/" + intendedTheta);
				if (s.getColor().getRed() == 255 && s.getColor().getGreen() == 255 && s.getColor().getBlue() == 255)
					color = WHITE;
				else 
					color = BLACK;

				driveAheadUntil(color, color, intendedTheta, p, s);
				System.out.println("ahead complete. Go to next field.");
				
				s.setGoal(false);
				i++;
			}
			else if (dX + dY < 0) {
				int color;
				System.out.println("back: " + currentTheta + "/" + intendedTheta);

				if (s.getColor().getRed() == 255 && s.getColor().getGreen() == 255 && s.getColor().getBlue() == 255)
					color = WHITE;
				else 
					color = BLACK;
				
				//myThymio.drive(-16.5);
				driveBackUntil(color, color, intendedTheta, p, s);
				System.out.println("backwards complete. Go to next field.");

				s.setGoal(false);
				i++;
			}			
		}
		
		for (i = 0; i < controls.size(); i++) controls.get(i).setOnPath(false);
	}
	
	private void driveUntil(int expectedColorLeft, int expectedColorRight, int direction, double intendedTheta, MapElement p, MapElement s) {
		MapElement c = myPanel.getCurrentPos();
		double [] attrVector, repVector, corrVector = new double[2];
		double length;
		double rotationUpdate;

		System.out.println("NEXT COLORS: " + expectedColorLeft + "," + expectedColorRight);

		do {
			synchronized (myThymio) {
				while (myThymio.isUpdating() || myThymio.isPaused() || myThymio.isDriving()) {
					try {
						myThymio.wait();
						System.out.println("pose update complete: " + myPanel.getOrientation());
					} catch (InterruptedException e) {
						continue;
					}					
				}
			}

			if (!myThymio.isPaused() && myThymio.isStopped()) {
				if (myThymio.poseUpdated()) {
					System.out.println("pose updated. readjust navigation ...");
					synchronized (myThymio) {
						attrVector = myThymio.computeAttractiveForce(s);
						repVector = myThymio.computeRepulsiveForces();

						corrVector[0] = attrVector[0] + repVector[0];
						corrVector[1] = attrVector[1] + repVector[1];

						length = Math.sqrt(corrVector[0]*corrVector[0]+corrVector[1]*corrVector[1]);

						rotationUpdate = Math.atan2(corrVector[1]/length, corrVector[0]/length);
					}

					if (Math.abs(rotationUpdate-myPanel.getOrientation()) >= Math.PI/90) {
						System.out.println("FORCE: " + corrVector[0] + "," + corrVector[1]);
						System.out.println("FORCED ORIENTATION : " + Math.atan2(corrVector[1]/length, corrVector[0]/length));
						System.out.println("CURRENT ORIENTATION: " + myPanel.getOrientation());

						if (!myThymio.isPaused()) myThymio.rotate(rotationUpdate);
					}
					else myThymio.drive(direction == 1);
				}
				else myThymio.drive(direction == 1);
			}

			if (!myThymio.isDriving()) {
				repVector = myThymio.computeRepulsiveForces();
				if ((repVector[0] != 0) || (repVector[1] != 0)) {
					double angle;
					attrVector = myThymio.computeAttractiveForce(s);
					corrVector[0] = attrVector[0] + repVector[0];
					corrVector[1] = attrVector[1] + repVector[1];

					length = Math.sqrt(corrVector[0]*corrVector[0]+corrVector[1]*corrVector[1]);
					rotationUpdate = Math.atan2(corrVector[1]/length, corrVector[0]/length);
					angle = myPanel.getOrientation() - Math.PI/36*Math.signum(rotationUpdate);
					
					if (Math.abs(angle) >= Math.PI/90) {
						System.out.println("AVOIDING obstacle: " + rotationUpdate + " at sensor: " + myPanel.getMinSensorId());
						System.out.println("FORCE: " + corrVector[0]/length + "," + corrVector[1]/length);

						myThymio.setSpeed((short)0, (short)0, false);
						myThymio.setStopped();
						if (!myThymio.isPaused()) myThymio.rotate(angle);
					}
				}
			}
			else if (!myThymio.isPaused() && myThymio.isStopped()) myThymio.drive(direction == 1);


			c = myPanel.getCurrentPos();
		}
		while (c.getID() != s.getID());
	}

	public void driveAheadUntil(int expectedColorLeft, int expectedColorRight, double intendedTheta, MapElement p, MapElement s) {
		driveUntil(expectedColorLeft, expectedColorRight, 1, intendedTheta, p, s);
	}

	public void driveBackUntil(int expectedColorLeft, int expectedColorRight, double intendedTheta, MapElement p, MapElement s) {
		driveUntil(expectedColorLeft, expectedColorRight, -1, -intendedTheta, p, s);	
	}
}

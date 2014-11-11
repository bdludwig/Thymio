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

	private void computeAttractiveForce(MapElement g) {
		double diffVector [] = myPanel.getDistVectorTo(g, myPanel.getEstimPosX(), myPanel.getEstimPosY());
		
		System.out.println("attraction: " + diffVector[0] + "," + diffVector[1]);
	}
	
	public void run() {
		MapElement p;
		MapElement s;

		double dX, dY;
		double currentTheta, intendedTheta;
		int i = 0;
		
		while (i < controls.size() - 1) {
			p = controls.get(i);
			s = controls.get(i+1);

			System.out.println("starting at: " + p + " to: " + s);
			dX = s.getPosX() - p.getPosX();
			dY = s.getPosY() - p.getPosY();
			s.setGoal(true);
			
			currentTheta = myPanel.getOrientation();
			intendedTheta = Math.atan2(dY,dX);
			
			if (Math.abs(currentTheta-intendedTheta) > 22.5*Math.PI/180) {
				System.out.println("CHANGE DIRECTION: " + currentTheta + "/" + intendedTheta);
				myThymio.setSpeed((short)0, (short)0);		
				myThymio.rotate(currentTheta-intendedTheta);
				try {
					myThymio.getThread().join();
					while (myPanel.correctingPosition()) Thread.sleep(ThymioDrivingThread.UPDATE_INTERVAL);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}

			computeAttractiveForce(s);
			
			if (dX + dY > 0) {
				int color;
				
				//myThymio.drive(16.5);
				System.out.println("ahead: " + currentTheta + "/" + intendedTheta);
				if (s.getColor().getRed() == 255 && s.getColor().getGreen() == 255 && s.getColor().getBlue() == 255)
					color = WHITE;
				else 
					color = BLACK;

				myThymio.driveAheadUntil(color, color, intendedTheta, p.getCoordinate(), s.getCoordinate());
				System.out.println("ahead complete. Go to next field.");
			}
			else if (dX + dY < 0) {
				int color;
				System.out.println("back: " + currentTheta + "/" + intendedTheta);

				if (s.getColor().getRed() == 255 && s.getColor().getGreen() == 255 && s.getColor().getBlue() == 255)
					color = WHITE;
				else 
					color = BLACK;
				
				//myThymio.drive(-16.5);
				myThymio.driveBackUntil(color, color, intendedTheta, p.getCoordinate(), s.getCoordinate());
				System.out.println("backwards complete. Go to next field.");
			}
			
			s.setGoal(false);
			i++;
		}
		
		for (i = 0; i < controls.size(); i++) controls.get(i).setOnPath(false);
	}
}

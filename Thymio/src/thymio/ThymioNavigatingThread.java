package thymio;

import java.util.ArrayList;

import main.Pathfinder;

public class ThymioNavigatingThread extends Thread {
	private ArrayList<Integer> controls;
	private Thymio myThymio;
	
	public ThymioNavigatingThread(Thymio t, ArrayList<Integer> c) {
		controls = c;
		myThymio = t;
	}
	
	public void run() {
		for(int i = 0; i < controls.size(); i++){
			System.out.println("COMMAND: " + i);
			switch(controls.get(i)){
			case Pathfinder.FORWARD:
				myThymio.drive(16.5);
				try {
					myThymio.getThread().join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;

			case Pathfinder.BACKWARDS:
				myThymio.drive(-16.5);
				try {
					myThymio.getThread().join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;

			case Pathfinder.RIGHT:
				myThymio.rotate(90);
				try {
					myThymio.getThread().join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;

			case Pathfinder.LEFT:
				myThymio.rotate(-90);
				try {
					myThymio.getThread().join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
		}
	}
}

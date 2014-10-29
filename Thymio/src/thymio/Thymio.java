package thymio;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import main.Pathfinder;
import math.MovingAverage;

import observer.MapPanel;
import observer.ThymioInterface;

public class Thymio {
	private short vleft;
	private short vright;
	private ThymioInterface myInterface;
	private ThymioDrivingThread myControlThread;
	private ThymioClient myClient;
	private long lastTimeStamp;
	private MapPanel myPanel;
	private PrintWriter logData;
	private List<Short> proxHorizontal;
	private MovingAverage odomLeftMean;
	private MovingAverage odomRightMean;
	
	public static final double MAXSPEED = 500;
	public static final double SPEEDCOEFF = 2.93;
	public static final double BASE_WIDTH = 95;
	public static final double VROTATION = 100;
	public static final double STRAIGHTON = 150;
	
	public Thymio(MapPanel p, String host) {
		vleft = vright = 0;
		
		myPanel = p;
		myClient = new ThymioClient(host);
		myInterface = new ThymioInterface(this);
		myControlThread = new ThymioDrivingThread(this);
		myControlThread.start();
		lastTimeStamp = Long.MIN_VALUE;
		
		odomLeftMean = new MovingAverage();
		odomRightMean = new MovingAverage();
		
		setVLeft((short)0);
		setVRight((short)0);
		
		try {
			logData = new PrintWriter(new FileWriter("./logdata.csv"));
			logData.println("motor.left.speed\tmotor.right.speed\tdelta x observed\tdelta x computed\tdelta theta observed\tdelta theta computed\tpos X\tposY\tacc 0\tacc 1\tacc 2\tdelta 0\tdelta 1\treflected 0\treflected 1");
			logData.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public ThymioInterface getInterface() {
		return myInterface;
	}
	
	public int getVLeft() {
		return vleft;
	}
	
	public synchronized void setVLeft(short v) {
		ArrayList<Short> data = new ArrayList<Short>();
		this.vleft = v;
		
		data.add(new Short(v));
		myClient.setVariable("motor.left.target", data);
		odomLeftMean.reset();
	}

	public synchronized void setVRight(short v) {
		ArrayList<Short> data = new ArrayList<Short>();
		this.vright = v;
		
		data.add(new Short(v));
		myClient.setVariable("motor.right.target", data);
		odomRightMean.reset();
	}

	public synchronized void setSpeed(short vleft, short vright) {
		ArrayList<Short> data = new ArrayList<Short>();
		this.vleft = vleft;
		this.vright = vright;

		data.add(new Short(vleft));
		data.add(new Short(vright));

		myClient.setSpeed(data);
	}

	public int getVRight() {
		return vright;
	}

	public synchronized void drive(double distCM) {
		try {
			double dt;
			
			if (distCM > 0) dt = 3/14.03541*distCM;
			else dt = -3/14.81564*distCM;
			
			this.wait();
			this.setSpeed((short)(0), (short)(0));
						
			this.wait();
			this.setSpeed((short)(Math.signum(distCM)*STRAIGHTON), (short)(Math.signum(distCM)*STRAIGHTON));
			new ThymioStopThread(this, (long)(dt*1000)).start();

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public synchronized void rotate(double deg) {
		double dt;
		
		try {
			this.wait();
			this.setSpeed((short)(0), (short)(0));

			dt = (Math.abs(deg)-1.09)/(0.36*VROTATION); // secs needed for rotation
			
			this.wait();
			this.setSpeed((short)(Math.signum(deg)*VROTATION), (short)(-Math.signum(deg)*VROTATION));
			System.out.println(Math.signum(deg)*VROTATION*360/(Math.PI*BASE_WIDTH*SPEEDCOEFF) + "/" + dt);
			new ThymioStopThread(this, (long)(dt*1000)).start();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public void updatePose(long now) {
		List<Short> sensorData;

		if (lastTimeStamp > Long.MIN_VALUE) {
			long dt = now - lastTimeStamp;
			double secsElapsed = ((double)dt)/1000.0;
			double distForward; // distance passed in secsElpased in forward direction of the robot
			double distRotation; // angle covered in secsElapsed around Thymio's center
			short odomLeft = Short.MIN_VALUE, odomRight = Short.MIN_VALUE;
			double ol, or;
			double odomForward;
			double odomRotation;

			synchronized (this) {
				sensorData = myClient.getVariable("motor.left.speed");
				if (sensorData.size() > 0) odomLeft = sensorData.get(0);
				else System.out.println("no data for motor.left.speed");

				sensorData = myClient.getVariable("motor.right.speed");
				if (sensorData.size() > 0) odomRight = sensorData.get(0);
				else System.out.println("no data for motor.right.speed");

				notify();
			}

			if (odomLeft == Short.MIN_VALUE || odomRight == Short.MIN_VALUE) return;
			else logData.print(odomLeft + "\t" + odomRight + "\t");

			odomLeftMean.addValue(odomLeft);
			odomRightMean.addValue(odomRight);

			ol = odomLeftMean.getMean();
			or = odomRightMean.getMean();

			odomForward = secsElapsed*(ol+or)/(2.0*10.0*SPEEDCOEFF); // estimated distance in cm travelled is secsElapsed seconds.
			//odomRotation = Math.atan2(secsElapsed*(odomRight-odomLeft), BASE_WIDTH);
			odomRotation = Math.atan2(secsElapsed*(or-ol)/SPEEDCOEFF, BASE_WIDTH);

			distForward = (vleft+vright)/(2.0*10.0*SPEEDCOEFF)*secsElapsed;
			distRotation = Math.atan2(secsElapsed*(vright-vleft)/SPEEDCOEFF, BASE_WIDTH);

			logData.print(odomForward + "\t" + distForward + "\t" + odomRotation + "\t" + distRotation + "\t");

			//if ((distRotation != 0) || (distForward != 0)) {
				myPanel.updatePose(distForward, distRotation,
						(distForward != 0 ? odomForward : 0), (distRotation != 0 ? odomRotation : 0),
						secsElapsed);
			//}
			/*
				else {
					myPanel.updatePose(distForward, distRotation,
							0, 0,
							secsElapsed);
				}*/

			logData.print(myPanel.getEstimPosX() + "\t" +myPanel.getEstimPosY());


			proxHorizontal = myClient.getVariable("prox.horizontal");
			myPanel.setProxHorizontal(proxHorizontal);
			
			sensorData = myClient.getVariable("acc");
			for (int i = 0; i < sensorData.size(); i++) logData.print("\t" + sensorData.get(i));

			sensorData = myClient.getVariable("prox.ground.ambiant");
			for (int i = 0; i < sensorData.size(); i++) logData.print("\t" + sensorData.get(i));
			
			sensorData = myClient.getVariable("prox.ground.delta");
			for (int i = 0; i < sensorData.size(); i++) logData.print("\t" + sensorData.get(i));

			sensorData = myClient.getVariable("prox.ground.reflected");
			for (int i = 0; i < sensorData.size(); i++) logData.print("\t" + sensorData.get(i));
			
			logData.print("\n");
			logData.flush();

			
			myPanel.updatePoseGround(sensorData,
					                 Math.atan2(secsElapsed*(or+odomRightMean.getStd()-ol-odomLeftMean.getStd())/SPEEDCOEFF, BASE_WIDTH));
					                 
			myPanel.repaint();
		}

		lastTimeStamp = now;
	}
	
	public void driveAstarPath() {
		Pathfinder myPath = new Pathfinder(myPanel.getMap());
		ArrayList<Integer> paths = myPath.getPathsForThymio();
		
		//Wird noch nicht funktionieren - wait Thread noetig?
		
		for(int i = 0; i < paths.size(); i++){
			switch(paths.get(i)){
			case 1:
				drive(16.5);
				break;
				
			case 0:
				drive(-16.5);
				break;
				
			case 2:
				rotate(90);
				break;
				
			case 3:
				rotate(-90);
				break;
			}
		}
	}
}

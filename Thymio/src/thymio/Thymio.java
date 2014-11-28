package thymio;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import main.Pathfinder;
import math.MovingAverage;
import observer.BeliefPanel;
import observer.EvalBeliefPanel;
import observer.MapPanel;
import observer.PositionBeliefPanel;
import observer.SensorBeliefPanel;
import observer.ThymioInterface;
import context.Coordinate;
import context.MapElement;

public class Thymio extends Thread {
	public static final int UPDATE_INTERVAL = 50;

	private short vleft;
	private short vright;
	private short vleftStored;
	private short vrightStored;
	private ThymioInterface myInterface;
	private ThymioClient myClient;
	private long lastTimeStamp;
	private MapPanel myPanel;
	private PrintWriter logData;
	private List<Short> proxHorizontal;
	private MovingAverage odomLeftMean;
	private MovingAverage odomRightMean;
	private Thread t;
	private boolean paused;
	private boolean driving;
	private boolean updating;
	private boolean poseUpdated;
	
	public static final double MAXSPEED = 500;
	public static final double SPEEDCOEFF = 2.93;
	public static final double BASE_WIDTH = 95;
	public static final short VROTATION = 150;
	public static final short STRAIGHTON = 150;
	public static final double DIFFSENSOR = 20.0*Math.PI/180.0;

	private int state;

	public static final int ROTATION_RIGHT = 0;
	public static final int ROTATION_LEFT = 1;
	public static final int AHEAD = 2;
	public static final int BACK = 3;
	public static final int STOPPED = 4;
	
	public Thymio(MapPanel p, PositionBeliefPanel bp, SensorBeliefPanel sp, EvalBeliefPanel ep, String host) {
		vleft = vright = 0;
		driving = false;
		updating = false;
		poseUpdated = false;
		
		myPanel = p;
		myClient = new ThymioClient(host);
		myInterface = new ThymioInterface(this, bp, sp, ep);
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
		
		state = STOPPED;
		paused = false;
	}

	public boolean isDriving() {
		return driving;
	}
	
	public boolean isUpdating() {
		return updating;
	}
	
	public void setStopped() {
		state = STOPPED;
	}
	
	public ThymioInterface getInterface() {
		return myInterface;
	}
	
	public short getVLeft() {
		return vleft;
	}
	
	public void setVLeft(short v) {
		ArrayList<Short> data = new ArrayList<Short>();
		this.vleft = v;
		
		data.add(new Short(v));
		myClient.setVariable("motor.left.target", data);
		odomLeftMean.reset();
	}

	public void setVRight(short v) {
		ArrayList<Short> data = new ArrayList<Short>();
		this.vright = v;
		
		data.add(new Short(v));
		myClient.setVariable("motor.right.target", data);
		odomRightMean.reset();
	}

	public void setSpeed(short vleft, short vright, boolean update) {
		ArrayList<Short> data = new ArrayList<Short>();
		
		this.vleft = vleft;
		this.vright = vright;

		data.add(new Short(vleft));
		data.add(new Short(vright));

		myClient.setSpeed(data);
		//this.invalidateTimeStamp();
	}

	private void invalidateTimeStamp() {
		lastTimeStamp = Long.MIN_VALUE;
	}
	
	public short getVRight() {
		return vright;
	}

	public void drive(boolean ahead) {
		System.out.println("driving straight on ...: " + paused);

		if (paused) return;
		else driving = true;


		state = (ahead ? AHEAD : BACK);
		this.setSpeed((short)((ahead ? 1 : -1)*STRAIGHTON), (short)((ahead ? 1 : -1)*STRAIGHTON), true);

		driving = false;
	}
	
	public synchronized void drive(double distCM) {
		long start = System.currentTimeMillis();
		long now = start;
		double dt = 3/14.03541*distCM;

		if (paused) {
			this.notifyAll();
			return;
		}
		else driving = true;
		
		this.setSpeed((short)(Math.signum(distCM)*STRAIGHTON), (short)(Math.signum(distCM)*STRAIGHTON), true);

		state = (distCM > 0) ? AHEAD : BACK;

		try {
			while (!paused && ((now = System.currentTimeMillis()) - start < (long)(dt*1000))) {
				Thread.sleep(Thymio.UPDATE_INTERVAL);
			}

			this.setStopped();
			this.setSpeed((short)0, (short)0, true);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		driving = false;
		this.notifyAll();
	}

	public void rotate(double rad) {
		double dt;
		double theta;
		int k;
		
		synchronized (this) {
			this.setDriving(true);
			this.setSpeed((short)0, (short)0, true);

			theta = myPanel.getOrientation();
			theta -= rad;

			k = (int)(0.5*theta/Math.PI);
			theta -= k*2*Math.PI;

			//dt = (Math.abs((theta-rad)*180/Math.PI)-1.09)/(0.36*VROTATION); /*Thymio's personal constant */; // secs needed for rotation

			if (theta > 0) {
				state = ROTATION_RIGHT;
				dt = theta/0.935328;
			}
			else {
				state = ROTATION_LEFT;
				dt = -theta/0.9153185;
			}

			System.out.println("set dt: " + dt + " for " + theta + " (current theta: " + myPanel.getOrientation() + ")");
		}

		new ThymioTimerThread(this, (long)(dt*1000), (short)(Math.signum(theta)*VROTATION), (short)(-Math.signum(theta)*VROTATION)).start();
	}

	public synchronized void updatePose(long now) {
		List<Short> sensorData;

		if (paused) {
			this.notifyAll();
			return;
		}
		else if (lastTimeStamp > Long.MIN_VALUE) {
			long dt = now - lastTimeStamp;
			double secsElapsed = ((double)dt)/1000.0;
			double distForward; // distance passed in secsElpased in forward direction of the robot
			double distRotation; // angle covered in secsElapsed around Thymio's center
			short odomLeft = Short.MIN_VALUE, odomRight = Short.MIN_VALUE;
			double ol, or;
			double odomForward;
			double odomRotation;
			
			updating = true;
			poseUpdated = false;
			
			sensorData = myClient.getVariable("motor.left.speed");
			if (sensorData.size() > 0) odomLeft = sensorData.get(0);
			else System.out.println("no data for motor.left.speed");

			sensorData = myClient.getVariable("motor.right.speed");
			if (sensorData.size() > 0) odomRight = sensorData.get(0);
			else System.out.println("no data for motor.right.speed");


			if (odomLeft == Short.MIN_VALUE || odomRight == Short.MIN_VALUE) return;

			odomLeftMean.addValue(odomLeft);
			odomRightMean.addValue(odomRight);

			ol = odomLeftMean.getMean();
			or = odomRightMean.getMean();

			distForward = secsElapsed*(vleft+vright)/(2.0*10.0*SPEEDCOEFF); // estimated distance in cm travelled is secsElapsed seconds.
			distRotation = Math.atan2(secsElapsed*(vright-vleft)/SPEEDCOEFF, BASE_WIDTH);

			if (state == STOPPED) {
				odomForward = odomRotation = 0;
			}
			else if ((state == AHEAD) || (state == BACK)) {
				if ((vleft != 0) && (vright !=0)) {
					
					odomForward = secsElapsed*(odomLeft+odomRight)/(2.0*10.0*SPEEDCOEFF); // estimated distance in cm travelled is secsElapsed seconds.
					odomRotation = -Math.atan2(secsElapsed*(odomRight-odomLeft), BASE_WIDTH);
					/*
					odomForward = secsElapsed*(ol+or)/(2.0*10.0*SPEEDCOEFF); // estimated distance in cm travelled is secsElapsed seconds.
					odomRotation = Math.atan2(secsElapsed*(or-ol)/SPEEDCOEFF, BASE_WIDTH);
					*/
				}
				else {
					odomForward = 0;
					odomRotation = 0;
				}
			}
			else if (state == ROTATION_LEFT || state == ROTATION_RIGHT) {
				/*
				distForward = 0;
				distRotation = ((0.36*VROTATION*((state == ROTATION_LEFT) ? 1 : -1)*secsElapsed+1.09)/180*Math.PI);
				*/
				odomForward = 0;
				odomRotation = ((0.36*VROTATION*((state == ROTATION_LEFT) ? 1 : -1)*secsElapsed+1.09)/180*Math.PI);

				/*
				if (state == ROTATION_RIGHT) {
					odomRotation = -secsElapsed*0.935328;
				}
				else {
					odomRotation = secsElapsed*0.9153185;
				}
				
				odomForward = secsElapsed*(ol+or)/(2.0*10.0*SPEEDCOEFF); // estimated distance in cm travelled is secsElapsed seconds.
				odomRotation = Math.atan2(secsElapsed*(or-ol)/SPEEDCOEFF, BASE_WIDTH);
				
				odomForward = secsElapsed*(odomLeft+odomRight)/(2.0*10.0*SPEEDCOEFF); // estimated distance in cm travelled is secsElapsed seconds
				odomRotation = Math.atan2(secsElapsed*(odomRight-odomLeft), BASE_WIDTH);
*/				
				//odomRotation = Math.atan2(secsElapsed*(odomRight-odomLeft), BASE_WIDTH);
				//odomRotation = Math.atan2(secsElapsed*(or-ol)/SPEEDCOEFF, BASE_WIDTH);
				//odomRotation = Math.atan2(secsElapsed*(vright-vleft)/SPEEDCOEFF, BASE_WIDTH);
				
				System.out.println("rotate: " + state + " dR: " + odomRotation + " driving: " + driving);
			}	
			else {
				System.out.println("UNKNOWN DRIVING STATE");
				odomForward = secsElapsed*(ol+or)/(2.0*10.0*SPEEDCOEFF); // estimated distance in cm travelled is secsElapsed seconds.
				odomRotation = Math.atan2(secsElapsed*(or-ol)/SPEEDCOEFF, BASE_WIDTH);
			}
			
			myPanel.observationData(odomForward, odomRotation);
			myPanel.updatePose(distForward, distRotation,
					odomForward, odomRotation,
					secsElapsed,
					state);

			proxHorizontal = myClient.getVariable("prox.horizontal");
			myPanel.setProxHorizontal(proxHorizontal);

			sensorData = myClient.getVariable("prox.ground.reflected");

			myInterface.updateSensorView(sensorData, myPanel.getSensorMapProbsLeft(), myPanel.getSensorMapProbsRight());
			if (state != ROTATION_LEFT && state != ROTATION_RIGHT) poseUpdated = myPanel.updatePoseGround(sensorData, this);
			
			updating = false;
		}
		
		lastTimeStamp = now;
		this.notifyAll();
	}
	
	public boolean poseUpdated() {
		return poseUpdated;
	}
	
	public double [] computeAttractiveForce(MapElement g) {
		double diffVector [] = myPanel.getDistVectorTo(g, myPanel.getEstimPosX(), myPanel.getEstimPosY());
		
		diffVector[0] = -diffVector[0]*0.33;
		diffVector[1] = -diffVector[1]*0.33;
		
		return diffVector;
	}
	
	public double [] computeRepulsiveForces() {
		double dist = myPanel.getMinSensorDist();
		double repulsionVector [] = new double[2];
		
		if (dist < Double.MAX_VALUE) {
			int sensor = myPanel.getMinSensorId();
			double x = -Math.cos(myPanel.getOrientation()+(2-sensor)*Thymio.DIFFSENSOR);
			double y = -Math.sin(myPanel.getOrientation()+(2-sensor)*Thymio.DIFFSENSOR);
			double length;
			int sensDist = Math.abs(2-sensor);
			
			
			repulsionVector[0] = x/Math.pow(dist, 4);
			repulsionVector[1] = y/Math.pow(dist, 4);
			
			length = Math.sqrt(repulsionVector[0]*repulsionVector[0]+repulsionVector[1]*repulsionVector[1]);
			length *= 0.1*Math.exp(-8*sensDist);
			
			repulsionVector[0] *= length;
			repulsionVector[1] *= length;
		}
		else {			
			repulsionVector[0] = 0;
			repulsionVector[1] = 0;
		}
		
		return repulsionVector;
	}
	
	public ArrayList<MapElement> computePath(MapElement start, MapElement goal) {
		Pathfinder myPath = new Pathfinder(myPanel.getMap(), start, goal);
		myPath.findPath();
		return myPath.getSolution();
	}
	
	public boolean localizationProblemLeft() {
		return myInterface.localizationProblemLeft();
	}
	
	public boolean localizationProblemRight() {
		return myInterface.localizationProblemRight();
	}
	
	public boolean localizationProblemAhead() {
		return myInterface.localizationProblemAhead();
	}
	
	public synchronized void setPause(boolean p) {
		paused = p;
		if (paused) {
			vleftStored = this.getVLeft();
			vrightStored = this.getVRight();
			this.setSpeed((short)0, (short)0, true);
		}
		else {
			this.setSpeed(vleftStored, vrightStored, true);
			lastTimeStamp = Long.MIN_VALUE;
			this.notifyAll();
		}
	}
	
	public boolean isStopped() {
		return state == STOPPED;
	}
	
	public boolean isPaused() {
		return paused;
	}
	
	public synchronized void setDriving(boolean v) {
		driving = v;
		lastTimeStamp = Long.MIN_VALUE;
	}
}

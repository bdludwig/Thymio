package thymio;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import main.Pathfinder;
import math.MovingAverage;
import observer.EvalBeliefPanel;
import observer.MapPanel;
import observer.PositionBeliefPanel;
import observer.SensorBeliefPanel;
import observer.ThymioInterface;
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
	private Thread timerThread;
	private Thread myMonitor;
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

	public int getThymioState() {
		return state;
	}
	
	public void setThymioState(int s) {
		state = s;
	}

	public boolean isRotating() {
		return driving && (state == ROTATION_LEFT || state == ROTATION_RIGHT);
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
	}

	public void setVRight(short v) {
		ArrayList<Short> data = new ArrayList<Short>();
		this.vright = v;
		
		data.add(new Short(v));
		myClient.setVariable("motor.right.target", data);
	}

	public void setSpeed(short vleft, short vright, boolean update) {
		ArrayList<Short> data = new ArrayList<Short>();
		
		if (update && (state != STOPPED)) this.updateKalmanEstimation(System.currentTimeMillis());
		
		this.vleft = vleft;
		this.vright = vright;

		data.add(new Short(vleft));
		data.add(new Short(vright));

		myClient.setSpeed(data);
		lastTimeStamp = System.currentTimeMillis();
	}

	public short getVRight() {
		return vright;
	}

	public void drive(boolean ahead) {
		System.out.println("driving straight on: " + myPanel.getOrientation());

		if (paused || ((ahead ? AHEAD : BACK) == state)) return;
		else driving = true;


		state = (ahead ? AHEAD : BACK);
		this.setSpeed((short)((ahead ? 1 : -1)*STRAIGHTON), (short)((ahead ? 1 : -1)*STRAIGHTON), true);
	}
	
	public void drive(double distCM) {
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
			this.setDriving(false);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		driving = false;
		this.notifyAll();
	}

	public Thread getTimerThread() {
		return timerThread;
	}
	
	public void setTimerThread(Thread t) {
		timerThread = t;
	}
	
	public void rotate(double rad) {
		double dt;
		double theta;
		int k;
		int s;

		this.setDriving(false);
		this.setSpeed((short)0, (short)0, true);
		this.setStopped();

		theta = myPanel.getOrientation();
		theta = rad - theta;

		k = (int)(0.5*theta/Math.PI);
		theta -= k*2*Math.PI;


		if (theta < 0) {
			s = ROTATION_RIGHT;
			//dt = theta/0.935328;
			//dt = theta/0.975328;
			dt = -theta/1.052852;
		}
		else {
			s = ROTATION_LEFT;
			//dt = -theta/0.9153185;
			//dt = -theta/0.975328;
			dt = theta/1.052852;
		}

		//dt = (Math.abs(theta*180/Math.PI)-1.09)/(0.36*VROTATION); /*Thymio's personal constant */; // secs needed for rotation

		System.out.println("set dt: " + dt + " for " + theta + " (current theta: " + myPanel.getOrientation() + ")");

		timerThread = new ThymioTimerThread((long)(dt*1000), this, s);
		timerThread.start();
		
		this.setDriving(true);
		System.out.println("rotate starts Thymio.");
		this.setSpeed((s == Thymio.ROTATION_LEFT ? -Thymio.VROTATION : Thymio.VROTATION), 
				      (s == Thymio.ROTATION_LEFT ? Thymio.VROTATION : -Thymio.VROTATION), true);
		this.setThymioState(s);
	}

	public void updateKalmanEstimation(long now) {
		long dt;

		if (lastTimeStamp == Long.MIN_VALUE) {
			System.out.println("last time stamp invalid");
			lastTimeStamp = now;
			return;
		}
		else {
			dt = now - lastTimeStamp;
		}
		
		double secsElapsed = ((double)dt)/1000.0;
		double distForward; // distance passed in secsElpased in forward direction of the robot
		double distRotation; // angle covered in secsElapsed around Thymio's center
		short odomLeft = Short.MIN_VALUE, odomRight = Short.MIN_VALUE;
		double odomForward;
		double odomRotation;
		List<Short> sensorData;

		//if ((vleft == 0) && (vright == 0)) return;

		sensorData = myClient.getVariable("motor.left.speed");
		if (sensorData.size() > 0) odomLeft = sensorData.get(0);
		else System.out.println("no data for motor.left.speed");

		sensorData = myClient.getVariable("motor.right.speed");
		if (sensorData.size() > 0) odomRight = sensorData.get(0);
		else System.out.println("no data for motor.right.speed");


		if (odomLeft == Short.MIN_VALUE || odomRight == Short.MIN_VALUE) return;

		distForward = secsElapsed*(vleft+vright)/(2.0*10.0*SPEEDCOEFF); // estimated distance in cm travelled is secsElapsed seconds.
		distRotation = Math.atan2(secsElapsed*(vright-vleft)/SPEEDCOEFF, BASE_WIDTH);

		if (state == STOPPED) {
			odomForward = odomRotation = 0;
		}
		else {
			odomForward = secsElapsed*(odomLeft+odomRight)/(2.0*10.0*SPEEDCOEFF); // estimated distance in cm travelled is secsElapsed seconds.
			odomRotation = Math.atan2(secsElapsed*(odomRight-odomLeft)/SPEEDCOEFF, BASE_WIDTH);
		}

		/*if ((vleft == 0) && (vright == 0)) System.out.println("no update");
		else {*/
			myPanel.observationData(odomForward, odomRotation);
			myPanel.updatePose(distForward, distRotation,
					odomForward, odomRotation,
					secsElapsed,
					state);
			System.out.println("state: " + state + " dR: " + odomRotation + " driving: " + this.isRotating() + " vleft: " + vleft + " vright: " + vright + " theta: " + myPanel.getOrientation() + " secs: " + secsElapsed);
		/*}*/
	}

	public void updateMapEstimation() {
		List<Short> sensorData;

		proxHorizontal = myClient.getVariable("prox.horizontal");
		myPanel.setProxHorizontal(proxHorizontal);

		sensorData = myClient.getVariable("prox.ground.reflected");

		myInterface.updateSensorView(sensorData, myPanel.getSensorMapProbsLeft(), myPanel.getSensorMapProbsRight());
		poseUpdated = myPanel.updatePoseGround(sensorData, this);
	}
	
	public void updatePose(long now) {
		if (lastTimeStamp > Long.MIN_VALUE) {
			updating = true;
			poseUpdated = false;

			updateKalmanEstimation(now);
			if (state != ROTATION_LEFT && state != ROTATION_RIGHT) updateMapEstimation();

			updating = false;
		}

		lastTimeStamp = now;
	}
	
	public void setMonitorThread(Thread t) {
		myMonitor = t;
	}
	
	public Thread getMonitorThread() {
		return myMonitor;
	}
	
	public boolean poseUpdated() {
		return poseUpdated;
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
	}
}

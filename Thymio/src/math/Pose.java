package math;

import java.awt.geom.AffineTransform;

public class Pose {
	private double x;
	private double y;
	private double theta;
	private double evalSensor;
	private double evalPosition;
	private AffineTransform [] sensorRotation;
	
	public Pose(double x, double y, double theta, double evalSensor, double evalPosition, AffineTransform sensor0, AffineTransform sensor1) {
		super();
		this.x = x;
		this.y = y;
		this.theta = theta;
		this.evalSensor = evalSensor;
		this.evalPosition = evalPosition;
		
		sensorRotation = new AffineTransform[2];
		sensorRotation[0] = sensor0;
		sensorRotation[1] = sensor1;		
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getTheta() {
		return theta;
	}

	public double getEval() {
		return evalSensor + evalPosition;
	}
	
	public String toString() {
		return x + "\t" + y + "\t" + String.format("%+1.5f", theta) + "\t" + String.format("%+1.20f", evalPosition)+ "\t" + String.format("%+1.20f", evalSensor);
	}
	
	public AffineTransform getSensorRotation(int i) {
		return sensorRotation[i];
	}
}

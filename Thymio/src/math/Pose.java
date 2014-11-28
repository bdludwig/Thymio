package math;

import java.awt.geom.AffineTransform;

public class Pose {
	private double x;
	private double y;
	private double theta;
	private double evalSensor;
	private double evalPosition;
	
	public Pose(double x, double y, double theta, double evalSensor, double evalPosition) {
		super();
		this.x = x;
		this.y = y;
		this.theta = theta;
		this.evalSensor = evalSensor;
		this.evalPosition = evalPosition;	
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
	
	public double getEvalPos() {
		return evalPosition;
	}
	
	public double getEvalSensor() {
		return evalSensor;
	}
	
	public String toString() {
		return x + "\t" + y + "\t" + String.format("%+1.5f", theta) + "\t" + String.format("%+1.20f", evalPosition)+ "\t" + String.format("%+1.20f", evalSensor);
	}
}

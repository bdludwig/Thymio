package math;

public class MovingAverage {
	private int n;
	double mean;
	double std;
	
	public MovingAverage() {
		init();
	}
	
	private void init() {
		n = 0;
		mean = 0.0;	
		std = 0.0;	
	}
	
	public void reset() {
		init();
	}
	
	public void addValue(double val) {
		double square;
		
		square = n*(val-mean)*(val-mean)/(n+1);
		mean = (((double)n)*mean + val)/(n+1);		
		if (n > 0) std = Math.sqrt(square/n);
	}
	
	public double getMean() {
		return mean;
	}
	
	public double getStd() {
		return std;
	}
}

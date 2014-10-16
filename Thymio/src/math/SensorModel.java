package math;

import java.util.List;

public class SensorModel {
	private List<Double> regrCoeffs;
	
	public SensorModel(List<Double> c) {
		regrCoeffs = c;
	}
	
	public double computeDistance(double sensorVal) {
		double res = regrCoeffs.get(0);
		
		// use Horner schema for computing the value of f(x) for f a polynomial of degree >= 0
		for (int i = 1; i < regrCoeffs.size(); i++) {
			res = res*sensorVal + regrCoeffs.get(i);
		}
		
 		return res;
	}
}

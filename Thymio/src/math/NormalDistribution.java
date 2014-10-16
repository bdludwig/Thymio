package math;

import java.util.Hashtable;
import java.util.Random;

public class NormalDistribution {
	private double mean;
	private double standardDev;
	private Random randGen;
	private int histogramMode = 0;
	
	public NormalDistribution(double m, double sd) {
		mean = m;
		standardDev = sd;
		randGen = new Random();
	}
	
	public double drawFromDistribution() {
		double val = 0;
		
		for (int i = 0; i < 12; i++) {
			val += randGen.nextGaussian()*standardDev;
		}
		
		return 0.5*val + mean;
	}
	
	public void setMean(double m) {
		mean = m;
	}
	
	public void setSD(double sd) {
		standardDev = sd;
	}
	
	public int getMode() {
		return histogramMode;
	}
	
	public Hashtable<Integer, Integer> generateHistogram(int sampleSize) {
		Hashtable<Integer, Integer> histogram = new Hashtable<Integer, Integer>();
		
		for (int i = 0; i < sampleSize; i++) {
			int r = (int)drawFromDistribution();
			Integer count;
			int freq;
			
			count = histogram.get(Integer.valueOf(r));
			if (count == null) {
				histogram.put(Integer.valueOf(r), new Integer(1));
				if (histogramMode == 0) histogramMode = 1;
			}
			else {
				freq = count.intValue() + 1;
				histogram.put(Integer.valueOf(r), new Integer(freq));
				if (freq > histogramMode) histogramMode = freq;
			}
		}
		
		return histogram;
	}
}
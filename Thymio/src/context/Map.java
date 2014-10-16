package context;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import math.KalmanFilter;
import math.SensorModel;

import org.ejml.data.DenseMatrix64F;

import observer.MapPanel;

public class Map {
	private int sizeX, sizeY; // number of elements in each direction 
							  // used to get a discrete model of the environment
	private int thymioX, thymioY; // coordinates of MapElement where Thymio is currently located on.
	private double posX, posY; // current position of Thymio in real units
	private double thymioTheta; // current orientation of Thymio in the global coordinate system
	private double [] infraredDist;

	private double estPosX, estPosY; // estimated current position of Thymio in real units
	private double estTheta; // estimated current orientation of Thymio in the global coordinate system

	private MapElement [][] element; // Array of MapElement representing the environment
	private double edgelength; // each element in this maps covers edgelength^2 square units.
	
	public static final int N = 20; // number of occupied elements
	
	private KalmanFilter posEstimate;
	private SensorModel proxHorSensor;
	private DenseMatrix64F R;
	
	public Map(int x, int y, double l) {
		edgelength = l;
		sizeX = x;
		sizeY = y;
		
		element = new MapElement[sizeX][sizeY];
		infraredDist = new double[7];
		for (int i = 0; i < infraredDist.length; i++) infraredDist[i] = Double.MAX_VALUE;
		
		initMap();
		initFilter();
		
		ArrayList<Double> proxHorCoeffs = new ArrayList<Double>();
		
		// see exercise 5.
		
		proxHorCoeffs.add(new Double(-3.937e-13));
		proxHorCoeffs.add(new Double(4.701e-09));
		proxHorCoeffs.add(new Double(-1.935e-05));
		proxHorCoeffs.add(new Double(2.833e-02));
		proxHorCoeffs.add(new Double(-1.829e-01));
		
		proxHorSensor = new SensorModel(proxHorCoeffs);
	}
	
	private void initFilter() {
		DenseMatrix64F F;
		DenseMatrix64F Q;
		DenseMatrix64F P;
		
		// state transition
		
		
		double [][] valF = {{1, 0, 0, 0},
				            {0, 1, 0, 0},
				            {0, 0, 0, 0},
				            {0, 0, 0, 0}};
		
		/*double [][] valF = {{1, 0, 0, 0, 0},
				            {0, 1, 0, 0, 0},
				            {0, 0, 1, 0, 0},
				            {0, 0, 0, 0, 0},
				            {0, 0, 0, 0, 0}};*/
		F = new DenseMatrix64F(valF);
		
		// process noise

		/*
		double [][] valQ = {{0, 0, 0, 0, 0},
	            {0, 0, 0, 0, 0},
	            {0, 0, 0, 0, 0},
	            {0, 0, 0, 0, 0},
	            {0, 0, 0, 0, 0}};
		*/

		
		double [][] valQ = {{0.0889258250, -0.0117208306, 0, 0},
				            {-0.0117208306, 0.0341476440, 0, 0},
				            {0, 0, 0, 0},
				            {0, 0, 0, 0}};
		/*
		double [][] valQ = {{0.0889258250, -0.0117208306, 1.609542e-04, 0, 0},
				            {-0.0117208306, 0.0341476440, -1.451464e-04, 0, 0},
				            {0.0001609542, -0.0001451464,  2.702792e-05, 0, 0},
				            {0, 0, 0, 0, 0},
				            {0, 0, 0, 0, 0}};*/

		Q = new DenseMatrix64F(valQ);
		
		
		// sensor noise
		
		double [][] valR = {{0.1554881, 0.0007000067}, {0.0007000067, 0.01394959}};
		R = new DenseMatrix64F(valR);
		
		// initial state
/*
		double [][] valP = {{0, 0, 0, 0, 0},
				            {0, 0, 0, 0, 0},
				            {0, 0, 0, 0, 0},
				            {0, 0, 0, 0, 0},
				            {0, 0, 0, 0, 0}};*/
		P = new DenseMatrix64F(valQ);
		
		double [] state = {posX, posY, 0, 0};
		
		posEstimate = new KalmanFilter();
		posEstimate.configure(F, Q);
		posEstimate.setState(DenseMatrix64F.wrap(4, 1, state), P);
	}
	
	public double getEdgeLength() {
		return edgelength;
	}
	
	public void setPose(double x, double y, double theta) {
		posX = x;
		posY = y;
		thymioTheta = theta;
		estTheta = theta;
		
		initFilter();
		
		updateCurrentPos();
	}
	
	public void updatePose(double dF, double dR, double dFobs, double dRobs, double dt) {
		double [] delta = new double[4];
		double lastEstX = estPosX, lastEstY = estPosY;
		
		delta[0] = Math.cos(estTheta)*dF;
		delta[1] = Math.sin(estTheta)*dF;
		delta[2] = dF;
		delta[3] = dR;
		
		DenseMatrix64F Gu = DenseMatrix64F.wrap(4, 1, delta);
		
		thymioTheta = thymioTheta + dR;
		posX += delta[0];
		posY += delta[1];
		
		// observation model
		
		double [][] valH = {{0, 0, 0, 0}, {0, 0, 0, 0}};
		valH[0][2] = 1/dt;
		valH[1][3] = 1/dt;
		DenseMatrix64F H = new DenseMatrix64F(valH);

		// sensor values
		
		double [] speed = {dFobs/dt, dRobs/dt};
		double dist;
		//double [] speed = {dF/dt, dR/dt};
		
		posEstimate.predict(Gu);
		posEstimate.update(DenseMatrix64F.wrap(2, 1, speed), H, R);
		
		DenseMatrix64F estimState = posEstimate.getState();
		estPosX = estimState.get(0);
		estPosY = estimState.get(1);
		estTheta += dRobs;

		thymioX = (int)(estPosX/MapPanel.LENGTH_EDGE_CM);
		thymioY = (int)(estPosY/MapPanel.LENGTH_EDGE_CM);
	}
	
	private void updateCurrentPos() {
		thymioX = (int)(posX/MapPanel.LENGTH_EDGE_CM);
		thymioY = (int)(posY/MapPanel.LENGTH_EDGE_CM);
		
		estPosX = posX;
		estPosY = posY;
	}
	
	public double getCovariance(int x, int y) {
		return posEstimate.getCovariance().get(x, y);
	}
	
	public int getThymioX() {
		return thymioX;
	}

	
	public int getThymioY() {
		return thymioY;
	}
	
	public double getEstimPosX() {
		return estPosX;
	}
	
	public double getEstimPosY() {
		return estPosY;
	}
		
	public double getPosX() {
		return posX;
	}
	
	public double getPosY() {
		return posY;
	}
	
	public MapElement getElement(int x, int y) {
		if ((0 <= x) && (x < sizeX) && (0 <= y) && (y < sizeY)) return element[x][y];
		else return null;
	}
	
	public double getThymioOrientation() {
		return thymioTheta;
	}
	
	public double getEstimOrientation() {
		return estTheta;
	}
	
	private void initMap() {
		Random r = new Random();
		ArrayList<Integer> occupiedElements = new ArrayList<Integer>();
		
		// initialize each element of the map
		
		for (int x = 0; x < sizeX; x++) {
			for (int y = 0; y < sizeY; y++) {
				element[x][y] = new MapElement(x, y);
				element[x][y].setColor((x+y)%2 == 0 ? Color.WHITE : Color.LIGHT_GRAY);
			}
		}
	}
	
	public void printMap() {
		for (int x = 0; x < sizeX; x++) {
			for (int y = 0; y < sizeY; y++) {
				MapElement e = element[x][y];
				
				System.out.print(e.isOccupied() ? "T" : "F");
				System.out.print(e.onBeam() ? "B" : "-");
				System.out.print("\t");
			}
			
			System.out.print("\n");
		}
	}

	public Path followBeam( int x1, int y1, int x2, int y2 ) {
		Path p = new Path();
		int x = x1, y = y1;
		
	    int w = x2 - x;
	    int h = y2 - y;
	    int dx1 = 0, dy1 = 0, dx2 = 0, dy2 = 0;
	    if (w<0) dx1 = -1; else if (w>0) dx1 = 1;
	    if (h<0) dy1 = -1; else if (h>0) dy1 = 1;
	    if (w<0) dx2 = -1; else if (w>0) dx2 = 1;
	    int longest = Math.abs(w);
	    int shortest = Math.abs(h);
	    if (!(longest>shortest)) {
	        longest = Math.abs(h);
	        shortest = Math.abs(w);
	        if (h<0) dy2 = -1; else if (h>0) dy2 = 1;
	        dx2 = 0;            
	    }
	    int numerator = longest >> 1;
	    for (int i=0;i<=longest;i++) {
			if (0 <= x && x < sizeX && 0 <= y && y < sizeY) p.add(element[x][y]);
			numerator += shortest;
	        if (!(numerator<longest)) {
	            numerator -= longest;
	            x += dx1;
	            y += dy1;
	        } else {
	            x += dx2;
	            y += dy2;
	        }
	    }
	    
	    return p;
	}
	
	public int getSizeX() {
		return sizeX;
	}
	
	public int getSizeY() {
		return sizeY;
	}
	
	public boolean isOnBeam(int x, int y) {
		return element[x][y].onBeam();
	}
	
	public void setProxHorizontal(List<Short> val) {
		for (int i = 0; i < val.size(); i++) {
			infraredDist[i] = proxHorSensor.computeDistance(val.get(i).doubleValue());
			if (infraredDist[i] < 0) infraredDist[i] = Double.MAX_VALUE;
		}
	}
	
	public double getProxHorizontal(int sensorNo) {
		return infraredDist[sensorNo];
	}
	
	public void clearOccupancy() {
		for (int i = 0; i < sizeX; i++) {
			for (int j = 0; j < sizeY; j++) {
				element[i][j].setOnBeam(false);
			}
		}
	}
}

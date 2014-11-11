package context;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import math.KalmanFilter;
import math.SensorModel;
import observer.MapPanel;

import org.ejml.data.DenseMatrix64F;

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
	
	private AffineTransform [] sensorRotation;
	private Rectangle2D sensorbox = new Rectangle2D.Double(-0.5*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM,
			                                               -0.25*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM,
			                                               MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM,
			                                               0.5*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM);
	private double [] sensorMapProbsLeft;
	private double [] sensorMapProbsRight;

	private double obsX, obsY, obsTheta;

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
		sensorRotation = new AffineTransform[2];
		
		// see exercise 5.
		
		proxHorCoeffs.add(new Double(-3.937e-13));
		proxHorCoeffs.add(new Double(4.701e-09));
		proxHorCoeffs.add(new Double(-1.935e-05));
		proxHorCoeffs.add(new Double(2.833e-02));
		proxHorCoeffs.add(new Double(-1.829e-01));
		
		proxHorSensor = new SensorModel(proxHorCoeffs);
		
		obsX = Double.NaN;
		obsY = Double.NaN;
		obsTheta = Double.NaN;
	}
	
	private void initFilter() {
		DenseMatrix64F F;
		DenseMatrix64F Q;
		DenseMatrix64F P;
		
		// state transition
		
/*		
		double [][] valF = {{1, 0, 0, 0},
				            {0, 1, 0, 0},
				            {0, 0, 0, 0},
				            {0, 0, 0, 0}};
*/	
		double [][] valF = {{1, 0, 0},
				            {0, 1, 0},
				            {0, 0, 1}};
		
		F = new DenseMatrix64F(valF);
		
		// process noise

		/*
		double [][] valQ = {{0, 0, 0, 0, 0},
	            {0, 0, 0, 0, 0},
	            {0, 0, 0, 0, 0},
	            {0, 0, 0, 0, 0},
	            {0, 0, 0, 0, 0}};
		*/

		/*
		double [][] valQ = {{0.0889258250, -0.0117208306, 0, 0},
				            {-0.0117208306, 0.0341476440, 0, 0},
				            {0, 0, 0, 0},
				            {0, 0, 0, 0}};
		*/
		
		double [][] valQ = {{0.0889258250, -0.0117208306, 1.609542e-04},
	            {-0.0117208306, 0.0341476440, -1.451464e-04},
	            {0.0001609542, -0.0001451464,  2.702792e-05}};
		/*
		double [][] valQ = {{0.0889258250, -0.0117208306, 1.609542e-04, 0, 0},
				            {-0.0117208306, 0.0341476440, -1.451464e-04, 0, 0},
				            {0.0001609542, -0.0001451464,  2.702792e-05, 0, 0},
				            {0, 0, 0, 0, 0},
				            {0, 0, 0, 0, 0}};
*/
		Q = new DenseMatrix64F(valQ);
		
		
		// sensor noise
		/*
		double [][] valR = {{0.1554881, 0.0007000067}, {0.0007000067, 0.01394959}};
		*/
		double [][] valR = {{0.0889258250, -0.0117208306, 1.609542e-04},
                            {-0.0117208306, 0.0341476440, -1.451464e-04},
                            {1.609542e-04, 1.451464e-04,  0.01394959}};
		
		R = new DenseMatrix64F(valR);
		
		// initial state

		double [][] valP = {{0, 0, 0},
				            {0, 0, 0},
				            {0, 0, 0}};
		P = new DenseMatrix64F(valP);
		
		double [] state = {posX, posY, thymioTheta};
		
		posEstimate = new KalmanFilter();
		posEstimate.configure(F, Q);
		posEstimate.setState(DenseMatrix64F.wrap(3, 1, state), P);
	}
	
	public double getEdgeLength() {
		return edgelength;
	}
	
	public void setPose(double x, double y, double theta) {
		posX = x;
		posY = y;
		
		thymioTheta = theta;
		estTheta = theta;

		obsX = x;
		obsY = y;
		obsTheta = theta;
		
		initFilter();
		
		updateCurrentPos();
	}
	
	public void updatePose(double dF, double dR, double dFobs, double dRobs, double dt) {
		double [] delta = new double[3];
		
		delta[0] = Math.cos(estTheta)*dF;
		delta[1] = Math.sin(estTheta)*dF;
		delta[2] = dR;
		
		DenseMatrix64F Gu = DenseMatrix64F.wrap(3, 1, delta);
		
		thymioTheta += dR;
		posX += delta[0];
		posY += delta[1];
		
		// observation model
		
		double [][] valH = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};

		DenseMatrix64F H = new DenseMatrix64F(valH);

		// sensor values
		
		double [] observation = {obsX, obsY, obsTheta};
		//double [] speed = {dF/dt, dR/dt};
		
		posEstimate.predict(Gu);
		posEstimate.update(DenseMatrix64F.wrap(3, 1, observation), H, R);
		
		DenseMatrix64F estimState = posEstimate.getState();
		estPosX = estimState.get(0);
		estPosY = estimState.get(1);
		estTheta = estimState.get(2);

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

	public MapElement getCurrentPos() {
		int x = (int)(estPosX/MapPanel.LENGTH_EDGE_CM);
		int y = (int)(estPosY/MapPanel.LENGTH_EDGE_CM);
		
		return element[x][y];
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
		int id = 0;
		// initialize each element of the map
		
		for (int x = 0; x < sizeX; x++) {
			for (int y = 0; y < sizeY; y++) {
				element[x][y] = new MapElement(id ++, x, y);
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
	
	public boolean isOccupied(int x, int y) {
		return element[x][y].isOccupied();
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
	
	public double rotationProbability(double angle) {
		double dist = angle - getEstimOrientation();
		double sigma = getCovariance(2, 2);
		
		return dist*dist/(sigma == 0 ? 2.702792e-05 : sigma);
	}
	
	public double posProbability(double x, double y) {
		double distPredX = x*MapPanel.LENGTH_EDGE_CM/MapPanel.LENGTHSCALE - getEstimPosX();
		double distPredY = y*MapPanel.LENGTH_EDGE_CM/MapPanel.LENGTHSCALE - getEstimPosY();
		
		//System.out.println("dist X:" + distPredX + "/dist Y:" + distPredY);
		return getCovariance(0,0)*distPredX*distPredX +
				 2*getCovariance(0,1)*distPredX*distPredY +
				 getCovariance(1,1)*distPredY*distPredY;
	}
	
	private double [] computeSensorProb(int sensorid, double posX, double posY, double angle, Graphics g, int height) {
		//AffineTransform tmp;
		double sensorx = posX + 7.25*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.cos(angle+estTheta);
		double sensory = posY + 7.25*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.sin(angle+estTheta);		
		
		sensorRotation[sensorid] = new AffineTransform();
		sensorRotation[sensorid].translate(sensorx, height - sensory);
		sensorRotation[sensorid].rotate(estTheta + angle);

		Path2D sensingArea = (Path2D)sensorRotation[sensorid].createTransformedShape(sensorbox);
		Rectangle2D transformedbox = sensingArea.getBounds2D();
		int lowerx = (int)transformedbox.getMinX();
		int upperx = (int)transformedbox.getMaxX();
		int lowery = (int)transformedbox.getMinY();
		int uppery = (int)transformedbox.getMaxY();
		int n = 0;
		double white = 0;
		double black = 0;
		double probDist [] = new double[2];
				
		for (int i = lowerx; i <= upperx; i++) {
			for (int j = lowery; j <= uppery; j++) {
				if (sensingArea.contains(i, j)) {
					n ++;
					
					try {
						MapElement e = element[(i/MapPanel.LENGTHSCALE)][(j/MapPanel.LENGTHSCALE)];
						if (e.getColor().getRed() == 255 && e.getColor().getGreen() == 255 && e.getColor().getBlue() == 255) white ++;
						else black ++;
					}
					catch (ArrayIndexOutOfBoundsException e) {
						// ignore any eventual out of bounds issue
					}
				}
			}
		}
		
		probDist[0] = white/n;
		probDist[1] = black/n;
		
		//((Graphics2D)g).setTransform(tmp);

		return probDist;
	}
	
	public AffineTransform getSensorRotation(int sensorid) {
		return sensorRotation[sensorid];
	}
	
	public Rectangle2D getSensorBoundings() {
		return sensorbox;
	}
	
	public double computeSensorProb(double x, double y, double dtheta, int val1, double angle1, int val2, double angle2, Graphics g, int height, boolean printDebug) {
		double [] sensorValDist;
		double sensorProb;
		
		// System.out.println("computing a posteriori for: " + posX + "/" + posY);
		sensorValDist = computeSensorProb(0, x, y, dtheta + angle1, g, height);
		sensorMapProbsLeft = sensorValDist;


		if (val1 > 300) {
			// light color
			
			sensorProb = sensorValDist[0];
			//System.out.println((int)x + "/" + (int)y + ": prob sensor 1 for " + val1 + ": " + sensorProb + "|" + sensorValDist[0] + "," + sensorValDist[1]);
		}
		else {
			// dark color
			
			sensorProb = sensorValDist[1];
			//System.out.println((int)x + "/" +(int) y + ": prob sensor 1 for " + val1 + ": " + sensorProb + "|" + sensorValDist[0] + "," + sensorValDist[1]);
		}

		sensorValDist = computeSensorProb(1, x, y, dtheta + angle2, g, height);
		sensorMapProbsRight = sensorValDist;

		if (val2 > 300) {
			// light color
			
			sensorProb *= sensorValDist[0];
			//System.out.println((int)x + "/" + (int)y + ": prob sensor 2 for " + val1 + ": " + sensorValDist[0] + "|" + sensorValDist[0] + "," + sensorValDist[1]);
		}
		else {
			// dark color
			
			sensorProb *= sensorValDist[1];
			//System.out.println((int)x + "/" + (int)y + ": prob sensor 2 for " + val2 + ": " + sensorValDist[1] + "|" + sensorValDist[0] + "," + sensorValDist[1]);
		}
		
		if (printDebug) {
			System.out.println("EXP LINKS: " + ((sensorMapProbsLeft[0] > 0) ? "weiss" : "schwarz ") + " RECHTS: " + ((sensorMapProbsRight[0] > 0) ? "weiss" : "schwarz ") + " PROB: " + sensorProb);
			System.out.println("OBS LINKS: " + ((val1 > 300) ? "weiss" : "schwarz ") + " RECHTS: " + ((val2 > 300) ? "weiss" : "schwarz ") + " PROB: " + sensorProb);
		}

		if (sensorProb == 0) return Double.POSITIVE_INFINITY;
		else return -Math.log(sensorProb);
	}
	
	public double [] getSensorMapProbsLeft() {
		return sensorMapProbsLeft;
	}

	public double [] getSensorMapProbsRight() {
		return sensorMapProbsRight;
	}
	
	public double getObsX() {
		return obsX;
	}
	
	public double getObsY() {
		return obsY;
	}
	
	public double getObsTheta() {
		return obsTheta;
	}
	
	public void observationData(double dist, double theta) {
		obsX = estPosX + Math.cos(obsTheta)*dist;
		obsY = estPosY + Math.sin(obsTheta)*dist;
		obsTheta = estTheta + theta;
	}
	
	public double [] getDistVectorTo(MapElement l, double x, double y) {
		double [] res = new double[2];
		
		res[0] = x - (l.getPosX() + 0.5)*MapPanel.LENGTH_EDGE_CM;
		res[1] = y - (l.getPosY() + 0.5)*MapPanel.LENGTH_EDGE_CM;
		
		return res;
	}
}

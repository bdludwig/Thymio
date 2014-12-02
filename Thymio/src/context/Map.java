package context;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import math.KalmanFilter;
import math.Pose;
import math.SensorModel;
import observer.MapPanel;

import org.ejml.data.DenseMatrix64F;
import org.ejml.simple.SimpleMatrix;

import thymio.Thymio;

public class Map {
	private int sizeX, sizeY; // number of elements in each direction 
							  // used to get a discrete model of the environment
	private double posX, posY; // current position of Thymio in real units
	private double thymioTheta; // current orientation of Thymio in the global coordinate system
	private double [] infraredDist;

	private double estPosX, estPosY; // estimated current position of Thymio in real units
	private double estTheta; // estimated current orientation of Thymio in the global coordinate system

	private MapElement [][] element; // Array of MapElement representing the environment
	private double edgelength; // each element in this maps covers edgelength^2 square units.
	
	private AffineTransform [] sensorRotation;
	private AffineTransform poseTransform;

	private Shape poseUncertainty;
	private Rectangle2D sensorbox = new Rectangle2D.Double(-0.5*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM,
			                                               -0.25*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM,
			                                               MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM,
			                                               0.5*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM);
	private double [] sensorMapProbsLeft;
	private double [] sensorMapProbsRight;

	private double obsX, obsY, obsTheta;
	private Pose bestPose;
	private Pose [][] bestPoseAtPos;
	private double minSensorDist;
	private int minSensorId;
	private boolean correcting;

	public static final int N = 20; // number of occupied elements
	public static final double ANGLERANGE = Math.PI/4;
	
	private KalmanFilter posEstimate;
	private SensorModel proxHorSensor;
	private DenseMatrix64F R;
	private DenseMatrix64F F;

	
	private double [][] valQ_ahead = {{0.0889258250, -0.0117208306, 1.609542e-04},
            {-0.0117208306, 0.0341476440, -1.451464e-04},
            {0.0001609542, -0.0001451464,  2.702792e-05}};
/*
	private double [][] valQ_ahead = {{0.0889258250, -0.117208306, 1.609542e-02},
            {-0.117208306, 0.0341476440, -1.451464e-02},
            {1.609542e-02, -1.451464e-02,  2.702792e-03}};
	
	private double [][] valQ_left = {{0, 0, 10},
	{0, 0, 10},
	{10, 10, 0.00287276}};
	
	private double [][] valQ_right = {{0, 0, 10},
	{0, 0, 10},
		{10, 10, 0.002554608}};
	/*/
	private double [][] valQ_left = {{0.03883038, -0.003296355, 0.009096681},
	{-0.003296355, 0.006193314, -0.001209062},
	{0.009096681, -0.001209062, 0.00287276}};
	
	private double [][] valQ_right = {{0.03631655, -0.0002997269, -0.007957954},
	{-0.0002997269, 0.01249882, -0.0006557905},
		{-0.007957954, -0.0006557905, 0.002554608}};

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
		DenseMatrix64F Q;
		DenseMatrix64F P;
		
		// state transition

		double [][] valF = {{1, 0, 0},
				            {0, 1, 0},
				            {0, 0, 1}};
		
		F = new DenseMatrix64F(valF);
		
		// process noise

		Q = new DenseMatrix64F(valQ_ahead);
		
		
		// sensor noise

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
		
		while (theta > Math.PI) theta -= Math.PI;
		thymioTheta = theta;
		estTheta = theta;

		//if (Double.isNaN(obsX)) {
		obsX = x;
		obsY = y;
		obsTheta = theta;
		//}
		
		initFilter();
		updateCurrentPos();
	}
	
	public void updatePose(double dF, double dR, double dFobs, double dRobs, double dt, int state) {
		double [] delta = new double[3];
		int k;
		
		delta[0] = Math.cos(estTheta)*dF;
		delta[1] = Math.sin(estTheta)*dF;
		delta[2] = dR;
		
		if (state == Thymio.ROTATION_LEFT) {
			posEstimate.configure(F, new DenseMatrix64F(valQ_left));
		}
		else if (state == Thymio.ROTATION_RIGHT) {
			posEstimate.configure(F, new DenseMatrix64F(valQ_right));
		}
		else {
			posEstimate.configure(F, new DenseMatrix64F(valQ_ahead));
		}
		
		DenseMatrix64F Gu = DenseMatrix64F.wrap(3, 1, delta);
		
		thymioTheta += dR;
		
		k = (int)(0.5*thymioTheta/Math.PI);
		thymioTheta -= k*2*Math.PI;

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
		
		k = (int)(0.5*estTheta/Math.PI);
		estTheta -= k*2*Math.PI;
		
		posEstimate.getState().set(2, estTheta);
	}
	
	private void updateCurrentPos() {
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
		minSensorDist = Double.MAX_VALUE;
		
		for (int i = 0; i < val.size(); i++) {
			infraredDist[i] = proxHorSensor.computeDistance(val.get(i).doubleValue());
			if (infraredDist[i] < 0) infraredDist[i] = Double.MAX_VALUE;
			else if (infraredDist[i] < minSensorDist) {
				minSensorDist = infraredDist[i];
				minSensorId = i;
			}
		}		
	}
	
	public int getMinSensorId() {
		return minSensorId;
	}
	
	public double getMinSensorDist() {
		return minSensorDist;
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
	
	public double posProbability(double x, double y, double th) {
		SimpleMatrix invCov = SimpleMatrix.wrap(posEstimate.getCovariance()).invert();
		double distPredX = x*MapPanel.LENGTH_EDGE_CM/MapPanel.LENGTHSCALE - getEstimPosX();
		double distPredY = y*MapPanel.LENGTH_EDGE_CM/MapPanel.LENGTHSCALE - getEstimPosY();
		double distTh = th - getEstimOrientation();
		double result;
		
		result = invCov.get(0,0)*distPredX*distPredX +
				 2*invCov.get(0,1)*distPredX*distPredY +
				 2*invCov.get(0,2)*distPredX*distTh +
				 invCov.get(1,1)*distPredY*distPredY+
				 2*invCov.get(1,2)*distPredY*distTh +
				 invCov.get(2,2)*distTh*distTh;

		return 0.5*result;
	}
	
	private double [] computeSensorProb(int sensorid, double posX, double posY, double angle, Graphics g, int height) {
		double sensorx = posX + 7.25*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.cos(angle+estTheta);
		double sensory = posY + 7.25*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.sin(angle+estTheta);		
		
		double white = 0;
		double black = 0;
		
		double probDist [] = new double[2];

		/*
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
		
		*/
		
		try {
			MapElement e = element[(int)(sensorx/MapPanel.LENGTHSCALE)][(int)(sensory/MapPanel.LENGTHSCALE)];
			
			if (e.getColor().getRed() == 255 && e.getColor().getGreen() == 255 && e.getColor().getBlue() == 255) white = 1;
			else black = 1;
		}
		catch (ArrayIndexOutOfBoundsException e) {
			// ignore any eventual out of bounds issue
		}
		probDist[0] = white;
		probDist[1] = black;
		
		//((Graphics2D)g).setTransform(tmp);

		return probDist;
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
		
		if (printDebug && sensorProb < 1) {
			System.out.println("EXP LINKS: " + ((sensorMapProbsLeft[0] > 0) ? "weiss" : "schwarz ") + " RECHTS: " + ((sensorMapProbsRight[0] > 0) ? "weiss" : "schwarz") + " PROB: " + sensorProb);
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
		obsX = estPosX + Math.cos(estTheta)*dist;
		obsY = estPosY + Math.sin(estTheta)*dist;
		obsTheta = estTheta + theta;
		/*
		obsX += Math.cos(obsTheta)*dist;
		obsY += Math.sin(obsTheta)*dist;
		obsTheta += theta;
		*/
	}
	
	public double [] getDistVectorTo(MapElement l, double x, double y) {
		double [] res = new double[2];
		
		res[0] = x - (l.getPosX() + 0.5)*MapPanel.LENGTH_EDGE_CM;
		res[1] = y - (l.getPosY() + 0.5)*MapPanel.LENGTH_EDGE_CM;
		
		return res;
	}
	
	public boolean updatePoseGround(List<Short> sensorVal, Thymio myThymio, Graphics g, int height) {
		Rectangle uncertaintyBounds;
		double maxProb;
		double bestX = Double.NaN, bestY = Double.NaN, bestTheta = Double.NaN;
		boolean updated = false;
		ArrayList<Pose> bestPositions;
		int centerX, centerY;

		centerX = (int)(this.getEstimPosX()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE);
		centerY = (int)(height - (this.getEstimPosY()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE));
		
		poseTransform = new AffineTransform();
		poseTransform.translate(this.getEstimPosX()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE,
				height - (this.getEstimPosY()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE));
		
		//poseTransform.rotate(this.getEstimOrientation());
		poseUncertainty = new Ellipse2D.Double(-0.5*MapPanel.LENGTHSCALE, -0.5*MapPanel.LENGTHSCALE, 1*MapPanel.LENGTHSCALE, 1*MapPanel.LENGTHSCALE);
		uncertaintyBounds = poseTransform.createTransformedShape(poseUncertainty).getBounds();
		
		// init maxProb, sensorRot0, and sensorRot1 to values for predicted pose
		
		maxProb = this.computeSensorProb(this.getEstimPosX()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE,
				                          this.getEstimPosY()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE,
				                          0.0,
				                          sensorVal.get(0), 0.155802,
				                          sensorVal.get(1), -0.155802,
				                          g, height, true);

		correcting = (maxProb > 0);
		if (correcting) {
			int lowerx, upperx;
			int lowery, uppery;
			int countx, county;
			
			myThymio.setSpeed((short)0, (short)0, false);
			myThymio.setStopped();
			myThymio.setDriving(false);
			
			System.out.println("IS CORRECTING: " + maxProb + " for THETA: " + this.getEstimOrientation());

			bestPositions = new ArrayList<Pose>();
			updated = false;
			
			if (maxProb < Double.POSITIVE_INFINITY) {
				double posProb = this.posProbability(this.getEstimPosX()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE,
						height - this.getEstimPosY()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE,
						this.getEstimOrientation());
				
				bestPose = new Pose(this.getEstimPosX()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE,
						this.getEstimPosY()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE,
						this.getEstimOrientation(),
						maxProb, posProb);
				bestPositions.add(bestPose);
				
				maxProb += posProb;
			}

			/**
			 * restrict search space for position update according to knowledge from the map (where am I in the map?, which displacement
			 * is plausible which one is not?
			 */
			
			lowerx = (int)uncertaintyBounds.getMinX();
			upperx = (int)uncertaintyBounds.getMaxX();

			lowery = (int)uncertaintyBounds.getMinY();
			uppery = (int)uncertaintyBounds.getMaxY();
			
			/**
			 * exhaustively sample the search space for a better position
			 */
			/*
			try {
				PrintStream gaussLog = new PrintStream(new File("gauss_log.csv"));
				gaussLog.println("x\ty\ttheta\tsensor prob\tpos prob");
			*/
			
			bestPoseAtPos = new Pose[MapPanel.LENGTHSCALE + 1][MapPanel.LENGTHSCALE + 1];

			countx = 0;
			for (int x = lowerx; x < upperx; x ++) {
				county = 0;
				for (int y = lowery; y < uppery; y ++) {
					bestPoseAtPos[countx][county] = null;
					
					for (double dtheta = -ANGLERANGE; dtheta <= ANGLERANGE; dtheta += Math.PI/360) {
						if (Math.sqrt((x-centerX)*(x-centerX)+(y-centerY)*(y-centerY)) < 0.5*MapPanel.LENGTHSCALE) {
							double p =  this.computeSensorProb(x, (double)height - y, dtheta,
									sensorVal.get(0), 0.155802,
									sensorVal.get(1), -0.155802,
									g, height, false);
							double apriori = this.posProbability(x, (double)height - y, dtheta + this.getEstimOrientation());

							//gaussLog.println((x*MapPanel.LENGTH_EDGE_CM/MapPanel.LENGTHSCALE) + "\t" + ((height - y)*MapPanel.LENGTH_EDGE_CM/MapPanel.LENGTHSCALE) + "\t" + (dtheta + this.getEstimOrientation()) + "\t" + p + "\t" + apriori);

							if ((p == 0) && ((p + apriori) < maxProb)) {
								Pose thisPose;
								// update pose if it can better explain the sensor values.

								bestPositions.clear();
								
								updated = true;
								bestX = x;
								bestY = height - y;
								bestTheta = dtheta + this.getEstimOrientation();
								maxProb = p + apriori;

								thisPose = new Pose(bestX*MapPanel.LENGTH_EDGE_CM/MapPanel.LENGTHSCALE, bestY*MapPanel.LENGTH_EDGE_CM/MapPanel.LENGTHSCALE, bestTheta, p, apriori);
								bestPositions.add(thisPose);
								bestPoseAtPos[countx][county] = thisPose;
							}
							else if (((p + apriori) == maxProb) && (maxProb < Double.POSITIVE_INFINITY))
								bestPositions.add(new Pose(bestX*MapPanel.LENGTH_EDGE_CM/MapPanel.LENGTHSCALE,
										          bestY*MapPanel.LENGTH_EDGE_CM/MapPanel.LENGTHSCALE,
										          bestTheta,
										          p, apriori));
							
							if ((bestPoseAtPos[countx][county] == null) || (bestPoseAtPos[countx][county].getEvalPos() > apriori)) {
								bestPoseAtPos[countx][county] = new Pose(x*MapPanel.LENGTH_EDGE_CM/MapPanel.LENGTHSCALE,
										                                 (height-y)*MapPanel.LENGTH_EDGE_CM/MapPanel.LENGTHSCALE,
										                                 dtheta + this.getEstimOrientation(),
										                                 p, apriori);
							}
						}
					}
					
					county ++;
				}
				
				countx ++;
			}
			/*
			gaussLog.close();

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			/**
			 * correct the position given the best estimation in the search space
			 */
			
			if (updated) {
				double currX = this.getEstimPosX();
				double currY = this.getEstimPosX();
				double currTheta = this.getEstimOrientation();
				double diffTheta;
				double orientation;
				double lengthPosDiff;
				double displDir;
				double displSide;
				
				double bestMapX = bestX*MapPanel.LENGTH_EDGE_CM/MapPanel.LENGTHSCALE;
				double bestMapY = bestY*MapPanel.LENGTH_EDGE_CM/MapPanel.LENGTHSCALE;
				
				this.setPose(bestMapX, bestMapY, bestTheta);			
				System.out.println("UPDATE TO POSE: " + bestPositions.get(bestPositions.size()-1));
				bestPose = bestPositions.get(bestPositions.size()-1);

				diffTheta = this.getEstimOrientation() - currTheta;
				orientation = (Math.cos(currTheta)*Math.cos(this.getEstimOrientation()) + Math.sin(currTheta)*Math.sin(this.getEstimOrientation()));
				lengthPosDiff = Math.sqrt((this.getEstimPosX() - currX)*(this.getEstimPosX() - currX) + (this.getEstimPosY() - currY)*(this.getEstimPosY() - currY));
				System.out.print("UPDATE (direction): " + (orientation > 0 ? "forward" : "backward"));
				System.out.print("\t(aside to) : " + (diffTheta > 0 ? "left" : "right"));
				
				displDir = Math.cos(currTheta)*(this.getEstimPosX() - currX) + Math.sin(currTheta)*(this.getEstimPosY() - currY)/lengthPosDiff;
				displSide = -Math.sin(currTheta)*(this.getEstimPosX() - currX) + Math.cos(currTheta)*(this.getEstimPosY() - currY)/lengthPosDiff;
						
				System.out.print("\t(direction): " + (displDir > 0 ? "forward" : "backward"));
				System.out.println("\t(aside to) : " + (displSide > 0 ? "left" : "right"));
			}
		}
		
		return updated;
	}
	
	public Pose [][] getBestPoses() {
		return bestPoseAtPos;
	}
	
	public Pose getBestPose() {
		return bestPose;
	}
	
	public Shape getPoseUncertainty() {
		return poseUncertainty;
	}
	
	public AffineTransform getPoseTransform() {
		return poseTransform;
	}
}
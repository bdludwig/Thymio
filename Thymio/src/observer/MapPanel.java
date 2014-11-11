package observer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import thymio.Thymio;

import context.Map;
import context.MapElement;
import context.Path;

public class MapPanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Map myMap;
	public static final int LENGTHSCALE = 35;
	public static final double LENGTH_EDGE_CM = 17;
	
	private double poseStdX, poseStdY;
	private Ellipse2D.Double poseUncertainty;
	private AffineTransform standardTransf, poseTransform, sensorRot0, sensorRot1;
	private double lastMaxProb;
	private boolean correcting;
	private int correctCycles;
	private double thetaStd;
	private short lastSpeedLeft, lastSpeedRight;
	
	public MapPanel(Map m, JFrame f) {
		myMap = m;
		
		this.setPreferredSize(new Dimension(myMap.getSizeX()*LENGTHSCALE, myMap.getSizeY()*LENGTHSCALE));
		this.setMaximumSize(new Dimension(myMap.getSizeX()*LENGTHSCALE, myMap.getSizeY()*LENGTHSCALE));
		this.setMinimumSize(new Dimension(myMap.getSizeX()*LENGTHSCALE, myMap.getSizeY()*LENGTHSCALE));
		
		poseStdX = poseStdY = 0;
		poseTransform = null;
		sensorRot0 = null;
		sensorRot1 = null;
		lastMaxProb = Double.NaN;
		
		lastSpeedLeft = 0;
		lastSpeedRight = 0;
		correctCycles = 1;
	}

	public Map getMap() {
		return myMap;
	}

	public boolean correctingPosition() {
		return correcting;
	}
	
	public void setPose(double x, double y, double theta) {
		myMap.setPose(x, y, theta);
		this.repaint();
	}
	
	public void updatePose(double dF, double dR, double dFobs, double dRobs, double dt) {
		myMap.updatePose(dF, dR, dFobs, dRobs, dt);
	}
	
	public void setProxHorizontal(List<Short> val) {
		myMap.setProxHorizontal(val);
	}
	
	public void updatePoseGround(List<Short> sensorVal, Thymio myThymio) {
		Rectangle2D uncertaintyBounds;
		double maxProb;
		double bestX = Double.NaN, bestY = Double.NaN, bestTheta = Double.NaN;
		boolean updated;
		boolean inverted;
		
		poseStdX = (myThymio.localizationProblemAhead() ? 2.5 : 1)*correctCycles*Math.sqrt(myMap.getCovariance(0, 0))/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE;
	    poseStdY = (myThymio.localizationProblemAhead() ? 1 : 2)*correctCycles*Math.sqrt(myMap.getCovariance(1, 1))/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE;
	    
		poseTransform = new AffineTransform();
		poseTransform.translate(myMap.getEstimPosX()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE,
				                this.getHeight() - myMap.getEstimPosY()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE);
		poseTransform.rotate(myMap.getEstimOrientation());
		poseUncertainty = new Ellipse2D.Double(-0.5*poseStdX, -0.5*poseStdY, poseStdX, poseStdY);
		
		uncertaintyBounds = poseTransform.createTransformedShape(poseUncertainty).getBounds2D();
		
		// init maxProb, sensorRot0, and sensorRot1 to values for predicted pose
		
		maxProb = myMap.computeSensorProb(myMap.getEstimPosX()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE,
				                          myMap.getEstimPosY()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE,
				                          0.0,
				                          sensorVal.get(0), 0.155802,
				                          sensorVal.get(1), -0.155802,
				                          this.getGraphics(), this.getHeight(), false);
		sensorRot0 = myMap.getSensorRotation(0);
		sensorRot1 = myMap.getSensorRotation(1);

		correcting = (maxProb > 0);
		if (correcting) {
			double lowerx, upperx;
			double lowery, uppery;
			
			thetaStd = (myThymio.localizationProblemAhead() ? 0.25 : 0.33)*correctCycles*Math.sqrt(myMap.getCovariance(2, 2));
			updated = false;
			maxProb = Double.POSITIVE_INFINITY;

			/**
			 * restrict search space for position update according to knowledge from the map (where am I in the map?, which displacement
			 * is plausible which one is not?
			 */
			
			if (myThymio.localizationProblemRight()) lowerx = myMap.getEstimPosX()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE;
			else lowerx = uncertaintyBounds.getMinX();
			
			if (myThymio.localizationProblemLeft()) upperx = myMap.getEstimPosX()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE;
			else upperx = uncertaintyBounds.getMaxX();

			if (myThymio.localizationProblemAhead() || myThymio.localizationProblemLeft() || myThymio.localizationProblemRight()) {
				lowery =  this.getHeight() - myMap.getEstimPosY()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE;
			}
			else lowery = uncertaintyBounds.getMinY();
			
			uppery = uncertaintyBounds.getMaxY();
			
			/**
			 * exhaustively sample the search space for a better position
			 */
			
			for (double x = lowerx; x <= upperx; x ++) {
				for (double y = lowery; y <= uppery; y ++) {
					for (double dtheta = -thetaStd; dtheta <= thetaStd; dtheta += Math.PI/360) {
						if (uncertaintyBounds.contains(x, y)) {
							double p =  myMap.computeSensorProb(x, (double)this.getHeight() - y, dtheta,
									sensorVal.get(0), 0.155802,
									sensorVal.get(1), -0.155802,
									this.getGraphics(), this.getHeight(), false);
							double apriori = myMap.posProbability(x, (double)this.getHeight() - y) +
									myMap.rotationProbability(dtheta + myMap.getEstimOrientation());
							//System.out.println("x: " + x + " y: " + y + " p: " + p + " - apriori pos: " + myMap.posProbability(x, y) + " - apriori rot: " + myMap.rotationProbability(myMap.getEstimOrientation() + dtheta));

							if ((p + apriori) < maxProb) {
								// update pose if it can better explain the sensor values.

								updated = true;
								bestX = x;
								bestY = this.getHeight() - y;
								bestTheta = dtheta + myMap.getEstimOrientation();
								maxProb = p + apriori;
								sensorRot0 = myMap.getSensorRotation(0);
								sensorRot1 = myMap.getSensorRotation(1);
							}
						}
					}
				}
			}

			/**
			 * correct the position given the best estimation in the search space
			 */
			
			if (updated) {
				double currX = myMap.getEstimPosX();
				double currY = myMap.getEstimPosX();
				double currTheta = myMap.getEstimOrientation();
				double diffTheta;
				double orientation;
				double lengthPosDiff;
				double displDir;
				double displSide;
				
				double bestMapX = bestX*MapPanel.LENGTH_EDGE_CM/MapPanel.LENGTHSCALE;
				double bestMapY = bestY*MapPanel.LENGTH_EDGE_CM/MapPanel.LENGTHSCALE;
				
				System.out.println("old position: (" + myMap.getEstimPosX() + "," + myMap.getEstimPosY() + "," + myMap.getEstimOrientation() + ")");
				myMap.setPose(bestMapX, bestMapY, bestTheta);			
				System.out.println("new position: (" + myMap.getEstimPosX() + "," + myMap.getEstimPosY() + "," + myMap.getEstimOrientation() + "):  apriori pos: " + myMap.posProbability(bestX, bestY) + " - apriori rot: " + myMap.rotationProbability(bestTheta));

				System.out.println("direction: " + myThymio.getDirection());
				diffTheta = myMap.getEstimOrientation() - currTheta;
				orientation = (Math.cos(currTheta)*Math.cos(myMap.getEstimOrientation()) + Math.sin(currTheta)*Math.sin(myMap.getEstimOrientation()));
				lengthPosDiff = Math.sqrt((myMap.getEstimPosX() - currX)*(myMap.getEstimPosX() - currX) + (myMap.getEstimPosY() - currY)*(myMap.getEstimPosY() - currY));
				System.out.println("changed orientation (direction): " + (orientation > 0 ? "forward" : "backward"));
				System.out.println("changed orientation (aside to) : " + (diffTheta > 0 ? "left" : "right"));
				
				displDir = Math.cos(currTheta)*(myMap.getEstimPosX() - currX) + Math.sin(currTheta)*(myMap.getEstimPosY() - currY)/lengthPosDiff;
				displSide = -Math.sin(currTheta)*(myMap.getEstimPosX() - currX) + Math.cos(currTheta)*(myMap.getEstimPosY() - currY)/lengthPosDiff;
						
				System.out.println("changed position (direction): " + (displDir > 0 ? "forward" : "backward"));
				System.out.println("changed position (aside to) : " + (displSide > 0 ? "left" : "right"));

				correcting = false;
				correctCycles = 1;
			}
			else correctCycles ++;
		}
	}
	
	public void paint(Graphics g) {
		double angle = myMap.getEstimOrientation();
		double diffSensor = 20.0*Math.PI/180.0;
		double x0, y0;

		int cellx0, celly0;
		
		myMap.clearOccupancy();

		g.setColor(Color.WHITE);
		g.clearRect(0,  0, this.getWidth(), this.getHeight());
	    standardTransf = ((Graphics2D)this.getGraphics()).getTransform();
		
		x0 = myMap.getEstimPosX()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE;
		y0 = myMap.getEstimPosY()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE;
		
		cellx0 = (int)(myMap.getEstimPosX()/MapPanel.LENGTH_EDGE_CM);
		celly0 = (int)(myMap.getEstimPosY()/MapPanel.LENGTH_EDGE_CM);
		
		g.setColor(Color.RED);

		for (int i = 0; i < 5; i++) {
			double sensorVal = myMap.getProxHorizontal(i)/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE;
			int cellx1, celly1;
			
			if (sensorVal < Double.MAX_VALUE) {
				double x1 = x0 + sensorVal*Math.cos(angle+(2-i)*diffSensor);
				double y1 = y0 - sensorVal*Math.sin(-(angle+(2-i)*diffSensor));
				Path p;
				
				g.drawLine((int)(x0), this.getHeight() - (int)(y0),
						   (int)(x1),
						   this.getHeight() - (int)(y1));
				
				cellx1 = (int)(x1/MapPanel.LENGTHSCALE);
				celly1 = (int)(y1/MapPanel.LENGTHSCALE);
				
				p = myMap.followBeam(cellx0, celly0, cellx1, celly1);

				for (MapElement mapElement : p) {
					mapElement.setOnBeam(true);
				}
			}
		}

		for (int x = 0; x < myMap.getSizeX(); x++) {
			for (int y = 0; y < myMap.getSizeY(); y++) {
				int posy = myMap.getSizeY() - y;

				if (myMap.isOnBeam(x, y)) g.setColor(new Color(255, 0, 0, 128));
				else if (myMap.isOccupied(x, y)) g.setColor(Color.RED);					
				else g.setColor(myMap.getElement(x, y).getColor());
				
				if (myMap.getElement(x, y).onPath()) {
					Color c = g.getColor();
					g.setColor(new Color(c.getRed(), 0, 255, 200));
				}
				
				g.fillRect(LENGTHSCALE*x, LENGTHSCALE*(posy-1), LENGTHSCALE, LENGTHSCALE);
				
				if (myMap.getElement(x, y).isGoal()) {
					g.setColor(Color.WHITE);
					g.fillArc((int)(LENGTHSCALE*(x + 0.5) - 4), (int)(LENGTHSCALE*(myMap.getSizeY() - (y + 0.5))  - 4), 8, 8, 0, 360);
				}
			}
		}
		
		int x0blue = (int)(myMap.getPosX()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE);
		int y0blue = (int)(myMap.getPosY()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE);
		
		// draw line for "programmed" orientation, i.e. orientation according to given controls
		
		g.setColor(Color.BLUE);
		g.fillRect(x0blue - 3, this.getHeight() - 3 - y0blue, 6, 6);
		g.drawLine(x0blue, this.getHeight() - y0blue,
				(int)(x0blue + 10*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.cos(myMap.getThymioOrientation())),
                (int)(this.getHeight() - (y0blue + 10*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.sin(myMap.getThymioOrientation()))));
				
		// draw line for estimated orientation
		
		g.setColor(Color.GREEN);	
		g.drawLine((int)x0, this.getHeight() - (int)y0, (int)(x0 + 10*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.cos(angle)),
				                     (int)(this.getHeight() - (y0 + 10*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.sin(angle))));
		
				// draw Ellipse for uncertainty of pose, i.e. main axes of covariance matrix as computed by the Kalman Filter.

		if (poseTransform != null) {
			((Graphics2D)g).setTransform(poseTransform);
			((Graphics2D)g).fill(poseUncertainty);
		}
		
		if (sensorRot0 != null) {
			g.setColor(Color.MAGENTA);
			((Graphics2D)g).setTransform(sensorRot0);
			((Graphics2D)g).fill(myMap.getSensorBoundings());
		}

		if (sensorRot1 != null) {
			g.setColor(Color.MAGENTA);
			((Graphics2D)g).setTransform(sensorRot1);
			((Graphics2D)g).fill(myMap.getSensorBoundings());
		}
		
		((Graphics2D)g).setTransform(standardTransf);
		
		double radius = 7.25*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM;

		g.setColor(Color.YELLOW);
		g.drawLine((int)x0, this.getHeight() - (int)y0, (int)(x0 + radius*Math.cos(angle - thetaStd)),
                (int)(this.getHeight() - (y0 + radius*Math.sin(angle - thetaStd))));
		g.drawLine((int)x0, this.getHeight() - (int)y0, (int)(x0 + radius*Math.cos(angle + thetaStd)),
                (int)(this.getHeight() - (y0 + radius*Math.sin(angle + thetaStd))));
		
		g.setColor(Color.RED);
		
		double ox = myMap.getObsX()*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM;
		double oy = myMap.getObsY()*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM;
		
		g.fillRect((int)(ox) - 2, this.getHeight() - 2 - (int)(oy), 4 ,4);
		g.drawLine((int)(ox), this.getHeight() - (int)(oy),
				   (int)(ox + 10*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.cos(myMap.getObsTheta())),
                   this.getHeight() - (int)(oy + 10*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.sin(myMap.getObsTheta())));
	}
	
	public double getEstimPosX() {
		return myMap.getEstimPosX();
	}
	
	public double getEstimPosY() {
		return myMap.getEstimPosY();
	}
	
	public double getOrientation() {
		return myMap.getEstimOrientation();
	}
	
	public double [] getSensorMapProbsLeft() {
		return myMap.getSensorMapProbsLeft();
	}

	public double [] getSensorMapProbRight() {
		return myMap.getSensorMapProbsRight();
	}
	
	public MapElement getCurrentPos() {
		return myMap.getCurrentPos();
	}
	
	public void observationData(double dist, double theta) {
		myMap.observationData(dist, theta);
	}
	
	public double [] getDistVectorTo(MapElement l, double x, double y) {
		return myMap.getDistVectorTo(l, x, y);
	}
}
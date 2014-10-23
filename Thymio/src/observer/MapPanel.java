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
	
	public MapPanel(Map m, JFrame f) {
		myMap = m;
		
		this.setPreferredSize(new Dimension(myMap.getSizeX()*LENGTHSCALE, myMap.getSizeY()*LENGTHSCALE));
		this.setMaximumSize(new Dimension(myMap.getSizeX()*LENGTHSCALE, myMap.getSizeY()*LENGTHSCALE));
		this.setMinimumSize(new Dimension(myMap.getSizeX()*LENGTHSCALE, myMap.getSizeY()*LENGTHSCALE));
		
		poseStdX = poseStdY = 0;
		poseTransform = null;
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
	
	public void updatePoseGround(List<Short> sensorVal, double rotStd) {
		Rectangle2D uncertaintyBounds;
		double maxProb;
		double bestX = Double.NaN, bestY = Double.NaN, bestTheta = Double.NaN;
		
		poseStdX = Math.sqrt(myMap.getCovariance(0, 0))/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE;
	    poseStdY = Math.sqrt(myMap.getCovariance(1, 1))/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE;
	    
	    standardTransf = ((Graphics2D)this.getGraphics()).getTransform();
		poseTransform = new AffineTransform();
		poseTransform.translate(myMap.getEstimPosX()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE,
				                this.getHeight() - myMap.getEstimPosY()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE);
		poseTransform.rotate(myMap.getEstimOrientation()+Math.PI/2);
		poseUncertainty = new Ellipse2D.Double(-0.5*poseStdX, -0.5*poseStdY, poseStdX, poseStdY);
		
		((Graphics2D)this.getGraphics()).setTransform(poseTransform);
		((Graphics2D)this.getGraphics()).fill(poseUncertainty);
		
		uncertaintyBounds = poseTransform.createTransformedShape(poseUncertainty).getBounds2D();
		maxProb = Double.MIN_VALUE;

		for (double dtheta = myMap.getEstimOrientation() - 0.5*rotStd; dtheta <= myMap.getEstimOrientation() + 0.5*rotStd; dtheta += Math.PI/360) {
			for (double x = uncertaintyBounds.getMinX(); x <= uncertaintyBounds.getMaxX(); x ++) {
				for (double y = uncertaintyBounds.getMinY(); y <= uncertaintyBounds.getMinY(); y ++) {
					double p =  myMap.computeSensorProb(x, (double)this.getHeight() - y, dtheta, sensorVal.get(0), 0.155802, sensorVal.get(1), -0.155802, this.getGraphics(), this.getHeight());

					System.out.println(myMap.getEstimPosX() + "/" + myMap.getEstimPosY() + "|" + x + "/" + y);
					if (p > maxProb) {
						bestX = x;
						bestY = this.getHeight() - y;
						bestTheta = dtheta;
						maxProb = p;
						sensorRot0 = myMap.getSensorRotation(0);
						sensorRot1 = myMap.getSensorRotation(1);
						
						System.out.println("new best position: (" + bestX + "," + bestY + "): " + maxProb);
					}
				}
			}
		}
		
		if (maxProb > Double.MIN_VALUE) {
			System.out.println("new pose: " + (bestX*MapPanel.LENGTH_EDGE_CM/MapPanel.LENGTHSCALE) + "/" + (bestY*MapPanel.LENGTH_EDGE_CM/MapPanel.LENGTHSCALE) + "/" + bestTheta);
			myMap.setPose(bestX*MapPanel.LENGTH_EDGE_CM/MapPanel.LENGTHSCALE, bestY*MapPanel.LENGTH_EDGE_CM/MapPanel.LENGTHSCALE, bestTheta);			
		}

		((Graphics2D)this.getGraphics()).setTransform(standardTransf);
	}
	
	public void paint(Graphics g) {
		double angle = myMap.getEstimOrientation();
		double diffSensor = 20.0*Math.PI/180.0;
		double x0, y0;

		int cellx0, celly0;
		
		myMap.clearOccupancy();

		g.setColor(Color.WHITE);
		g.clearRect(0,  0, this.getWidth(), this.getHeight());
		
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

				if (myMap.isOnBeam(x,y)) g.setColor(new Color(255, 0, 0, 128));
				else if (myMap.isOccupied(x, y)) g.setColor(Color.RED);					
				else if (myMap.getElement(x, y).getColor() != Color.WHITE) g.setColor(myMap.getElement(x, y).getColor());
				
				g.fillRect(LENGTHSCALE*x, LENGTHSCALE*(posy-1), LENGTHSCALE, LENGTHSCALE);
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
			((Graphics2D)g).transform(poseTransform);
			((Graphics2D)g).fill(poseUncertainty);
			((Graphics2D)g).setTransform(standardTransf);
		}
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
}

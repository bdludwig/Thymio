package observer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import thymio.Thymio;
import context.Map;
import context.MapElement;
import context.Path;

public class MapPanel extends JPanel implements Runnable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Map myMap;
	private Thymio myThymio;
	
	public static final int LENGTHSCALE = 35;
	public static final double LENGTH_EDGE_CM = 17;
	
	private AffineTransform standardTransf;
	
	public MapPanel(Map m, JFrame f) {
		myMap = m;
		
		this.setPreferredSize(new Dimension(myMap.getSizeX()*LENGTHSCALE, myMap.getSizeY()*LENGTHSCALE));
		this.setMaximumSize(new Dimension(myMap.getSizeX()*LENGTHSCALE, myMap.getSizeY()*LENGTHSCALE));
	}

	public void setThymio(Thymio t) {
		myThymio = t;
	}
	
	public Map getMap() {
		return myMap;
	}

	public void setPose(double x, double y, double theta) {
		myMap.setPose(x, y, theta);
		this.repaint();
	}
	
	public void updatePose(double dF, double dR, double dFobs, double dRobs, double dt, int state) {
		//System.out.println(this.getClass().getName() + ": " + dR);
		myMap.updatePose(dF, dR, dFobs, dRobs, dt, state);
	}
	
	public void setProxHorizontal(List<Short> val) {
		myMap.setProxHorizontal(val);
	}
	
	public boolean updatePoseGround(List<Short> sensorVal, Thymio myThymio) {
		return myMap.updatePoseGround(sensorVal, myThymio, this.getGraphics(), this.getHeight());
	}
	
	public void paint(Graphics g) {
		double angle = myMap.getEstimOrientation();
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
				double x1 = x0 + sensorVal*Math.cos(angle+(2-i)*Thymio.DIFFSENSOR);
				double y1 = y0 - sensorVal*Math.sin(-(angle+(2-i)*Thymio.DIFFSENSOR));
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

		if (myMap.getPoseTransform() != null && myMap.getPoseUncertainty() != null) {
			g.setColor(new Color(0, 255, 0, 100));
			((Graphics2D)g).setTransform(myMap.getPoseTransform());
			((Graphics2D)g).fill(myMap.getPoseUncertainty());
			((Graphics2D)g).setTransform(standardTransf);
		}

		double [] probs = myMap.getSensorMapProbsLeft();
		if (probs != null) {
			float colorProb = ((float)probs[0]);
			g.setColor(new Color(colorProb, colorProb, colorProb));

			double sensorx = x0 + 7.25*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.cos(angle+0.155802);
			double sensory = y0 + 7.25*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.sin(angle+0.155802);			
			g.fillOval((int)sensorx - 2, this.getHeight() - 2 - (int)sensory, 4, 4);
		}

		probs = myMap.getSensorMapProbsRight();
		if (probs != null) {
			float colorProb = ((float)probs[0]);
			g.setColor(new Color(colorProb, colorProb, colorProb));

			double sensorx = x0 + 7.25*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.cos(angle-0.155802);
			double sensory = y0 + 7.25*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.sin(angle-0.155802);	
			g.fillOval((int)sensorx - 2, this.getHeight() - 2 - (int)sensory, 4, 4);
		}

		double radius = 7.25*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM;

		g.setColor(Color.MAGENTA);
		g.drawLine((int)x0, this.getHeight() - (int)y0, (int)(x0 + radius*Math.cos(angle - Map.ANGLERANGE)),
				(int)(this.getHeight() - (y0 + radius*Math.sin(angle - Map.ANGLERANGE))));
		g.drawLine((int)x0, this.getHeight() - (int)y0, (int)(x0 + radius*Math.cos(angle + Map.ANGLERANGE)),
                (int)(this.getHeight() - (y0 + radius*Math.sin(angle + Map.ANGLERANGE))));
		
		/*
		 * visualize odometry data
		 * 
		g.setColor(Color.RED);
		
		double ox = myMap.getObsX()*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM;
		double oy = myMap.getObsY()*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM;
		
		g.fillRect((int)(ox) - 2, this.getHeight() - 2 - (int)(oy), 4 ,4);
		g.drawLine((int)(ox), this.getHeight() - (int)(oy),
				   (int)(ox + 10*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.cos(myMap.getObsTheta())),
                   this.getHeight() - (int)(oy + 10*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.sin(myMap.getObsTheta())));
        */
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

	public double [] getSensorMapProbsRight() {
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
	
	public int getMinSensorId() {
		return myMap.getMinSensorId();
	}
	
	public double getMinSensorDist() {
		return myMap.getMinSensorDist();
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(Thymio.UPDATE_INTERVAL);

				synchronized (this) {
					this.repaint();
					myThymio.getInterface().repaint();
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
}
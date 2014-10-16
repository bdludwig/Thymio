package observer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
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
	
	public MapPanel(Map m, JFrame f) {
		myMap = m;
		
		this.setPreferredSize(new Dimension(myMap.getSizeX()*LENGTHSCALE, myMap.getSizeY()*LENGTHSCALE));
		this.setMaximumSize(new Dimension(myMap.getSizeX()*LENGTHSCALE, myMap.getSizeY()*LENGTHSCALE));
		this.setMinimumSize(new Dimension(myMap.getSizeX()*LENGTHSCALE, myMap.getSizeY()*LENGTHSCALE));
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
	
	public void paint(Graphics g) {
		double angle = myMap.getEstimOrientation();
		double diffSensor = 20.0*Math.PI/180.0;
		double x0, y0;
		double stdX = Math.sqrt(myMap.getCovariance(0, 0))/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE,
			   stdY = Math.sqrt(myMap.getCovariance(1, 1))/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE;
		int cellx0, celly0;
		AffineTransform tmp, rot;
		
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

		/*
		for (int i = 1; i < myMap.getSizeX(); i++) g.drawLine(LENGTHSCALE *i-1, 0, LENGTHSCALE * i-1, this.getHeight());
		for (int i = 1; i < myMap.getSizeY(); i++) g.drawLine(0, LENGTHSCALE * i-1, this.getWidth(), LENGTHSCALE * i-1);
		*/
		for (int x = 0; x < myMap.getSizeX(); x++) {
			for (int y = 0; y < myMap.getSizeY(); y++) {
				/*
				if (x == myMap.getThymioX() && y == myMap.getThymioY()) {
					int [] rotX = new int[3];
					int [] rotY = new int[3];
					
					double endX = LENGTHSCALE*(x+Math.cos(angle));
					double endY = LENGTHSCALE*(y+Math.sin(angle));
					
					g.setColor(Color.BLUE);
					rotX[0]= (int)endX;
					rotY[0]= (int) Math.round(this.getHeight()-endY-0.5*LENGTHSCALE);

					endX = LENGTHSCALE*(x+0.5*Math.cos(-Math.PI/2+angle));
					endY = LENGTHSCALE*(y+0.5*Math.sin(angle-Math.PI/2));
					rotX[1]= (int)endX;
					rotY[1]= (int)Math.round(this.getHeight()-endY-0.5*LENGTHSCALE);

					endX = LENGTHSCALE*(x+0.5*Math.cos(Math.PI/2+angle));
					endY = LENGTHSCALE*(y+0.5*Math.sin(Math.PI/2+angle));
					rotX[2]= (int)endX;
					rotY[2]= (int)Math.round(this.getHeight()-endY-0.5*LENGTHSCALE);
					
					g.fillPolygon(rotX, rotY, 3);
				}
				else */
				if (myMap.isOnBeam(x,y)) {
					int posy = myMap.getSizeY() - y;

					g.setColor(new Color(255, 0, 0, 128));
					g.fillRect(LENGTHSCALE*x, LENGTHSCALE*(posy-1), LENGTHSCALE, LENGTHSCALE);					
				}
				else if (myMap.getElement(x, y).getColor() != Color.WHITE) {
					int posy = myMap.getSizeY() - y;

					g.setColor(myMap.getElement(x, y).getColor());
					g.fillRect(LENGTHSCALE*x, LENGTHSCALE*(posy-1), LENGTHSCALE, LENGTHSCALE);
				}
			}
		}
		
		int x0blue = (int)(myMap.getPosX()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE);
		int y0blue = (int)(myMap.getPosY()/MapPanel.LENGTH_EDGE_CM*MapPanel.LENGTHSCALE);
		
		int px, py;
		g.setColor(Color.BLUE);
		g.fillRect(x0blue - 3, this.getHeight() - 3 - y0blue, 6, 6);
		g.drawLine(x0blue, this.getHeight() - y0blue,
				(int)(x0blue + 10*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.cos(myMap.getThymioOrientation())),
                (int)(this.getHeight() - (y0blue + 10*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.sin(myMap.getThymioOrientation()))));
		
		g.setColor(Color.MAGENTA);
		
		px = (int)(x0 + 7.25*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.cos(angle+0.155802));
		py = (int)(y0 + 7.25*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.sin(angle+0.155802));
		
		System.out.println((int)(px/MapPanel.LENGTHSCALE) + "/" + (int)(py/MapPanel.LENGTHSCALE));
		g.drawLine((int)x0, this.getHeight() - (int)y0, px, this.getHeight() - (int)py);
		
		px = (int)(x0 + 7.25*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.cos(angle-0.155802));
		py = (int)(y0 + 7.25*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.sin(angle-0.155802));
		
		System.out.println((int)(px/MapPanel.LENGTHSCALE) + "/" + (int)(py/MapPanel.LENGTHSCALE));
		g.drawLine((int)x0, this.getHeight() - (int)y0, px, this.getHeight() - (int)py);
		
		g.setColor(Color.GREEN);	
		g.drawLine((int)x0, this.getHeight() - (int)y0, (int)(x0 + 10*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.cos(angle)),
				                     (int)(this.getHeight() - (y0 + 10*MapPanel.LENGTHSCALE/MapPanel.LENGTH_EDGE_CM*Math.sin(angle))));
		tmp = ((Graphics2D)g).getTransform();
		rot = new AffineTransform();
		rot.rotate(angle+Math.PI/2, x0, this.getHeight() - y0);
		((Graphics2D)g).transform(rot);
		((Graphics2D)g).fill(new Ellipse2D.Double(x0 - 0.5*stdX, this.getHeight() - y0 - 0.5*stdY,
				  									   stdX,
				                                       stdY));
		((Graphics2D)g).transform(tmp);
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

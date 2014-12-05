package observer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

import math.Pose;

import context.Map;

public abstract class BeliefPanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected Map myMap;
	protected int CELL_EXT;
	
	public BeliefPanel() {
		myMap = null;
	}
	
	public BeliefPanel(Map m) {
		myMap = m;
		CELL_EXT = 10;

		this.setMinimumSize(new Dimension(MapPanel.LENGTHSCALE*CELL_EXT, MapPanel.LENGTHSCALE*CELL_EXT));
		this.setPreferredSize(new Dimension(MapPanel.LENGTHSCALE*CELL_EXT, MapPanel.LENGTHSCALE*CELL_EXT));
	}
	
	protected void drawProb(int x, int y, double eval, Graphics g) {
		g.setColor(new Color((float)eval, (float)eval, (float)eval));
		g.fillRect(CELL_EXT*x, this.getHeight() - CELL_EXT*(y + 1), CELL_EXT, CELL_EXT);
	}
	
	protected void drawTheta(int x, int y, Pose p, Graphics g) {
		if ((p != null) && (p.getEvalSensor() == 0)){
			g.setColor(Color.RED);
			g.drawLine((int)(CELL_EXT*(x + 0.5 - 0.5*Math.cos(p.getTheta()))),
					   this.getHeight() - (int)(CELL_EXT*(y + 0.5 - 0.5*Math.sin(p.getTheta()))),
					   (int)(CELL_EXT*(x + 0.5 + 0.5*Math.cos(p.getTheta()))),
					   this.getHeight() - (int)(CELL_EXT*(y + 0.5 + 0.5*Math.sin(p.getTheta()))));
		}
	}
}

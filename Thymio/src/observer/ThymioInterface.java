package observer;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import thymio.Thymio;

import main.MainController;


public class ThymioInterface extends JFrame {
	private double [] leftValueProbs;
	private double [] rightValueProbs;
	
	private static final long serialVersionUID = 1L;
	private ThymioPanel myPanel;
	
	public ThymioInterface(Thymio c) {
		super("Thymio");

		myPanel = new ThymioPanel(c, this);
		this.setContentPane(myPanel);
		this.pack();
		this.setVisible(true);
	}

	public int getOrientation() {
		return myPanel.getOrientation();
	}
	
	public int getVForward() {
		return myPanel.getVForward();
	}
	
	public void setRotationSpeed(double s) {
		myPanel.setRotationSpeed((int)s);
	}	
	
	public void setForwardSpeed(double s) {
		myPanel.setForwardSpeed((int)s);
	}
	
	public void updateSensorView(List<Short> sensorData, double [] mapProbsLeft, double [] mapProbsRight) {
		short val;
		
		leftValueProbs = new double[2];
		val = sensorData.get(0).shortValue();
		leftValueProbs[0] = (val > 300) ? 1 : 0;
		leftValueProbs[1] = 1 - leftValueProbs[0];
		
		myPanel.setLeftValueProbs(leftValueProbs);

		rightValueProbs = new double[2];		
		val = sensorData.get(1).shortValue();
		rightValueProbs[0] = (val > 300) ? 1 : 0;
		rightValueProbs[1] = 1 - rightValueProbs[0];
		
		myPanel.setRightValueProbs(rightValueProbs);
		
		myPanel.setLeftMapProbs(mapProbsLeft);
		myPanel.setRightMapProbs(mapProbsRight);
		
		this.repaint();
	}
	
	public double [] getLeftValueProbs() {
		return leftValueProbs;
	}
	
	public double [] getRightValueProbs() {
		return rightValueProbs;
	}
}

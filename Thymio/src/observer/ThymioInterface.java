package observer;

import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

import thymio.Thymio;

public class ThymioInterface extends JFrame {
	private double [] leftValueProbs;
	private double [] rightValueProbs;
	
	private static final long serialVersionUID = 1L;
	private ThymioPanel myPanel;
	
	public ThymioInterface(Thymio c, PositionBeliefPanel bp, SensorBeliefPanel sp, EvalBeliefPanel ep) {
		super("Thymio");
		
		JPanel j = new JPanel();
		j.setLayout(new BoxLayout(j, BoxLayout.Y_AXIS));
		JPanel beliefPanel = new JPanel();
		beliefPanel.setLayout(new BoxLayout(beliefPanel, BoxLayout.X_AXIS));
		beliefPanel.add(bp);
		beliefPanel.add(sp);
		beliefPanel.add(ep);


		myPanel = new ThymioPanel(c, this);
		j.add(myPanel);
		j.add(beliefPanel);
		
		this.setContentPane(j);
		this.setLocation(500,0);
		this.pack();
		this.setPreferredSize(this.getSize());
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
	
	public boolean localizationProblemLeft() {
		return myPanel.localizationProblemLeft();
	}	

	public boolean localizationProblemRight() {
		return myPanel.localizationProblemRight();
	}
	
	public boolean localizationProblemAhead() {
		return myPanel.localizationProblemAhead();
	}
	
	public double [] getLeftValueProbs() {
		return leftValueProbs;
	}
	
	public double [] getRightValueProbs() {
		return rightValueProbs;
	}
}

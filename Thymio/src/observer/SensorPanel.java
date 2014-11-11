package observer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JPanel;

public class SensorPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private double [] probsSensorValueLeft;
	private double [] probsSensorValueRight;
	private double [] probsMapLeft;
	private double [] probsMapRight;
	
	public SensorPanel() {
		this.setMinimumSize(new Dimension(200, 200));
		this.setPreferredSize(new Dimension(200, 200));
	}
	
	public void setLeftValueProbs(double [] p) {
		probsSensorValueLeft = p;
	}
	
	public void setRightValueProbs(double [] p) {
		probsSensorValueRight = p;
	}

	public void setLeftMapProbs(double [] p) {
		probsMapLeft = p;
	}
	
	public void setRightMapProbs(double [] p) {
		probsMapRight = p;
	}

	public void paint(Graphics g) {
		g.clearRect(0, 0, this.getWidth(), this.getHeight());

		if (probsSensorValueLeft != null && probsSensorValueRight != null) {
			if (probsSensorValueLeft[0] > probsSensorValueLeft[1]) g.setColor(Color.WHITE);
			else g.setColor(Color.BLACK);

			g.fillRect(0, 0, this.getWidth()/2, this.getHeight()/2);

			if (probsSensorValueRight[0] > probsSensorValueRight[1]) g.setColor(Color.WHITE);
			else g.setColor(Color.BLACK);

			g.fillRect(this.getWidth()/2+1, 0, this.getWidth()/2, this.getHeight()/2);
		}

		if (probsMapLeft != null && probsMapRight != null) {
			if (probsMapLeft[0] > probsMapLeft[1]) g.setColor(Color.WHITE);
			else g.setColor(Color.BLACK);


			g.fillRect(0, this.getHeight()/2+1, this.getWidth()/2, this.getHeight()/2);

			if (probsMapRight[0] > probsMapRight[1]) g.setColor(Color.WHITE);
			else g.setColor(Color.BLACK);

			g.fillRect(this.getWidth()/2+1, this.getHeight()/2+1, this.getWidth()/2, this.getHeight()/2);
		}
	}
	
	public boolean localizationProblemAhead() {
		if (probsMapLeft != null && probsSensorValueLeft != null)
			return (probsMapLeft[0] != probsSensorValueLeft[0]) && (probsMapRight[0] != probsSensorValueRight[0]);
		else
			return false;
	}

	
	public boolean localizationProblemLeft() {
		if (probsMapLeft != null && probsSensorValueLeft != null)
			return (probsMapLeft[0] != probsSensorValueLeft[0]) && (probsMapRight[0] == probsSensorValueRight[0]);
		else
			return false;
	}
	
	
	public boolean localizationProblemRight() {
		if (probsMapLeft != null && probsSensorValueLeft != null)
			return (probsMapLeft[0] == probsSensorValueLeft[0]) && (probsMapRight[0] != probsSensorValueRight[0]);
		else
			return false;
	}
}

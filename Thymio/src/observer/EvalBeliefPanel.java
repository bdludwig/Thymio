package observer;

import java.awt.Color;
import java.awt.Graphics;

import math.Pose;
import context.Map;

public class EvalBeliefPanel extends BeliefPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public EvalBeliefPanel() {
		super();
	}
	
	public EvalBeliefPanel(Map m) {
		super(m);
	}

	public void paint(Graphics g) {
		Pose [][] bestPoses = myMap.getBestPoses();
		Pose best = myMap.getBestPose();
		
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, this.getWidth(), this.getHeight());
		
		if (bestPoses == null || best == null) return;
		
		for (int x = 0; x < MapPanel.LENGTHSCALE; x ++) {
			for (int y = 0; y < MapPanel.LENGTHSCALE; y ++) {
				Pose p = bestPoses[x][y];
				double eval;
				
				if (p == null) eval = 0;
				else {
					eval = Math.exp(-p.getEval()/best.getEval());
					//System.out.println(this.getClass().getName() + ": " + eval);
				}
				
				drawProb(x, y, eval, g);
				drawTheta(x, y, p, g);
			}
		}
	}
}

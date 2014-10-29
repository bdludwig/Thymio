package observer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import thymio.Thymio;

import context.Map;
import context.MapElement;

public class ExperimentPanel extends JPanel implements ActionListener {
	private static final long serialVersionUID = 1L;
	private final int numObstacles = 10;
	
	private Map myMap;
	private Thymio myThymio;
	
	private JFrame myFrame;
	private JButton setObstacles;
	private JButton startRun;
	
	public ExperimentPanel(Map m, Thymio t, JFrame f) {
		myMap = m;
		myFrame = f;
		myThymio = t;
		
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		setObstacles = new JButton("Random Obstacles");
		startRun = new JButton("Ready, Set, Go ...");
		
		startRun.addActionListener(this);
		setObstacles.addActionListener(this);
		
		this.add(startRun);
		this.add(setObstacles);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == setObstacles) {
			populateMapWithObstacles();
		}
		else if (e.getSource() == startRun) {
			myThymio.driveAstarPath();
		}
	}	
	
	private void populateMapWithObstacles() {
		int dimX = myMap.getSizeX();
		int dimY = myMap.getSizeY();
		int obstaclesToGo = numObstacles;
		Random r = new Random();
		
		for (int rx = 0; rx < dimX; rx ++) {
			for(int ry = 0; ry < dimY; ry ++) {
				myMap.getElement(rx, ry).setOccupied(false);
			}
		}
		
		while (obstaclesToGo > 0) {
			int rx = r.nextInt(dimX);
			int ry = r.nextInt(dimY);
			MapElement e = myMap.getElement(rx, ry);
			
			// no obstacle on target location
			
			if ((rx == dimX - 1) && (ry == dimY - 1)) continue;
			
			if (!e.isOccupied()) {
				e.setOccupied(true);
				obstaclesToGo --;
			}
		}
		
		myFrame.repaint();
	}
}
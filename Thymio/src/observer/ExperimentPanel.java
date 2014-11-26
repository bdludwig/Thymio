package observer;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import main.Pathfinder;

import thymio.Thymio;
import thymio.ThymioNavigatingThread;
import context.Map;
import context.MapElement;

public class ExperimentPanel extends JPanel implements ActionListener {
	private static final long serialVersionUID = 1L;
	private final int numObstacles = 10;
	
	private Map myMap;
	private Thymio myThymio;
	private MapPanel mapPanel;
	
	private JFrame myFrame;
	private JButton setObstacles;
	private JButton startRun;
	private JButton pause;
	
	private ArrayList <String> occupied;
	
	public ExperimentPanel(Map m, Thymio t, MapPanel p, JFrame f) {
		myMap = m;
		mapPanel = p;
		myFrame = f;
		myThymio = t;
		
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		setObstacles = new JButton("Set Map");
		startRun = new JButton("Ready, Set, Go!");
		pause = new JButton("Wait!");
		
		startRun.addActionListener(this);
		setObstacles.addActionListener(this);
		pause.addActionListener(this);
		
		this.add(setObstacles);
		this.add(startRun);
		this.add(pause);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == setObstacles) {
			try {
				populateMapWithObstacles();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		else if (e.getSource() == startRun) {
			driveAstarPath();
		}
		else if (e.getSource() == pause) {
			if (myThymio.isPaused()) {
				myThymio.setPause(false);
				pause.setText("Wait!");
			}
			else {
				myThymio.setPause(true);
				pause.setText("Go on!");
			}
			
			this.invalidate();
		}
	}	
	
	private void driveAstarPath() {
		Pathfinder myPath = new Pathfinder(mapPanel.getMap(),
				                           mapPanel.getCurrentPos(),
				                           mapPanel.getMap().getElement(mapPanel.getMap().getSizeX() - 1, mapPanel.getMap().getSizeY() - 2));
		ArrayList<MapElement> solution;
		
		myPath.findPath();
		solution = myPath.getSolution();
		
		if (solution == null) System.out.println("NO PATH");
		else if (solution.size() > 0) {
			new ThymioNavigatingThread(myThymio, mapPanel, solution).start();
		}
		// else do nothing.
	}
	private void populateMapWithObstacles() throws IOException {
		int dimX = myMap.getSizeX();
		int dimY = myMap.getSizeY();
		int obstaclesToGo = numObstacles;
		occupied = new ArrayList <String>();
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
				occupied.add(rx+","+ry);
			}
		}
		
		myFrame.repaint();

		writeOccupied();
	}
	
	private void writeOccupied() throws IOException {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Specify a file to save");

		int userSelection = fileChooser.showSaveDialog(this);
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File fileToSave = fileChooser.getSelectedFile();

			FileWriter writer = null;

			writer = new FileWriter(fileToSave,true);

			for(String s : occupied){

				writer.append(s+"\n");
				writer.flush();


			}
			writer.close();
			System.out.println("Save as file: " + fileToSave.getAbsolutePath());
		}
	}
}

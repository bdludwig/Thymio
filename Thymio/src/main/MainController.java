package main;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

import observer.ExperimentPanel;
//import observer.ExperimentPanel;
import observer.MapPanel;
import observer.ThymioInterface;
import thymio.Thymio;
import context.Map;

public class MainController extends JFrame {
	private static final long serialVersionUID = 1L;
	private ThymioInterface observer;
	private Map myMap;
	private Thymio myThymio;
	private MapPanel myPanel;
	private ExperimentPanel exPanel;
	private JPanel box;
	
	public static final int MAPSIZE_X = 9;
	public static final int MAPSIZE_Y = 21;
	
	public MainController(String host) {
		super("Map");
		
		myMap = new Map(MAPSIZE_X, MAPSIZE_Y, MapPanel.LENGTH_EDGE_CM);
		myPanel = new MapPanel(myMap, this);
		myThymio = new Thymio(myPanel, host);
		observer = myThymio.getInterface();
		exPanel = new ExperimentPanel(myMap, myThymio, this);
	}

	public void init() {
		box = new JPanel();
		box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
		box.add(myPanel);
		box.add(exPanel);
		
		myPanel.setPose(7*myMap.getEdgeLength(), 1*myMap.getEdgeLength(), 0);
		
		this.setContentPane(box);
		this.pack();
		this.setVisible(true);
	}
	
	public void run() {
		myPanel.repaint();
		/*
		myThymio.setSpeed((short)-150, (short)150);
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		myThymio.setSpeed((short)0, (short)0);
	}

	public static void main(String [] args) {
		if (args.length == 1) {
			MainController mc = new MainController(args[0]);
		
				mc.init();
				mc.run();
		}
		else System.out.println("USAGE: MainController <HOSTNAME>");
	}
}

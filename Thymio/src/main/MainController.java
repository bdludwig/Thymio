package main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Random;

import javax.swing.JFrame;


import context.Map;
import context.Path;

import observer.MapPanel;
import observer.ThymioInterface;
import thymio.Thymio;

public class MainController extends JFrame {
	private static final long serialVersionUID = 1L;
	private ThymioInterface observer;
	private Map myMap;
	private Thymio myThymio;
	private MapPanel myPanel;
	
	public static final int MAPSIZE_X = 9;
	public static final int MAPSIZE_Y = 21;
	
	public MainController() {
		super("Map");
		
		myMap = new Map(MAPSIZE_X, MAPSIZE_Y, MapPanel.LENGTH_EDGE_CM);
		myPanel = new MapPanel(myMap, this);
		myThymio = new Thymio(myPanel);
		observer = myThymio.getInterface();
	}
	
	public void init() {		
		myPanel.setPose(7*myMap.getEdgeLength(), 1*myMap.getEdgeLength(), 0);
		
		this.setContentPane(myPanel);
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
		MainController mc = new MainController();
		
		mc.init();
		
		mc.run();
	}
}

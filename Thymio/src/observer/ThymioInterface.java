package observer;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;

import thymio.Thymio;

import main.MainController;


public class ThymioInterface extends JFrame {
	/**
	 * 
	 */
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
}

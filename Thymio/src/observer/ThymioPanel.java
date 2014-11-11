package observer;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.BoxLayout;

import main.Pathfinder;
import thymio.Thymio;

public class ThymioPanel extends JPanel implements ChangeListener, KeyListener, ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Thymio myThymio;
	private ThymioInterface myUI;
	private JSlider vForward, theta;
	private JLabel valVelocity, valTheta;
	private JButton stop, leftTurn, rightTurn, ahead, back;
	private SensorPanel mySensorView;
	
	public ThymioPanel(Thymio t, ThymioInterface ui) {
		myThymio = t;
		myUI = ui;
		
		initUI();
		this.addKeyListener(this);
        this.setFocusable(true);
        this.requestFocusInWindow();
	}
	
	private void initUI() {
		JPanel buttonPanel = new JPanel();
		
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		
		vForward = new JSlider(JSlider.HORIZONTAL, -(short)(Thymio.MAXSPEED/(10*Thymio.SPEEDCOEFF)), (short)(Thymio.MAXSPEED/(10*Thymio.SPEEDCOEFF)), 0);
		vForward.addChangeListener(this);

		//Turn on labels at major tick marks.
		vForward.setMajorTickSpacing(10);
		vForward.setMinorTickSpacing(1);
		vForward.setPaintTicks(true);
		//vForward.setPaintLabels(true);

		theta = new JSlider(JSlider.HORIZONTAL, -90, 90, 0);
		theta.addChangeListener(this);

		//Turn on labels at major tick marks.
		theta.setMajorTickSpacing(10);
		theta.setMinorTickSpacing(1);
		theta.setPaintTicks(true);
		//vRight.setPaintLabels(true);
		
		valVelocity = new JLabel("velocity (cm/sec): " + vForward.getValue());
		valTheta = new JLabel("turn angle (degree): " + theta.getValue());
		stop = new JButton("STOP");
		stop.addActionListener(this);
		leftTurn = new JButton("LEFT");
		leftTurn.addActionListener(this);
		rightTurn = new JButton("RIGHT");
		rightTurn.addActionListener(this);
		ahead = new JButton("FIELD AHEAD");
		ahead.addActionListener(this);
		back = new JButton("FIELD BACK");
		back.addActionListener(this);
		
		/* moved Button for running Thymio with A*
		astern = new JButton("A* Start");
		astern.addActionListener(this);
		*/
		
		this.add(valVelocity);
		this.add(valTheta);
		this.add(new JLabel("Rotation Speed:"));
		this.add(theta);
		this.add(new JLabel("Forward Speed:"));
		this.add(vForward);
		
		buttonPanel.add(stop);
		buttonPanel.add(leftTurn);
		buttonPanel.add(rightTurn);
		buttonPanel.add(ahead);
		buttonPanel.add(back);		
		
		this.add(buttonPanel);
		
		mySensorView = new SensorPanel();
		this.add(mySensorView);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		
		// TODO Auto-generated method stub
	
		if (e.getSource().equals(vForward)) {
			valVelocity.setText("velocity (cm/sec): " + vForward.getValue());
			updateThymio();
		}
		else if(e.getSource().equals(theta)) {
			/*
			if (!theta.getValueIsAdjusting()) {
				updateThymio();
			}
			*/
			valTheta.setText("turn angle (degree): " + theta.getValue());
			updateThymio();

		}

		myUI.repaint();
		if(!this.isFocusOwner()) this.requestFocus();
	}
	
	private void updateThymio() {
		double v = ((double)vForward.getValue()*Thymio.SPEEDCOEFF*10);
		double angle = -0.5*(Math.PI/180.0*theta.getValue())*Thymio.BASE_WIDTH*Thymio.SPEEDCOEFF;
		double k = Math.max(Math.abs(v-angle), Math.abs(v+angle));
		
		if (k > Thymio.MAXSPEED) {
			v = v*Thymio.MAXSPEED/k;
			angle = angle*Thymio.MAXSPEED/k;
		}

		synchronized (myThymio) {
			try {
				myThymio.wait();
				myThymio.setSpeed((short)(v-angle), (short)(v+angle));
				myThymio.updatePose(System.currentTimeMillis());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public int getOrientation() {
		return -theta.getValue();
	}
	
	public int getVForward() {
		return vForward.getValue();
	}
	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		
		switch (e.getKeyCode()) {
		case KeyEvent.VK_LEFT:
			theta.setValue(theta.getValue()-1);
			break;

		case KeyEvent.VK_RIGHT:
			theta.setValue(theta.getValue()+1);
			break;

		case KeyEvent.VK_UP:
			vForward.setValue(vForward.getValue()+1);
			break;

		case KeyEvent.VK_DOWN:
			vForward.setValue(vForward.getValue()-1);
			break;

		default:
			break;
		}

		this.repaint();
	}

	public void setRotationSpeed(int s) {
		theta.setValue(s);
		this.repaint();
	}
	
	public void setForwardSpeed(int s) {
		vForward.setValue(s);
		this.repaint();
	}
	
	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == stop) {
			theta.setValue(0);
			vForward.setValue(0);
			updateThymio();
		}
		else if (e.getSource() == leftTurn) {
			myThymio.rotate(-Math.PI/2);
		}
		else if (e.getSource() == rightTurn) {
			myThymio.rotate(Math.PI/2);
		}
		else if (e.getSource() == ahead) {
			myThymio.drive(16.5);
		}
		else if (e.getSource() == back) {
			myThymio.drive(-16.5);
		}

		if(!this.isFocusOwner()) this.requestFocus();
	}
	
	public void setLeftMapProbs(double [] p) {
		mySensorView.setLeftMapProbs(p);
	}
	
	public void setRightMapProbs(double [] p) {
		mySensorView.setRightMapProbs(p);
	}
	
	public void setLeftValueProbs(double [] p) {
		mySensorView.setLeftValueProbs(p);
	}
	
	public void setRightValueProbs(double [] p) {
		mySensorView.setRightValueProbs(p);
	}
	
	public boolean localizationProblemAhead() {
		return mySensorView.localizationProblemAhead();
	}
	
	public boolean localizationProblemLeft() {
		return mySensorView.localizationProblemLeft();
	}
	
	public boolean localizationProblemRight() {
		return mySensorView.localizationProblemRight();
	}
}

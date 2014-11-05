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
	
	private ArrayList <String> occupied;
	
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
			try {
				populateMapWithObstacles();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		else if (e.getSource() == startRun) {
			myThymio.driveAstarPath();
		}
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
		writeOccupied();
		
		myFrame.repaint();
	}
	private void writeOccupied() throws IOException{
		String filenameout="obsticles.csv";
		File newFile = null;
		FileWriter writer = null;
		
//		writer = new FileWriter(filenameout,true);
//		
//		for(String s : occupied){
//			
//				writer.append(s+"\n");
//				writer.flush();
//			
//			
//		}
//	writer.close();
		
		System.out.println("Choose folder to create file");
		JFileChooser c = new JFileChooser();
		c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		c.showOpenDialog(c);
		c.getSelectedFile();
		newFile = c.getSelectedFile(); // File f - global variable
		String newfile = newFile + "\\obsticles.csv";//.txt or .doc or .html
		File file = new File(newfile);
		    try 
		    {
		        //System.out.println(f);
		        boolean flag = file.createNewFile();

		        JFrame rootPane = new JFrame();
				if(flag==true)
		        {
		            JOptionPane.showMessageDialog(rootPane, "File created successfully");
		        }
		        else
		        {
		            JOptionPane.showMessageDialog(rootPane, "File already exists");
		        }
		        /* or use exists() function as follows:
		            if(file.exists()==true)
		            {
		                JOptionPane.showMessageDialog(rootPane, "File already exists");
		            }
		            else
		            {
		                JOptionPane.showMessageDialog(rootPane, "File created successfully");
		            }

		        */
		    }

		    catch(Exception e)
		    {
		        //any exception handling method of your choice
		    }
		
	}
}
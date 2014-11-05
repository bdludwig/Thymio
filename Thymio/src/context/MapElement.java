package context;

import java.awt.Color;
import java.util.ArrayList;

public class MapElement {
	private Coordinate pos;			// position in the map
	
	private boolean occupied; 		// element is known in advance to be occupied
	private double probOccupied;	// estimation of the state of this element
									// from observations
	
	private boolean onBeam;			// set temporarily if the element is hit by infrared beam
	private boolean onPath;			// set temporarily if the element is on the path to Thymio's destination
	private Color myColor;

	private MapElement predecessor;

	private char content;

	private double distToGoal;

	private double distFromStart;

	private ArrayList<MapElement> linkMapElements;

	private int id;
	
	public int getPosX() {
		return pos.getX();
	}
	
	public int getPosY() {
		return pos.getY();
	}
	
	public boolean isOccupied() {
		return occupied;
	}
	
	public double getProbOccupied() {
		return probOccupied;
	}
	
	public Coordinate getCoordinate() {
		return pos;
	}
	
	public MapElement(int id, int posX, int posY) {
		super();
		this.id = id;
		pos = new Coordinate(posX, posY);
	}
	
	public MapElement(int id, int posX, int posY, boolean occupied) {
		super();
		this.id = id;
		pos = new Coordinate(posX, posY);
		this.occupied = occupied;
	}
	
	public void setOccupied(boolean occ) {
		occupied = occ;
	}

	public boolean onPath() {
		return onPath;
	}
	
	public void setOnPath(boolean onPath) {
		this.onPath = onPath;
	}
	
	public boolean onBeam() {
		return onBeam;
	}

	public void setOnBeam(boolean onBeam) {
		this.onBeam = onBeam;
	}
	
	public String toString() {
		return pos.toString() + ": " + (occupied ? "OCC" : "FREE");
	}
	
	public void setColor(Color c) {
		myColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), 128);
	}
	
	public Color getColor() {
		return myColor;
	}
	
	public void setPredecessor(MapElement n) {
		predecessor = n;
	}

	public MapElement getPredecessor() {
		return predecessor;
	}

	public double getDistTo(MapElement nextMapElement) {
		double dist = pos.getDistanceTo(predecessor,
				nextMapElement.getCoordinate());
		return dist;
	}

	public void setContent(char c) {
		content = c;
	}

	public void setDist(MapElement start, MapElement goal) {
		if (this != goal) {
			distToGoal = getDistTo(goal);
		}
		if (this != start) {
			distFromStart = getDistTo(start);
		}
	}

	public double getDistFromStart() {
		return distFromStart;
	}

	public void setDistFromStart(double d) {
		distFromStart = d;
	}

	public double getF() {
		return distToGoal + distFromStart;
	}

	public char getContent() {
		return content;
	}

	public void setLinkNodes(ArrayList<MapElement> n) {
		linkMapElements = n;
	}

	public ArrayList<MapElement> getLinkNodes() {
		return linkMapElements;
	}

	public int getID() {
		return id;
	}

	public double getDistToGoal() {
		return distToGoal;
	}
}

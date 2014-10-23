package context;

import java.awt.Color;

public class MapElement {
	private int posX;				// position in the map
	private int posY;
	
	private boolean occupied; 		// element is known in advance to be occupied
	private double probOccupied;	// estimation of the state of this element
									// from observations
	
	private boolean onBeam;			// set temporarily if the element is hit by infrared beam
	private Color myColor;
	
	public int getPosX() {
		return posX;
	}
	
	public int getPosY() {
		return posY;
	}
	
	public boolean isOccupied() {
		return occupied;
	}
	
	public double getProbOccupied() {
		return probOccupied;
	}
	
	public MapElement(int posX, int posY) {
		super();
		this.posX = posX;
		this.posY = posY;
	}
	
	public MapElement(int posX, int posY, boolean occupied) {
		super();
		this.posX = posX;
		this.posY = posY;
		this.occupied = occupied;
	}
	
	public void setOccupied(boolean occ) {
		occupied = occ;
	}

	public boolean onBeam() {
		return onBeam;
	}

	public void setOnBeam(boolean onBeam) {
		this.onBeam = onBeam;
	}
	
	public String toString() {
		return "(" + posX + "/" + posY + ")";
	}
	
	public void setColor(Color c) {
		myColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), 128);
	}
	
	public Color getColor() {
		return myColor;
	}
}

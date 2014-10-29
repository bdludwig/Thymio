package context;

import java.util.ArrayList;

public class Node {

	private int id;
	private Coordinate coordinate;
	private ArrayList<Node> linkNodes;
	private char content;
	private double distToGoal = 0;
	private double distFromStart = 0;
	private Node predecessor;

	public Node(int id, int posX, int posY) {
		this.id = id;
		coordinate = new Coordinate(posX, posY);
	}

/* bdludwig moved this to MapElement.java 
	public void setPredecessor(Node n) {
		predecessor = n;
	}

	public Node getPredecessor() {
		return predecessor;
	}

	
	public double getDistTo(Node nextNode) {
		double dist = coordinate.getDistanceTo(predecessor,
				nextNode.getCoordinate());
		return dist;
	}

	public void setContent(char c) {
		content = c;
	}

	public void setDist(Node start, Node goal) {
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

	public void setLinkNodes(ArrayList<Node> n) {
		linkNodes = n;
	}

	public ArrayList<Node> getLinkNodes() {
		return linkNodes;
	}

	public int getID() {
		return id;
	}

	public Coordinate getCoordinate() {
		return coordinate;
	}

	public double getDistToGoal() {
		return distToGoal;
	}
*/
}

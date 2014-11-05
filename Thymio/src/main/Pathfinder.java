package main;

import java.util.ArrayList;

import context.Map;
import context.MapElement;
import context.OpenList;

public class Pathfinder {

	/*
	 * Pathfinder currently works only without Thymio in its own main scope.
	 * It creates a map with a size of HEIGHT * WIDTH,
	 * populates the map with OBSTACLE_COUNT random obstacles
	 * sets the startNode to the bottom left corner,
	 * sets the goalNode to the top right corner,
	 * and through its findPath method finds the shortest path from start to goal using A-Star.
	 * It prefers the path with the least turnings.
	 * TODO Next step would be to integrate the Pathfinder in the Thymio environment.
	 * Hope that's not that big of a deal ;)
	 */

	public static final int BACKWARDS = 0;
	public static final int FORWARD = 1;
	public static final int RIGHT = 2;
	public static final int LEFT = 3;

	private MapElement startNode;
	private MapElement goalNode;
	private OpenList ol = new OpenList();
	private ArrayList<MapElement> closedList = new ArrayList<MapElement>();
	private ArrayList<MapElement> solution = new ArrayList<MapElement>();

	private Map myMap;
	private int[][] thymioNodes;
	
	public Pathfinder(Map m, MapElement start, MapElement goal) {
		/* Map.java
		populateMap();
		*/
		myMap = m;		
		startNode = start;
		goalNode = goal;
		
		System.out.println("AStar: start: " + startNode + ", goal: " + goalNode);
		
		setLinkNodes();
	}

	private void markPath() {
		MapElement n = goalNode;
		
		do {
			n.setOnPath(true);
			solution.add(0, n);
			n = n.getPredecessor();
		}
		while (n != startNode);

		startNode.setOnPath(true);
		solution.add(0, startNode);
	}

	private void setLinkNodes() {
		MapElement curNode;
		ArrayList<MapElement> linkNodes;

		for (int x = 0; x < myMap.getSizeX(); x++) {
			for (int y = 0; y < myMap.getSizeY(); y++) {
				linkNodes = new ArrayList<MapElement>();
				curNode = myMap.getElement(x, y);

				// cut out upper row
				if (x - 1 >= 0) {
					MapElement e = myMap.getElement(x - 1, y);
		
					if (!e.isOccupied()) linkNodes.add(e);
				}
				// cut out lower row
				if (x + 1 < myMap.getSizeX()) {
					MapElement e = myMap.getElement(x + 1, y);

					if (!e.isOccupied()) linkNodes.add(e);
				}
				// check left
				if (y - 1 >= 0) {
					MapElement e = myMap.getElement(x, y - 1);

					if (!e.isOccupied()) linkNodes.add(e);
				}
				// check right
				if (y + 1 < myMap.getSizeY()) {
					MapElement e = myMap.getElement(x, y + 1);

					if (!e.isOccupied()) linkNodes.add(e);
				}
				curNode.setLinkNodes(linkNodes);
				curNode.setDist(startNode, goalNode);
			}
		}
	}

	public void findPath() {
		MapElement curNode;

		ol.enqueue(startNode, 0);
		solution = new ArrayList<MapElement>();

		while (!ol.isEmpty()) {
			curNode = ol.removeMin();
			if (curNode.getID() == goalNode.getID()) {
				markPath();
				return;
			}
			else {
				closedList.add(curNode);
				expandNode(curNode);
			}

		}
		
		solution = null;
	}

	private void expandNode(MapElement n) {
		double tentative_g;
		
		for (MapElement nextNode : n.getLinkNodes()) {
			if (!closedList.contains(nextNode)) {
				tentative_g = n.getDistFromStart() + n.getDistTo(nextNode);
				if (!ol.contains(nextNode) || tentative_g < nextNode.getDistFromStart()) {		
					double f = tentative_g + nextNode.getDistToGoal();
					
					nextNode.setPredecessor(n);
					nextNode.setDistFromStart(tentative_g);
					
					if (ol.contains(nextNode)) {
						ol.decreaseKey(nextNode, f);
					} else {
						ol.enqueue(nextNode, f);
					}
				}
			}
		}
	}
	
	public ArrayList<MapElement> getSolution() {
		return solution;
	}
}

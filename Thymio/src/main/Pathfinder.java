package main;

import java.io.ObjectInputStream.GetField;
import java.util.ArrayList;

import context.Coordinate;
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

	/* Map.java
	private final int HEIGHT = 20;
	private final int WIDTH = 8;
	private final int OBSTACLE_COUNT = 20;
	*/
	private final Integer BACKWARDS = 0;
	private final Integer FORWARD = 1;
	private final Integer RIGHT = 2;
	private final Integer LEFT =3;

	/* Map. java
	private final int START = HEIGHT * WIDTH - WIDTH;
	private final int GOAL = WIDTH - 1;
	*/
	
	private MapElement startNode;
	private MapElement goalNode;

	/* Map.java
	private Node[] nodes = new Node[HEIGHT * WIDTH];
	private int[][] thymioNodes = new int[HEIGHT][WIDTH];
	 */
	
	private OpenList ol = new OpenList();
	private ArrayList<MapElement> closedList = new ArrayList<MapElement>();
	private ArrayList<String> thymioPath = new ArrayList<String>();

	private Map myMap;
	private int[][] thymioNodes;
	
	public Pathfinder(Map m) {
		/* Map.java
		populateMap();
		*/
		myMap = m;
		thymioNodes = new int[myMap.getSizeX()][myMap.getSizeY()];
		
		startNode = m.getCurrentPos();
		goalNode = m.getElement(myMap.getSizeX() - 1, myMap.getSizeY() - 1);
		
		setLinkNodes();
		
		findPath();
		printThymioNodes();
	}

	/*
	 * obstacles are set by ExperimentPanel.java 

	private int[] getRandomObstacles() {
		int[] randoms = new int[OBSTACLE_COUNT];
		for (int i = 0; i < randoms.length; i++) {
			int r = (int) (Math.random() * WIDTH * HEIGHT);
			for (int j = 0; j < i; j++) {
				if (randoms[j] == r || randoms[j] == START
						|| randoms[j] == GOAL) {
					i--;
					break;
				}
			}
			randoms[i] = r;
		}
		return randoms;
	}
	*/

	/*
	 * moved to Map.java

	private void populateMap() {
		int[] obstacles = getRandomObstacles();
		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = new Node(i, i / WIDTH, i % WIDTH);
			if (i == START) {
				nodes[i].setContent('T');
				startNode = nodes[i];
			} else if (i == GOAL) {
				nodes[i].setContent('G');
				goalNode = nodes[i];
			} else {
				nodes[i].setContent('_');
				for (int j = 0; j < obstacles.length; j++) {
					if (obstacles[j] == i) {
						nodes[i].setContent('O');
						closedList.add(nodes[i]);
					}
				}
			}
		}
	}
	 */
	
	private void markPath() {
		MapElement n = goalNode;
		while (n.getPredecessor() != startNode) {
			n.getPredecessor().setContent('x');
			n = n.getPredecessor();
		}
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
					// cut out left col
					// if (y - 1 >= 0) {
					// linkNodes.add(nodes[getNodeID(x - 1, y - 1)]);
					// }
					// // cut out right col
					// if (y + 1 < WIDTH) {
					// linkNodes.add(nodes[getNodeID(x - 1, y + 1)]);
					// }
					// always needed
					linkNodes.add(myMap.getElement(x - 1, y)/*nodes[getNodeID(x - 1, y)]*/);
				}
				// cut out lower row
				if (x + 1 < myMap.getSizeX()) {
					// // cut out left col
					// if (y - 1 >= 0) {
					// linkNodes.add(nodes[getNodeID(x + 1, y - 1)]);
					// }
					// // cut out right col
					// if (y + 1 < WIDTH) {
					// linkNodes.add(nodes[getNodeID(x + 1, y + 1)]);
					// }
					// always needed
					linkNodes.add(myMap.getElement(x + 1, y)/*nodes[getNodeID(x + 1, y)]*/);
				}
				// check left
				if (y - 1 >= 0) {
					linkNodes.add(myMap.getElement(x, y - 1)/*nodes[getNodeID(x, y - 1)]*/);
				}
				// check right
				if (y + 1 < myMap.getSizeY()) {
					linkNodes.add(myMap.getElement(x, y + 1)/*nodes[getNodeID(x, y + 1)]*/);
				}
				curNode.setLinkNodes(linkNodes);
				curNode.setDist(startNode, goalNode);
			}
		}
	}
	
		/* Map.java
	private int getNodeID(int x, int y) {
		return x * WIDTH + y;
	}
	*/

	private void printMap() {
		for (int x = 0; x < myMap.getSizeX(); x++) {
			for (int y = 0; y < myMap.getSizeY(); y++) {
				MapElement e = myMap.getElement(x, y);

				System.out.print(e.getContent() + " ");
				if (e.getContent() == '_' || e.getContent() == 'O'){
					thymioNodes[x][y] = 0;
				}
				else {
					thymioNodes[x][y] = 1;
				}
			}
		}
	}
	
	public void findPath() {
		ol.enqueue(startNode, 0);
		MapElement curNode;
		while (!ol.isEmpty()) {
			curNode = ol.removeMin();
			if (curNode == goalNode) {
				System.out.println("Done with cost of: " + ol.getCost());
				markPath();
				printMap();
				return;
			}
			closedList.add(curNode);
			expandNode(curNode);
		}
		System.out.println("No Path");
		printMap();
	}

	private void expandNode(MapElement n) {
		double tentative_g;
		for (MapElement nextNode : n.getLinkNodes()) {
			if (closedList.contains(nextNode)) {
				continue;
			}
			tentative_g = n.getDistFromStart() + n.getDistTo(nextNode);
			if (ol.contains(nextNode)
					&& tentative_g >= nextNode.getDistFromStart()) {
				continue;
			}
			nextNode.setPredecessor(n);
			nextNode.setDistFromStart(tentative_g);
			double f = tentative_g + nextNode.getDistToGoal();
			if (ol.contains(nextNode)) {
				ol.decreaseKey(nextNode, f);
			} else {
				ol.enqueue(nextNode, f);
			}
		}
	}
	
	public ArrayList<Integer> getPathsForThymio(){
		int count = 0;
		ArrayList<Integer> thymioPathList = new ArrayList<Integer>();

		for(int i = 0; i < myMap.getSizeX(); i++) {
			for(int k = 0; k < myMap.getSizeY(); k++) {
				if(thymioNodes[i][k] == 1){
					thymioNodes[i][k] = 2;
					count++;
				}
			}
			if(count == 1) {
				thymioPathList.add(FORWARD);
				count = 0; 
			}
			if(count > 1) {
				thymioPathList.add(RIGHT);
				for(int y = 0; y < count-1; y++){
					thymioPathList.add(FORWARD);
				}
				thymioPathList.add(LEFT);
				thymioPathList.add(FORWARD);
				count = 0; 
			}
		}
		System.out.println(thymioPathList);
		return thymioPathList; 
	}

	public void printThymioNodes(){
		String check = "\n";
		for(int i = 0; i < myMap.getSizeX(); i++) {
			for(int k = 0; k < myMap.getSizeY(); k++) {
				check += thymioNodes[i][k] + " ";
			}
			check += "\n";
		}

		System.out.println(check);
	}

}

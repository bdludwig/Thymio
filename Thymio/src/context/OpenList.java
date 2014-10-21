package context;

import java.util.ArrayList;

public class OpenList extends ArrayList<Node> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private double cost;

	public OpenList() {
		super();
		cost = 0;
	}
	
	public double getCost() {
		return cost;
	}

	public void enqueue(Node n, double c) {
		cost = c;
		add(n);
	}
	
	public void decreaseKey(Node n, double c) {
		cost = c;
	}

	public Node removeMin() {
		Node n = get(0);
		int index = 0;
		for (int i = 1; i < this.size(); i++) {
			if (n.getF() > get(i).getF()) {
				n = get(i);
				index = i;
			}
		}
		n = remove(index);
		return n;
	}
}

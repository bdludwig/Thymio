package context;

public class Coordinate {
	private int x;
	private int y;
	private final double COSTFORTURN = 2;

	// Test
	public Coordinate(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	public double getDistanceTo(Node predecessor, Coordinate nextCoordinate) {
		int x2 = nextCoordinate.getX();
		int y2 = nextCoordinate.getY();
		int additionalCost = 0;
		if (predecessor != null) {
			int x0 = predecessor.getCoordinate().getX();
			int y0 = predecessor.getCoordinate().getY();
			if (x0 == x && x != x2 || y0 == y && y != y2) {
				additionalCost += COSTFORTURN;
			}
		}
		double d = Math.sqrt(Math.pow((x2 - x), 2) + Math.pow((y2 - y), 2))
				+ additionalCost;
		return d;
	}

	public String toString() {
		return x + "/" + y;
	}
}

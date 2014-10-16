package math;

public class MovementModel {
	private NormalDistribution nd;
	
	public MovementModel() {
		nd = new NormalDistribution(0, 0);
	}
	
	public void run() {
		int n = 1;
		double currentPos = 0;
		double deltaX;
		double currentV = 0.2;
		double currentStd = 0.01;
		double meanX = 0;
		double squareX = 0;

		nd.setSD(currentStd);
		
		while (n < 600) {
			deltaX = currentV + nd.drawFromDistribution();
			squareX += deltaX*deltaX;

			meanX = ((n-1)*meanX + deltaX)/n;
			currentPos += deltaX;
			
			if (n > 1) {
				double var = (squareX-n*meanX*meanX)/(n-1);
				if (var >= 0) {
					currentStd = Math.sqrt(var);
					nd.setSD(currentStd);
				}
			}
			
			n++;

			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String [] args) {
		MovementModel mm;
		
		mm = new MovementModel();
		mm.run();
	}
}

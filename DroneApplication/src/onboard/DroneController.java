package onboard;
import serial.Sender;

public class DroneController {
	private Sender sender;
	private int channel1Mid = 0;
	private int channel2Mid = 0;
	private int channel3Low = 0;
	private int channel4Mid = 0;
	private int testValue = 150; // the offset for the rc override messages
//	private double minRange = 0.3; //metres
	private double previousOffset = 9999999;
	private String previousDirection = "";
	private int n = 1;
	private DroneApplication droneApplication;
	private int minPWM = 70, maxPWM = 170;
	
	public DroneController(Sender sender, DroneApplication droneApplication) {
		this.sender = sender;
		this.droneApplication = droneApplication;
	}
	
	public void setChannels(int c1, int c2, int c3, int c4) {
		this.channel1Mid = c1;
		this.channel2Mid = c2;
		this.channel3Low = c3;
		this.channel4Mid = c4;
		System.out.println("got rc raw message! " + channel1Mid + " " + channel2Mid + " " + channel3Low + " " + channel4Mid);

	}
	
	
	
	public void control(double offsetX, double offsetY, double altitude, double customMode) {
		if (offsetX == -1 && offsetY == -1) {
			if (customMode == 9 && altitude > 1.5) { //land mode
				sender.mode("loiter", true);
			}
			return;
		}
		
		//land mode
		if (customMode == 5 || customMode == 0) { //previously in loiter/stabilize mode
			sender.mode("land", true);
		}
		
//		double c = (maxPWM - minPWM)/(6.00 - 1.00); // PWM = c * h + d (70-170)
		double c = (maxPWM - minPWM)/(2.0 - 0.0); // PWM = c * h + d (0.0-2)
		double d = minPWM;

		double minRange = 0.4; // try constant 0.4
	//	double minRange = Math.max(0.4, altitude * a + b);
	//	int PWM = (int)Math.max(70, Math.min(500,c * altitude + d)); //min = 50, max = 150
		int xPWMDiff = (int)Math.max(100, Math.min(500,c * offsetX + d));
		int yPWMDiff = (int)Math.max(100, Math.min(500,c * offsetY + d));

		//	PWM += 5*(n-1); //add PWM if we haven't got closer for a while
		System.out.println("c: " + c + ", d: " + d + " , xPWMDiff=" + xPWMDiff + ", yPWMDiff=" + yPWMDiff);
		String directionX = offsetX > 0 ? "right" : "left";
		String directionY = offsetY > 0 ? "forwards" : "backwards";
		int xPWM = offsetX > 0 ? channel1Mid+xPWMDiff : channel1Mid-xPWMDiff;
		int yPWM = offsetY > 0 ? channel2Mid-yPWMDiff : channel2Mid+yPWMDiff;
		// set a minimum where we keep drone steady (if less than 2m altitude)
		if (Math.abs(offsetX) < minRange/* && altitude < 2*/) { xPWM = 0; directionX = "none"; }
		if (Math.abs(offsetY) < minRange/* && altitude < 2*/) { yPWM = 0; directionY = "none"; }
		String currentDirection = directionX + directionY;
		boolean isDescending = true; //(xPWM == 0 && yPWM == 0) || altitude > 2;
		int throttlePWM = isDescending? channel3Low : 0; //descend
		long waitTime = 200;//800;
//		//land mode
//		if (isDescending && altitude <= 2) {
//			sender.mode("land", true);
//			waitTime = 400;
//		}
		
		double offsetMagnitude = Math.sqrt(Math.pow(offsetX, 2) + Math.pow(offsetY, 2));
		if (!currentDirection.equals(previousDirection) || offsetMagnitude < previousOffset) {
			n = 1; //we've gotten closer so don't increment n
		} else {
			n += 1; //we haven't got any closer, and still same direction, so increase n
		}
		//move toward pattern for n secs
		sender.rc(xPWM, yPWM, throttlePWM, 0);
		previousDirection = currentDirection;
		System.out.println("Moving: " + directionX + ", " + directionY + " for " + n + " times, descending="+(throttlePWM!=0));
		waitFor((xPWM == 0 && yPWM == 0), waitTime);
		
		previousOffset = offsetMagnitude;
	}

	private void waitFor(boolean justDescend, long waitTime) {
		try {
			int length = 200;//justDescend? 800 : 400; //300
			Thread.sleep(length);
//			System.out.println("stopping command after " + length + " seconds");
			cancel();
//			Thread.sleep(waitTime); //may need to change this
			droneApplication.setReadyForCommand(true);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void circularSearch() {
		int elev = 200;
		int ail = 0;
		int xPWM, yPWM;
		
		while (elev > -200) {
			xPWM = channel1Mid+ail;
			yPWM = channel2Mid+elev;
			sender.rc(xPWM, yPWM, 0, 0);
			elev -= 10;
			ail = elev > 0 ? ail + 10 : ail - 10;
		}
		while (elev < 200) {
			xPWM = channel1Mid+ail;
			yPWM = channel2Mid+elev;
			sender.rc(xPWM, yPWM, 0, 0);
			elev += 10;
			ail = elev < 0 ? ail - 10 : ail + 10;
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		cancel();
	}
	
	public void setPWMranges(int minPWM, int maxPWM){
		System.out.println("Setting pwm ranges to: " + minPWM + " - " + maxPWM);
		this.minPWM = minPWM;
		this.maxPWM = maxPWM;
	}

	public void setTestValue(int value) {
		System.out.println("Setting test value to: " + value);
		this.testValue = value;
	}
	
	public void forward() {
		sender.rc(0, channel2Mid-testValue-20, 0, 0);//	public boolean rc(int aileronValue, int elevatorValue, int throttleValue, int rudderValue) {
	}
	
	public void backward() {
		sender.rc(0, channel2Mid+testValue+20, 0, 0); // elevator only (controls pitch)
	}
	
	public void left() {
		sender.rc(channel1Mid-testValue, 0, 0, 0);//aileron only (controls roll)
	}
	
	public void right() {
		sender.rc(channel1Mid+testValue, 0, 0, 0);
	}

	public void cancel() {
	//	System.out.println("cancelling");
		sender.rc(0, 0, 0, 0); //cancel all		
	}
	
}

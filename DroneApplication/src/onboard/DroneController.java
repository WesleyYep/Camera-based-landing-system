package onboard;
import serial.Sender;

public class DroneController {
	private Sender sender;
	private int channel1Mid = 0;
	private int channel2Mid = 0;
	private int channel3Low = 0;
	private int channel4Mid = 0;
	private int testValue = 150; // the offset for the rc override messages for testing purposes
	private int minPWM = 70, maxPWM = 170;
	
	public DroneController(Sender sender) {
		this.sender = sender;
	}
	
	/**
	 * Sets the default values of the channels
	 * @param c1 middle value for channel 1 (aileron)
	 * @param c2 middle value for channel 2 (elevator)
	 * @param c3 lowest value for channel 3 (throttle)
	 * @param c4 middle value for channel 4 (rudder) - unused
	 */
	public void setChannels(int c1, int c2, int c3, int c4) {
		this.channel1Mid = c1;
		this.channel2Mid = c2;
		this.channel3Low = c3;
		this.channel4Mid = c4;
		System.out.println("got rc raw message! " + channel1Mid + " " + channel2Mid + " " + channel3Low + " " + channel4Mid);

	}
	
	/**
	 * Carries out an iteration of the control algorithm
	 * @param offsetX offset in metres in x direction
	 * @param offsetY offset in metres in y direction
	 * @param altitude in metres
	 * @param customMode what mode the drone is currently in (loiter or land)
	 */
	public void control(double offsetX, double offsetY, double altitude, double customMode) {
		if (offsetX == -1 && offsetY == -1) {
			if (customMode == 9 && altitude > 1.5) { // if it's in land mode and doesn't see pattern, go to loiter mode (stop descending)
				sender.mode("loiter", true);
			}
			return;
		}
		
		//if it's in loiter/stabilize mode and you see the pattern, switch to land mode (start descending)
		if (customMode == 5 || customMode == 0) { 
			sender.mode("land", true);
		}
		
		// PWM = c * h + d (0.0-2)
		double c = (maxPWM - minPWM)/(2.0 - 0.0); 
		double d = minPWM;

		double minRange = 0.4; // under this range, will not move in that direction
		// PPM_x = c * offsetX + d
		// PPM_y = c * offsetY + d
		int xPWMDiff = (int)Math.max(100, Math.min(500,c * offsetX + d));
		int yPWMDiff = (int)Math.max(100, Math.min(500,c * offsetY + d));

		System.out.println("c: " + c + ", d: " + d + " , xPWMDiff=" + xPWMDiff + ", yPWMDiff=" + yPWMDiff);
		String directionX = offsetX > 0 ? "right" : "left";
		String directionY = offsetY > 0 ? "forwards" : "backwards";
		int xPWM = offsetX > 0 ? channel1Mid+xPWMDiff : channel1Mid-xPWMDiff;
		int yPWM = offsetY > 0 ? channel2Mid-yPWMDiff : channel2Mid+yPWMDiff;
		// set a minimum where we keep drone steady
		if (Math.abs(offsetX) < minRange) { xPWM = 0; directionX = "none"; }
		if (Math.abs(offsetY) < minRange) { yPWM = 0; directionY = "none"; }
		String currentDirection = directionX + directionY;
		int throttlePWM = channel3Low; //descend
		long waitTime = 200;
		double offsetMagnitude = Math.sqrt(Math.pow(offsetX, 2) + Math.pow(offsetY, 2));
		sender.rc(xPWM, yPWM, throttlePWM, 0);
		System.out.println("Moving: " + directionX + ", " + directionY + ", descending="+(throttlePWM!=0));
		waitFor(waitTime);
	}

	/**
	 * Wait for a bit of time before canceling the movement 
	 */
	private void waitFor(long waitTime) {
		try {
			Thread.sleep(waitTime);
			cancel();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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
		sender.rc(0, 0, 0, 0); //cancel all		
	}
	
}

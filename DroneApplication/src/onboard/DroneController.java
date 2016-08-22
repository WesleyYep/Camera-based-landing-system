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
	
	
	
	public void control(double offsetX, double offsetY, double altitude) {
		if (offsetX == -1 && offsetY == -1) {
			return;
		}
		double a = 0.05333, b = 0.373333; // range = a * h + b (where a = altitude) 0.4-0.8
		double c = 14,d = 100; // PWM = c * h + d (set maximum is 150 though - at 5m)
		double minRange = altitude * a + b;
		int PWM = (int)Math.max(100, Math.min(170,c * altitude + d)); //min = 50, max = 150
		PWM += 5*n;
		System.out.println("range: " + minRange + "   PWM = " + PWM);
		String directionX = offsetX > 0 ? "right" : "left";
		String directionY = offsetY > 0 ? "forwards" : "backwards";
		int xPWM = offsetX > 0 ? channel1Mid+PWM : channel1Mid-PWM;
		int yPWM = offsetY > 0 ? channel2Mid-PWM : channel2Mid+PWM;
		// set a minimum where we keep drone steady
		if (Math.abs(offsetX) < minRange) { xPWM = 0; directionX = "none"; }
		if (Math.abs(offsetY) < minRange) { yPWM = 0; directionY = "none"; }
		String currentDirection = directionX + directionY;
		boolean isDescending = (xPWM == 0 && yPWM == 0);
		int throttlePWM = isDescending? channel3Low : 0; //descend
		double offsetMagnitude = Math.sqrt(Math.pow(offsetX, 2) + Math.pow(offsetY, 2));
		if (!currentDirection.equals(previousDirection) || offsetMagnitude < previousOffset) {
			n = 1; //we've gotten closer so just keep moving n=1 sec at a time
		} else {
			n += 1; //we haven't got any closer, and still same direction, so increase n
		}
		//move toward pattern for n secs
		sender.rc(xPWM, yPWM, throttlePWM, 0);
		previousDirection = currentDirection;
		System.out.println("Moving: " + directionX + ", " + directionY + " for " + n + " times, descending="+(throttlePWM!=0));
		waitFor(offsetMagnitude, isDescending);
	}

	private void waitFor(double offsetMagnitude, boolean isDescending) {
		try {
			int length = isDescending? 200 : 500;
			Thread.sleep(length);
			System.out.println("stopping command");
			cancel();
			Thread.sleep(800);
			previousOffset = offsetMagnitude;
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
		System.out.println("cancelling");
		sender.rc(0, 0, 0, 0); //cancel all		
	}
	
}

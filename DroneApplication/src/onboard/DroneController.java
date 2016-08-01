package onboard;

import serial.Sender;

public class DroneController {
	private Sender sender;
	private int channel1Mid = 0;
	private int channel2Mid = 0;
	private int channel3Mid = 0;
	private int channel4Mid = 0;
	private int testValue = 150; // the offset for the rc override messages
	private double previousOffset = 9999999;
	private int n = 1;
	
	public DroneController(Sender sender) {
		this.sender = sender;
	}
	
	public void setChannels(int c1, int c2, int c3, int c4) {
		this.channel1Mid = c1;
		this.channel2Mid = c2;
		this.channel3Mid = c3;
		this.channel4Mid = c4;
		System.out.println("got rc raw message! " + channel1Mid + " " + channel2Mid + " " + channel3Mid + " " + channel4Mid);

	}
	
	
	
	public void control(double offsetX, double offsetY) {
		String directionX = offsetX > 0 ? "right" : "left";
		String directionY = offsetY > 0 ? "forwards" : "backwards";
		int xPWM = offsetX > 0 ? channel1Mid+testValue : channel1Mid-testValue;
		int yPWM = offsetY > 0 ? channel2Mid-testValue : channel2Mid+testValue;
		// set a range of 1m where we keep drone steady
		if (Math.abs(offsetX) < 0.5) { xPWM = 0; }
		if (Math.abs(offsetY) < 0.5) { yPWM = 0; }
		double offsetMagnitude = Math.sqrt(Math.pow(offsetX, 2) + Math.pow(offsetY, 2));
		if (offsetMagnitude < previousOffset) {
			n = 1; //we've gotten closer so just keep moving n=1 sec at a time
		} else {
			n += 1; //we haven't got any closer, so increase n
		}
		//move toward pattern for n secs
		sender.rc(xPWM, yPWM, 0, 0);
		System.out.println("Moving in direction: " + directionX + ", " + directionY + " for " + n + " seconds");
		waitFor(n);
		System.out.println("stopping command");
		cancel();
		previousOffset = offsetMagnitude;
	}

	private void waitFor(double seconds) {
		try {
			Thread.sleep((long) (seconds * 1000));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void setTestValue(int value) {
		System.out.println("Setting test value to: " + value);
		this.testValue = value;
	}
	
	public void forward() {
		sender.rc(0, channel2Mid-testValue, 0, 0);//	public boolean rc(int aileronValue, int elevatorValue, int throttleValue, int rudderValue) {
	}
	
	public void backward() {
		sender.rc(0, channel2Mid+testValue, 0, 0); // elevator only (controls pitch)
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

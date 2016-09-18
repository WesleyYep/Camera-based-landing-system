package onboard;
/**
 * This class represents the Drone, and is updated based on the data stream that is read from the HKPilot
 */
public class Drone {
	public float pitch = 0;
	public float roll = 0;
	public float yaw = 0;
	public float initialAltitude = 0;
	public float altitude = 0;
	public double currentMode = 0;
	public double currentCustomMode = 0;
}

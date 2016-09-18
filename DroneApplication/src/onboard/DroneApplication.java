/**
 * $Id: TestMavlinkReader.java 10 2013-04-26 13:04:11Z ghelle31@gmail.com $
 * $Date: 2013-04-26 15:04:11 +0200 (ven., 26 avr. 2013) $
 *
 * ======================================================
 * Copyright (C) 2012 Guillaume Helle.
 * Project : MAVLINK Java
 * Module : org.mavlink.library
 * File : org.mavlink.TestMavlinkReader.java
 * Author : Guillaume Helle
 *
 * ======================================================
 * HISTORY
 * Who       yyyy/mm/dd   Action
 * --------  ----------   ------
 * ghelle	31 aout 2012		Create
 * 
 * ====================================================================
 * Licence: MAVLink LGPL
 * ====================================================================
 */

package onboard;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.util.Timer;
import java.util.TimerTask;

import org.mavlink.MAVLinkReader;
import org.mavlink.messages.MAVLinkMessage;
import org.mavlink.messages.ja4rtor.msg_ahrs2;
import org.mavlink.messages.ja4rtor.msg_global_position_int;
import org.mavlink.messages.ja4rtor.msg_heartbeat;
import org.mavlink.messages.ja4rtor.msg_rc_channels_raw;
import jssc.SerialPortList;
import network.Client;
import serial.Reader;
import serial.Sender;
import serial.SerialPortCommunicator;

/**
 * This runs onboard the drone
 * It creates the threads for reading the HKPilot through UART serial cable, communication with the ground
 * station via Wifi, and for the image processing algorithm
 *
 */
public class DroneApplication {
	public Drone drone = new Drone(); //represents drone properties eg. yaw, roll, pitch
	private String direction = "";
	private Sender sender;
	private String ipAddress = "169.254.110.196";
	private boolean testMode = false;
	private DroneController controller;
	private boolean calibrated = false;
	private double xOffsetValue, yOffsetValue, altitude;
	
    /**
     * Entry point of onboard drone application
     */
    public static void main(String[] args) {
    	DroneApplication application = new DroneApplication();
    	application.start(args);
    }
    
    /**
     * Start processing
     * @param args - the command line arguments given for testing certain features
     */
    public void start(String[] args) {
		SerialPortCommunicator spc = new SerialPortCommunicator();
		sender = new Sender(spc);
    	try {
			System.out.println("Trying to open " + SerialPortList.getPortNames()[0]);
			spc.openPort(SerialPortList.getPortNames()[0]); //this should be ttyAMA0 for RPi 2
			if (!spc.isOpened()) {
				System.err.println("Port not opened");
			} else {
				System.out.println("Port opened!");
			}
    	} catch (Exception ex) {
    		System.err.println("No ports available");
    	}
	
    	controller = new DroneController(sender);
		ImageProcessing imageProcessing = new ImageProcessing(drone, this);
		
		imageProcessing.client = new Client(ipAddress, 55555, data ->{
			// this method is the callback for when the client sends a command to server
			// format is "command:param:param"
			// commands are "stream, slider, arm, mode, land, command, test, pattern, calibrate, snapshot, range"
			System.out.println(data.toString());		
			String[] arr = data.toString().split(":");
			if (data.toString().startsWith("stream:")) {
				// deals with starting and stopping the streaming
				imageProcessing.isStreaming = arr[1].equals("true");
			} else if (data.toString().startsWith("slider:")) {
				// deals with seting colour threshold values from slider values
				if (arr[1].equals("h")) {
					imageProcessing.hMin = Double.parseDouble(arr[2]);
					imageProcessing.hMax = Double.parseDouble(arr[3]);
				} else if (arr[1].equals("s")) {
					imageProcessing.sMin = Double.parseDouble(arr[2]);
					imageProcessing.sMax = Double.parseDouble(arr[3]);
				} else if (arr[1].equals("v")) {
					imageProcessing.vMin = Double.parseDouble(arr[2]);
					imageProcessing.vMax = Double.parseDouble(arr[3]);
				}
			} else if (data.toString().startsWith("arm:")) {
				// deals with arming or disarming the drone
				testArm(sender, arr[1].equals("true"));	
			} else if (data.toString().startsWith("mode:")) {
				// deals with changing the mode of the drone
				changeMode(arr[1], arr[2].equals("true"));
			} else if (data.toString().startsWith("land:")) {
				// deals with testing the landing of drone
				land(sender, arr[1]); 
			} else if (data.toString().startsWith("command:")) {
				// deals with receiving directional commands
				direction = arr[1];
				if (direction.equals("forward")) {
					controller.forward();
				} else if (direction.equals("backward")) {
					controller.backward();
				} else if (direction.equals("left")) {
					controller.left();
				} else if (direction.equals("right")) {
					controller.right();
				} else if (direction.equals("cancel")) {
					controller.cancel();
				}
			} else if (data.toString().startsWith("test:")) {
				// deals with switching to the autonomous landing "test" mode
				if (arr[1].equals("test")) {
					testMode = Boolean.parseBoolean(arr[2]);
					if (testMode) {
						command();
					}
				} else {
					controller.setTestValue(Integer.parseInt(arr[1]));
				}
			} else if (data.toString().startsWith("pattern:")) {
				// deals with switching between full size pattern and half size pattern
				imageProcessing.setBigPattern(arr[1].equals("big"));
			} else if (data.toString().equals("calibrate")) {
				// calibrates drone based on current PPM positions of RC controller
        		sender.send(3);
				calibrated = false;
			} else if (data.toString().equals("snapshot")) {
				// deals with taking a snapshot of what the drone sees
				imageProcessing.snapshot();
			} else if (data.toString().startsWith("range:")) {
				// deals with setting the max and min PPM values of the drone
				controller.setPWMranges(Integer.parseInt(arr[1]), Integer.parseInt(arr[2]));
			}
		});
	
    	//start camera for QR detection
    	Thread imageProcessingThread = new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					imageProcessing.start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
    	});
    	
    	//thread that deals with sending and receiving mavlink messages
    	Thread mavlinkThread = new Thread(new Runnable() {
    		@Override
			public void run() {
				String cmd = "";
				if (args.length != 0) {
					cmd = args[0];
				}
				// These commands are used from command line and can be used to test sending/receiving Mavlink messags
				// Is not actually used for the autonomous landing apart from testAngle
				// Default is testAngle which receives the roll and pitch values from the HKPilot, used for the image processing algorithm
				if (cmd.equals("send")) {
					testSendToSerial(sender, Integer.parseInt(args[1]));
				} else if (cmd.equals("rec")){  // rec
					if (args.length > 1 && args[1].equals("hb")) { // rec hb
						sender.send(0);
					}
					testFromSerial(spc);
				} else if (cmd.equals("angle")){
					testAngle(sender, spc);
				} else if (cmd.equals("cmd")) {
					testCommands(sender, Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
				} else if (cmd.equals("arm")) {
					testArm(sender, args.length > 1 && args[1].equals("true"));
				} else if (cmd.equals("hb")) {
					testHeartBeat(sender);
				} else if (cmd.equals("mode")) {
					sender.heartbeat();
					sender.mode(args.length > 1 ? args[1] : "", args.length > 2 && args[2].equals("armed"));
				} else if (cmd.equals("land")) {
					land(sender, args[1]);
				} else if (cmd.equals("test")) {
					//sender.test(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
					//testGuidedCommand(Double.parseDouble(args[1]));
				} else {
					testAngle(sender, spc);
				}
    		}
    	});
    	 	
		imageProcessingThread.start();
		mavlinkThread.start();
    }

    /**
     * This is used to change the mode of the drone
     * @param mode either stabilize, loiter, land
     * @param armed whether drone should be armed or not
     */
    public void changeMode(String mode, boolean armed) {
    	sender.heartbeat();
		sender.mode(mode, armed);
	}

    /**
     * Send commands to the drone based on the x and y offsets that are received from image processing
     */
	public void command() {
		Thread t = new Thread(new Runnable () {
			@Override
			public void run() {
				System.out.println("test mode activated!!!");
				while (testMode) {
					sender.heartbeat();		
					controller.control(xOffsetValue, yOffsetValue, altitude, drone.currentCustomMode);
				}
				System.out.println("test mode deactivated");
			}
		});
		t.start();
	}

	/**
	 * Used to test land mode
	 */
    private void land(Sender sender, String degreesString) {
    	while (true) {
			if(sender.heartbeat()) {
	    		System.out.println("Successfully sent heartbeat");
	    	}
			sender.land(Float.parseFloat(degreesString), 0);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}
	}    
    
    /**
     * Used to test sending heartbeat to drone
     */
	private void testHeartBeat(Sender sender) {
		if(sender.heartbeat()) {
    		System.out.println("Successfully sent heartbeat");
    	}
	}
    
	/**
	 * Used to test sending commands to the drone
	 */
    private void testCommands(Sender sender, int x, int y, int z) {
		while (true) {
			if(sender.heartbeat()) {
	    		System.out.println("Successfully set heartbeat");
	    	}
			if (sender.command(x, y, z)) {
				System.out.println("sent manual move message");
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}
    }
      
    /**
     * Receive angle/orientation messages from the drone
     */
    private void testAngle(Sender sender, SerialPortCommunicator spc) {
    	sender.send(0); //stops all streams
    	sender.send(3); //rc raw values
		sender.send(6); // barometer for altitude
		sender.send(10); //pitch, yaw, roll
    	System.out.println("Sent request for orientation/altitude messages");
    	try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        MAVLinkReader reader;
        int nb = 0;
    	Reader rdr = new Reader(spc);
    	PipedInputStream in = rdr.read();
        DataInputStream dis = new DataInputStream(in);
        reader = new MAVLinkReader(dis);
        try {
            while (true) {
                MAVLinkMessage msg = reader.getNextMessage();
                if (msg != null && msg.messageType == msg_ahrs2.MAVLINK_MSG_ID_AHRS2) {
                    nb++;
                    drone.pitch = ((msg_ahrs2)msg).pitch;
                    drone.yaw = ((msg_ahrs2)msg).yaw;
                    drone.roll = -((msg_ahrs2)msg).roll;
                } else if (msg != null && msg.messageType == msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT) {
                	nb++;
                	drone.currentMode = ((msg_heartbeat)msg).base_mode;
                	drone.currentCustomMode = ((msg_heartbeat)msg).custom_mode;
                } else if (!calibrated && msg != null && msg.messageType == msg_rc_channels_raw.MAVLINK_MSG_ID_RC_CHANNELS_RAW) {
            		nb++;
            		controller.setChannels(((msg_rc_channels_raw)msg).chan1_raw,
            				((msg_rc_channels_raw)msg).chan2_raw,
            				((msg_rc_channels_raw)msg).chan3_raw,
            				((msg_rc_channels_raw)msg).chan4_raw);
            		sender.send(-3);
            		calibrated = true;
                }
            }
        } catch (IOException e) {
        	e.printStackTrace();
        }

        System.out.println("TOTAL BYTES = " + reader.getTotalBytesReceived());
        System.out.println("NBMSG (" + nb + ") : " + reader.getNbMessagesReceived() + " NBCRC=" + reader.getBadCRC() + " NBSEQ="
                          + reader.getBadSequence() + " NBLOST=" + reader.getLostBytes());
	}

    /**
     * Arm and disarm the drone
     */
	public void testArm(Sender sender, boolean arm) {
		sender.heartbeat();
    	if(sender.arm(arm)) {
    		System.out.println("Successfully set ARMED to: " + arm);
    	}
    }
	
	/**
	 * This is used to test sending to the serial port to the HKPilot from RPi
	 */
    public void testSendToSerial(Sender sender, int streamId) {
    	if(sender.send(streamId)) {
    		System.out.println("sent successfully");
    	}    	
    }
    
	/**
	 * This is used to test receiving from the serial port from HKPilot to RPi
	 */
    public void testFromSerial(SerialPortCommunicator spc) {
        MAVLinkReader reader;
        int nb = 0;
    	Reader rdr = new Reader(spc);
    	PipedInputStream in = rdr.read();
        DataInputStream dis = new DataInputStream(in);
        reader = new MAVLinkReader(dis);
        try {
            while (true /*dis.available() > 0*/) {
                MAVLinkMessage msg = reader.getNextMessage();
                if (msg != null) {
                    nb++;
                    System.out.println("SysId=" + msg.sysId + " CompId=" + msg.componentId + " seq=" + msg.sequence + " " + msg.toString());
                }
            }
        } catch (IOException e) {
        	e.printStackTrace();
        }

        System.out.println("TOTAL BYTES = " + reader.getTotalBytesReceived());
        System.out.println("NBMSG (" + nb + ") : " + reader.getNbMessagesReceived() + " NBCRC=" + reader.getBadCRC() + " NBSEQ="
                          + reader.getBadSequence() + " NBLOST=" + reader.getLostBytes());
    }

    /**
     * Set the offsets of x and y direction so it can be used in control algorithm
     */
	public void setOffsetValues(double x, double y, double altitude) {
		this.xOffsetValue = x;
		this.yOffsetValue = y;
		this.altitude = altitude;
	}

    
}

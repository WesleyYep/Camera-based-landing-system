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
 * @author ghelle
 * @version $Rev: 10 $
 *
 */
public class TestMavlinkReader {

	public static float pitch = 0;
	public static float roll = 0;
	public static float yaw = 0;
	private static float initialAltitude = 0;
	public static float altitude = 0;
	public static double currentMode = 0;
	public static double currentCustomMode = 0;
	private static String direction = "";
	private static Sender sender;
	private static int channel1Mid = 0;
	private static int channel2Mid = 0;
	private static int channel3Mid = 0;
	private static int channel4Mid = 0;
	private static int testValue = 100;
//	private static boolean testMode = false;
	private static boolean testMode = true;
	
    /**
     * @param args
     */
    public static void main(String[] args) {
		SerialPortCommunicator spc = new SerialPortCommunicator();
		sender = new Sender(spc);
    	try {
			System.out.println("Trying to open " + SerialPortList.getPortNames()[0]);
			spc.openPort(SerialPortList.getPortNames()[0]);
			
			if (!spc.isOpened()) {
				System.err.println("Port not opened");
			} else {
				System.out.println("Port opened!");
			}
    	} catch (Exception ex) {
    		System.err.println("No ports available");
    	}
//    	
		Thread t3 = new Thread(new Runnable() {
			@Override
			public void run() {
				//start move message sending
				command(sender);
			}
		});
		
    	TestColourDetection.client = new Client("169.254.110.196", 55555, data ->{
			System.out.println(data.toString());		
			String[] arr = data.toString().split(":");
			if (data.toString().startsWith("stream:")) {
				TestColourDetection.isStreaming = arr[1].equals("true");
			} else if (data.toString().startsWith("slider:")) {
				if (arr[1].equals("h")) {
					TestColourDetection.hMin = Double.parseDouble(arr[2]);
					TestColourDetection.hMax = Double.parseDouble(arr[3]);
				} else if (arr[1].equals("s")) {
					TestColourDetection.sMin = Double.parseDouble(arr[2]);
					TestColourDetection.sMax = Double.parseDouble(arr[3]);
				} else if (arr[1].equals("v")) {
					TestColourDetection.vMin = Double.parseDouble(arr[2]);
					TestColourDetection.vMax = Double.parseDouble(arr[3]);
				}
			} else if (data.toString().startsWith("arm:")) {
				testArm(sender, arr[1].equals("true"));	
			} else if (data.toString().startsWith("mode:")) {
				changeMode(arr[1], arr[2].equals("true"));
			} else if (data.toString().startsWith("land:")) {
				land(sender, arr[1]); //eg. land:10
			} else if (data.toString().startsWith("command:")) {
				direction = arr[1];
			} else if (data.toString().startsWith("test:")) {
				if (arr[1].equals("test")) {
					testMode = Boolean.parseBoolean(arr[2]);
				}
				System.out.println("Setting test value to: " + arr[1]);
				testValue = Integer.parseInt(arr[1]);
			}
		});
	
    	//start camera for QR detection
    	Thread t = new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					TestColourDetection.start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
    	});
    	
    	Thread t2 = new Thread(new Runnable() {
    		@Override
			public void run() {
				String cmd = "";
				if (args.length != 0) {
					cmd = args[0];
				}
				
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
    	
		t.start();
		t2.start();
		t3.start();
    }
    
    public static void changeMode(String mode, boolean armed) {
    	sender.heartbeat();
		sender.mode(mode, armed);
	}

	private static void command(Sender sender) {
    	while (true) {
			sender.heartbeat();
			
			if (testMode && TestColourDetection.xOffsetValue != -99999 && TestColourDetection.yOffsetValue != -99999) {
				if (currentMode == 209) { //stabilize, alt_hold or land + ARMED mode
					//may need to reverse orientation after testing
					int xDirection = TestColourDetection.xOffsetValue > 0 ? channel1Mid+testValue : channel1Mid-testValue;
					int yDirection = TestColourDetection.yOffsetValue > 0 ? channel2Mid-testValue : channel2Mid+testValue;
					sender.rc(yDirection, xDirection, 0, 0);
				}
				
			} else {
				if (direction.equals("forward")) {
					if (currentCustomMode == 9){ sender.land(0, 30); }
					else if (currentCustomMode == 4){ sender.command(0, 0.5, 0); }
					else { sender.rc(0, channel2Mid+testValue, 0, 0); } //	public boolean rc(int aileronValue, int elevatorValue, int throttleValue, int rudderValue) {
				} else if (direction.equals("backward")) {
					if (currentCustomMode == 9){ sender.land(0, -30); }
					else if (currentCustomMode == 4){ sender.command(0, -0.5, 0); }
					else { sender.rc(0, channel2Mid-testValue, 0, 0); } // elevator only (controls pitch)
				} else if (direction.equals("left")) {
					if (currentCustomMode == 9){ sender.land(-30, 0); }
					else if (currentCustomMode == 4){ sender.command(-0.5, 0, 0); }
					else { sender.rc(channel1Mid-testValue, 0, 0, 0); } //aileron only (controls roll)
				} else if (direction.equals("right")) {
					if (currentCustomMode == 9){ sender.land(30, 0); }
					else if (currentCustomMode == 4){ sender.command(0.5, 0, 0); }
					else { sender.rc(channel1Mid+testValue, 0, 0, 0); }
				} else if (direction.equals("centre")) {
					if (currentCustomMode == 9){ sender.land(0, 0); }
					else if (currentCustomMode == 4){ sender.command(0,0,0); }
					else { sender.rc(0, 0, channel3Mid + testValue, 0); } // throttle only
				} else if (direction.equals("descend")) {
					if (currentCustomMode == 9){ sender.land(0, 0); }
					else if (currentCustomMode == 4){ sender.command(0,0,0.5); }
					else { sender.rc(0, 0, 0, 0); } //cancel all
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}
	}   
    
    private static void land(Sender sender, String degreesString) {
    	while (true) {
			if(sender.heartbeat()) {
	    		System.out.println("Successfully set heartbeat");
	    	}
			sender.land(Float.parseFloat(degreesString), 0);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}
	}    
    
	private static void testHeartBeat(Sender sender) {
		if(sender.heartbeat()) {
    		System.out.println("Successfully set heartbeat");
    	}
	}
    
    private static void testCommands(Sender sender,/* int value*/ int x, int y, int z) {
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
      
    private static void testAngle(Sender sender, SerialPortCommunicator spc) {
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
            while (true /*dis.available() > 0*/) {
                MAVLinkMessage msg = reader.getNextMessage();
                if (msg != null && msg.messageType == msg_ahrs2.MAVLINK_MSG_ID_AHRS2) {
                    nb++;
                    //System.out.println("SysId=" + msg.sysId + " CompId=" + msg.componentId + " seq=" + msg.sequence + " " + msg.toString());
                    pitch = ((msg_ahrs2)msg).pitch;
                    yaw = ((msg_ahrs2)msg).yaw;
                    roll = -((msg_ahrs2)msg).roll;
          //          System.out.println("pitch=" + pitch + " - roll="+roll + " - yaw=" +yaw);
                } else if (msg != null && msg.messageType == msg_global_position_int.MAVLINK_MSG_ID_GLOBAL_POSITION_INT) {
                    nb++;
                	altitude = ((msg_global_position_int)msg).alt - initialAltitude;
          //      	System.out.println("altitude=" + altitude);
                } else if (msg != null && msg.messageType == msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT) {
          //      	System.out.println("got heartbeat message!!!!!");
                	nb++;
                	currentMode = ((msg_heartbeat)msg).base_mode;
                	currentCustomMode = ((msg_heartbeat)msg).custom_mode;
                } else if (msg != null && msg.messageType == msg_rc_channels_raw.MAVLINK_MSG_ID_RC_CHANNELS_RAW) {
                	if (((msg_rc_channels_raw)msg).chan1_raw != ((msg_rc_channels_raw)msg).chan2_raw && ((msg_rc_channels_raw)msg).chan1_raw != ((msg_rc_channels_raw)msg).chan3_raw
                			 && ((msg_rc_channels_raw)msg).chan1_raw != ((msg_rc_channels_raw)msg).chan4_raw  && ((msg_rc_channels_raw)msg).chan2_raw != ((msg_rc_channels_raw)msg).chan3_raw) {
                		nb++;
                		channel1Mid = ((msg_rc_channels_raw)msg).chan1_raw;
                		channel2Mid = ((msg_rc_channels_raw)msg).chan2_raw;
                		channel3Mid = ((msg_rc_channels_raw)msg).chan3_raw;
                		channel4Mid = ((msg_rc_channels_raw)msg).chan4_raw;
                		sender.send(-3);
                		System.out.println("got rc raw message!!!!! " + channel1Mid + " " + channel2Mid + " " + channel3Mid + " " + channel4Mid);
                	}
                }
            }
        } catch (IOException e) {
        	e.printStackTrace();
        }

        System.out.println("TOTAL BYTES = " + reader.getTotalBytesReceived());
        System.out.println("NBMSG (" + nb + ") : " + reader.getNbMessagesReceived() + " NBCRC=" + reader.getBadCRC() + " NBSEQ="
                          + reader.getBadSequence() + " NBLOST=" + reader.getLostBytes());
	}

	public static void testArm(Sender sender, boolean arm) {
		sender.heartbeat();
    	if(sender.arm(arm)) {
    		System.out.println("Successfully set ARMED to: " + arm);
    	}
    }
    
    public static void testSendToSerial(Sender sender, int streamId) {
    	if(sender.send(streamId)) {
    		System.out.println("sent successfully");
    	}    	
    }
    
    public static void testFromSerial(SerialPortCommunicator spc) {
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

    
}

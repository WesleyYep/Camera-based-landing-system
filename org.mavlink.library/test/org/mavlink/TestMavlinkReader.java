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

package org.mavlink;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import org.mavlink.messages.MAVLinkMessage;
import org.mavlink.messages.ja4rtor.msg_ahrs2;
import org.mavlink.messages.ja4rtor.msg_global_position_int;
import org.mavlink.messages.ja4rtor.msg_heartbeat;

import jssc.SerialPortList;

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
	private static float xVel, yVel = 0;
	
    /**
     * @param args
     */
    public static void main(String[] args) {
		SerialPortCommunicator spc = new SerialPortCommunicator();
		Sender sender = new Sender(spc);
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
				sender.heartbeat();
				sender.mode(arr[1], arr[2].equals("true"));
			} else if (data.toString().startsWith("land:")) {
				land(sender, arr[1]); //eg. land:10
			} else if (data.toString().startsWith("command:")) {
				direction = arr[1];
//				xVel = Integer.parseInt(arr[1]) / 100.0;
//				yVel = Integer.parseInt(arr[2]) / 100.0;
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
					sender.test(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
				} else {
					testAngle(sender, spc);
				}
    		}
    	});
    	
		t.start();
	//	t2.start();
    	t3.start();
    }
    
    private static void command(Sender sender) {
    	while (true) {
			sender.heartbeat();
//			sender.command(xVel, yVel, 0);
			if (direction.equals("forward")) {
//				sender.land(0, 10);
				sender.command(0, 1, 0);
			} else if (direction.equals("backward")) {
//				sender.land(0, -10);
				sender.command(0, -1, 0);
			} else if (direction.equals("left")) {
//				sender.land(-10, 0);
				sender.command(-1, 0, 0);
			} else if (direction.equals("right")) {
//				sender.land(10, 0);
				sender.command(1, 0, 0);
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
    
    static public void testFromSerial(SerialPortCommunicator spc) {
        MAVLinkReader reader;
  //      String fileOut = filename + "-resultat.out";
        int nb = 0;
  //          System.setOut(new PrintStream(fileOut));
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

    static public void testFile(String filename) {
        MAVLinkReader reader;
        String fileOut = filename + "-resultat.out";
        int nb = 0;
        try {
            System.setOut(new PrintStream(fileOut));
            DataInputStream dis = new DataInputStream(new FileInputStream(filename));
            reader = new MAVLinkReader(dis);
            while (dis.available() > 0) {
                MAVLinkMessage msg = reader.getNextMessage();
                //MAVLinkMessage msg = reader.getNextMessageWithoutBlocking();
                if (msg != null) {
                    nb++;
                    System.out.println("SysId=" + msg.sysId + " CompId=" + msg.componentId + " seq=" + msg.sequence + " " + msg.toString());
                }
            }
            dis.close();

            System.out.println("TOTAL BYTES = " + reader.getTotalBytesReceived());
            System.out.println("NBMSG (" + nb + ") : " + reader.getNbMessagesReceived() + " NBCRC=" + reader.getBadCRC() + " NBSEQ="
                               + reader.getBadSequence() + " NBLOST=" + reader.getLostBytes());
        }
        catch (Exception e) {
            System.out.println("ERROR : " + e);
        }
    }

    static public void testBuffer(String filename) {
        MAVLinkReader reader;
        String fileOut = filename + "-resultat-buffer.out";
        int nb = 0;
        int sizeToRead = 4096;
        int available;
        byte[] buffer = new byte[4096];
        MAVLinkMessage msg;
        try {
            System.setOut(new PrintStream(fileOut));
            DataInputStream dis = new DataInputStream(new FileInputStream(filename));
            reader = new MAVLinkReader();
            while (dis.available() > 0) {
                msg = reader.getNextMessage(buffer, 0);
                if (msg != null) {
                    nb++;
                    System.out.println("SysId=" + msg.sysId + " CompId=" + msg.componentId + " seq=" + msg.sequence + " " + msg.toString());
                }
                else {
                    if (dis.available() > 0) {
                        available = dis.available();
                        if (available > sizeToRead) {
                            available = sizeToRead;
                        }
                        dis.read(buffer, 0, available);
                        msg = reader.getNextMessage(buffer, available);
                        if (msg != null) {
                            nb++;
                            System.out.println("SysId=" + msg.sysId + " CompId=" + msg.componentId + " seq=" + msg.sequence + " " + msg.toString());
                        }
                    }
                }
            }
            do {
                msg = reader.getNextMessage(buffer, 0);
                if (msg != null) {
                    nb++;
                    System.out.println("SysId=" + msg.sysId + " CompId=" + msg.componentId + " seq=" + msg.sequence + " " + msg.toString());
                }
            }
            while (msg != null);
            dis.close();

            System.out.println("TOTAL BYTES = " + reader.getTotalBytesReceived());
            System.out.println("NBMSG (" + nb + ") : " + reader.getNbMessagesReceived() + " NBCRC=" + reader.getBadCRC() + " NBSEQ="
                               + reader.getBadSequence() + " NBLOST=" + reader.getLostBytes());
        }
        catch (Exception e) {
            System.out.println("ERROR : " + e);
            e.printStackTrace();
        }

    }
}

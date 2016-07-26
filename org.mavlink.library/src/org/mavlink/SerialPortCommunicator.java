package org.mavlink;

import jssc.SerialPort;
import jssc.SerialPortException;

public class SerialPortCommunicator {

	private SerialPort currentSerialPort = null;
	
	public boolean isOpened() {
		return currentSerialPort.isOpened();
	}

	public boolean openPort(String portName) {
		currentSerialPort = new SerialPort(portName);
		try {				
			if (currentSerialPort.openPort()) {
				//setup the port setting
//				currentSerialPort.setParams(
//						SerialPort.BAUDRATE_115200,
//						SerialPort.DATABITS_8,
//						SerialPort.STOPBITS_1,
//						SerialPort.PARITY_NONE
//						);
				return true;
			} else {
				return false;
			}
		} catch (SerialPortException ex) {
			System.out.println(ex);
			return false;
		}
	}


	public boolean writeData(byte[] data) {
		if (currentSerialPort != null && currentSerialPort.isOpened()) {
			//System.out.println(System.currentTimeMillis());
			try {
				currentSerialPort.writeBytes(data);
				//System.out.println(System.currentTimeMillis());
				return true;
			} catch (SerialPortException e) {
				e.printStackTrace();
				return false;
			}
		} else {
			return false;
		}
	}
	
	public byte[] readData() {
		if (currentSerialPort != null && currentSerialPort.isOpened()) {
			//System.out.println(System.currentTimeMillis());
			try {
				return currentSerialPort.readBytes();
				//System.out.println(System.currentTimeMillis());
			} catch (SerialPortException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("port is closed");
		}
		return null;
	}

	public boolean closePort() {
		try {
			if (currentSerialPort != null && currentSerialPort.closePort()) {
				return true;
			} else {
				return false;
			}
		} catch (SerialPortException e1) {
			e1.printStackTrace();
			return false;
		}
	}

}

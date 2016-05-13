package org.mavlink;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import jssc.SerialPortList;

public class Reader {
	private SerialPortCommunicator spc;
	private PipedInputStream in = new PipedInputStream();
	private PipedOutputStream out;
	
	public Reader(){
		spc = new SerialPortCommunicator();
		System.out.println("Trying to open " + SerialPortList.getPortNames()[0]);
		spc.openPort(SerialPortList.getPortNames()[0]);
		try {
			out = new PipedOutputStream(in);
		} catch (IOException e) {
			out = null;
			e.printStackTrace();
		}
	}

	public PipedInputStream read() {
		Thread thread = new Thread(new Runnable(){
			@Override
			public void run() {
				while (true){
					byte[] data = spc.readData();
					try {
						if (data != null) {
							out.write(data);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
//					if (data[5] == 0) {
//						//do nothing - heartbeat message
//					} else {
//						System.err.println("different message found!!@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
//					}
				}
			}
		});
		thread.start();
		return in;
	}
	
}

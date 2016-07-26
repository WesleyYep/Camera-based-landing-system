package serial;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import jssc.SerialPortList;

public class Reader {
	private SerialPortCommunicator spc;
	private PipedInputStream in = new PipedInputStream();
	private PipedOutputStream out;
	
	public Reader(SerialPortCommunicator spc){
		this.spc = spc;
		try {
			out = new PipedOutputStream(in);
		} catch (IOException e) {
			out = null;
			e.printStackTrace();
		}
	}

	public PipedInputStream read() {
		Thread thread = new Thread(new Runnable(){
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
				}
			}
		});

		thread.setDaemon(false);
		thread.start();
		return in;
	}
	
}

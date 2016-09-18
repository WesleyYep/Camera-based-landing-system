package network;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.DataInputStream;
import java.io.IOException;

import groundstation.GroundStation;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;

/**
 * This class acts as the server so the base station server can receive the data of the image stream from drone
 *
 */
public class StreamServer extends NetworkConnection {
	private int port;
	private GroundStation ca;
	
	public StreamServer(int port, GroundStation ca){
		super(null);
		this.port = port;
		this.ca = ca;
	}
	
	public void receiveBytes() {
		DataInputStream in;
		try {
			while (true) {
				if (connThread.socket != null) {
					in = new DataInputStream(connThread.socket.getInputStream());
					int numBytes = 640*480*3;
					int type = BufferedImage.TYPE_3BYTE_BGR;
					byte[] buf = new byte[numBytes];
					in.readFully(buf, 0, numBytes);
					
					try {
						BufferedImage bufferedImage = new BufferedImage(640, 480, type);
						final byte[] targetPixels = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
						System.arraycopy(buf, 0, targetPixels, 0, numBytes);
						WritableImage image = new WritableImage(640,480);
						SwingFXUtils.toFXImage(bufferedImage, image);
						ca.setStreamImage(image);
					} catch (Exception e) { e.printStackTrace(); }
				}else {
					Thread.sleep(1000);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	
	@Override
	protected boolean isServer(){
		return true;
	}
	
	@Override
	protected String getIP(){
		return null;
	}
	
	@Override
	protected int getPort(){
		return port;
	}

	@Override
	protected boolean isStreamingServer() {
		return true;
	}
}

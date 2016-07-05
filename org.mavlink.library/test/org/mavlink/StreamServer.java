package org.mavlink;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.function.Consumer;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import application.ChatApp;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class StreamServer extends NetworkConnection {
	
	private int port;
	private ChatApp ca;
	
	public StreamServer(int port, ChatApp ca){
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
					byte[] buf = new byte[640*480*3];
					in.readFully(buf, 0, 640*480*3);
					
					try {
						BufferedImage bufferedImage = new BufferedImage(640, 480, BufferedImage.TYPE_3BYTE_BGR);
						final byte[] targetPixels = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
						System.arraycopy(buf, 0, targetPixels, 0, 640*480*3);
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

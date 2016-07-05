package org.mavlink;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

public abstract class NetworkConnection {
	
	protected ConnectionThread connThread = new ConnectionThread();
	private Consumer<Serializable> onReceiveCallback;
	
	public NetworkConnection(Consumer<Serializable> onReceiveCallback){
		this.onReceiveCallback = onReceiveCallback;
		connThread.setDaemon(true);
	}
	
	public void startConnection() throws Exception{
		connThread.start();
	}
	public void send(Serializable data) throws Exception{
		connThread.out.writeObject(data); 
	}
	public void sendBytes(byte[] bytes) throws IOException {
		connThread.socket.getOutputStream().write(bytes);
	}
	
	public void closeConnection() throws Exception{
		connThread.socket.close();
	}
	
	protected abstract boolean isServer();
	protected abstract boolean isStreamingServer();
	protected abstract String getIP();
	protected abstract int getPort();
	
	
	protected class ConnectionThread extends Thread{
		protected Socket socket;
		private ObjectOutputStream out;
		
		@Override
		public void run(){
			while (true) {
				if (isStreamingServer()) {
					System.out.println("Streaming Server waiting for connection");
				} else if (isServer()) {
					System.out.println("Server waiting for connection");
				}
				try(ServerSocket server = isServer() ? new ServerSocket(getPort()) : null;
						Socket socket = isServer() ? server.accept() : new Socket(getIP(),getPort());
						ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
						ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
	
					this.socket = socket;
					this.out = out;
					socket.setTcpNoDelay(true);
					System.out.println("----------------------" + isStreamingServer());
					if (isStreamingServer() || getPort() == 55556) {
						while (true) {}
					}
					while(true){
						Serializable data = (Serializable) in.readObject();
						onReceiveCallback.accept(data);
					}
				}catch (Exception e){
					onReceiveCallback.accept("Connection closed");
				}
			}
		}
	}
	
}











package application;

import java.io.Serializable;
import java.util.function.Consumer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Client extends NetworkConnection {

	private String ip;
	private int port;
	
	public Client(String ip, int port, Consumer<Serializable> onReceiveCallback){
		super(onReceiveCallback);
		this.ip = ip;
		this.port = port;
	}
	
	@Override
	protected boolean isServer(){		
		return false;
	}
	
	@Override
	protected String getIP(){
		return ip;
	}
	
	@Override
	protected int getPort(){
		return port;
	}
	
	
	public static void main(String[] args) throws Exception {
		Client client = new Client("127.0.0.1", 55555, data ->{
			System.out.println(data.toString());
		});
		client.startConnection();

		while (true) {
			Thread.sleep(5000);
			try{
				client.send("Air station response");
				client.send("pos:-78:-6");
			}catch (Exception e){
				System.out.println("failed");
			}
		}
	}
	
}

package network;

import java.io.Serializable;
import java.util.function.Consumer;

import javafx.application.Application;
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

/**
 * This class acts as the server running on base station which the drone will connect to and send information to
 *
 */
public class Server extends NetworkConnection {
	
	private int port;
	
	public Server(int port, Consumer<Serializable> onReceiveCallback){
		super(onReceiveCallback);
		this.port = port;
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
		return false;
	}
}

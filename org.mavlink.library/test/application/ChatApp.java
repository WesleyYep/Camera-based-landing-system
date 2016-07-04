package application;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class ChatApp extends Application {

	private TextArea messages = new TextArea();
	private TextField input = new TextField();
	private Button btn = new Button("Send");
	private Button arm = new Button("Arm");
	private MenuBar menuBar = new MenuBar();
	private Menu menuA = new Menu("Menu");
	private HBox topMenu;
	private HBox botMenu;
	private Pane display;
	private Polygon landingPad;
	private NetworkConnection connection = createServer();
	private Label distanceText = new Label("Total distance: ");
	private Label altitudeText = new Label("Altitude: ");
	private Label positionText = new Label("Relative Position: ");
	private final EmbeddedMediaPlayerComponent mediaPlayerComponent = new EmbeddedMediaPlayerComponent();

	private Parent createContent(){
		messages.setPrefHeight(550);
		//Send event
		btn.setOnAction(event -> {
			if(!input.getText().isEmpty()){
				String message = "BaseStation: " ;
				message += input.getText();
				input.clear();

				messages.appendText(message + "\n");
				try{
					connection.send(message);
				}catch (Exception e){
					messages.appendText("Failed to send\n");
				}
			}
		});

		//Arm event
		arm.setOnAction(event -> {
			try{
				connection.send("ARMMMMMMMMMM");
			}catch (Exception e){
				messages.appendText("Failed to send\n");
			}

		});
		
		VBox root = new VBox(10, messages, input);
		//root.getChildren().add(arm);
		topMenu = new HBox();
		menuA.getItems().add(new MenuItem("first"));
		menuBar.getMenus().addAll(menuA);
		topMenu.getChildren().add(menuBar);
		botMenu = new HBox(5,btn,arm);
		
//		Polygon drone = new Polygon(172, 128, 212, 128, 192, 88); 
		landingPad = new Polygon(172, 128, 212, 128, 192, 78);
		landingPad.setFill(Color.RED);
        display = new Pane(landingPad, distanceText, altitudeText, positionText);
        altitudeText.setTranslateY(20);
        positionText.setTranslateY(40);
        distanceText.setFont(new Font("Serif", 18));
        altitudeText.setFont(new Font("Serif", 18));
        positionText.setFont(new Font("Serif", 18));
        display.setPrefSize(384, 216);
        display.setMaxHeight(216);
        display.setBackground(new Background(new BackgroundFill(Color.ALICEBLUE, null, null)));
		return root;
	}


	@Override
	public void init() throws Exception{
		connection.startConnection();
	}


	@Override
	public void start(Stage primaryStage) throws Exception{
		input.setPromptText("Message");
		BorderPane borderPane = new BorderPane();
		borderPane.setPadding(new Insets(10,10,10,10));
		borderPane.setLeft(createContent());
		borderPane.setTop(topMenu);
		borderPane.setBottom(botMenu);
		borderPane.setRight(display);
		BorderPane.setMargin(display, new Insets(20,20,20,20));
		Scene scene = new Scene(borderPane,904,400);
		scene.getStylesheets().add(getClass().getResource("/application/Chat.css").toExternalForm());
		primaryStage.setScene(scene);
		primaryStage.setTitle("Base Station");
		primaryStage.show();
		
		displayStreamFrame();
	}


	private void displayStreamFrame() {
        JFrame frame = new JFrame("Video Stream");
        frame.setContentPane(mediaPlayerComponent);
        frame.setLocation(100, 100);
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
//        mediaPlayerComponent.getMediaPlayer().playMedia("test3.mp4");
        mediaPlayerComponent.getMediaPlayer().playMedia("http://192.168.1.3:8080/?action=stream");
	}


	@Override
	public void stop() throws Exception{
		connection.closeConnection();
	}

	private Server createServer(){
		return new Server(55555, data ->{
			Platform.runLater(() -> {
				messages.appendText(data.toString() + "\n");
				if (data.toString().startsWith("pos:")) {
					//pos:x:y
					String[] arr = data.toString().split(":");
					double x = Double.parseDouble(arr[1]) * -1; //they appear reversed
					double y = Double.parseDouble(arr[2]) * -1;
					landingPad.setRotate(Math.toDegrees(Math.atan2(-x, -y))); //swap due to camera orientation
					positionText.setText(String.format("Relative Position: x=%.2f y=%.2f", x, y));
				} else if (data.toString().startsWith("dist:")) {
					distanceText.setText(String.format("Total Distance: %.2f", Double.parseDouble(data.toString().split(":")[1])));
				} else if (data.toString().startsWith("alt:")) {
					altitudeText.setText(String.format("Altitude: %.2f" , Double.parseDouble(data.toString().split(":")[1])));
				}
			});
		});
	}

	public static void main(String[] args) {
		NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), "C:\\Program Files\\VideoLAN\\VLC");
        Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
		launch(args);
	}

}

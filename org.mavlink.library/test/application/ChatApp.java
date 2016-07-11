package application;

import org.controlsfx.control.RangeSlider;
import org.mavlink.NetworkConnection;
import org.mavlink.Server;
import org.mavlink.StreamServer;
import org.opencv.core.Core;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;

public class ChatApp extends Application {

	private TextArea messages = new TextArea();
	private TextField input = new TextField();
	private Button btn = new Button("Send");
	private MenuBar menuBar = new MenuBar();
	private Menu menuA = new Menu("Menu");
	private CheckBox streamToggle = new CheckBox("Stream");
//	private CheckBox binaryToggle = new CheckBox("Binary");
	private HBox topMenu;
	private HBox botMenu;
	private Pane display;
	private Polygon landingPad;
	private NetworkConnection connection = createServer();
	private Label distanceText = new Label("Total distance: ");
	private Label altitudeText = new Label("Altitude: ");
	private Label positionText = new Label("Relative Position: ");
	private ImageView imgView = new ImageView();
	private RangeSlider hSlider = new RangeSlider(0,180, 0, 10);
	private RangeSlider sSlider = new RangeSlider(0,255,100,255);
	private RangeSlider vSlider = new RangeSlider(0,255,100,255);
	private CheckBox armCheckBox = new CheckBox("Arm");
	private RadioButton stabilizeModeButton = new RadioButton("Stabilize");
	private RadioButton loiterModeButton = new RadioButton("Loiter");
	private RadioButton landModeButton = new RadioButton("Land");
	private Label modeLabel = new Label("Mode: 0 , Custom Mode: 0");
	private Label landLabel = new Label("Landing pad not flat");
	
    static {
        // Load the native OpenCV library
        System.out.println(System.getProperty("java.library.path"));
        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
    }
	
	private Parent createContent(){
		messages.setPrefHeight(550);
		//Send event
		btn.setOnAction(event -> {
			if(!input.getText().isEmpty()){
//				String message = "BaseStation: " ;
				String message = input.getText();
				input.clear();

				messages.appendText(message + "\n");
				try{
					connection.send(message);
				}catch (Exception e){
					messages.appendText("Failed to send\n");
				}
			}
		});

		//ARM/DISARM event
		armCheckBox.setOnAction(event -> {
			try {
				if(armCheckBox.isSelected() == true){
					System.out.println("ARMED");
					connection.send("arm:true");
				} else{
					System.out.println("DISARMED");
					//cbox.setSelected(true);
					connection.send("arm:false");
				}
			} catch (Exception e) {
				messages.appendText("Failed to send\n");
			}
		});
				
		//Changing mode
		stabilizeModeButton.setOnAction(event -> {
			try{
				if(stabilizeModeButton.isSelected() == true){
					System.out.println("STABILIZE");
					connection.send("mode:stabilize:"+armCheckBox.isSelected());
				} 				
			}catch (Exception e){
				messages.appendText("Failed to send\n");
			}

		});
		
		loiterModeButton.setOnAction(event -> {
			try{
				if(loiterModeButton.isSelected() == true){
					System.out.println("LOITER");
					connection.send("mode:loiter:"+armCheckBox.isSelected());
				}
			}catch (Exception e){
				messages.appendText("Failed to send\n");
			}

		});
		
		landModeButton.setOnAction(event -> {
			try{
				if(landModeButton.isSelected() == true){
					System.out.println("LAND");
					connection.send("mode:land:"+armCheckBox.isSelected());
				}
			}catch (Exception e){
				messages.appendText("Failed to send\n");
			}
		});
		
		streamToggle.setOnAction(event -> {
			try{
				connection.send("stream:" + streamToggle.isSelected());
				if (!streamToggle.isSelected()) {
					imgView.setImage(new WritableImage(640,480));
				}
			}catch (Exception e){
				messages.appendText("Failed to send\n");
			}
		});
		
		ToggleGroup group = new ToggleGroup();
		stabilizeModeButton.setToggleGroup(group);
		loiterModeButton.setToggleGroup(group);
		landModeButton.setToggleGroup(group);
		
		hSlider.setOnMouseReleased(event -> {sliderChanged("h");});
		sSlider.setOnMouseReleased(event -> {sliderChanged("s");});
		vSlider.setOnMouseReleased(event -> {sliderChanged("v");});
		hSlider.setPrefWidth(250);
		hSlider.setShowTickLabels(true);
		hSlider.setShowTickMarks(true);
		sSlider.setPrefWidth(250);
		sSlider.setShowTickLabels(true);
		sSlider.setShowTickMarks(true);		
		vSlider.setPrefWidth(250);
		vSlider.setShowTickLabels(true);
		vSlider.setShowTickMarks(true);
		VBox sliderBox = new VBox(new Label("H"), hSlider, new Label("S"),  sSlider, new Label("V"), vSlider, modeLabel);
		sliderBox.setTranslateY(300);
		landLabel.setTranslateY(250);
		
		imgView.setImage(new WritableImage(640, 480));
		imgView.setFitWidth(640);
		imgView.setFitHeight(480);
		imgView.setStyle("-fx-background-color: BLACK");
				
		VBox root = new VBox(10, imgView, input);
		//root.getChildren().add(arm);
		topMenu = new HBox();
		menuA.getItems().add(new MenuItem("first"));
		menuBar.getMenus().addAll(menuA);
		topMenu.getChildren().add(menuBar);
		botMenu = new HBox(5,btn, streamToggle, armCheckBox, stabilizeModeButton, loiterModeButton, landModeButton);
		
//		Polygon drone = new Polygon(172, 128, 212, 128, 192, 88); 
		landingPad = new Polygon(172, 128, 212, 128, 192, 78);
		landingPad.setFill(Color.RED);
        display = new Pane(landingPad, /*distanceText,*/ altitudeText, positionText, landLabel, sliderBox);
        altitudeText.setTranslateY(20);
        positionText.setTranslateY(40);
        distanceText.setFont(new Font("Serif", 18));
        altitudeText.setFont(new Font("Serif", 18));
        positionText.setFont(new Font("Serif", 18));
        landLabel.setFont(new Font("Serif", 18));
        display.setPrefSize(384, 216);
        display.setMaxHeight(216);
        display.setBackground(new Background(new BackgroundFill(Color.ALICEBLUE, null, null)));
		return root;
	}


	private void sliderChanged(String type) {
		try{
			if (type.equals("h") || type.equals("all")) { connection.send("slider:h:" + hSlider.getLowValue() + ":" + hSlider.getHighValue());}
			if (type.equals("s") || type.equals("all")) { connection.send("slider:s:" + sSlider.getLowValue() + ":" + sSlider.getHighValue());}
			if (type.equals("v") || type.equals("all")) { connection.send("slider:v:" + vSlider.getLowValue() + ":" + vSlider.getHighValue());}
		}catch (Exception e){
			messages.appendText("Failed to send\n");
		}
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
		Scene scene = new Scene(borderPane,1100,600);
		scene.getStylesheets().add(getClass().getResource("/application/Chat.css").toExternalForm());
		primaryStage.setScene(scene);
		primaryStage.setTitle("Base Station");
		primaryStage.show();
	}

	@Override
	public void stop() throws Exception{
		connection.closeConnection();
	}

	private Server createServer(){
		//create stream server
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				StreamServer streamServer = new StreamServer(55556, ChatApp.this);
				try {
					streamServer.startConnection();
					streamServer.receiveBytes();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.start();
		
		//create message server
		return new Server(55555, data ->{
			Platform.runLater(() -> {
//				System.out.println(data.toString());
				String[] arr = data.toString().split(":");
				if (data.toString().startsWith("start")) {
					sliderChanged("all");
				} else if (data.toString().startsWith("pos:")) {
					//pos:x:y
					double x = Double.parseDouble(arr[1]) * -1; //they appear reversed
					double y = Double.parseDouble(arr[2]) * -1;
					landingPad.setRotate(Math.toDegrees(Math.atan2(-x, -y))); //swap due to camera orientation
					positionText.setText(String.format("Relative Position: x=%.2f y=%.2f", x, y));
				} else if (data.toString().startsWith("dist:")) {
//					distanceText.setText(String.format("Total Distance: %.2f", Double.parseDouble(data.toString().split(":")[1])));
				} else if (data.toString().startsWith("alt:")) {
					altitudeText.setText(String.format("Altitude: %.2f" , Double.parseDouble(data.toString().split(":")[1])));
				} else if (data.toString().startsWith("mode:")) {
					modeLabel.setText("Mode: " + arr[1] + " , Custom Mode: " + arr[2]);
				} else if (data.toString().startsWith("flat")) {
					if (arr[1].equals("true")) {
						landLabel.setText("Landing pad is flat, can land safely");
					} else {
						landLabel.setText("Landing pad not flat");
					}
				}
			});
		});
	}

	public static void main(String[] args) {
		launch(args);
	}


	public void setStreamImage(Image i) {
		imgView.setImage(i);
	}

}

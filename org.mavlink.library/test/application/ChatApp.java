package application;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.opencv.core.Core;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
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
	private WritableImage image;
	private ImageView imgView = new ImageView();
	
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
		image = new WritableImage(640, 480);
		imgView.setImage(image);
				
		VBox root = new VBox(10, imgView, input);
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
		return new Server(55555, data ->{
			Platform.runLater(() -> {
//				messages.appendText(data.toString() + "\n");
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
				} else if (data.toString().startsWith("[")) {
					try {
						ByteArrayInputStream bais = new ByteArrayInputStream(convertToBytes(data));
						ObjectInputStream ois = new ObjectInputStream(bais);
						Object obj = ois.readObject();
						ois.close();
						BufferedImage bufferedImage = new BufferedImage(640, 480, BufferedImage.TYPE_3BYTE_BGR);
						final byte[] targetPixels = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
						byte[] b = ((byte[])obj);
						System.arraycopy(b, 0, targetPixels, 0, b.length);
						SwingFXUtils.toFXImage(bufferedImage, image);
						imgView.setImage(image);
					} catch (Exception e) { e.printStackTrace(); }
				}
			});
		});
	}
	
	private byte[] convertToBytes(Serializable data) throws IOException {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutput out = new ObjectOutputStream(bos)) {
			out.writeObject(data);
			return bos.toByteArray();
		}
	}


	public static void main(String[] args) {
		launch(args);
	}

}

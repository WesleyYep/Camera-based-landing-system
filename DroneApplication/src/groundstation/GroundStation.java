package groundstation;

import org.controlsfx.control.RangeSlider;
import org.opencv.core.Core;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import network.NetworkConnection;
import network.Server;
import network.StreamServer;
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
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;

public class GroundStation extends Application {

	private TextArea messages = new TextArea();
	private TextField input = new TextField();
	private Button btn = new Button("Send");
	private MenuBar menuBar = new MenuBar();
	private Menu menuA = new Menu("Menu");
	private CheckBox streamToggle = new CheckBox("Stream");
	// private CheckBox binaryToggle = new CheckBox("Binary");
	private HBox topMenu;
	private HBox botMenu;
	private Pane display;
	private Polygon landingArrow;
	private NetworkConnection connection = createServer();
	private Label distanceText = new Label("Total distance: ");
	private Label altitudeText = new Label("Altitude: ");
	private Label positionText = new Label("Relative Position: ");
	private ImageView imgView = new ImageView();
	private RangeSlider hSlider = new RangeSlider(0, 180, 0, 10);
	private RangeSlider sSlider = new RangeSlider(0, 255, 100, 255);
	private RangeSlider vSlider = new RangeSlider(0, 255, 100, 255);
	private CheckBox armCheckBox = new CheckBox("Arm");
	private CheckBox testCheckBox = new CheckBox("Test");
	private RadioButton stabilizeModeButton = new RadioButton("Stabilize");
	private RadioButton loiterModeButton = new RadioButton("PosHold");
	private RadioButton landModeButton = new RadioButton("Land");
	private RadioButton guidedModeButton = new RadioButton("Guided");
	private RadioButton altHoldModeButton = new RadioButton("Alt_Hold");
	private Label modeLabel = new Label("Mode: 0 , Custom Mode: 0");
	private Label landLabel = new Label("Landing pad not flat");
	private long lastModeChangedTime;

	static {
		// Load the native OpenCV library
		System.out.println(System.getProperty("java.library.path"));
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	private Parent createContent() {
		messages.setPrefHeight(550);
		// Send event
		btn.setOnAction(event -> {
			if (!input.getText().isEmpty()) {
				// String message = "BaseStation: " ;
				String message = input.getText();
				//input.clear();
				//System.out.println(message + "\n");
				try {
					connection.send(message);
				} catch (Exception e) {
					System.out.println("failed to send");
				}
			}
		});

		// ARM/DISARM event
		armCheckBox.setOnAction(event -> {
			try {
				startTimer();
				if (armCheckBox.isSelected() == true) {
					System.out.println("ARMED");
					connection.send("arm:true");
				} else {
					System.out.println("DISARMED");
					// cbox.setSelected(true);
					connection.send("arm:false");
				}
			} catch (Exception e) {
				System.out.println("failed to send");
			}
		});

		//switch to start/stop the landing test
		// ARM/DISARM event
		testCheckBox.setOnAction(event -> {
			try {
				if (testCheckBox.isSelected() == true) {
					System.out.println("TEST TRACKING");
					connection.send("test:test:true");
				} else {
					System.out.println("STOP TRACKING");
					// cbox.setSelected(true);
					connection.send("test:test:false");
				}
			} catch (Exception e) {
				System.out.println("failed to send");
			}
		});
		
		// Changing mode
		stabilizeModeButton.setOnAction(event -> {
			try {
				if (stabilizeModeButton.isSelected() == true) {
					System.out.println("STABILIZE");
					startTimer();
					connection.send("mode:stabilize:" + armCheckBox.isSelected());
				}
			} catch (Exception e) {
				System.out.println("failed to send");
			}

		});

		loiterModeButton.setOnAction(event -> {
			try {
				if (loiterModeButton.isSelected() == true) {
					System.out.println("LOITER");
					startTimer();
					connection.send("mode:loiter:" + armCheckBox.isSelected());
				}
			} catch (Exception e) {
				System.out.println("failed to send");
			}

		});

		landModeButton.setOnAction(event -> {
			try {
				if (landModeButton.isSelected() == true) {
					System.out.println("LAND");
					startTimer();
					connection.send("mode:land:" + armCheckBox.isSelected());
				}
			} catch (Exception e) {
				System.out.println("failed to send");
			}
		});

		guidedModeButton.setOnAction(event -> {
			try {
				if (guidedModeButton.isSelected() == true) {
					System.out.println("GUIDED");
					startTimer();
					connection.send("mode:guided:" + armCheckBox.isSelected());
				}
			} catch (Exception e) {
				System.out.println("failed to send");
			}
		});
		
		altHoldModeButton.setOnAction(event -> {
			try {
				if (altHoldModeButton.isSelected() == true) {
					System.out.println("ALT_HOLD");
					startTimer();
					connection.send("mode:alt_hold:" + armCheckBox.isSelected());
				}
			} catch (Exception e) {
				System.out.println("failed to send");
			}
		});

		streamToggle.setOnAction(event -> {
			try {
				connection.send("stream:" + streamToggle.isSelected());
				if (!streamToggle.isSelected()) {
					imgView.setImage(new WritableImage(640, 480));
				}
			} catch (Exception e) {
				System.out.println("failed to send");
			}
		});

		ToggleGroup group = new ToggleGroup();
		stabilizeModeButton.setToggleGroup(group);
		loiterModeButton.setToggleGroup(group);
		landModeButton.setToggleGroup(group);
		guidedModeButton.setToggleGroup(group);
		altHoldModeButton.setToggleGroup(group);

		hSlider.setOnMouseReleased(event -> {
			sliderChanged("h");
		});
		sSlider.setOnMouseReleased(event -> {
			sliderChanged("s");
		});
		vSlider.setOnMouseReleased(event -> {
			sliderChanged("v");
		});
		hSlider.setPrefWidth(250);
		hSlider.setShowTickLabels(true);
		hSlider.setShowTickMarks(true);
		sSlider.setPrefWidth(250);
		sSlider.setShowTickLabels(true);
		sSlider.setShowTickMarks(true);
		vSlider.setPrefWidth(250);
		vSlider.setShowTickLabels(true);
		vSlider.setShowTickMarks(true);
		VBox sliderBox = new VBox(new Label("H"), hSlider, new Label("S"), sSlider, new Label("V"), vSlider, modeLabel);
		sliderBox.setTranslateY(300);
		landLabel.setTranslateY(250);

		imgView.setImage(new WritableImage(640, 480));
		imgView.setFitWidth(640);
		imgView.setFitHeight(480);
		imgView.setStyle("-fx-background-color: BLACK");

		VBox root = new VBox(10, imgView, input);
		// root.getChildren().add(arm);
		topMenu = new HBox();
		menuA.getItems().add(new MenuItem("first"));
		menuBar.getMenus().addAll(menuA);
		topMenu.getChildren().add(menuBar);
		botMenu = new HBox(5, btn, streamToggle, armCheckBox, stabilizeModeButton, loiterModeButton, landModeButton,
				guidedModeButton, altHoldModeButton, testCheckBox);

		// Polygon drone = new Polygon(172, 128, 212, 128, 192, 88);
		landingArrow = new Polygon(172, 128, 212, 128, 192, 78);
		landingArrow.setFill(Color.RED);
		display = new Pane(landingArrow, /* distanceText, */ altitudeText, positionText, landLabel, sliderBox);
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

	private void startTimer() {
		lastModeChangedTime = System.currentTimeMillis();
	}

	private void sliderChanged(String type) {
		try {
			if (type.equals("h") || type.equals("all")) {
				connection.send("slider:h:" + hSlider.getLowValue() + ":" + hSlider.getHighValue());
			}
			if (type.equals("s") || type.equals("all")) {
				connection.send("slider:s:" + sSlider.getLowValue() + ":" + sSlider.getHighValue());
			}
			if (type.equals("v") || type.equals("all")) {
				connection.send("slider:v:" + vSlider.getLowValue() + ":" + vSlider.getHighValue());
			}
		} catch (Exception e) {
			System.out.println("failed to send");
		}
	}

	@Override
	public void init() throws Exception {
		connection.startConnection();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		input.setPromptText("Message");
		BorderPane borderPane = new BorderPane();
		borderPane.setPadding(new Insets(10, 10, 10, 10));
		borderPane.setLeft(createContent());
		borderPane.setTop(topMenu);
		borderPane.setBottom(botMenu);
		borderPane.setRight(display);
		BorderPane.setMargin(display, new Insets(20, 20, 20, 20));
		Scene scene = new Scene(borderPane, 1100, 600);
		scene.getStylesheets().add(getClass().getResource("/application/Chat.css").toExternalForm());
		primaryStage.setScene(scene);
		primaryStage.setTitle("Base Station");
		primaryStage.show();

		scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				try {
					switch (event.getCode()) {
					case W:
						connection.send("command:forward");
						break;
					case A:
						connection.send("command:left");
						break;
					case S:
						connection.send("command:backward");
						break;
					case D:
						connection.send("command:right");
						break;
					case X:
						connection.send("command:centre");
						break;
					case C:
						connection.send("command:descend");
						break;
					case DIGIT1:
						connection.send("mode:stabilize:" + armCheckBox.isSelected());
						startTimer();
						break;
					case DIGIT2:
						connection.send("mode:loiter:" + armCheckBox.isSelected());
						startTimer();
						break;
					case DIGIT3:
						connection.send("mode:land:" + armCheckBox.isSelected());
						startTimer();
						break;
					case DIGIT4:
						connection.send("mode:guided:" + armCheckBox.isSelected());
						startTimer();
						break;
					case DIGIT5:
						connection.send("mode:alt_hold:" + armCheckBox.isSelected());
						startTimer();
						break;
					default:
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void stop() throws Exception {
		connection.closeConnection();
	}

	private Server createServer() {
		// create stream server
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				StreamServer streamServer = new StreamServer(55556, GroundStation.this);
				try {
					streamServer.startConnection();
					streamServer.receiveBytes();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.start();

		// create message server
		return new Server(55555, data -> {
			Platform.runLater(() -> {
				// System.out.println(data.toString());
				String[] arr = data.toString().split(":");
				if (data.toString().startsWith("start")) {
					sliderChanged("all");
					// Joystick joystick = new Joystick(connection);
					// joystick.setVisible(true);
				} else if (data.toString().startsWith("pos:")) {
					// pos:x:y
					double x = Double.parseDouble(arr[1]) * -1; 
					double y = Double.parseDouble(arr[2]);
					landingArrow.setRotate(Math.toDegrees(Math.atan2(-x, -y))); 
					positionText.setText(String.format("Relative Position: x=%.2f y=%.2f", x, y));
				} else if (data.toString().startsWith("dist:")) {
					// distanceText.setText(String.format("Total Distance:
					// %.2f",
					// Double.parseDouble(data.toString().split(":")[1])));
				} else if (data.toString().startsWith("alt:")) {
					altitudeText.setText(
							String.format("Altitude: %.2f", Double.parseDouble(data.toString().split(":")[1])));
				} else if (data.toString().startsWith("mode:")) {
					modeLabel.setText("Mode: " + arr[1] + " , Custom Mode: " + arr[2]);
					int mode = (int) Double.parseDouble(arr[1]);
					int customMode = (int) Double.parseDouble(arr[2]);
					if (mode > 200 && System.currentTimeMillis() - lastModeChangedTime > 3000) {
						armCheckBox.setSelected(true);
					} else if (mode < 200 && System.currentTimeMillis() - lastModeChangedTime > 3000) {
						armCheckBox.setSelected(false);
					}
					if ((mode == 81 || mode == 209) && customMode == 0
							&& System.currentTimeMillis() - lastModeChangedTime > 3000) {
						stabilizeModeButton.setSelected(true);
					} else if ((mode == 81 || mode == 209) && customMode == 9
							&& System.currentTimeMillis() - lastModeChangedTime > 3000) {
						landModeButton.setSelected(true);
					} else if ((mode == 81 || mode == 209) && customMode == 16 //5 - previously
							&& System.currentTimeMillis() - lastModeChangedTime > 3000) {
						loiterModeButton.setSelected(true);
					} else if ((mode == 89 || mode == 218) && customMode == 4
							&& System.currentTimeMillis() - lastModeChangedTime > 3000) {
						guidedModeButton.setSelected(true);
					} else if ((mode == 81 || mode == 209) && customMode == 2
							&& System.currentTimeMillis() - lastModeChangedTime > 3000) {
						altHoldModeButton.setSelected(true);
					}
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
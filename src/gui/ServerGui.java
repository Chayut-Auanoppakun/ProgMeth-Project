package gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import logic.ClientLogic;
import logic.ServerLogic;
import logic.State;

import java.util.Random;
import gui.GameWindow; // not used yet
import javafx.scene.layout.Pane;
import gui.MainMenuPane;

public class ServerGui extends Pane {
	private static TextArea logArea = new TextArea(); // Create a TextArea for logging
	private static TextArea typeArea = new TextArea(); // Create a TextArea for typing messages
	private static Button sendButton = new Button("Send"); // Create a Send button
	private static Button startButton = new Button("Connect"); // Create a Connect button
	private static Button toGameButton = new Button("Start Match");
	private int serverPort;
	static State CurState;
	private static boolean isGameWindow = false;
	Stage thisStage;
	public ServerGui(State state, Stage primaryStage) {
		thisStage = primaryStage;
		this.setStyle("-fx-background-color: #1e1e1e;"); // Dark background
		CurState = state;
		serverPort = new Random().nextInt(64512) + 1024; // Gen Port for Broad Cast

		String LabelName = "Host";
		startButton.setText("Start");
		if (CurState.equals(logic.State.CLIENT)) {
			LabelName = "Client";
		}
		// Label styling
		Label label = new Label(LabelName);
		label.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: bold;");

		// Chat history (log area)
		logArea.setPrefSize(450, 250);
		logArea.setStyle("-fx-background-color: black; -fx-text-fill: black; -fx-border-color: #444;");
		logArea.setEditable(false);
		// Message input field
		typeArea.setPrefSize(380, 30);
		typeArea.setStyle("-fx-background-color: #222; -fx-text-fill: black;");

		// Send button
		sendButton.setPrefSize(65, 30);
		sendButton.setStyle("-fx-background-color: #444; -fx-text-fill: white;");

		startButton.setPrefSize(65, 30);
		startButton.setStyle("-fx-background-color: #444; -fx-text-fill: white;");

		toGameButton.setPrefSize(140, 30);
		toGameButton.setStyle("-fx-background-color: #444; -fx-text-fill: white;");

		// Enable Disable Buttons
		sendButton.setDisable(true);
		toGameButton.setDisable(true);

		// set functions
		// 1. The Start/Connect Button
		// 2. The Send Button
		// Should work as long as we gen a new ServerGui obj every time we open the Server Window
		if (CurState.equals(logic.State.SERVER)) {
			startButton.setOnAction(event -> {
				broadcastServer();
				ServerLogic.startServer(CurState, logArea, serverPort);
			});
			sendButton.setOnAction(event -> {
				ServerLogic.sendMessageToClients(typeArea.getText(), logArea);
				logArea.clear();
			});

		} else if (CurState.equals(logic.State.CLIENT)) {
			startButton.setOnAction(event -> connectClient());
			sendButton.setOnAction(event -> ClientLogic.sendMessage(typeArea.getText(), logArea));
		}
		
		toGameButton.setOnAction(event -> {
			startGame();
		});

		// Chat box layout (Stack messages horizontally)
		HBox buttonSet = new HBox(10, sendButton, startButton);
		VBox vbuttonSet = new VBox(10, buttonSet, toGameButton);
		HBox messageInput = new HBox(10, typeArea, vbuttonSet);
		messageInput.setPadding(new Insets(5));
		messageInput.setAlignment(Pos.CENTER_LEFT);

		// Main chat layout (Vertical stacking)
		VBox chatBox = new VBox(5, label, logArea, messageInput);
		chatBox.setPadding(new Insets(10));
		chatBox.setStyle("-fx-border-color: #555; -fx-border-width: 2px;");

		// Add to the Pane
		this.getChildren().add(chatBox);
        primaryStage.setOnCloseRequest(this::handleWindowClose);
	}

	public void broadcastServer() {
			log("Starting broadcast...");
			logArea.clear();
			sendButton.setDisable(false);
			startButton.setDisable(true);
			
			startButton.setDisable(true); // Disable Start BT
			MainMenuPane.setNameDisable(true); // Disable nameField
			MainMenuPane.setHostDisable(true); // Disable HOST
			MainMenuPane.setJoinDisable(true); // Disable JOIN
			typeArea.setPromptText("Send massage to connected clients...");
			CurState = logic.State.SERVER;
			ServerLogic.startBroadcasting(CurState, logArea, MainMenuPane.getServerName(), serverPort);
	}

	public void connectClient() {
			log("Connecting as client...");
			logArea.clear();
			MainMenuPane.setNameDisable(true); // Disable nameField
			MainMenuPane.setHostDisable(true); // Disable HOST
			MainMenuPane.setJoinDisable(true); // Disable JOIN
			startButton.setText("Connect");
			startButton.setOnAction(event -> {
	            String message = typeArea.getText();
	            try {
	                int serverIndex = Integer.parseInt(message.trim()) - 1;
	                boolean success = ClientLogic.connectToServer(serverIndex, logArea, MainMenuPane.getPlayerName());
	                if (success) {
	                toGameButton.setDisable(false);
	                sendButton.setDisable(false);
	                startButton.setDisable(true);
	                typeArea.clear();
	                typeArea.setPromptText("Type your message here...");
	                }
	            } catch (NumberFormatException e) {
	                log("Invalid input. Please enter a number.");
	            }
	        });
			//startButton.setDisable(true);
			typeArea.setPromptText("Enter server number to connect first...");
			CurState = logic.State.CLIENT;
			ClientLogic.startClient(CurState, logArea);
	}

	public static void stopGame() {
		log("Stopping ...");
		MainMenuPane.setNameDisable(false); // Re-enable nameField
		MainMenuPane.setHostDisable(false); // Enable HOST
		MainMenuPane.setJoinDisable(false); // Enable JOIN
		typeArea.setPromptText("");

		if (CurState.equals(logic.State.SERVER)) {
			CurState = logic.State.IDLE;
			ServerLogic.stopServer();
		}

		if (CurState.equals(logic.State.CLIENT)) {
			CurState = logic.State.IDLE;
			ClientLogic.stopClient(logArea); // Pass logArea to stopClient
		}
		sendButton.setDisable(true);
		startButton.setDisable(false);
		toGameButton.setDisable(true);
		logArea.clear();
	}

	public void handleWindowClose(WindowEvent event) {
		log("Window is closing...");
		CurState = logic.State.IDLE;
		ServerLogic.stopServer();
		ClientLogic.stopClient(logArea);
		System.out.println("Window is closing...");
		Platform.exit();
		System.exit(0);
	}

	private static void log(String message) {
		logArea.appendText(message + "\n");
	}

	public static void switchbuttonDisable() {
		if (sendButton.isDisable() && !startButton.isDisable()) {
			sendButton.setDisable(false);
			startButton.setDisable(true);
		} else if (!sendButton.isDisable() && startButton.isDisable()) {
			sendButton.setDisable(true);
			startButton.setDisable(false);
		}
	}

	

	public static void settoGamedisable(boolean set) {
		toGameButton.setDisable(set);
	}

	private void startGame() {
		isGameWindow = true;
		Stage gameStage = new Stage();
		new GameWindow().start(gameStage); // Start the GameWindow in a new window
		settoGamedisable(true);
	}

	public static boolean isGameWindow() {
		return isGameWindow;
	}

	public static int getState() { //0 Idle, 1 Server, 2 Client;
		if (CurState.equals(logic.State.SERVER)) {
			return 1;
		}
		else if (CurState.equals(logic.State.CLIENT)) {
			return 2;
		}
		else {
			return 0;
		}
	}

}

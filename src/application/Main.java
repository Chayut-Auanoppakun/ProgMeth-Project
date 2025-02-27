package application;

import javafx.application.Application;
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
import server.GameServer;
import client.GameClient;
import java.util.Random;

public class Main extends Application {

    private static SharedState sharedState = new SharedState(); // Create a SharedState instance
    private static TextArea logArea = new TextArea(); // Create a TextArea for logging
    private static TextArea typeArea = new TextArea(); // Create a TextArea for typing messages
    private static Button sendButton = new Button("Send"); // Create a Send button
    private static Button connectButton = new Button("Connect"); // Create a Connect button
    private static Button hostServerButton = new Button("Host a Server");
    private static Button connectClientButton = new Button("Connect as Client");
    private static Button cancelButton = new Button("Cancel");
    private static Button toGameButton = new Button("To game");
    private static TextField nameField = new TextField(); // Create a TextField for user name
    private int serverPort;
    private static boolean isGameWindow = false;
    private static int State = 0; //0 Idle, 1 Server, 2 Client;
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Main Test Window");
        
        serverPort = new Random().nextInt(64512) + 1024;
        
        Label label = new Label("Choose Function!");
        nameField.setPromptText("Enter your name");

        hostServerButton.setOnAction(event -> {
            broadcastServer();
            GameServer.startServer(sharedState, logArea,serverPort); // Start the server for message handling
        });
        
        connectClientButton.setOnAction(event -> connectClient());

        cancelButton.setOnAction(event -> cancelBroadcast());
        cancelButton.setDisable(true);
        toGameButton.setDisable(true);
        toGameButton.setPrefWidth(140);
        sendButton.setPrefWidth(65);
        connectButton.setPrefWidth(65);


        VBox controls = new VBox();
        controls.setSpacing(20);
        controls.setAlignment(Pos.CENTER);
        controls.getChildren().addAll(label, nameField, hostServerButton, connectClientButton, cancelButton);
        controls.setPadding(new Insets(10));

        logArea.setEditable(false);
        logArea.setPrefWidth(600);
        logArea.setPrefHeight(350);

        typeArea.setPrefHeight(50);
        typeArea.setPrefWidth(500);
        

        sendButton.setDisable(true);
        connectButton.setDisable(true);
        sendButton.setOnAction(event -> {
            if (sharedState.isClient()) {
                GameClient.sendMessage(typeArea.getText(), logArea);
            } else {
                GameServer.sendMessageToClients(typeArea.getText(), logArea);
            }
            typeArea.clear();
        });

        connectButton.setOnAction(event -> {
            String message = typeArea.getText();
            try {
                int serverIndex = Integer.parseInt(message.trim()) - 1;
                boolean success = GameClient.connectToServer(serverIndex, logArea, getPlayerName());
                if (success) {
                toGameButton.setDisable(false);
                sendButton.setDisable(false);
                connectButton.setDisable(true);
                typeArea.clear();
                typeArea.setPromptText("Type your message here...");
                }
            } catch (NumberFormatException e) {
                log("Invalid input. Please enter a number.");
            }
        });
        
        toGameButton.setOnAction(event ->{
        	System.out.println("to game");
        	startGame();
        });
        HBox SCbuttons = new HBox();
        SCbuttons.setSpacing(10);
        SCbuttons.getChildren().addAll(sendButton, connectButton);
        
        VBox buttonStack = new  VBox();
        buttonStack.setSpacing(5);
        buttonStack.getChildren().addAll(SCbuttons, toGameButton);
        
        HBox messageBox = new HBox();
        messageBox.setSpacing(10);
        messageBox.getChildren().addAll(typeArea, buttonStack);

        VBox Rbox = new VBox();
        Rbox.setSpacing(8);
        Rbox.setPadding(new Insets(5));
        Rbox.getChildren().addAll(logArea, messageBox);

        HBox root = new HBox();
        root.setSpacing(20);
        root.setPadding(new Insets(10));
        root.getChildren().addAll(controls, Rbox);

        Scene scene = new Scene(root, 900, 450);
        primaryStage.setScene(scene);

        primaryStage.setOnCloseRequest(event -> handleWindowClose(event));
        primaryStage.show();
    }

    public void broadcastServer() {
        if (!sharedState.isBroadcast()) {
            log("Starting broadcast...");
            State = 1;
            logArea.clear();
            sendButton.setDisable(false);
	        connectButton.setDisable(true);
            hostServerButton.setDisable(true);
            connectClientButton.setDisable(true);
            cancelButton.setDisable(false);
            nameField.setDisable(true); // Disable nameField
            typeArea.setPromptText("Send massage to connected clients...");
            sharedState.setBroadcast(true);
            GameServer.startBroadcasting(sharedState, logArea, getServerName(), serverPort);        
            }
    }

    public void connectClient() {
        if (!sharedState.isClient()) {
            log("Connecting as client...");
            State = 2;
            logArea.clear();
            hostServerButton.setDisable(true);
            connectClientButton.setDisable(true);
            cancelButton.setDisable(false);
            nameField.setDisable(true); // Disable nameField
            connectButton.setDisable(false);
            typeArea.setPromptText("Enter server number to connect first...");
            sharedState.setClient(true);
            sharedState.setBroadcast(false);
            GameClient.startClient(sharedState, logArea);
        }
    }

    public void cancelBroadcast() {
        log("Stopping ...");
        State = 0;
        cancelButton.setDisable(true);
        nameField.setDisable(false); // Re-enable nameField
        typeArea.setPromptText("");
        
        if (sharedState.isBroadcast()) {
            sharedState.setBroadcast(false);
            GameServer.stopServer();
        }
        
        if (sharedState.isClient()) {
            sharedState.setClient(false);
            sendButton.setDisable(true);
            connectButton.setDisable(true);
            GameClient.stopClient(logArea); // Pass logArea to stopClient
        }
        hostServerButton.setDisable(false);
        connectClientButton.setDisable(false);
        logArea.clear();
    }

    private void handleWindowClose(WindowEvent event) {
        log("Window is closing...");
        sharedState.setBroadcast(false);
        sharedState.setClient(false);
        GameServer.stopServer();
        GameClient.stopClient(logArea);
        System.out.println("Window is closing...");
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void log(String message) {
        logArea.appendText(message + "\n");
    }
    
    public static void switchbuttonDisable()
	{
		if (sendButton.isDisable() && !connectButton.isDisable()) {
			sendButton.setDisable(false);
			connectButton.setDisable(true);
		}
		else if (!sendButton.isDisable() && connectButton.isDisable()) {
			sendButton.setDisable(true);
			connectButton.setDisable(false);
		}
	}
    
    public static String getPlayerName() {
    	return nameField.getText().isEmpty() ? "Player" : nameField.getText();
    }
    
    public static String getServerName() {
    	return nameField.getText().isEmpty() ? "Host" : nameField.getText();
    }
    
    public static void settoGamedisable(boolean  set) {
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
    
    public static int getState() {
    	return State;
    }

}

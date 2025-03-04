package gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import logic.ServerLogic;
import logic.ClientLogic;
import logic.GameLogic;
import logic.PlayerLogic;
import logic.State;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class PrepGui extends VBox {

	private Label codeLabel;
	private Label serverInfoLabel;
	private Label playerCountLabel;
	private static Button readyButton;
	private boolean isReady = false;
	private AtomicBoolean isVisible = new AtomicBoolean(true);

	public PrepGui(State state) {
		this.setAlignment(Pos.CENTER);
		this.setSpacing(3);
		this.setPadding(new Insets(10, 15, 10, 15));

		// Remove background entirely to integrate with game
		this.setBackground(Background.EMPTY);

		// Remove border as well for seamless integration
		this.setBorder(Border.EMPTY);

		// Keep subtle drop shadow for better readability against game background
		this.setEffect(new DropShadow(5, Color.rgb(0, 0, 0, 0.7)));

		// IP label
		codeLabel = new Label("Server Address");
		codeLabel.setTextFill(Color.BLACK);
		codeLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 16));

		// Server info (IP:Port)
		serverInfoLabel = new Label("Loading...");
		serverInfoLabel.setTextFill(Color.BLACK);
		serverInfoLabel.setFont(Font.font("Monospace", FontWeight.LIGHT, 14));

		// IP section on left
		VBox ipSection = new VBox(2);
		ipSection.setAlignment(Pos.CENTER_LEFT);
		ipSection.getChildren().addAll(codeLabel, serverInfoLabel);

		// Ready button
		readyButton = new Button("READY");
		readyButton.setStyle("-fx-background-color: #333333; " + "-fx-text-fill: white; "
				+ "-fx-border-color: #555555; " + "-fx-border-width: 1; " + "-fx-font-family: 'Monospace'; "
				+ "-fx-font-weight: bold; " + "-fx-border-radius: 15; " + "-fx-background-radius: 15;");
		readyButton.setPrefWidth(100);
		readyButton.setFont(Font.font("Monospace", FontWeight.BOLD, 12));
		readyButton.setOnAction(e -> toggleReady());

		if (MainMenuPane.getState().equals(logic.State.SERVER)) {
				readyButton.setDisable(true);
				readyButton.setText("Start");
		}

		// Player icon and count
		Label playerIcon = new Label("\uD83D\uDC64"); // Person icon
		playerIcon.setTextFill(Color.BLACK);
		playerIcon.setFont(Font.font("Arial", 16));

		playerCountLabel = new Label("0/10");
		playerCountLabel.setTextFill(Color.rgb(0, 100, 0));
		playerCountLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 16));

		HBox playerCountContainer = new HBox(5);
		playerCountContainer.setAlignment(Pos.CENTER);
		playerCountContainer.getChildren().addAll(playerIcon, playerCountLabel);

		// Create the main container with a 3-column grid-like layout
		HBox mainContainer = new HBox();
		mainContainer.setPadding(new Insets(10));

		// Left region (IP) - takes more width now
		StackPane leftRegion = new StackPane(ipSection);
		leftRegion.setAlignment(Pos.CENTER_LEFT);
		leftRegion.setPrefWidth(180); // Increased from 180 to 250
		leftRegion.setMinWidth(180);

		// Center region (Ready button) - takes less width
		StackPane centerRegion = new StackPane(readyButton);
		centerRegion.setAlignment(Pos.CENTER);
		centerRegion.setPrefWidth(100); // Explicitly set width

		// Right region (Player count) - takes standard width
		StackPane rightRegion = new StackPane(playerCountContainer);
		rightRegion.setAlignment(Pos.CENTER_RIGHT);
		rightRegion.setPrefWidth(100); // Explicitly set width

		// Add spacers to force correct positioning
		Region leftSpacer = new Region();
		leftSpacer.setMinWidth(25);
		Region rightSpacer = new Region();
		rightSpacer.setMinWidth(55);

		// Add all elements to the container in the correct order
		mainContainer.getChildren().addAll(leftRegion, leftSpacer, centerRegion, rightSpacer, rightRegion);

		// Create the background rectangle
		Rectangle backdropRect = new Rectangle();
		backdropRect.setWidth(500); // Increased width to accommodate more IP space
		backdropRect.setHeight(70);
		backdropRect.setArcWidth(35);
		backdropRect.setArcHeight(35);
		backdropRect.setFill(Color.rgb(173, 216, 230, 0.85));
		backdropRect.setStroke(Color.rgb(135, 206, 235, 0.9));
		backdropRect.setStrokeWidth(5);

		// Stack the container on top of the background
		StackPane backdrop = new StackPane();
		backdrop.getChildren().addAll(backdropRect, mainContainer);

		// Add the backdrop to the main VBox
		this.getChildren().add(backdrop);

		// Load server info based on state
		loadServerInfo(state);

		// Set size for the panel
		this.setMaxWidth(500); // Increased to match backdrop width
		this.setMaxHeight(80);
	}

	private void loadServerInfo(State state) {
		// Get server info based on current state
		try {
			if (state == State.SERVER) {
				// Get local server info for hosts
				String serverAddress = InetAddress.getLocalHost().getHostAddress();
				int serverPort = ServerLogic.getServerSocket().getLocalPort();
				serverInfoLabel.setText(serverAddress + ":" + serverPort);
			} else if (state == State.CLIENT) {
				// For clients, retrieve connected server info
				String serverInfo = ClientLogic.getConnectedServerInfo();
				if (serverInfo != null) {
					serverInfoLabel.setText(serverInfo);
				} else {
					serverInfoLabel.setText("Not connected");
				}
			}
		} catch (Exception e) {
			serverInfoLabel.setText("NULL");
			System.err.println("Error loading server info: " + e.getMessage());
		}
	}

	public void updatePlayerCount(int currentPlayers, int maxPlayers) {
		playerCountLabel.setText(currentPlayers + "/" + maxPlayers);

		// Update color based on player count
		if (currentPlayers == maxPlayers) {
			playerCountLabel.setTextFill(Color.RED);
		} else if (currentPlayers >= maxPlayers / 2) {
			playerCountLabel.setTextFill(Color.YELLOW);
		} else {
			playerCountLabel.setTextFill(Color.rgb(0, 255, 0));
		}
	}

	// add the ability to differencate server and client
	private void toggleReady() {
		if (MainMenuPane.getState().equals(logic.State.SERVER)) { //Press to start game for server
			GameLogic.setPrepEnded(true); 
		} else { //for players
			isReady = !isReady;
			PlayerLogic.setPlayerReady(isReady);
			if (isReady) {
				readyButton.setText("UNREADY");
				readyButton.setStyle("-fx-background-color: #006400; " + // Dark green background
						"-fx-text-fill: white; " + "-fx-border-color: #008800; " + "-fx-border-width: 1; "
						+ "-fx-font-family: 'Monospace'; " + "-fx-font-weight: bold; " + "-fx-border-radius: 15; "
						+ "-fx-background-radius: 15;");
			} else {
				readyButton.setText("READY");
				readyButton.setStyle("-fx-background-color: #333333; " + "-fx-text-fill: white; "
						+ "-fx-border-color: #555555; " + "-fx-border-width: 1; " + "-fx-font-family: 'Monospace'; "
						+ "-fx-font-weight: bold; " + "-fx-border-radius: 15; " + "-fx-background-radius: 15;");
			}

			// Notify game logic that the player is ready/unready
			try {
				PlayerLogic.setPlayerReady(isReady);
			} catch (Exception e) {
				System.err.println("Error setting ready status: " + e.getMessage());
			}
		}
	}

	public void show() {
		if (!isVisible.get()) {
			this.setVisible(true);
			this.setOpacity(0);

			FadeTransition fadeIn = new FadeTransition(Duration.millis(300), this);
			fadeIn.setFromValue(0);
			fadeIn.setToValue(1);
			fadeIn.play();

			isVisible.set(true);
		}
	}

	public void hide() {
		if (isVisible.get()) {
			FadeTransition fadeOut = new FadeTransition(Duration.millis(300), this);
			fadeOut.setFromValue(1);
			fadeOut.setToValue(0);
			fadeOut.setOnFinished(e -> this.setVisible(false));
			fadeOut.play();

			isVisible.set(false);
		}
	}

	public boolean isPlayerReady() {
		return isReady;
	}
	
	 public static void setReadydisable(boolean disable) {
	        if (readyButton != null) {
	            Platform.runLater(() -> readyButton.setDisable(disable));
	        }
	    }
}
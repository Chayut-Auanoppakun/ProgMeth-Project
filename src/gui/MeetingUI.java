package gui;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import logic.ClientLogic;
import logic.GameLogic;
import logic.PlayerLogic;
import logic.ServerLogic;
import logic.State;
import server.ClientInfo;
import server.PlayerInfo;

public class MeetingUI extends StackPane {

	// UI Components
	private Rectangle backgroundDim;
	private BorderPane mainPanel;
	private FlowPane playerGrid;
	private Button skipVoteButton;
	private Text timerText;
	private TextArea chatArea;
	private TextField messageInput;
	private Button sendButton;

	// Game Data
	private Map<String, PlayerVoteCard> playerCards = new HashMap<>();
	private List<String> playerKeys = new ArrayList<>();
	private int votingTimeSeconds = 60;
	private Timeline votingTimer;
	private String reportedBodyPlayer = null;
	private String reporterKey = null;

	// Voting data
	private String selectedPlayerKey = null;
	private Map<String, Integer> votes = new HashMap<>(); // Player key -> count of votes
	private Map<String, String> playerVotes = new HashMap<>(); // Player key -> who they voted for
	private boolean hasVoted = false;
	private final String meetingId;

	// References
	private GameWindow gameWindow;
	private ScheduledExecutorService executor;

	/**
	 * Creates a new voting interface for emergency meetings
	 * 
	 * @param gameWindow     Reference to the game window
	 * @param reportedPlayer The name of the reported dead player (null if emergency
	 *                       button)
	 * @param reporterKey    The key of the player who called the meeting
	 */
	public MeetingUI(GameWindow gameWindow, String reportedPlayer, String reporterKey) {
		this.gameWindow = gameWindow;
		this.reportedBodyPlayer = reportedPlayer;
		this.reporterKey = reporterKey;

		// Create a daemon thread executor to prevent application hang on shutdown
		this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r);
			t.setDaemon(true); // Make the thread a daemon thread
			return t;
		});

		// Generate a unique meeting ID based on time and reporter
		this.meetingId = reporterKey + "_" + System.currentTimeMillis();

		initializeUI();
		populatePlayerGrid();
		startVotingTimer();

		// Add initial announcement
		String reporterName = getPlayerNameByKey(reporterKey);
		if (reportedBodyPlayer != null) {
			addChatMessage("SYSTEM", reporterName + " reported " + reportedBodyPlayer + "'s body!");
		} else {
			addChatMessage("SYSTEM", reporterName + " called an emergency meeting!");
		}
	}

	/**
	 * Sets up the main UI components
	 */
	private void initializeUI() {
		// Set size to match screen
		setPrefWidth(gameWindow.getWidth());
		setPrefHeight(gameWindow.getHeight());

		// Create darkened background
		backgroundDim = new Rectangle(gameWindow.getWidth(), gameWindow.getHeight());
		backgroundDim.setFill(Color.rgb(0, 0, 0, 0.85));

		// Create main panel with BorderPane layout
		mainPanel = createMainPanel();

		// Center the panel in the screen
		mainPanel.setMaxWidth(800);
		mainPanel.setMaxHeight(600);

		// Add components to the stack
		getChildren().addAll(backgroundDim, mainPanel);

		// Center the mainPanel
		StackPane.setAlignment(mainPanel, Pos.CENTER);

		// Add ghost overlay for dead players
		boolean isPlayerDead = "dead".equals(PlayerLogic.getStatus());
		if (isPlayerDead) {
			applyDeadPlayerUI();
		}
	}

	private void applyDeadPlayerUI() {
		// Add ghost overlay to the entire UI
		Rectangle ghostOverlay = new Rectangle(getWidth(), getHeight());
		ghostOverlay.setFill(Color.rgb(0, 0, 150, 0.2)); // Slight blue tint
		getChildren().add(1, ghostOverlay); // Add between background and panel

		// Add ghost text label
		Text ghostLabel = new Text("GHOST - SPECTATING ONLY");
		ghostLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 18));
		ghostLabel.setFill(Color.rgb(180, 180, 255));
		ghostLabel.setStroke(Color.BLACK);
		ghostLabel.setStrokeWidth(1);
		ghostLabel.setEffect(new DropShadow(10, Color.BLACK));

		StackPane ghostLabelPane = new StackPane(ghostLabel);
		ghostLabelPane.setPadding(new Insets(10));
		ghostLabelPane.setStyle("-fx-background-color: rgba(0, 0, 50, 0.7); -fx-background-radius: 5;");
		getChildren().add(ghostLabelPane);

		// Position at bottom of screen
		StackPane.setAlignment(ghostLabelPane, Pos.BOTTOM_CENTER);
		StackPane.setMargin(ghostLabelPane, new Insets(0, 0, 30, 0));
	}

	/**
	 * Creates the main panel for the meeting UI with optimized player grid
	 */
	private BorderPane createMainPanel() {
		BorderPane panel = new BorderPane();
		panel.setPadding(new Insets(20));

		// Style the panel with a dark background and border to match game theme
		panel.setBackground(new Background(new BackgroundFill(Color.rgb(30, 30, 50, 0.95), // Dark blue-gray background
																							// matching
																							// CharacterSelectGui
				new CornerRadii(0), Insets.EMPTY)));

		panel.setBorder(new Border(new BorderStroke(Color.rgb(30, 144, 255), // Blue border to match game theme
				BorderStrokeStyle.SOLID, new CornerRadii(0), new BorderWidths(3))));

		// Add drop shadow for depth
		DropShadow shadow = new DropShadow();
		shadow.setRadius(15);
		shadow.setOffsetX(5);
		shadow.setOffsetY(5);
		shadow.setColor(Color.rgb(0, 0, 0, 0.6));
		panel.setEffect(shadow);

		// Create header section
		VBox headerBox = createHeaderSection();
		panel.setTop(headerBox);

		// Create player grid for voting - optimized for more players
		playerGrid = new FlowPane();
		playerGrid.setHgap(8); // Smaller gap
		playerGrid.setVgap(8);
		playerGrid.setAlignment(Pos.CENTER);
		playerGrid.setPrefWrapLength(760); // Set preferred width for wrapping
		playerGrid.setPadding(new Insets(10));

		// Wrap player grid in a scroll pane for many players
		ScrollPane scrollPane = new ScrollPane(playerGrid);
		scrollPane.setFitToWidth(true);
		scrollPane.setFitToHeight(true);
		scrollPane.setPannable(true);
		scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
		scrollPane.setPrefHeight(200); // Reduced height

		panel.setCenter(scrollPane);

		// Create bottom section with chat and skip vote button
		VBox bottomSection = createBottomSection();
		panel.setBottom(bottomSection);

		return panel;
	}

	/**
	 * Populates the player grid with player cards for voting
	 */
	private void populatePlayerGrid() {
		playerGrid.getChildren().clear();
		playerCards.clear();
		playerKeys.clear();

		// Collect all players including local player
		playerKeys.add(PlayerLogic.getLocalAddressPort());
		for (String key : GameLogic.playerList.keySet()) {
			if (!playerKeys.contains(key)) {
				playerKeys.add(key);
			}
		}

		// Create player cards
		for (String playerKey : playerKeys) {
			PlayerVoteCard playerCard;

			if (playerKey.equals(PlayerLogic.getLocalAddressPort())) {
				// Local player card
				playerCard = new PlayerVoteCard(PlayerLogic.getName(), PlayerLogic.getCharID(), playerKey);
			} else {
				// Other player card
				PlayerInfo player = GameLogic.playerList.get(playerKey);
				if (player != null) {
					playerCard = new PlayerVoteCard(player.getName(), player.getCharacterID(), playerKey);
				} else {
					continue; // Skip invalid players
				}
			}

			// Check if player is dead
			if (playerKey.equals(PlayerLogic.getLocalAddressPort())) {
				if ("dead".equals(PlayerLogic.getStatus())) {
					playerCard.markAsDead();
				}
			} else {
				PlayerInfo player = GameLogic.playerList.get(playerKey);
				if (player != null && "dead".equals(player.getStatus())) {
					playerCard.markAsDead();
				}
			}

			// Add card to the grid
			playerGrid.getChildren().add(playerCard);
			playerCards.put(playerKey, playerCard);
		}
	}

	/**
	 * Creates the bottom section with chat area and skip vote button
	 */
	private VBox createBottomSection() {
		VBox bottomSection = new VBox(10);
		bottomSection.setPadding(new Insets(10, 0, 0, 0));

		boolean isPlayerDead = "dead".equals(PlayerLogic.getStatus());

		// Skip vote button - only for alive players
		skipVoteButton = new Button("SKIP VOTE");
		skipVoteButton.getStyleClass().add("skip-vote-button");
		skipVoteButton.setStyle("-fx-background-color: #333333; " + "-fx-text-fill: white; "
				+ "-fx-font-family: 'Monospace'; " + "-fx-font-weight: bold; " + "-fx-font-size: 14px; "
				+ "-fx-padding: 8 15 8 15; " + "-fx-border-color: #555555; " + "-fx-border-width: 2px; "
				+ "-fx-background-radius: 5; " + "-fx-border-radius: 5;");

		skipVoteButton.setOnAction(e -> handleSkipVote());

		// Disable skip vote button if player is dead
		if (isPlayerDead) {
			skipVoteButton.setDisable(true);
			skipVoteButton.setStyle(skipVoteButton.getStyle() + "-fx-opacity: 0.5; -fx-background-color: #555555;");
		}

		HBox buttonBox = new HBox(skipVoteButton);
		buttonBox.setAlignment(Pos.CENTER);
		buttonBox.setPadding(new Insets(0, 0, 5, 0));

		// Chat area with different styling for dead players
		VBox chatBox = createChatSection(isPlayerDead);

		// Add elements to bottom section
		bottomSection.getChildren().addAll(buttonBox, chatBox);

		return bottomSection;
	}

	private VBox createChatSection(boolean isPlayerDead) {
		VBox chatBox = new VBox(5);
		chatBox.setPadding(new Insets(10, 0, 0, 0));

		// Chat header with different text for dead players
		Text chatHeader;
		if (isPlayerDead) {
			chatHeader = new Text("SPECTATOR CHAT (GHOST)");
			chatHeader.setFill(Color.rgb(150, 150, 255)); // Ghostly blue
		} else {
			chatHeader = new Text("DISCUSSION");
			chatHeader.setFill(Color.LIGHTBLUE);
		}
		chatHeader.setFont(Font.font("Monospace", FontWeight.BOLD, 16));

		// Chat area
		chatArea = new TextArea();
		chatArea.setPrefHeight(100);
		chatArea.setEditable(false);
		chatArea.setWrapText(true);

		// Different styling for dead players
		if (isPlayerDead) {
			chatArea.setStyle("-fx-control-inner-background: #2a2a40; " + // Slightly bluer background
					"-fx-text-fill: #aaaaff; " + // Bluer text
					"-fx-font-family: 'Monospace'; " + "-fx-border-color: #5555aa; " + // Bluer border
					"-fx-border-width: 1px; " + "-fx-opacity: 0.9;"); // Slightly transparent
		} else {
			chatArea.setStyle("-fx-control-inner-background: #2a2a2a; " + "-fx-text-fill: white; "
					+ "-fx-font-family: 'Monospace'; " + "-fx-border-color: #555555; " + "-fx-border-width: 1px;");
		}

		// Input area - only for alive players
		HBox inputBox = new HBox(5);

		messageInput = new TextField();
		messageInput.setPrefHeight(30);
		messageInput.setPromptText(isPlayerDead ? "Ghosts cannot speak to the living..." : "Type your message...");

		if (isPlayerDead) {
			messageInput.setDisable(true);
			messageInput.setStyle("-fx-background-color: #3a3a4a; " + // Bluer background
					"-fx-text-fill: #8888aa; " + // Ghostly text
					"-fx-font-family: 'Monospace'; " + "-fx-border-color: #5555aa; " + // Bluer border
					"-fx-border-width: 1px; " + "-fx-opacity: 0.7;"); // More transparent
		} else {
			messageInput.setStyle("-fx-background-color: #2a2a2a; " + "-fx-text-fill: white; "
					+ "-fx-font-family: 'Monospace'; " + "-fx-border-color: #555555; " + "-fx-border-width: 1px;");
		}
		HBox.setHgrow(messageInput, Priority.ALWAYS);

		sendButton = new Button("Send");
		sendButton.setPrefHeight(30);

		if (isPlayerDead) {
			sendButton.setDisable(true);
			sendButton.setStyle("-fx-background-color: #5555aa; " + // Bluer background
					"-fx-text-fill: #aaaaff; " + // Ghostly text
					"-fx-font-family: 'Monospace'; " + "-fx-font-weight: bold; " + "-fx-border-color: #7777aa; " + // Bluer
																													// border
					"-fx-border-width: 1px; " + "-fx-opacity: 0.7;"); // More transparent
		} else {
			sendButton.setStyle(
					"-fx-background-color: #1e90ff; " + "-fx-text-fill: white; " + "-fx-font-family: 'Monospace'; "
							+ "-fx-font-weight: bold; " + "-fx-border-color: #87cefa; " + "-fx-border-width: 1px;");
		}

		sendButton.setOnAction(e -> {
			if (!isPlayerDead) {
				sendChatMessage();
			}
		});

		// Handle Enter key in message input - only for alive players
		if (!isPlayerDead) {
			messageInput.setOnAction(e -> sendChatMessage());
		}

		inputBox.getChildren().addAll(messageInput, sendButton);

		// Add all to chat box
		chatBox.getChildren().addAll(chatHeader, chatArea, inputBox);

		// Additional info for dead players
		if (isPlayerDead) {
			Text ghostInfo = new Text("Dead players can only observe. The living cannot see your messages.");
			ghostInfo.setFont(Font.font("Monospace", FontWeight.NORMAL, 12));
			ghostInfo.setFill(Color.rgb(150, 150, 255)); // Ghostly blue
			ghostInfo.setTextAlignment(TextAlignment.CENTER);

			chatBox.getChildren().add(ghostInfo);
		}

		return chatBox;
	}

	/**
	 * Creates the header section with title and timer
	 */
	private VBox createHeaderSection() {
		VBox headerBox = new VBox(10);
		headerBox.setAlignment(Pos.CENTER);
		headerBox.setPadding(new Insets(10, 0, 15, 0));

		// Title text
		Text titleText = new Text("EMERGENCY MEETING");
		titleText.setFont(Font.font("Monospace", FontWeight.BOLD, 36));
		titleText.setFill(Color.rgb(255, 80, 80)); // Red text
		titleText.setStroke(Color.WHITE);
		titleText.setStrokeWidth(1.5);
		titleText.setTextAlignment(TextAlignment.CENTER);

		// Add drop shadow to title
		DropShadow textShadow = new DropShadow();
		textShadow.setRadius(3);
		textShadow.setOffsetX(2);
		textShadow.setOffsetY(2);
		textShadow.setColor(Color.rgb(0, 0, 0, 0.5));
		titleText.setEffect(textShadow);

		// Subtitle text
		Text subtitleText = new Text();
		if (reportedBodyPlayer != null) {
			subtitleText.setText(getPlayerNameByKey(reporterKey) + " reported " + reportedBodyPlayer + "'s body");
		} else {
			subtitleText.setText(getPlayerNameByKey(reporterKey) + " called an emergency meeting");
		}
		subtitleText.setFont(Font.font("Monospace", FontWeight.BOLD, 18));
		subtitleText.setFill(Color.LIGHTGRAY);
		subtitleText.setTextAlignment(TextAlignment.CENTER);

		// Timer text
		timerText = new Text("Voting Ends: " + votingTimeSeconds + "s");
		timerText.setFont(Font.font("Monospace", FontWeight.BOLD, 18));
		timerText.setFill(Color.ORANGE);

		headerBox.getChildren().addAll(titleText, subtitleText, timerText);

		return headerBox;
	}

	/**
	 * Creates the chat section similar to ServerSelectGui
	 */
	private VBox createChatSection() {
		VBox chatBox = new VBox(5);
		chatBox.setPadding(new Insets(10, 0, 0, 0));

		// Chat header
		Text chatHeader = new Text("DISCUSSION");
		chatHeader.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
		chatHeader.setFill(Color.LIGHTBLUE);

		// Chat area
		chatArea = new TextArea();
		chatArea.setPrefHeight(100);
		chatArea.setEditable(false);
		chatArea.setWrapText(true);
		chatArea.setStyle("-fx-control-inner-background: #2a2a2a; " + "-fx-text-fill: white; "
				+ "-fx-font-family: 'Monospace'; " + "-fx-border-color: #555555; " + "-fx-border-width: 1px;");

		// Input area
		HBox inputBox = new HBox(5);

		messageInput = new TextField();
		messageInput.setPrefHeight(30);
		messageInput.setPromptText("Type your message...");
		messageInput.setStyle("-fx-background-color: #2a2a2a; " + "-fx-text-fill: white; "
				+ "-fx-font-family: 'Monospace'; " + "-fx-border-color: #555555; " + "-fx-border-width: 1px;");
		HBox.setHgrow(messageInput, Priority.ALWAYS);

		sendButton = new Button("Send");
		sendButton.setPrefHeight(30);
		sendButton.setStyle(
				"-fx-background-color: #1e90ff; " + "-fx-text-fill: white; " + "-fx-font-family: 'Monospace'; "
						+ "-fx-font-weight: bold; " + "-fx-border-color: #87cefa; " + "-fx-border-width: 1px;");

		sendButton.setOnAction(e -> {
			sendChatMessage();
		});

		// Handle Enter key in message input
		messageInput.setOnAction(e -> sendChatMessage());

		inputBox.getChildren().addAll(messageInput, sendButton);

		// Add all to chat box
		chatBox.getChildren().addAll(chatHeader, chatArea, inputBox);

		return chatBox;
	}

	/**
	 * Sends the current message in the input field
	 */
	private void sendChatMessage() {
		String message = messageInput.getText().trim();
		if (!message.isEmpty()) {
			// Get local player name
			String playerName = PlayerLogic.getName();

			// Display message locally first
			addChatMessage(playerName, message);

			try {
				// Create meeting-specific JSON message
				JSONObject meetingChatData = new JSONObject();
				meetingChatData.put("type", "chat");
				meetingChatData.put("name", playerName);
				meetingChatData.put("message", message);
				meetingChatData.put("meetingId", meetingId);
				meetingChatData.put("status", PlayerLogic.getStatus()); // Include player status

				String meetingMessage = "/meeting/" + meetingChatData.toString();

				// Send message based on client/server role
				if (MainMenuPane.getState() == State.SERVER) {
					// If server, handle locally and broadcast to all clients

					// First, update our own chat
					addChatMessage(playerName, message);

					// Then broadcast to all clients
					for (ClientInfo clientInfo : ServerLogic.getConnectedClients()) {
						try {
							DatagramSocket socket = ServerLogic.getServerSocket();
							if (socket != null) {
								byte[] buf = meetingMessage.getBytes(StandardCharsets.UTF_8);
								DatagramPacket packet = new DatagramPacket(buf, buf.length, clientInfo.getAddress(),
										clientInfo.getPort());
								socket.send(packet);
							}
						} catch (IOException e) {
							System.err.println("Error broadcasting meeting chat: " + e.getMessage());
						}
					}
				} else {
					// If client, send to server using existing ClientLogic
					ClientLogic.sendMessage(meetingMessage, null);
				}
			} catch (Exception e) {
				System.err.println("Error sending meeting chat: " + e.getMessage());
			}

			// Clear the input field
			messageInput.clear();
		}
	}

	public void receiveChatMessage(String playerName, String message, String playerStatus) {
		Platform.runLater(() -> {
			// Check if the message is from a dead player
			boolean isDeadMessage = "dead".equals(playerStatus);

			// Add the message to the chat
			String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

			// Format message differently based on sender status
			String formattedMessage;
			if (isDeadMessage) {
				formattedMessage = "[" + timestamp + "] 👻 " + playerName + " (GHOST): " + message + "\n";
			} else {
				formattedMessage = "[" + timestamp + "] " + playerName + ": " + message + "\n";
			}

			// Add to chat area
			chatArea.appendText(formattedMessage);
			chatArea.setScrollTop(Double.MAX_VALUE); // Scroll to bottom
		});
	}

	/**
	 * Adds a message to the chat area
	 */
	public void addChatMessage(String playerName, String message) {
		Platform.runLater(() -> {
			String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

			// Check if the message is from a dead player
			boolean isDeadMessage = false;

			// System messages always show for everyone
			boolean isSystemMessage = "SYSTEM".equals(playerName);

			if (!isSystemMessage) {
				// Check if this is a message from the local player who is dead
				if (playerName.equals(PlayerLogic.getName()) && "dead".equals(PlayerLogic.getStatus())) {
					isDeadMessage = true;
				}
				// For other players, check the player list
				else {
					for (String key : GameLogic.playerList.keySet()) {
						PlayerInfo playerInfo = GameLogic.playerList.get(key);
						if (playerInfo != null && playerInfo.getName().equals(playerName)
								&& "dead".equals(playerInfo.getStatus())) {
							isDeadMessage = true;
							break;
						}
					}
				}
			}

			// Format message differently based on sender status
			String formattedMessage;
			if (isSystemMessage) {
				formattedMessage = "[" + timestamp + "] " + playerName + ": " + message + "\n";
			} else if (isDeadMessage) {
				formattedMessage = "[" + timestamp + "] 👻 " + playerName + " (GHOST): " + message + "\n";
			} else {
				formattedMessage = "[" + timestamp + "] " + playerName + ": " + message + "\n";
			}

			// Add to chat area
			chatArea.appendText(formattedMessage);
			chatArea.setScrollTop(Double.MAX_VALUE); // Scroll to bottom
		});
	}

	/**
	 * Gets a player's name based on their key
	 */
	private String getPlayerNameByKey(String playerKey) {
		if (playerKey.equals(PlayerLogic.getLocalAddressPort())) {
			return PlayerLogic.getName();
		} else {
			PlayerInfo player = GameLogic.playerList.get(playerKey);
			return player != null ? player.getName() : "Unknown Player";
		}
	}

	/**
	 * Starts the voting timer
	 */
	private void startVotingTimer() {
		votingTimer = new Timeline();
		votingTimer.setCycleCount(votingTimeSeconds);

		KeyFrame frame = new KeyFrame(Duration.seconds(1), event -> {
			votingTimeSeconds--;

			// Update timer
			timerText.setText("Voting Ends: " + votingTimeSeconds + "s");

			// Change timer color when it gets low
			if (votingTimeSeconds <= 10) {
				timerText.setFill(Color.RED); // Red for urgency
			}

			if (votingTimeSeconds <= 0) {
				endVoting();
			}
		});

		votingTimer.getKeyFrames().add(frame);
		votingTimer.play();
	}

	/**
	 * Handles when a player is selected for voting
	 */
	private void handlePlayerSelected(String playerKey) {
		// Can't vote if already voted or if player is dead
		if (hasVoted || "dead".equals(PlayerLogic.getStatus())) {
			return;
		}

		// Can't vote for dead players
		PlayerVoteCard card = playerCards.get(playerKey);
		if (card.isDead()) {
			return;
		}

		// Update selection
		if (selectedPlayerKey != null && playerCards.containsKey(selectedPlayerKey)) {
			playerCards.get(selectedPlayerKey).setSelected(false);
		}

		selectedPlayerKey = playerKey;
		card.setSelected(true);
	}

	/**
	 * Confirms vote for the selected player
	 */
	private void confirmVote(String targetPlayerKey) {
		if (!hasVoted && !"dead".equals(PlayerLogic.getStatus())) {
			// Record the vote
			playerVotes.put(PlayerLogic.getLocalAddressPort(), targetPlayerKey);

			// Update vote count
			int currentVotes = votes.getOrDefault(targetPlayerKey, 0);
			votes.put(targetPlayerKey, currentVotes + 1);

			// Show vote indicator
			playerCards.get(targetPlayerKey).addVote(PlayerLogic.getCharID());

			// Mark as voted
			hasVoted = true;

			// Send vote to server
			if (MainMenuPane.getState() == State.SERVER) {
				// If server, handle the vote locally and broadcast to clients
				ServerLogic.handleVote(PlayerLogic.getLocalAddressPort(), targetPlayerKey, meetingId, null);
			} else {
				// If client, send vote to server
				sendVoteToServer(targetPlayerKey);
			}

			// Display confirmation in chat
			String targetName = getPlayerNameByKey(targetPlayerKey);
			addChatMessage("SYSTEM", "You voted for " + targetName);
		}
	}

	/**
	 * Sends a vote to the server (client only)
	 */
	private void sendVoteToServer(String targetPlayerKey) {
		try {
			// Create vote JSON
			Map<String, Object> voteData = new HashMap<>();
			voteData.put("voter", PlayerLogic.getLocalAddressPort());
			voteData.put("target", targetPlayerKey);
			voteData.put("meetingId", meetingId);

			// Convert to JSON string
			String jsonStr = new org.json.JSONObject(voteData).toString();
			String message = "/vote/" + jsonStr;

			// Send to server
			ClientLogic.sendMessage(message, null);
		} catch (Exception e) {
			System.err.println("Error sending vote to server: " + e.getMessage());
		}
	}

	/**
	 * Handles skipping vote
	 */
	private void handleSkipVote() {
		if (!hasVoted && !"dead".equals(PlayerLogic.getStatus())) {
			// Clear any selection
			if (selectedPlayerKey != null && playerCards.containsKey(selectedPlayerKey)) {
				playerCards.get(selectedPlayerKey).setSelected(false);
				selectedPlayerKey = null;
			}

			// Record skip vote
			playerVotes.put(PlayerLogic.getLocalAddressPort(), "skip");

			// Update skip vote count
			int currentSkips = votes.getOrDefault("skip", 0);
			votes.put("skip", currentSkips + 1);

			// Mark as voted
			hasVoted = true;

			// Send vote to server
			if (MainMenuPane.getState() == State.SERVER) {
				// If server, handle the vote locally and broadcast to clients
				ServerLogic.handleVote(PlayerLogic.getLocalAddressPort(), "skip", meetingId, null);
			} else {
				// If client, send vote to server
				sendVoteToServer("skip");
			}

			// Display confirmation in chat
			addChatMessage("SYSTEM", "You skipped voting");

			// Disable skip button
			skipVoteButton.setDisable(true);
		}
	}

	/**
	 * Gets the unique ID for this meeting
	 */
	public String getMeetingId() {
		return meetingId;
	}

	/**
	 * Handles a vote received from the server
	 */
	public void receiveVote(String voterKey, String targetKey) {
		Platform.runLater(() -> {
			// Update vote count
			int currentVotes = votes.getOrDefault(targetKey, 0);
			votes.put(targetKey, currentVotes + 1);

			// Record who voted for whom
			playerVotes.put(voterKey, targetKey);

			// If it's a skip vote
			if ("skip".equals(targetKey)) {
				// Add chat message
				String voterName = getPlayerNameByKey(voterKey);
				addChatMessage("SYSTEM", voterName + " skipped voting");
				return;
			}

			// Check if the target exists in our player cards
			if (playerCards.containsKey(targetKey)) {
				// Get voter's character ID
				int voterCharId;
				if (voterKey.equals(PlayerLogic.getLocalAddressPort())) {
					voterCharId = PlayerLogic.getCharID();
				} else {
					PlayerInfo voter = GameLogic.playerList.get(voterKey);
					voterCharId = voter != null ? voter.getCharacterID() : 0;
				}

				// Show vote icon
				playerCards.get(targetKey).addVote(voterCharId);

				// Add chat message
				String voterName = getPlayerNameByKey(voterKey);
				String targetName = getPlayerNameByKey(targetKey);

				addChatMessage("SYSTEM", voterName + " voted for " + targetName);
			}
		});
	}

	/**
	 * Ends the voting period and shows results
	 */
	private void endVoting() {
		votingTimer.stop();
		timerText.setText("Voting Complete");

		// Disable voting
		skipVoteButton.setDisable(true);
		for (PlayerVoteCard card : playerCards.values()) {
			card.disableVoting();
		}

		// Show result message in chat
		addChatMessage("SYSTEM", "Voting has ended");

		// For server: calculate results and broadcast
		if (MainMenuPane.getState() == State.SERVER) {
			// Calculate results
			String ejectedPlayerKey = calculateVotingResult();

			// Broadcast results to all clients
			ServerLogic.broadcastVotingResults(ejectedPlayerKey, votes, meetingId, null);

			// Handle ejection
			if (ejectedPlayerKey != null) {
				// If local player was ejected
				if (ejectedPlayerKey.equals(PlayerLogic.getLocalAddressPort())) {
					PlayerLogic.setStatus("dead");
				}
				// If another player was ejected
				else if (GameLogic.playerList.containsKey(ejectedPlayerKey)) {
					PlayerInfo player = GameLogic.playerList.get(ejectedPlayerKey);
					player.setStatus("dead");
				}
			}

			// Show the results locally
			showVotingResults(ejectedPlayerKey);
		}

		// For clients: wait for server to send results
		// The results will be displayed when the server sends them
	}

	/**
	 * Calculates the result of the voting
	 */
	private String calculateVotingResult() {
		// Find the player with the most votes
		String mostVotedPlayer = null;
		int highestVotes = 0;

		for (Map.Entry<String, Integer> entry : votes.entrySet()) {
			if (entry.getValue() > highestVotes) {
				highestVotes = entry.getValue();
				mostVotedPlayer = entry.getKey();
			}
		}

		// Check for tie with skip
		int skipVotes = votes.getOrDefault("skip", 0);
		if (skipVotes >= highestVotes) {
			return null; // No one is ejected in a tie or if skips win
		}

		return mostVotedPlayer;
	}

	/**
	 * Shows the voting results
	 */
	public void showVotingResults(String ejectedPlayerKey) {
		Platform.runLater(() -> {
			// Create results display
			VBox resultsBox = new VBox(20);
			resultsBox.setAlignment(Pos.CENTER);
			resultsBox.setPadding(new Insets(20));
			resultsBox.setBackground(
					new Background(new BackgroundFill(Color.rgb(0, 0, 0, 0.8), new CornerRadii(10), Insets.EMPTY)));

			// Results text
			Text resultsText;
			if (ejectedPlayerKey == null) {
				resultsText = new Text("No one was ejected (Skipped)");
			} else {
				String playerName = getPlayerNameByKey(ejectedPlayerKey);
				resultsText = new Text(playerName + " was ejected");
			}

			resultsText.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
			resultsText.setFill(Color.WHITE);

			// Add to results box
			resultsBox.getChildren().add(resultsText);

			// If a player was ejected, show if they were an impostor
			if (ejectedPlayerKey != null) {
				String roleText;
				boolean wasImpostor = false;

				if (ejectedPlayerKey.equals(PlayerLogic.getLocalAddressPort())) {
					wasImpostor = PlayerLogic.getStatus().equals("imposter");
					roleText = wasImpostor ? PlayerLogic.getName() + " was an Impostor."
							: PlayerLogic.getName() + " was not an Impostor.";
				} else {
					PlayerInfo player = GameLogic.playerList.get(ejectedPlayerKey);
					wasImpostor = player != null && player.getStatus().equals("imposter");
					roleText = wasImpostor ? player.getName() + " was an Impostor."
							: player.getName() + " was not an Impostor.";
				}

				Text imposterText = new Text(roleText);
				imposterText.setFont(Font.font("Monospace", FontWeight.BOLD, 22));
				imposterText.setFill(wasImpostor ? Color.rgb(255, 100, 100) : Color.rgb(100, 255, 100));

				resultsBox.getChildren().add(imposterText);
			}

			// Add vote counts
			Text voteCounts = new Text("Votes: ");
			voteCounts.setFont(Font.font("Monospace", FontWeight.NORMAL, 18));
			voteCounts.setFill(Color.WHITE);

			StringBuilder voteDetails = new StringBuilder();
			for (Map.Entry<String, Integer> entry : votes.entrySet()) {
				if (entry.getKey().equals("skip")) {
					voteDetails.append("Skip: ").append(entry.getValue()).append("\n");
				} else {
					String playerName = getPlayerNameByKey(entry.getKey());
					voteDetails.append(playerName).append(": ").append(entry.getValue()).append("\n");
				}
			}

			Text voteDetailsText = new Text(voteDetails.toString());
			voteDetailsText.setFont(Font.font("Monospace", FontWeight.NORMAL, 16));
			voteDetailsText.setFill(Color.WHITE);

			resultsBox.getChildren().addAll(voteCounts, voteDetailsText);

			// Add the results to the meeting UI
			mainPanel.setCenter(resultsBox);

			// Animate results appearance
			FadeTransition fadeIn = new FadeTransition(Duration.millis(500), resultsBox);
			fadeIn.setFromValue(0);
			fadeIn.setToValue(1);
			fadeIn.play();

			// Schedule closing the meeting UI after showing results
			if (executor != null && !executor.isShutdown()) {
				executor.schedule(() -> Platform.runLater(this::closeMeetingUI), 7, TimeUnit.SECONDS);
			} else {
				// Fallback if executor is already shut down
				Timeline closingTimer = new Timeline(new KeyFrame(Duration.seconds(7), e -> closeMeetingUI()));
				closingTimer.play();
			}
		});
	}

	/**
	 * Add a vote result handler for when results are received from the server
	 */
	public void showVotingResults(String ejectedPlayerKey, Map<String, Integer> voteResults) {
		// Update our local vote counts with the server's data
		this.votes = voteResults;

		// Show the results
		showVotingResults(ejectedPlayerKey);
	}

	/**
	 * Inner class representing a player card for voting with smaller dimensions
	 */
	private class PlayerVoteCard extends StackPane {
		private final String playerKey;
		private final String playerName;
		private final int characterId;
		private boolean isSelected = false;
		private boolean isDead = false;
		private VBox voteIconsBox;
		private Rectangle cardBg;
		private ImageView characterImage;
		private Text nameText;

		public PlayerVoteCard(String name, int charId, String key) {
			this.playerName = name;
			this.characterId = charId;
			this.playerKey = key;

			// Configure card with smaller dimensions
			setPrefSize(100, 120);
			setMaxSize(100, 120);
			setPadding(new Insets(3));

			// Create card background
			cardBg = new Rectangle(100, 120);
			cardBg.setArcWidth(8);
			cardBg.setArcHeight(8);
			cardBg.setFill(Color.rgb(50, 50, 70, 0.8)); // Dark blue-gray background
			cardBg.setStroke(Color.rgb(30, 144, 255)); // Blue border
			cardBg.setStrokeWidth(2);

			// Create content VBox
			VBox content = new VBox(5);
			content.setAlignment(Pos.CENTER);
			content.setPadding(new Insets(5));

			// Create character image - smaller size
			characterImage = createCharacterImage(charId);

			// Player name - smaller font
			nameText = new Text(name);
			nameText.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
			nameText.setFill(getColorForCharacter(charId));
			nameText.setTextAlignment(TextAlignment.CENTER);

			// Shorten long names
			if (name.length() > 8) {
				nameText.setText(name.substring(0, 7) + "...");
			}

			// Container for vote icons - smaller and more compact
			voteIconsBox = new VBox(2);
			voteIconsBox.setAlignment(Pos.CENTER);
			voteIconsBox.setMaxHeight(30); // Limit height for vote icons

			// Add elements to content
			content.getChildren().addAll(characterImage, nameText, voteIconsBox);

			// Add to pane
			getChildren().addAll(cardBg, content);

			// Set up click handler
			setOnMouseClicked(e -> {
				if (!isDead && !hasVoted) {
					handlePlayerSelected(playerKey);
					if (isSelected) {
						confirmVote(playerKey);
					}
				}
			});

			// Hover effect
			setOnMouseEntered(e -> {
				if (!isDead && !hasVoted) {
					cardBg.setFill(Color.rgb(70, 70, 100, 0.9));
					cardBg.setStroke(Color.rgb(100, 180, 255));
				}
			});

			setOnMouseExited(e -> {
				if (!isSelected && !isDead) {
					cardBg.setFill(Color.rgb(50, 50, 70, 0.8));
					cardBg.setStroke(Color.rgb(30, 144, 255));
				}
			});
		}

		/**
		 * Creates an ImageView for the character profile
		 */
		private ImageView createCharacterImage(int charId) {
			String profilePath = String.format("/player/profile/%02d.png", (charId + 1));

			try {
				Image charImage = new Image(getClass().getResourceAsStream(profilePath));
				ImageView imageView = new ImageView(charImage);
				imageView.setFitWidth(50); // Smaller image
				imageView.setFitHeight(50);
				imageView.setPreserveRatio(true);

				// Add styling to match CharacterSelectGui but smaller
				imageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 3, 0, 0, 1);"
						+ "-fx-border-color: #1e90ff;" + "-fx-border-width: 1px;");

				return imageView;
			} catch (Exception e) {
				// Fallback for missing image
				Rectangle placeholder = new Rectangle(50, 50);
				placeholder.setFill(getColorForCharacter(charId));
				placeholder.setStroke(Color.WHITE);
				placeholder.setStrokeWidth(1);

				return new ImageView();
			}
		}

		/**
		 * Creates a vote badge for the player card - smaller size
		 */
		private HBox createVoteBadge(int voterCharId) {
			HBox badge = new HBox(3);
			badge.setAlignment(Pos.CENTER);
			badge.setPadding(new Insets(2, 5, 2, 5));
			badge.setStyle("-fx-background-color: rgba(30, 30, 50, 0.8);" + "-fx-background-radius: 8;"
					+ "-fx-border-color: " + colorToHex(getColorForCharacter(voterCharId)) + ";"
					+ "-fx-border-radius: 8;" + "-fx-border-width: 1;");

			// Create small circle with voter color
			Rectangle colorDot = new Rectangle(8, 8);
			colorDot.setFill(getColorForCharacter(voterCharId));
			colorDot.setArcWidth(8);
			colorDot.setArcHeight(8);

			// Text saying "voted" - smaller font
			Text voteText = new Text("vote");
			voteText.setFont(Font.font("Monospace", FontWeight.BOLD, 8));
			voteText.setFill(Color.WHITE);

			badge.getChildren().addAll(colorDot, voteText);
			return badge;
		}

		/**
		 * Converts a Color object to hex string for use in styles
		 */
		private String colorToHex(Color color) {
			return String.format("#%02X%02X%02X", (int) (color.getRed() * 255), (int) (color.getGreen() * 255),
					(int) (color.getBlue() * 255));
		}

		/**
		 * Adds a vote from the specified character
		 */
		public void addVote(int voterCharId) {
			HBox voteBadge = createVoteBadge(voterCharId);
			voteIconsBox.getChildren().add(voteBadge);

			// Limit to 3 visible votes to save space
			if (voteIconsBox.getChildren().size() > 3) {
				// Add a +N more indicator instead of showing all votes
				int extraVotes = voteIconsBox.getChildren().size() - 3;
				voteIconsBox.getChildren().remove(3, voteIconsBox.getChildren().size());

				Text moreVotes = new Text("+" + extraVotes + " more");
				moreVotes.setFont(Font.font("Monospace", FontWeight.BOLD, 8));
				moreVotes.setFill(Color.LIGHTGRAY);

				voteIconsBox.getChildren().add(moreVotes);
			}
		}

		/**
		 * Sets whether this card is selected
		 */
		public void setSelected(boolean selected) {
			this.isSelected = selected;

			// Update visual appearance
			if (selected) {
				cardBg.setFill(Color.rgb(80, 100, 160, 0.9)); // Lighter blue highlight
				cardBg.setStroke(Color.rgb(100, 200, 255)); // Brighter blue border
				cardBg.setStrokeWidth(3); // Thicker border when selected
			} else {
				if (isDead) {
					cardBg.setFill(Color.rgb(70, 70, 70, 0.7)); // Grey for dead
					cardBg.setStroke(Color.rgb(120, 120, 120));
				} else {
					cardBg.setFill(Color.rgb(50, 50, 70, 0.8));
					cardBg.setStroke(Color.rgb(30, 144, 255));
					cardBg.setStrokeWidth(2);
				}
			}
		}

		/**
		 * Marks this player as dead
		 */
		public void markAsDead() {
			this.isDead = true;

			// Create red X mark
			Rectangle redOverlay = new Rectangle(100, 120);
			redOverlay.setFill(Color.rgb(200, 0, 0, 0.3)); // Semi-transparent red
			redOverlay.setArcWidth(8);
			redOverlay.setArcHeight(8);

			// Add X lines - smaller for smaller card
			javafx.scene.shape.Line line1 = new javafx.scene.shape.Line(20, 20, 80, 100);
			line1.setStroke(Color.RED);
			line1.setStrokeWidth(3);

			javafx.scene.shape.Line line2 = new javafx.scene.shape.Line(80, 20, 20, 100);
			line2.setStroke(Color.RED);
			line2.setStrokeWidth(3);

			// Grey out the card background
			cardBg.setFill(Color.rgb(70, 70, 70, 0.7)); // Grey background
			cardBg.setStroke(Color.rgb(120, 120, 120)); // Darker grey border

			// Grey out the name text
			nameText.setFill(Color.rgb(170, 170, 170)); // Light grey text

			// Reduce opacity of the player image
			characterImage.setOpacity(0.7);

			// Add the X mark on top
			getChildren().addAll(redOverlay, line1, line2);

			// Disable interactions
			setDisable(true);
		}

		/**
		 * Disables voting on this card
		 */
		public void disableVoting() {
			setDisable(true);
		}

		/**
		 * Returns whether this player is dead
		 */
		public boolean isDead() {
			return isDead;
		}

		/**
		 * Returns a color associated with a character ID
		 */
		private Color getColorForCharacter(int charId) {
			// Map character IDs to colors - same as in your game
			switch (charId % 10) {
			case 0:
				return Color.rgb(0, 180, 0); // Green
			case 1:
				return Color.rgb(30, 144, 255); // Blue
			case 2:
				return Color.rgb(128, 0, 128); // Purple
			case 3:
				return Color.rgb(255, 140, 0); // Orange
			case 4:
				return Color.rgb(220, 20, 60); // Red
			case 5:
				return Color.rgb(255, 105, 180); // Pink
			case 6:
				return Color.rgb(0, 191, 255); // Cyan
			case 7:
				return Color.rgb(255, 215, 0); // Yellow
			case 8:
				return Color.rgb(139, 69, 19); // Brown
			case 9:
				return Color.rgb(128, 128, 128); // Gray
			default:
				return Color.WHITE;
			}
		}
	}

	/**
	 * Closes the meeting UI
	 */
	private void closeMeetingUI() {
		// Fade out animation
		FadeTransition fadeOut = new FadeTransition(Duration.millis(500), this);
		fadeOut.setFromValue(1);
		fadeOut.setToValue(0);
		fadeOut.setOnFinished(event -> {
			try {
				// Remove from parent if we have one
				if (getParent() != null) {
					((Pane) getParent()).getChildren().remove(this);
				}

				// Clear reference in GameWindow
				if (gameWindow != null) {
					gameWindow.clearActiveMeetingUI();
				}
			} catch (Exception e) {
				System.err.println("Error closing meeting UI: " + e.getMessage());
				e.printStackTrace();

				// Emergency fallback - force remove from parent
				if (getParent() != null) {
					Platform.runLater(() -> {
						((Pane) getParent()).getChildren().remove(this);
					});
				}
			} finally {
				// Shutdown executor if it exists and is not already shut down
				if (executor != null && !executor.isShutdown()) {
					executor.shutdownNow();
				}
			}
		});
		fadeOut.play();
	}

}
package gui;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PathTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
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
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
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
	private Set<String> processedChatMessages = new HashSet<>();
	private long lastLocalMessageTimestamp = 0;
	private Set<String> processedVoterKeys = new HashSet<>();
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
		// Add ethereal background effect
		Rectangle ghostOverlay = new Rectangle(getWidth(), getHeight());

		// Create a more subtle and eerie ghost effect with radial gradient
		RadialGradient etherealGlow = new RadialGradient(0, 0, 0.5, 0.5, 1.0, true, CycleMethod.NO_CYCLE,
				new Stop(0, Color.rgb(20, 20, 80, 0.0)), new Stop(0.8, Color.rgb(30, 30, 120, 0.1)),
				new Stop(1.0, Color.rgb(50, 50, 150, 0.25)));

		ghostOverlay.setFill(etherealGlow);
		getChildren().add(1, ghostOverlay); // Add between background and panel

		// Add subtle ghost particles in the background
		createGhostParticles();

		// Apply ethereal glow effect to main panel
		DropShadow ghostGlow = new DropShadow();
		ghostGlow.setColor(Color.rgb(130, 130, 255, 0.7));
		ghostGlow.setRadius(15);
		ghostGlow.setSpread(0.4);
		mainPanel.setEffect(ghostGlow);

		// Change border color of the main panel to a ghostly blue
		mainPanel.setBorder(new Border(new BorderStroke(Color.rgb(140, 140, 255, 0.7), // Ethereal blue border
				BorderStrokeStyle.SOLID, new CornerRadii(0), new BorderWidths(3))));

		// Apply a subtle animation to the panel to make it feel ethereal
		Timeline pulseAnimation = new Timeline(
				new KeyFrame(Duration.ZERO, new KeyValue(mainPanel.opacityProperty(), 0.92),
						new KeyValue(ghostOverlay.opacityProperty(), 0.7)),
				new KeyFrame(Duration.seconds(3), new KeyValue(mainPanel.opacityProperty(), 0.97),
						new KeyValue(ghostOverlay.opacityProperty(), 0.9)),
				new KeyFrame(Duration.seconds(6), new KeyValue(mainPanel.opacityProperty(), 0.92),
						new KeyValue(ghostOverlay.opacityProperty(), 0.7)));
		pulseAnimation.setCycleCount(Animation.INDEFINITE);
		pulseAnimation.play();
	}

	private void createGhostParticles() {
		// Create a pane for the ghost particles
		Pane particlePane = new Pane();
		particlePane.setPrefSize(getWidth(), getHeight());
		particlePane.setMouseTransparent(true);

		// Create a number of subtle ghost particles
		Random random = new Random();
		for (int i = 0; i < 12; i++) {
			// Create particle
			Circle particle = new Circle();
			particle.setRadius(random.nextDouble() * 2 + 1);
			particle.setFill(Color.rgb(180, 180, 255, 0.3));

			// Random starting position
			double startX = random.nextDouble() * getWidth();
			double startY = random.nextDouble() * getHeight();
			particle.setCenterX(startX);
			particle.setCenterY(startY);

			// Add glow effect
			DropShadow glow = new DropShadow();
			glow.setColor(Color.rgb(150, 150, 255, 0.6));
			glow.setRadius(4);
			particle.setEffect(glow);

			// Add to pane
			particlePane.getChildren().add(particle);

			// Create floating animation
			double endX = startX + (random.nextDouble() * 100 - 50);
			double endY = startY - (random.nextDouble() * 100 + 50); // Always float upward

			// Duration between 10-20 seconds for slow, eerie movement
			double duration = 10 + random.nextDouble() * 10;

			// Create path transition
			Path path = new Path();
			path.getElements().add(new MoveTo(startX, startY));

			// Add control points for curved path
			double controlX1 = startX + (random.nextDouble() * 100 - 50);
			double controlY1 = startY - (random.nextDouble() * 50);
			double controlX2 = endX - (random.nextDouble() * 100 - 50);
			double controlY2 = endY + (random.nextDouble() * 50);

			path.getElements().add(new CubicCurveTo(controlX1, controlY1, controlX2, controlY2, endX, endY));

			PathTransition transition = new PathTransition();
			transition.setDuration(Duration.seconds(duration));
			transition.setPath(path);
			transition.setNode(particle);
			transition.setCycleCount(Animation.INDEFINITE);

			// Add fade in/out during movement
			FadeTransition fade = new FadeTransition(Duration.seconds(duration), particle);
			fade.setFromValue(0.1);
			fade.setToValue(0.5);
			fade.setCycleCount(Animation.INDEFINITE);
			fade.setAutoReverse(true);

			// Play animations
			transition.play();
			fade.play();
		}
		getChildren().add(1, particlePane);
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

		// Chat header with different text and styling for dead players
		Text chatHeader;
		if (isPlayerDead) {
			chatHeader = new Text("ETHEREAL COMMUNICATION");
			chatHeader.setFill(Color.rgb(180, 180, 255)); // Brighter ghostly blue

			// Add a subtle glow effect to the header
			DropShadow headerGlow = new DropShadow();
			headerGlow.setColor(Color.rgb(140, 140, 255, 0.8));
			headerGlow.setRadius(10);
			headerGlow.setSpread(0.4);
			chatHeader.setEffect(headerGlow);
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

		// Enhanced styling for dead players
		if (isPlayerDead) {
			chatArea.setStyle("-fx-control-inner-background: rgba(25, 25, 50, 0.9); " + // Darker blue background with
																						// transparency
					"-fx-text-fill: rgba(180, 180, 255, 0.9); " + // Brighter ghostly text
					"-fx-font-family: 'Monospace'; " + "-fx-border-color: rgba(100, 100, 200, 0.8); " + // More visible
																										// border
					"-fx-border-width: 1.5px; " + // Slightly thicker border
					"-fx-background-radius: 5px; " + // Rounded corners
					"-fx-border-radius: 5px; " // Rounded corners for border
			);
		} else {
			chatArea.setStyle("-fx-control-inner-background: #2a2a2a; " + "-fx-text-fill: white; "
					+ "-fx-font-family: 'Monospace'; " + "-fx-border-color: #555555; " + "-fx-border-width: 1px;");
		}

		// Input area - enhanced for ghost players
		HBox inputBox = new HBox(5);

		messageInput = new TextField();
		messageInput.setPrefHeight(30);

		// Enhanced styling for ghost chat input
		if (isPlayerDead) {
			messageInput.setPromptText("Communicate with the beyond...");
			messageInput.setStyle("-fx-background-color: rgba(40, 40, 80, 0.8); " + // Dark blue with transparency
					"-fx-text-fill: rgba(200, 200, 255, 0.9); " + // Bright ghostly text
					"-fx-font-family: 'Monospace'; " + "-fx-border-color: rgba(120, 120, 220, 0.7); " + // Ghostly
																										// border
					"-fx-border-width: 1.5px; " + // Slightly thicker border
					"-fx-background-radius: 5px; " + // Rounded corners
					"-fx-border-radius: 5px; " // Rounded corners for border
			);
		} else {
			messageInput.setPromptText("Type your message...");
			messageInput.setStyle("-fx-background-color: #2a2a2a; " + "-fx-text-fill: white; "
					+ "-fx-font-family: 'Monospace'; " + "-fx-border-color: #555555; " + "-fx-border-width: 1px;");
		}
		HBox.setHgrow(messageInput, Priority.ALWAYS);

		sendButton = new Button("Send");
		sendButton.setPrefHeight(30);

		if (isPlayerDead) {
			sendButton.setStyle("-fx-background-color: rgba(80, 80, 180, 0.8); " + // Bright ghostly blue
					"-fx-text-fill: rgba(220, 220, 255, 0.9); " + // Almost white text
					"-fx-font-family: 'Monospace'; " + "-fx-font-weight: bold; "
					+ "-fx-border-color: rgba(140, 140, 220, 0.7); " + // Ghostly border
					"-fx-border-width: 1.5px; " + // Slightly thicker border
					"-fx-background-radius: 5px; " + // Rounded corners
					"-fx-border-radius: 5px; " + // Rounded corners for border
					"-fx-effect: dropshadow(three-pass-box, rgba(120, 120, 255, 0.5), 5, 0, 0, 0);" // Ghostly glow
			);

			// Add hover effect for ghost button
			sendButton.setOnMouseEntered(e -> sendButton.setStyle("-fx-background-color: rgba(100, 100, 220, 0.9); " + // Brighter
																														// on
																														// hover
					"-fx-text-fill: white; " + "-fx-font-family: 'Monospace'; " + "-fx-font-weight: bold; "
					+ "-fx-border-color: rgba(160, 160, 255, 0.8); " + "-fx-border-width: 1.5px; "
					+ "-fx-background-radius: 5px; " + "-fx-border-radius: 5px; "
					+ "-fx-effect: dropshadow(three-pass-box, rgba(140, 140, 255, 0.6), 8, 0, 0, 0);"));

			sendButton.setOnMouseExited(e -> sendButton.setStyle("-fx-background-color: rgba(80, 80, 180, 0.8); "
					+ "-fx-text-fill: rgba(220, 220, 255, 0.9); " + "-fx-font-family: 'Monospace'; "
					+ "-fx-font-weight: bold; " + "-fx-border-color: rgba(140, 140, 220, 0.7); "
					+ "-fx-border-width: 1.5px; " + "-fx-background-radius: 5px; " + "-fx-border-radius: 5px; "
					+ "-fx-effect: dropshadow(three-pass-box, rgba(120, 120, 255, 0.5), 5, 0, 0, 0);"));
		} else {
			sendButton.setStyle(
					"-fx-background-color: #1e90ff; " + "-fx-text-fill: white; " + "-fx-font-family: 'Monospace'; "
							+ "-fx-font-weight: bold; " + "-fx-border-color: #87cefa; " + "-fx-border-width: 1px;");
		}

		sendButton.setOnAction(e -> sendChatMessage());
		messageInput.setOnAction(e -> sendChatMessage());

		inputBox.getChildren().addAll(messageInput, sendButton);

		// Add all to chat box
		chatBox.getChildren().addAll(chatHeader, chatArea, inputBox);

		// Subtle information for dead players
		if (isPlayerDead) {
			Text ghostInfo = new Text("Only souls of the departed can see your messages");
			ghostInfo.setFont(Font.font("Monospace", FontWeight.NORMAL, 12));
			ghostInfo.setFill(Color.rgb(160, 160, 255, 0.8)); // Ghostly blue
			ghostInfo.setTextAlignment(TextAlignment.CENTER);

			// Add subtle glow
			DropShadow textGlow = new DropShadow();
			textGlow.setColor(Color.rgb(100, 100, 220, 0.6));
			textGlow.setRadius(5);
			ghostInfo.setEffect(textGlow);

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
	 * Sends the current message in the input field
	 */
	private void sendChatMessage() {
		String message = messageInput.getText().trim();
		if (!message.isEmpty()) {
			// Get local player name and status
			String playerName = PlayerLogic.getName();
			String playerStatus = PlayerLogic.getStatus();
			boolean isGhost = "dead".equals(playerStatus);

			// Generate a timestamp for this message
			long timestamp = System.currentTimeMillis();
			// Store the timestamp for deduplication
			lastLocalMessageTimestamp = timestamp;

			// Create a unique message ID
			String messageId = playerName + ":" + message + ":" + timestamp;

			// Add to processed set to prevent duplication when it comes back from server
			processedChatMessages.add(messageId);

			// Display message locally
			addChatMessage(playerName, message, playerStatus);

			try {
				// Create meeting-specific JSON message with the timestamp
				JSONObject meetingChatData = new JSONObject();
				meetingChatData.put("type", "chat");
				meetingChatData.put("name", playerName);
				meetingChatData.put("message", message);
				meetingChatData.put("meetingId", meetingId);
				meetingChatData.put("status", playerStatus);
				meetingChatData.put("timestamp", timestamp); // Add timestamp for deduplication
				meetingChatData.put("isGhostMessage", isGhost); // Flag if it's a ghost message

				String meetingMessage = "/meeting/" + meetingChatData.toString();

				// Send message based on client/server role
				if (MainMenuPane.getState() == State.SERVER) {
					// If server, broadcast to appropriate clients
					for (ClientInfo clientInfo : ServerLogic.getConnectedClients()) {
						try {
							// Skip sending ghost messages to living players
							if (isGhost) {
								String clientKey = clientInfo.getAddress().getHostAddress() + ":"
										+ clientInfo.getPort();
								PlayerInfo targetPlayer = GameLogic.playerList.get(clientKey);

								// Only send to other ghosts
								if (targetPlayer == null || !"dead".equals(targetPlayer.getStatus())) {
									continue; // Skip living players
								}
							}

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
					// If client, send to server
					ClientLogic.sendMessage(meetingMessage, null);
				}
			} catch (Exception e) {
				System.err.println("Error sending meeting chat: " + e.getMessage());
			}

			// Clear the input field
			messageInput.clear();
		}
	}

	/**
	 * Updated addChatMessage method to support ghost messages This should replace
	 * the current addChatMessage method in MeetingUI.java
	 */
	public void addChatMessage(String playerName, String message, String playerStatus) {
		Platform.runLater(() -> {
			try {
				String timestamp = java.time.LocalTime.now()
						.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

				// Create a message ID for deduplication
				String messageId = playerName + ":" + message + ":" + System.currentTimeMillis();

				// If this is from us and very similar to a recent message, don't add it
				if (playerName.equals(PlayerLogic.getName())) {
					String lastMessage = chatArea.getText();
					String[] lines = lastMessage.split("\n");
					for (String line : lines) {
						if (line.contains(playerName + ": " + message)) {
							System.out.println("MEETING UI: Similar message already displayed, ignoring");
							return;
						}
					}
				}

				// Check if this is a ghost message
				boolean isGhostMessage = "dead".equals(playerStatus);

				// Check if the local player is a ghost
				boolean localPlayerIsGhost = "dead".equals(PlayerLogic.getStatus());

				// If this is a ghost message and local player is NOT a ghost, don't show it
				if (isGhostMessage && !localPlayerIsGhost && !"SYSTEM".equals(playerName)) {
					System.out.println("Not showing ghost message to living player: " + message);
					return;
				}

				// Format message based on sender status
				String formattedMessage;
				if ("SYSTEM".equals(playerName)) {
					formattedMessage = "[" + timestamp + "] " + playerName + ": " + message + "\n";
				} else if (isGhostMessage) {
					// Enhanced ghost message format with more subtle indication
					formattedMessage = "[" + timestamp + "] " + playerName + " \uD83D\uDC7B: " + message + "\n";

					// If we're also a ghost, apply color styling directly using CSS
					if (localPlayerIsGhost) {
						// Since TextArea doesn't support inline styles, we need to identify ghost
						// messages
						// by their format when styling the overall display
					}
				} else {
					formattedMessage = "[" + timestamp + "] " + playerName + ": " + message + "\n";
				}

				// Add to chat area
				chatArea.appendText(formattedMessage);
				chatArea.setScrollTop(Double.MAX_VALUE); // Scroll to bottom
			} catch (Exception e) {
				System.err.println("Error adding chat message: " + e.getMessage());
				e.printStackTrace();
			}
		});
	}

	public void receiveChatMessage(String playerName, String message, String playerStatus) {
		Platform.runLater(() -> {
			try {
				// Check if the chat area is properly initialized
				if (chatArea == null) {
					System.err.println("Chat area is null in MeetingUI");
					return;
				}

				// Check if this is a ghost message
				boolean isGhostMessage = "dead".equals(playerStatus);

				// Check if the local player is a ghost
				boolean localPlayerIsGhost = "dead".equals(PlayerLogic.getStatus());

				// If this is a ghost message and local player is NOT a ghost, don't show it
				if (isGhostMessage && !localPlayerIsGhost && !"SYSTEM".equals(playerName)) {
					System.out.println("Not showing ghost message to living player: " + message);
					return;
				}

				// Create a message ID for deduplication
				String messageId = playerName + ":" + message + ":" + lastLocalMessageTimestamp;

				// Check if this is our own message that we've already displayed
				if (processedChatMessages.contains(messageId)) {
					System.out.println("MEETING UI: Ignoring duplicate chat message: " + messageId);
					return;
				}

				// If the message is from us but doesn't match our last timestamp,
				// it could be a different message or one from a previous session
				if (playerName.equals(PlayerLogic.getName())
						&& System.currentTimeMillis() - lastLocalMessageTimestamp < 10000) {
					// If it's a recent message from us, check if the content is similar
					String lastMessage = chatArea.getText();
					String[] lines = lastMessage.split("\n");
					for (String line : lines) {
						if (line.contains(playerName + ": " + message)) {
							System.out.println("MEETING UI: Similar message already displayed, ignoring");
							return;
						}
					}
				}

				// Format and add the message to the chat area
				String timestamp = java.time.LocalTime.now()
						.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
				String formattedMessage = "[" + timestamp + "] ";

				// Format differently based on player status
				if ("SYSTEM".equals(playerName)) {
					formattedMessage += playerName + ": " + message + "\n";
				} else if (isGhostMessage) {
					formattedMessage += "👻 " + playerName + " (GHOST): " + message + "\n";
				} else {
					formattedMessage += playerName + ": " + message + "\n";
				}

				// Add to chat
				chatArea.appendText(formattedMessage);
				chatArea.setScrollTop(Double.MAX_VALUE); // Scroll to bottom
			} catch (Exception e) {
				System.err.println("MEETING UI ERROR: Failed to receive chat message: " + e.getMessage());
				e.printStackTrace();
			}
		});
	}

	/**
	 * Adds a message to the chat area
	 */
	public void addChatMessage(String playerName, String message) {
		Platform.runLater(() -> {
			String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
			// Create a message ID for deduplication
			String messageId = playerName + ":" + message + ":" + System.currentTimeMillis();

			// If this is from us and very similar to a recent message, don't add it
			if (playerName.equals(PlayerLogic.getName())) {
				String lastMessage = chatArea.getText();
				String[] lines = lastMessage.split("\n");
				for (String line : lines) {
					if (line.contains(playerName + ": " + message)) {
						System.out.println("MEETING UI: Similar message already displayed, ignoring");
						return;
					}
				}
			}
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
			// Record local vote state
			selectedPlayerKey = targetPlayerKey;
			playerVotes.put(PlayerLogic.getLocalAddressPort(), targetPlayerKey);
			hasVoted = true;

			// Disable the skip button since we've voted
			skipVoteButton.setDisable(true);

			// Send vote to server
			if (MainMenuPane.getState() == State.SERVER) {
				// If server, handle the vote locally via ServerLogic
				ServerLogic.handleVote(PlayerLogic.getLocalAddressPort(), targetPlayerKey, meetingId, null);
			} else {
				// If client, send vote to server
				sendVoteToServer(targetPlayerKey);
			}

			// The vote UI will update when the server broadcasts the vote to all clients
			// This ensures all clients see consistent voting state
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

			// Record skip vote locally
			playerVotes.put(PlayerLogic.getLocalAddressPort(), "skip");
			hasVoted = true;

			// Send vote to server
			if (MainMenuPane.getState() == State.SERVER) {
				// If server, handle the vote directly
				ServerLogic.handleVote(PlayerLogic.getLocalAddressPort(), "skip", meetingId, null);
			} else {
				// If client, send vote to server
				sendVoteToServer("skip");
			}

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
			try {

				if (processedVoterKeys.contains(voterKey)) {
					// Check if the vote is the same as before
					String previousVote = playerVotes.get(voterKey);
					if (previousVote != null && previousVote.equals(targetKey)) {
						System.out.println("MEETING UI: Ignoring duplicate vote from " + voterKey);
						return;
					}
					// If vote is different, allow the change
					System.out.println("MEETING UI: Player " + voterKey + " changed vote from " + previousVote + " to "
							+ targetKey);
				}

				// Mark this voter as processed
				processedVoterKeys.add(voterKey);

				System.out.println("MEETING UI: Processing vote from " + voterKey + " for " + targetKey);

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
					checkAllPlayersVoted();
					addChatMessage("SYSTEM", voterName + " voted for " + targetName);
				} else {
					System.err.println("MEETING UI: Could not find player card for target: " + targetKey);
				}
			} catch (Exception e) {
				System.err.println("MEETING UI ERROR: Failed to process vote: " + e.getMessage());
				e.printStackTrace();
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
			ServerLogic.endMeetingAndBroadcastResults(meetingId, chatArea);
		}
		System.out.println("FLAG CHECK");
		// For clients: wait for server to send results
		// The results will be displayed when the server sends them
		ensureProperClosing();
	}

	private void checkAllPlayersVoted() {
		// Skip if timer is already accelerated or we're already at 3 seconds or less
		if (votingTimeSeconds <= 3) {
			return;
		}

		// Count alive players (only alive players can vote)
		int alivePlayers = 0;

		// Check if local player is alive
		if (!"dead".equals(PlayerLogic.getStatus())) {
			alivePlayers++;
		}

		// Count other alive players
		for (String key : GameLogic.playerList.keySet()) {
			PlayerInfo player = GameLogic.playerList.get(key);
			if (player != null && !"dead".equals(player.getStatus())) {
				alivePlayers++;
			}
		}

		// Count all votes (including skip votes)
		int votesCount = playerVotes.size();

		System.out
				.println("MEETING UI: Vote check - " + votesCount + " votes out of " + alivePlayers + " alive players");

		// If all alive players have voted
		if (votesCount >= alivePlayers && alivePlayers > 0) {
			System.out.println("MEETING UI: All players have voted! Reducing timer to 3 seconds.");

			// Stop existing timer
			if (votingTimer != null) {
				votingTimer.stop();
			}

			// Set remaining time to 3 seconds
			votingTimeSeconds = 3;
			timerText.setText("Voting Ends: " + votingTimeSeconds + "s");
			timerText.setFill(Color.RED); // Set to red for urgency

			// Start a new timer for the last 3 seconds
			votingTimer = new Timeline();
			votingTimer.setCycleCount(3);

			KeyFrame frame = new KeyFrame(Duration.seconds(1), event -> {
				votingTimeSeconds--;
				timerText.setText("Voting Ends: " + votingTimeSeconds + "s");

				if (votingTimeSeconds <= 0) {
					endVoting();
				}
			});

			votingTimer.getKeyFrames().add(frame);
			votingTimer.play();

			// Add message in chat about shortened timer
			addChatMessage("SYSTEM", "All players have voted! Discussion time shortened.");
		}
	}

	/**
	 * Modified version of the showVotingResults method in MeetingUI class This
	 * version properly handles ejections by flagging players as ejected rather than
	 * just killed. It skips showing ejection text in the meeting UI.
	 */
	public void showVotingResults(String ejectedPlayerKey, Map<String, Integer> voteResults) {
		Platform.runLater(() -> {
			try {
				System.out.println("CLIENT UI: Showing voting results - Ejected: "
						+ (ejectedPlayerKey != null ? ejectedPlayerKey : "None") + ", Votes: " + voteResults);

				// Store vote results
				this.votes = new HashMap<>(voteResults);

				// Create a flag to track ejection state for the local player
				boolean localPlayerEjected = false;
				boolean wasLocalPlayerImposter = false;

				// Handle ejection
				if (ejectedPlayerKey != null) {
					// If it's the local player
					if (ejectedPlayerKey.equals(PlayerLogic.getLocalAddressPort())) {
						localPlayerEjected = true;
						wasLocalPlayerImposter = "imposter".equals(PlayerLogic.getStatus()); // check if we're imposter
						PlayerLogic.flagEjected(true);
						System.out.println("CLIENT: You were ejected!");
					}
					// If it's another player
					else if (GameLogic.playerList.containsKey(ejectedPlayerKey)) {
						System.out.println("OTHER PLAYER EJECTED");
						PlayerInfo player = GameLogic.playerList.get(ejectedPlayerKey);
						if (player != null) {
							// Update player status
							player.setStatus("dead");
							System.out.println("MEETING UI: Player " + player.getName() + " was ejected!");
						}
					}

					// Store the ejection state for use when the meeting UI closes
					boolean finalWasImposter = false;

					if (ejectedPlayerKey.equals(PlayerLogic.getLocalAddressPort())) {
						// For local player
						finalWasImposter = wasLocalPlayerImposter;
					} else {
						// Get imposter status from server broadcast
						finalWasImposter = voteResults.containsKey("wasImposter")
								? (voteResults.get("wasImposter").intValue() == 1)
								: false;
						System.out.println("Final was Imposter : " + finalWasImposter);
					}

					// Add a custom callback to the GameWindow class when closing the meeting UI
					if (gameWindow != null) {
						gameWindow.setEjectionInfo(ejectedPlayerKey, finalWasImposter);
					}

					// Close meeting UI quickly to show ejection screen - REDUCED DELAY from 5+
					// seconds
					if (executor != null && !executor.isShutdown()) {
						executor.schedule(() -> Platform.runLater(this::closeMeetingUI), 1, TimeUnit.SECONDS);
					} else {
						// Fallback if executor is already shut down
						Timeline closingTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> closeMeetingUI()));
						closingTimer.play();
					}

					// Skip showing ejection text in meeting UI
					return;
				}
				int skipVotes = voteResults.getOrDefault("skip", 0);
				int totalVotes = 0;
				int highestVotes = 0;
				boolean hasTie = false;

				// Count votes and check for tie
				for (Map.Entry<String, Integer> entry : voteResults.entrySet()) {
					if (!entry.getKey().equals("wasImposter")) {
						totalVotes += entry.getValue();

						if (!entry.getKey().equals("skip") && entry.getValue() > highestVotes) {
							highestVotes = entry.getValue();
						}
					}
				}
				boolean isSkip = skipVotes > highestVotes;

				// Close the meeting UI quickly
				if (executor != null && !executor.isShutdown()) {
					executor.schedule(() -> Platform.runLater(this::closeMeetingUI), 1, TimeUnit.SECONDS);
				} else {
					// Fallback if executor is already shut down
					Timeline closingTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> closeMeetingUI()));
					closingTimer.play();
				}

				// Show the skip/draw panel after closing the meeting UI
				if (gameWindow != null) {
					final boolean finalIsSkip = isSkip;
					Platform.runLater(() -> {
						gameWindow.showSkipDrawPanel(finalIsSkip, voteResults);
					});
				}
				// Create results display only for no ejection case
				VBox resultsBox = new VBox(20);
				resultsBox.setAlignment(Pos.CENTER);
				resultsBox.setPadding(new Insets(20));
				resultsBox.setBackground(
						new Background(new BackgroundFill(Color.rgb(0, 0, 0, 0.8), new CornerRadii(10), Insets.EMPTY)));

				// Results text for skip/tie only
				Text resultsText = new Text("No one was ejected (Skipped or Tie)");
				resultsText.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
				resultsText.setFill(Color.WHITE);

				// Add to results box
				resultsBox.getChildren().add(resultsText);

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

				// Schedule closing the meeting UI after showing results - REDUCED DELAY from 7
				// seconds
				if (executor != null && !executor.isShutdown()) {
					executor.schedule(() -> Platform.runLater(this::closeMeetingUI), 3, TimeUnit.SECONDS);
				} else {
					// Fallback if executor is already shut down
					Timeline closingTimer = new Timeline(new KeyFrame(Duration.seconds(3), e -> closeMeetingUI()));
					closingTimer.play();
				}
			} catch (Exception e) {
				System.err.println("CLIENT UI ERROR: Failed to show voting results: " + e.getMessage());
				e.printStackTrace();
			}
		});
	}

	private void ensureProperClosing() {
		// Schedule the UI to close after results are displayed - REDUCED DELAY
		if (executor != null && !executor.isShutdown()) {
			executor.schedule(() -> Platform.runLater(this::closeMeetingUI), 3, TimeUnit.SECONDS);
		} else {
			// Fallback if executor is unavailable
			Timeline closingTimer = new Timeline(new KeyFrame(Duration.seconds(3), e -> closeMeetingUI()));
			closingTimer.play();
		}
	}

	private void debugPrintVotes() {
		System.out.println("===== MEETING UI VOTE DEBUG =====");
		System.out.println("Meeting ID: " + meetingId);
		System.out.println("Player votes: " + playerVotes);
		System.out.println("Vote counts: " + votes);
		System.out.println("Has voted: " + hasVoted);
		System.out.println("Selected player: " + selectedPlayerKey);
		System.out.println("================================");
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

	private void closeMeetingUI() {
		// First clean up resources
		if (votingTimer != null) {
			votingTimer.stop();
			votingTimer = null;
		}

		if (executor != null && !executor.isShutdown()) {
			executor.shutdownNow();
			executor = null;
		}

		// Clean up references
		playerCards.clear();
		processedChatMessages.clear();
		processedVoterKeys.clear();
		playerVotes.clear();
		votes.clear();

		// Perform a safe removal using the JavaFX application thread
		Platform.runLater(() -> {
			try {
				// Create fade out transition
				FadeTransition fadeOut = new FadeTransition(Duration.millis(500), this);
				fadeOut.setFromValue(1);
				fadeOut.setToValue(0);

				fadeOut.setOnFinished(event -> {
					try {
						// Reset references in GameWindow
						if (gameWindow != null) {
							gameWindow.clearActiveMeetingUI();
							// Call the new method to handle post-meeting transitions
							gameWindow.handleMeetingClosed();
						}

						// Remove from parent
						if (getParent() != null) {
							if (getParent() instanceof Pane) {
								((Pane) getParent()).getChildren().remove(this);
							} else if (getParent() instanceof Group) {
								((Group) getParent()).getChildren().remove(this);
							} else {
								System.err.println("Unknown parent type: " + getParent().getClass().getName());
								// Try generic approach for unknown parent types
								try {
									java.lang.reflect.Method getChildrenMethod = getParent().getClass()
											.getMethod("getChildren");
									Object childrenList = getChildrenMethod.invoke(getParent());

									if (childrenList instanceof javafx.collections.ObservableList) {
										((javafx.collections.ObservableList<?>) childrenList).remove(this);
									}
								} catch (Exception ex) {
									System.err.println("Failed to remove meeting UI from parent: " + ex.getMessage());
								}
							}
						}

						System.out.println("Meeting UI successfully closed and removed");
					} catch (Exception e) {
						System.err.println("Error during meeting UI cleanup: " + e.getMessage());
						e.printStackTrace();
					}
				});

				// Start the transition
				fadeOut.play();
			} catch (Exception e) {
				System.err.println("Error starting meeting UI fade out: " + e.getMessage());
				e.printStackTrace();

				// Emergency removal as fallback
				if (getParent() != null) {
					try {
						if (getParent() instanceof Pane) {
							((Pane) getParent()).getChildren().remove(this);
						} else if (getParent() instanceof Group) {
							((Group) getParent()).getChildren().remove(this);
						}

						// Reset reference in GameWindow
						if (gameWindow != null) {
							gameWindow.clearActiveMeetingUI();
							// Call the new method to handle post-meeting transitions
							gameWindow.handleMeetingClosed();
						}

						System.out.println("Meeting UI removed through emergency cleanup");
					} catch (Exception ex) {
						System.err.println("Emergency removal also failed: " + ex.getMessage());
					}
				}
			}
		});
	}

}
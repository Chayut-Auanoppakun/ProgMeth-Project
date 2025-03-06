package gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
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
import server.PlayerInfo;

public class MeetingUI extends StackPane {
    
    // UI Components
    private Rectangle backgroundDim;
    private StackPane mainPanel;
    private GridPane playerGrid;
    private Button skipVoteButton;
    private Button chatButton;
    private Text timerText;
    private VBox chatPanel;
    private TextArea chatArea;
    private TextArea messageInput;
    private boolean isChatVisible = false;
    
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
     * @param gameWindow Reference to the game window
     * @param reportedPlayer The name of the reported dead player (null if emergency button)
     * @param reporterKey The key of the player who called the meeting
     */
    public MeetingUI(GameWindow gameWindow, String reportedPlayer, String reporterKey) {
        this.gameWindow = gameWindow;
        this.reportedBodyPlayer = reportedPlayer;
        this.reporterKey = reporterKey;
        this.executor = Executors.newSingleThreadScheduledExecutor();
        
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
        
        // Create main panel - tablet style
        mainPanel = createTabletPanel();
        
        // Center the tablet in the screen
        mainPanel.setTranslateX((gameWindow.getWidth() - mainPanel.getMaxWidth()) / 2);
        mainPanel.setTranslateY((gameWindow.getHeight() - mainPanel.getMaxHeight()) / 2);
        
        // Add components to the stack
        getChildren().addAll(backgroundDim, mainPanel);
    }
    
    /**
     * Creates the tablet-styled panel that contains the voting interface
     */
    private StackPane createTabletPanel() {
        StackPane tabletPanel = new StackPane();
        tabletPanel.setMaxWidth(800);
        tabletPanel.setMaxHeight(600);
        tabletPanel.setPadding(new Insets(15));
        
        // Create tablet frame (grey border as in the screenshot)
        Rectangle tabletFrame = new Rectangle(800, 600);
        tabletFrame.setArcWidth(30);
        tabletFrame.setArcHeight(30);
        tabletFrame.setFill(Color.rgb(180, 180, 190));
        tabletFrame.setStroke(Color.rgb(100, 100, 110));
        tabletFrame.setStrokeWidth(10);
        
        // Inner screen area (light blue background as in the screenshot)
        Rectangle innerScreen = new Rectangle(760, 560);
        innerScreen.setArcWidth(20);
        innerScreen.setArcHeight(20);
        innerScreen.setFill(Color.rgb(200, 230, 255, 0.9)); // Slightly transparent light blue
        innerScreen.setStroke(Color.rgb(100, 100, 120, 0.5));
        innerScreen.setStrokeWidth(4);
        
        // Add drop shadow to tablet
        DropShadow shadow = new DropShadow();
        shadow.setRadius(15);
        shadow.setOffsetX(5);
        shadow.setOffsetY(5);
        shadow.setColor(Color.rgb(0, 0, 0, 0.6));
        tabletFrame.setEffect(shadow);
        
        // Create tablet button (circle at bottom)
        StackPane homeButton = new StackPane();
        Circle homeCircle = new Circle(20);
        homeCircle.setFill(Color.WHITE);
        homeButton.getChildren().add(homeCircle);
        homeButton.setAlignment(Pos.BOTTOM_CENTER);
        homeButton.setPadding(new Insets(0, 0, 10, 0));
        
        // Content area for the voting interface
        BorderPane contentArea = new BorderPane();
        contentArea.setPadding(new Insets(15));
        contentArea.setPrefSize(740, 540);
        
        // Title text - larger serif font with shadow as in the screenshot
        Text titleText = new Text("Who Is The Impostor?");
        titleText.setFont(Font.font("Serif", FontWeight.BOLD, 36));
        titleText.setFill(Color.BLACK);
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
        
        // Container for title and timer
        BorderPane topSection = new BorderPane();
        topSection.setCenter(titleText);
        
        // Chat button in top right as in screenshot
        chatButton = createIconButton("chat_icon.png", "Chat");
        chatButton.setOnAction(e -> toggleChatPanel());
        
        topSection.setRight(chatButton);
        
        // Timer display in top right
        timerText = new Text("Voting Ends In: " + votingTimeSeconds + "s");
        timerText.setFont(Font.font("Monospace", FontWeight.BOLD, 18));
        timerText.setFill(Color.BLACK);
        topSection.setBottom(timerText);
        BorderPane.setAlignment(timerText, Pos.CENTER_RIGHT);
        
        // Player grid for voting - large central area
        playerGrid = new GridPane();
        playerGrid.setHgap(60); // More space between columns as in screenshot
        playerGrid.setVgap(30); // More space between rows
        playerGrid.setAlignment(Pos.CENTER);
        
        // Skip vote button - position at bottom center
        skipVoteButton = new Button("SKIP VOTE");
        skipVoteButton.getStyleClass().add("skip-vote-button");
        skipVoteButton.setStyle("-fx-background-color: #333333; " +
                               "-fx-text-fill: white; " +
                               "-fx-font-family: 'Monospace'; " +
                               "-fx-font-weight: bold; " +
                               "-fx-font-size: 16px; " +
                               "-fx-padding: 10 20 10 20; " +
                               "-fx-border-color: #555555; " +
                               "-fx-border-width: 2px; " +
                               "-fx-background-radius: 5; " +
                               "-fx-border-radius: 5;");
        
        skipVoteButton.setOnAction(e -> handleSkipVote());
        
        // Voting result section - positioned at bottom
        HBox bottomSection = new HBox();
        bottomSection.setAlignment(Pos.CENTER);
        bottomSection.getChildren().add(skipVoteButton);
        bottomSection.setPadding(new Insets(20, 0, 30, 0)); // Extra bottom padding
        
        // Assemble content
        contentArea.setTop(topSection);
        contentArea.setCenter(playerGrid);
        contentArea.setBottom(bottomSection);
        
        // Create chat panel (initially hidden)
        chatPanel = createChatPanel();
        chatPanel.setVisible(false);
        chatPanel.setManaged(false);
        
        // Add components to the tablet
        StackPane screenArea = new StackPane();
        screenArea.getChildren().addAll(innerScreen, contentArea, chatPanel);
        
        tabletPanel.getChildren().addAll(tabletFrame, screenArea, homeButton);
        
        return tabletPanel;
    }
    
    /**
     * Creates a VBox containing the chat interface
     */
    private VBox createChatPanel() {
        VBox chatBox = new VBox(10);
        chatBox.setPadding(new Insets(10));
        chatBox.setMaxWidth(400);
        chatBox.setMaxHeight(450);
        chatBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.8); " +
                         "-fx-background-radius: 10;");
        
        // Chat area header
        Text chatHeader = new Text("EMERGENCY MEETING CHAT");
        chatHeader.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        chatHeader.setFill(Color.WHITE);
        
        // Chat display area
        chatArea = new TextArea();
        chatArea.setPrefHeight(300);
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setStyle("-fx-control-inner-background: #333333; " +
                          "-fx-text-fill: white; " +
                          "-fx-font-family: 'Monospace';");
        
        // Message input
        messageInput = new TextArea();
        messageInput.setPrefHeight(60);
        messageInput.setPrefRowCount(2);
        messageInput.setWrapText(true);
        messageInput.setPromptText("Type your message...");
        messageInput.setStyle("-fx-control-inner-background: #333333; " +
                             "-fx-text-fill: white; " +
                             "-fx-font-family: 'Monospace';");
        
        // Send button
        Button sendButton = new Button("SEND");
        sendButton.setStyle("-fx-background-color: #2196F3; " +
                           "-fx-text-fill: white; " +
                           "-fx-font-family: 'Monospace'; " +
                           "-fx-font-weight: bold;");
        
        sendButton.setOnAction(e -> {
            String message = messageInput.getText().trim();
            if (!message.isEmpty()) {
                addChatMessage(PlayerLogic.getName(), message);
                
                // Send message to server/other clients
                if (MainMenuPane.getState() == State.SERVER) {
                    // If server, broadcast to all clients
                    // TODO: Implement server-side chat broadcast
                } else {
                    // If client, send to server
                    // TODO: Implement client-side chat message
                }
                
                messageInput.clear();
            }
        });
        
        // Input container
        HBox inputContainer = new HBox(10);
        inputContainer.getChildren().addAll(messageInput, sendButton);
        HBox.setHgrow(messageInput, javafx.scene.layout.Priority.ALWAYS);
        
        // Close button
        Button closeButton = new Button("X");
        closeButton.setStyle("-fx-background-color: #ff5555; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold;");
        closeButton.setOnAction(e -> toggleChatPanel());
        
        // Header with close button
        HBox headerBox = new HBox();
        headerBox.getChildren().addAll(chatHeader, closeButton);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(chatHeader, javafx.scene.layout.Priority.ALWAYS);
        
        // Add all components
        chatBox.getChildren().addAll(headerBox, chatArea, inputContainer);
        
        // Position the chat panel
        StackPane.setAlignment(chatBox, Pos.TOP_RIGHT);
        StackPane.setMargin(chatBox, new Insets(60, 20, 0, 0));
        
        return chatBox;
    }
    
    /**
     * Adds a message to the chat area
     */
    public void addChatMessage(String playerName, String message) {
        Platform.runLater(() -> {
            String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            chatArea.appendText("[" + timestamp + "] " + playerName + ": " + message + "\n");
            chatArea.setScrollTop(Double.MAX_VALUE); // Scroll to bottom
        });
    }
    
    /**
     * Creates a button with an icon
     */
    private Button createIconButton(String iconPath, String tooltip) {
        Button button = new Button();
        
        // Try to load the icon image
        try {
            ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/icons/" + iconPath)));
            icon.setFitWidth(24);
            icon.setFitHeight(24);
            button.setGraphic(icon);
        } catch (Exception e) {
            // Fallback if image not found
            button.setText("💬"); // Unicode chat symbol
        }
        
        button.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8); " +
                       "-fx-background-radius: 5; " +
                       "-fx-padding: 5 10 5 10;");
        
        return button;
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
        
        // Create player cards and arrange in a grid
        int row = 0;
        int col = 0;
        int maxCols = 2; // Two columns of player cards
        
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
            playerGrid.add(playerCard, col, row);
            playerCards.put(playerKey, playerCard);
            
            // Move to next grid position
            col++;
            if (col >= maxCols) {
                col = 0;
                row++;
            }
        }
        
        // Add a skip button to the grid (similar to the screenshot)
        skipVoteButton.setText("SKIP VOTE");
        skipVoteButton.setStyle("-fx-background-color: #333333; " +
                               "-fx-text-fill: white; " +
                               "-fx-font-family: 'Monospace'; " +
                               "-fx-font-weight: bold; " +
                               "-fx-font-size: 16px; " +
                               "-fx-padding: 10 20 10 20; " +
                               "-fx-border-color: #555555; " +
                               "-fx-border-width: 2px; " +
                               "-fx-background-radius: 5; " +
                               "-fx-border-radius: 5;");
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
            
            // Update timer with format matching screenshot (e.g., "Voting Ends In: 40s")
            timerText.setText("Voting Ends In: " + votingTimeSeconds + "s");
            
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
     * Toggles the chat panel visibility
     */
    private void toggleChatPanel() {
        isChatVisible = !isChatVisible;
        chatPanel.setVisible(isChatVisible);
        chatPanel.setManaged(isChatVisible);
        
        if (isChatVisible) {
            // Focus on the input field when opening chat
            Platform.runLater(() -> messageInput.requestFocus());
        }
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
            resultsBox.setBackground(new Background(
                new BackgroundFill(Color.rgb(0, 0, 0, 0.8), new CornerRadii(10), Insets.EMPTY)));
            
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
                if (ejectedPlayerKey.equals(PlayerLogic.getLocalAddressPort())) {
                    roleText = PlayerLogic.getStatus().equals("imposter") ? 
                        PlayerLogic.getName() + " was an Impostor." : 
                        PlayerLogic.getName() + " was not an Impostor.";
                } else {
                    PlayerInfo player = GameLogic.playerList.get(ejectedPlayerKey);
                    roleText = player.getStatus().equals("imposter") ? 
                        player.getName() + " was an Impostor." : 
                        player.getName() + " was not an Impostor.";
                }
                
                Text imposterText = new Text(roleText);
                imposterText.setFont(Font.font("Monospace", FontWeight.BOLD, 22));
                imposterText.setFill(Color.rgb(255, 100, 100));
                
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
            mainPanel.getChildren().add(resultsBox);
            
            // Animate results appearance
            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), resultsBox);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
            
            // Schedule closing the meeting UI after showing results
            executor.schedule(() -> {
                Platform.runLater(() -> closeMeetingUI());
            }, 7, TimeUnit.SECONDS);
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
     * Closes the meeting UI
     */
    private void closeMeetingUI() {
        // Fade out animation
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), this);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> {
            // Remove from parent
            if (getParent() != null) {
                ((Pane) getParent()).getChildren().remove(this);
            }
            
            // Clear reference in GameWindow
            gameWindow.clearActiveMeetingUI();
            
            // Shutdown executor
            executor.shutdown();
        });
        fadeOut.play();
    }
    
    /**
     * Inner class representing a player card for voting
     */
    private class PlayerVoteCard extends StackPane {
        private static final int X_MARK_SIZE = 60;
        private static final int VOTE_ICON_SIZE = 20;
        
        private final String playerKey;
        private final String playerName;
        private final int characterId;
        private final StackPane cardPane;
        private final HBox voteIconsBox;
        private boolean isSelected = false;
        private boolean isDead = false;
        private Node deadMark;
        private Rectangle cardBg;
        private ImageView characterImage;
        private Text nameText;
        
        public PlayerVoteCard(String name, int charId, String key) {
            this.playerName = name;
            this.characterId = charId;
            this.playerKey = key;
            
            // Configure card to match screenshot style
            setPrefSize(320, 100); // Larger size to match screenshot
            setPadding(new Insets(5));
            
            // Create card background - white box with border
            cardBg = new Rectangle(320, 100);
            cardBg.setArcWidth(0); // Square corners like screenshot
            cardBg.setArcHeight(0);
            cardBg.setFill(Color.WHITE);
            cardBg.setStroke(Color.LIGHTGRAY);
            cardBg.setStrokeWidth(1);
            
            // Character image - pixel art sprite as in screenshot
            characterImage = createCharacterImage(charId);
            
            // Player name - colored text with their character color
            nameText = new Text(name);
            nameText.setFont(Font.font("Monospace", FontWeight.BOLD, 20));
            
            // Set color based on character id
            Color nameColor = getColorForCharacter(charId);
            nameText.setFill(nameColor);
            
            // Container for vote icons
            voteIconsBox = new HBox(5);
            voteIconsBox.setAlignment(Pos.CENTER_LEFT);
            
            // Layout for card content - image on left, name centered
            HBox contentLayout = new HBox(15); // More spacing
            contentLayout.setAlignment(Pos.CENTER_LEFT);
            contentLayout.setPadding(new Insets(5, 10, 5, 10));
            contentLayout.getChildren().addAll(characterImage, nameText);
            
            // Card layout
            cardPane = new StackPane();
            cardPane.getChildren().addAll(cardBg, contentLayout);
            
            // Add vote icons area 
            VBox cardContent = new VBox(5);
            cardContent.getChildren().addAll(contentLayout, voteIconsBox);
            
            // Add to main pane
            getChildren().add(cardContent);
            
            // X mark for dead players (red X as in screenshot)
            this.deadMark = createDeadPlayerMark();
            getChildren().add(deadMark);
            
            // Set up click handler
            setOnMouseClicked(e -> {
                if (!isDead) {
                    handlePlayerSelected(playerKey);
                    if (isSelected) {
                        confirmVote(playerKey);
                    }
                }
            });
            
            // Hover effect - highlight when hoverable
            setOnMouseEntered(e -> {
                if (!isDead && !hasVoted) {
                    cardBg.setFill(Color.rgb(220, 240, 255));
                    cardBg.setStroke(Color.rgb(100, 150, 255));
                }
            });
            
            setOnMouseExited(e -> {
                if (!isSelected && !isDead) {
                    cardBg.setFill(Color.WHITE);
                    cardBg.setStroke(Color.LIGHTGRAY);
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
                imageView.setFitWidth(80); // Larger image size to match screenshot
                imageView.setFitHeight(80);
                imageView.setPreserveRatio(true);
                return imageView;
            } catch (Exception e) {
                // Fallback for missing image
                Circle circle = new Circle(40); // Larger size
                circle.setFill(getColorForCharacter(charId));
                circle.setStroke(Color.WHITE);
                circle.setStrokeWidth(2);
                
                ImageView imageView = new ImageView();
                imageView.setFitWidth(80);
                imageView.setFitHeight(80);
                return imageView;
            }
        }
        
        /**
         * Creates an X mark overlay for dead players
         */
        private Node createDeadPlayerMark() {
            // Create red X mark (using the large red X from screenshot)
            Group xMark = new Group();
            
            // Create X shape with two crossed lines
            Line line1 = new Line(0, 0, X_MARK_SIZE, X_MARK_SIZE);
            Line line2 = new Line(0, X_MARK_SIZE, X_MARK_SIZE, 0);
            
            // Style the lines - thicker, bright red
            line1.setStroke(Color.RED);
            line1.setStrokeWidth(8); // Thicker line
            line2.setStroke(Color.RED);
            line2.setStrokeWidth(8);
            
            xMark.getChildren().addAll(line1, line2);
            xMark.setVisible(false); // Initially hidden
            
            return xMark;
        }
        
        /**
         * Creates a vote icon for the specified character
         */
        private Node createVoteIcon(int voterCharId) {
            Circle voteCircle = new Circle(VOTE_ICON_SIZE / 2);
            voteCircle.setFill(getColorForCharacter(voterCharId));
            voteCircle.setStroke(Color.WHITE);
            voteCircle.setStrokeWidth(1);
            
            return voteCircle;
        }
        
        /**
         * Adds a vote from the specified character
         */
        public void addVote(int voterCharId) {
            Node voteIcon = createVoteIcon(voterCharId);
            voteIconsBox.getChildren().add(voteIcon);
        }
        
        /**
         * Sets whether this card is selected
         */
        public void setSelected(boolean selected) {
            this.isSelected = selected;
            
            // Update visual appearance
            if (selected) {
                cardBg.setFill(Color.rgb(227, 242, 253)); // Light blue highlight
                cardBg.setStroke(Color.rgb(33, 150, 243)); // Blue border
                cardBg.setStrokeWidth(3); // Thicker border when selected
            } else {
                if (isDead) {
                    cardBg.setFill(Color.rgb(200, 200, 200)); // Grey for dead
                    cardBg.setStroke(Color.rgb(150, 150, 150));
                } else {
                    cardBg.setFill(Color.WHITE);
                    cardBg.setStroke(Color.LIGHTGRAY);
                    cardBg.setStrokeWidth(1);
                }
            }
        }
        
        /**
         * Marks this player as dead
         */
        public void markAsDead() {
            this.isDead = true;
            
            // Show the red X mark
            deadMark.setVisible(true);
            
            // Grey out the card background (match screenshot)
            cardBg.setFill(Color.rgb(200, 200, 200)); // Grey background
            cardBg.setStroke(Color.rgb(150, 150, 150)); // Darker grey border
            
            // Grey out the name text
            nameText.setFill(Color.rgb(100, 100, 100)); // Dark grey text
            
            // Reduce opacity of the player image
            characterImage.setOpacity(0.7);
            
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
            // Map character IDs to colors
            switch (charId % 10) {
                case 0: return Color.rgb(0, 180, 0);      // Green
                case 1: return Color.rgb(30, 144, 255);   // Blue
                case 2: return Color.rgb(128, 0, 128);    // Purple
                case 3: return Color.rgb(255, 140, 0);    // Orange
                case 4: return Color.rgb(220, 20, 60);    // Red
                case 5: return Color.rgb(255, 105, 180);  // Pink
                case 6: return Color.rgb(0, 191, 255);    // Cyan
                case 7: return Color.rgb(255, 215, 0);    // Yellow
                case 8: return Color.rgb(139, 69, 19);    // Brown
                case 9: return Color.rgb(128, 128, 128);  // Gray
                default: return Color.WHITE;
            }
        }
    }
}
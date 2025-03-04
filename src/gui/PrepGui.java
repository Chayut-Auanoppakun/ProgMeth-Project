package gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import logic.ServerLogic;
import logic.ClientLogic;
import logic.PlayerLogic;
import logic.State;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class PrepGui extends VBox {
    
    private Label codeLabel;
    private Label serverInfoLabel;
    private Label playerCountLabel;
    private Button readyButton;
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
        codeLabel = new Label("IP");
        codeLabel.setTextFill(Color.BLACK);
        codeLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        
        // Server info (IP:Port)
        serverInfoLabel = new Label("Loading...");
        serverInfoLabel.setTextFill(Color.BLACK);
    	serverInfoLabel.setFont(Font.font("Monospace", FontWeight.LIGHT, 10));
        
        // Player count container (right side)
        HBox playerCountContainer = new HBox();
        playerCountContainer.setAlignment(Pos.CENTER);
        playerCountContainer.setSpacing(5);
        
        // Player icon
        Label playerIcon = new Label("\uD83D\uDC64"); // Person icon
        playerIcon.setTextFill(Color.BLACK);
        playerIcon.setFont(Font.font("Arial", 16));
        
        // Player count with bright green color
        playerCountLabel = new Label("2...");
        playerCountLabel.setTextFill(Color.rgb(0, 100, 0));
        playerCountLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        
        playerCountContainer.getChildren().addAll(playerIcon, playerCountLabel);
        
        // Ready button (now in the middle) with a pixelated game style
        readyButton = new Button("READY");
        readyButton.setStyle(
            "-fx-background-color: #333333; " +
            "-fx-text-fill: white; " +
            "-fx-border-color: #555555; " +
            "-fx-border-width: 1; " +
            "-fx-font-family: 'Monospace'; " +
            "-fx-font-weight: bold; " +
            "-fx-border-radius: 15; " +
            "-fx-background-radius: 15;"
        );
        readyButton.setPrefWidth(100);
        readyButton.setFont(Font.font("Monospace", FontWeight.BOLD, 12));
        readyButton.setOnAction(e -> toggleReady());
        
        // IP section (now on the left)
        VBox ipSection = new VBox(2);
        ipSection.setAlignment(Pos.CENTER_LEFT);
        ipSection.getChildren().addAll(codeLabel, serverInfoLabel);
        
        // Main container with less spacing to match screenshot
        HBox mainContainer = new HBox(25);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setPadding(new Insets(5, 20, 5, 20));
        mainContainer.getChildren().addAll(ipSection, readyButton, playerCountContainer);
        
        // Add a light blue backdrop with rounded edges
        StackPane backdrop = new StackPane();
        Rectangle backdropRect = new Rectangle();
        backdropRect.setWidth(400);  // Make it longer
        backdropRect.setHeight(70);
        backdropRect.setArcWidth(35); // More circular edges
        backdropRect.setArcHeight(35); // More circular edges
        
        backdropRect.setFill(Color.rgb(173, 216, 230, 0.85)); // Light blue, semi-transparent
        backdropRect.setStroke(Color.rgb(135, 206, 235, 0.9)); // Slightly darker blue for border
        backdropRect.setStrokeWidth(5);
        
        backdrop.getChildren().addAll(backdropRect, mainContainer);
        
        this.getChildren().add(backdrop);
        
        // Load server info based on state
        loadServerInfo(state);
        
        // Set size to accommodate the wider panel
        this.setMaxWidth(600);
        this.setMaxHeight(80);
    }
    
    private void loadServerInfo(State state) {
        // Get server info based on current state
        try {
            if (state == State.SERVER) {
                // Get local server info for hosts
                String serverAddress = InetAddress.getLocalHost().getHostAddress();
                int serverPort = ServerLogic.getServerSocket().getLocalPort();
                
                // Format like "192.168.1.39:1..."
                String displayAddress = serverAddress + ":" + serverPort;
                if (displayAddress.length() > 16) {
                    // Truncate with "..." if too long
                    displayAddress = displayAddress.substring(0, 13) + "...";
                }
                
                serverInfoLabel.setText(displayAddress);
            } else if (state == State.CLIENT) {
                // For clients, retrieve connected server info
                String serverInfo = ClientLogic.getConnectedServerInfo();
                if (serverInfo != null) {
                    // Format and truncate if needed
                    if (serverInfo.length() > 16) {
                        serverInfo = serverInfo.substring(0, 13) + "...";
                    }
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
    
    private void toggleReady() {
        isReady = !isReady;
        if (isReady) {
            readyButton.setText("UNREADY");
            readyButton.setStyle(
                "-fx-background-color: #006400; " + // Dark green background
                "-fx-text-fill: white; " +
                "-fx-border-color: #008800; " +
                "-fx-border-width: 1; " +
                "-fx-font-family: 'Monospace'; " +
                "-fx-font-weight: bold; " +
                "-fx-border-radius: 15; " +
                "-fx-background-radius: 15;"
            );
        } else {
            readyButton.setText("READY");
            readyButton.setStyle(
                "-fx-background-color: #333333; " +
                "-fx-text-fill: white; " +
                "-fx-border-color: #555555; " +
                "-fx-border-width: 1; " +
                "-fx-font-family: 'Monospace'; " +
                "-fx-font-weight: bold; " +
                "-fx-border-radius: 15; " +
                "-fx-background-radius: 15;"
            );
        }
        
        // Notify game logic that the player is ready/unready
        try {
            PlayerLogic.setPlayerReady(isReady);
            
            // If using client/server model
//            if (MainMenuPane.getState() == State.CLIENT) {
//                ClientLogic.setPlayerReady(isReady);
//            }
        } catch (Exception e) {
            System.err.println("Error setting ready status: " + e.getMessage());
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
}
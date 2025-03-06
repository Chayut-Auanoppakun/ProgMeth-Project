package gui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;
import logic.GameLogic;
import logic.PlayerLogic;
import logic.SoundLogic;
import logic.TaskLogic;
import server.PlayerInfo;

/**
 * Manages the in-game UI overlay that shows task progress and action buttons.
 * This class handles creating and updating the UI elements that appear during gameplay.
 */
public class GameOverlayManager {
    // UI Components
    private Pane overlayPane;
    private Rectangle taskProgressBar; // Now using Rectangle instead of ProgressBar
    private double progressBarMaxWidth;
    private Text taskProgressText;
    private Button actionButton;
    private Button reportButton;
    private VBox taskContainer;
    private HBox buttonContainer;
    
    // State tracking
    private boolean isVisible = false;
    private boolean isInitialized = false;
    private long lastActionTime = 0;
    private long lastReportTime = 0;
    
    // Reference to game window
    private GameWindow gameWindow;
    
    /**
     * Creates a new GameOverlayManager for handling the in-game UI overlay.
     * 
     * @param gameWindow Reference to the main GameWindow instance
     */
    public GameOverlayManager(GameWindow gameWindow) {
        this.gameWindow = gameWindow;
    }
    
    /**
     * Initializes the overlay UI components and adds them to the root node.
     * 
     * @param root The root node to add overlay elements to
     * @param screenWidth The width of the game screen
     * @param screenHeight The height of the game screen
     */
    public void initialize(Group root, double screenWidth, double screenHeight) {
        if (isInitialized) return;
        
        // Create the overlay pane
        overlayPane = new Pane();
        overlayPane.setPrefSize(screenWidth, screenHeight);
        overlayPane.setMouseTransparent(true); // Let mouse events pass through
        overlayPane.setOpacity(0);
        overlayPane.setVisible(false);
        
        // Create task progress elements
        createTaskProgressUI(screenWidth);
        
        // Create action buttons
        createActionButtonsUI(screenWidth, screenHeight);
        
        // Add all elements to the overlay
        overlayPane.getChildren().addAll(taskContainer, buttonContainer);
        
        // Add overlay to root
        root.getChildren().add(overlayPane);
        
        isInitialized = true;
    }
    
    /**
     * Creates the task progress UI elements (top-left corner)
     */
    private void createTaskProgressUI(double screenWidth) {
        // Task progress container
        taskContainer = new VBox(5);
        taskContainer.setAlignment(Pos.CENTER_LEFT);
        taskContainer.setPadding(new Insets(20, 15, 10, 15));
        taskContainer.setMaxWidth(screenWidth * 0.3);
        
        // Create stylized backing panel
        Rectangle progressBg = new Rectangle(screenWidth * 0.3, 68);
        progressBg.setArcWidth(12);
        progressBg.setArcHeight(12);
        progressBg.setFill(Color.rgb(30, 30, 50, 0.85));
        progressBg.setStroke(Color.rgb(70, 130, 180, 0.8));
        progressBg.setStrokeWidth(2);
        
        // Add drop shadow for depth
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(5.0);
        dropShadow.setOffsetX(3.0);
        dropShadow.setOffsetY(3.0);
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.5));
        progressBg.setEffect(dropShadow);
        
        // Progress title
        Text title = new Text("Task Progress");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        title.setFill(Color.WHITE);
        
        // Custom progress bar using shapes instead of JavaFX ProgressBar
        // This avoids the styling issues with ProgressBar
        
        // Container for our custom progress bar
        StackPane customProgressBar = new StackPane();
        customProgressBar.setAlignment(Pos.CENTER_LEFT);
        customProgressBar.setPrefWidth(screenWidth * 0.25);
        customProgressBar.setPrefHeight(16);
        
        // Background rectangle (empty progress)
        Rectangle progressBgRect = new Rectangle(screenWidth * 0.25, 16);
        progressBgRect.setFill(Color.rgb(42, 42, 58)); // Dark background
        progressBgRect.setStroke(Color.rgb(135, 206, 250)); // Light blue border (#87cefa)
        progressBgRect.setStrokeWidth(1);
        
        // Foreground rectangle (filled progress)
        Rectangle progressFillRect = new Rectangle(0, 14); // Start with 0 width
        progressFillRect.setFill(Color.rgb(33, 150, 243)); // Blue fill (#2196F3)
        progressFillRect.setTranslateX(-((screenWidth * 0.25) / 2) + 1); // Align to left with 1px offset for border
        
        // Add both rectangles to the stack with background on bottom
        customProgressBar.getChildren().addAll(progressBgRect, progressFillRect);
        
        // Store reference to the fill rectangle to update later
        taskProgressBar = progressFillRect;
        
        // Progress text
        taskProgressText = new Text("0%");
        taskProgressText.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        taskProgressText.setFill(Color.LIGHTBLUE);
        
        // Save the max width for calculations
        progressBarMaxWidth = screenWidth * 0.25;
        
        // Create layout for progress elements
        HBox progressRow = new HBox(15);
        progressRow.setAlignment(Pos.CENTER_LEFT);
        progressRow.getChildren().addAll(customProgressBar, taskProgressText);
        
        // Stack everything in the container
        StackPane progressPanel = new StackPane();
        
        VBox content = new VBox(8);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(title, progressRow);
        
        progressPanel.getChildren().addAll(progressBg, content);
        
        taskContainer.getChildren().add(progressPanel);
        taskContainer.setLayoutX(20);
        taskContainer.setLayoutY(20);
    }
    
    /**
     * Creates the action buttons UI (bottom-right corner)
     */
    private void createActionButtonsUI(double screenWidth, double screenHeight) {
        buttonContainer = new HBox(10);
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setPadding(new Insets(15));
        
        // Button styles
        String baseButtonStyle = 
            "-fx-background-color: #1e90ff;" +
            "-fx-text-fill: white;" +
            "-fx-font-family: 'Monospace';" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 8 20 8 20;" +
            "-fx-border-color: #87cefa;" +
            "-fx-border-width: 2px;" +
            "-fx-background-radius: 0;" +
            "-fx-border-radius: 0;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);";
            
        String hoverButtonStyle = 
            "-fx-background-color: #00bfff;" +
            "-fx-text-fill: white;" +
            "-fx-font-family: 'Monospace';" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 8 20 8 20;" +
            "-fx-border-color: #b0e2ff;" +
            "-fx-border-width: 2px;" +
            "-fx-background-radius: 0;" +
            "-fx-border-radius: 0;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 8, 0, 0, 1);";
        
        // Create action button
        actionButton = new Button("INTERACT");
        actionButton.setPrefWidth(150);
        actionButton.setStyle(baseButtonStyle);
        
        // Add hover effects
        actionButton.setOnMouseEntered(e -> {
            if (!"dead".equals(PlayerLogic.getStatus())) {
                actionButton.setStyle(hoverButtonStyle);
            }
        });
        
        actionButton.setOnMouseExited(e -> {
            if (!"dead".equals(PlayerLogic.getStatus())) {
                actionButton.setStyle(baseButtonStyle);
            }
        });
        
        // Set action button handler
        actionButton.setOnAction(e -> {
            if (System.currentTimeMillis() - lastActionTime < 250) return; // Debounce
            lastActionTime = System.currentTimeMillis();
            
            if ("imposter".equals(PlayerLogic.getStatus()) && !"dead".equals(PlayerLogic.getStatus())) {
                // For imposters: try to kill
                PlayerInfo target = gameWindow.findClosestKillablePlayer();
                if (target != null) {
                    gameWindow.killPlayer(target);
                } else {
                    // Flash button if no target
                    flashButton(actionButton, "-fx-background-color: #550000;");
                }
            } else {
                // For crewmates: interact with tasks
                gameWindow.interactWithNearbyTask();
            }
        });
        
        // Create report button
        reportButton = new Button("REPORT");
        reportButton.setPrefWidth(150);
        reportButton.setStyle(baseButtonStyle);
        
        // Add hover effects
        reportButton.setOnMouseEntered(e -> {
            if (!"dead".equals(PlayerLogic.getStatus())) {
                reportButton.setStyle(hoverButtonStyle);
            }
        });
        
        reportButton.setOnMouseExited(e -> {
            if (!"dead".equals(PlayerLogic.getStatus())) {
                reportButton.setStyle(baseButtonStyle);
            }
        });
        
        // Set report button handler
        reportButton.setOnAction(e -> {
            if (System.currentTimeMillis() - lastReportTime < 250) return; // Debounce
            lastReportTime = System.currentTimeMillis();
            
            if (!"dead".equals(PlayerLogic.getStatus())) {
                gameWindow.reportNearbyCorpse();
            }
        });
        
        // Add buttons to container
        buttonContainer.getChildren().addAll(actionButton, reportButton);
        
        // Position the container
        buttonContainer.setLayoutX(screenWidth - 350);
        buttonContainer.setLayoutY(screenHeight - 100);
    }
    
    /**
     * Shows the overlay with a fade-in animation
     */
    public void show() {
        if (!isInitialized || isVisible) return;
        
        overlayPane.setVisible(true);
        
        // Fade in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), overlayPane);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
        
        isVisible = true;
    }
    
    /**
     * Hides the overlay with a fade-out animation
     */
    public void hide() {
        if (!isInitialized || !isVisible) return;
        
        // Fade out animation
        FadeTransition fadeOut = new FadeTransition(Duration.millis(250), overlayPane);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> overlayPane.setVisible(false));
        fadeOut.play();
        
        isVisible = false;
    }
    
    /**
     * Updates the task progress display
     */
    public void updateTaskProgress() {
        if (!isInitialized || !isVisible) return;
        
        // Get current task completion percentage
        double progressValue = TaskLogic.getTaskCompletionPercentage() / 100.0;
        String progressText = String.format("%.0f%%", progressValue * 100);
        
        // Update UI on JavaFX thread
        Platform.runLater(() -> {
            // Update progress text
            taskProgressText.setText(progressText);
            
            // Update the custom progress bar width
            double newWidth = progressBarMaxWidth * progressValue;
            taskProgressBar.setWidth(newWidth);
            
            // Update progress color based on completion
            if (progressValue > 0.8) {
                // Green for high completion
                taskProgressBar.setFill(Color.rgb(76, 175, 80)); // #4CAF50
                taskProgressText.setFill(Color.rgb(76, 175, 80));
            } else if (progressValue > 0.5) {
                // Yellow for medium completion
                taskProgressBar.setFill(Color.rgb(255, 193, 7)); // #FFC107
                taskProgressText.setFill(Color.rgb(255, 193, 7));
            } else {
                // Blue for low completion
                taskProgressBar.setFill(Color.rgb(33, 150, 243)); // #2196F3
                taskProgressText.setFill(Color.rgb(33, 150, 243));
            }
        });
    }
    
    /**
     * Updates the buttons based on the player's current role
     */
    public void updatePlayerRoleUI() {
        if (!isInitialized) return;
        
        Platform.runLater(() -> {
            boolean isImposter = "imposter".equals(PlayerLogic.getStatus());
            boolean isDead = "dead".equals(PlayerLogic.getStatus());
            
            // Style definitions
            String baseButtonStyle = 
                "-fx-background-color: #1e90ff;" +
                "-fx-text-fill: white;" +
                "-fx-font-family: 'Monospace';" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8 20 8 20;" +
                "-fx-border-color: #87cefa;" +
                "-fx-border-width: 2px;" +
                "-fx-background-radius: 0;" +
                "-fx-border-radius: 0;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);";
                
            String imposterButtonStyle = 
                "-fx-background-color: #d32f2f;" +
                "-fx-text-fill: white;" +
                "-fx-font-family: 'Monospace';" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8 20 8 20;" +
                "-fx-border-color: #ff6347;" +
                "-fx-border-width: 2px;" +
                "-fx-background-radius: 0;" +
                "-fx-border-radius: 0;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);";
                
            String ghostButtonStyle = 
                "-fx-background-color: #424242;" +
                "-fx-text-fill: #aaaaaa;" +
                "-fx-font-family: 'Monospace';" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8 20 8 20;" +
                "-fx-border-color: #666666;" +
                "-fx-border-width: 2px;" +
                "-fx-background-radius: 0;" +
                "-fx-border-radius: 0;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 1);";
                
            // Update action button
            if (isDead) {
                // Ghost appearance
                actionButton.setText("INTERACT");
                actionButton.setStyle(ghostButtonStyle);
                reportButton.setStyle(ghostButtonStyle);
            } else if (isImposter) {
                // Imposter appearance
                actionButton.setText("KILL");
                actionButton.setStyle(imposterButtonStyle);
                reportButton.setStyle(baseButtonStyle);
            } else {
                // Crewmate appearance
                actionButton.setText("INTERACT");
                actionButton.setStyle(baseButtonStyle);
                reportButton.setStyle(baseButtonStyle);
            }
            
            // Update hover handlers based on new state
            updateButtonHoverHandlers(isImposter, isDead);
        });
    }
    
    /**
     * Updates the hover handlers for buttons based on player role
     */
    private void updateButtonHoverHandlers(boolean isImposter, boolean isDead) {
        // Hover styles
        String hoverButtonStyle = 
            "-fx-background-color: #00bfff;" +
            "-fx-text-fill: white;" +
            "-fx-font-family: 'Monospace';" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 8 20 8 20;" +
            "-fx-border-color: #b0e2ff;" +
            "-fx-border-width: 2px;" +
            "-fx-background-radius: 0;" +
            "-fx-border-radius: 0;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 8, 0, 0, 1);";
        
        String baseButtonStyle = 
            "-fx-background-color: #1e90ff;" +
            "-fx-text-fill: white;" +
            "-fx-font-family: 'Monospace';" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 8 20 8 20;" +
            "-fx-border-color: #87cefa;" +
            "-fx-border-width: 2px;" +
            "-fx-background-radius: 0;" +
            "-fx-border-radius: 0;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);";
            
        String imposterButtonStyle = 
            "-fx-background-color: #d32f2f;" +
            "-fx-text-fill: white;" +
            "-fx-font-family: 'Monospace';" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 8 20 8 20;" +
            "-fx-border-color: #ff6347;" +
            "-fx-border-width: 2px;" +
            "-fx-background-radius: 0;" +
            "-fx-border-radius: 0;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);";
            
        String imposterHoverStyle = 
            "-fx-background-color: #ff1744;" +
            "-fx-text-fill: white;" +
            "-fx-font-family: 'Monospace';" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 8 20 8 20;" +
            "-fx-border-color: #ff8a80;" +
            "-fx-border-width: 2px;" +
            "-fx-background-radius: 0;" +
            "-fx-border-radius: 0;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 8, 0, 0, 1);";
            
        String ghostButtonStyle = 
            "-fx-background-color: #424242;" +
            "-fx-text-fill: #aaaaaa;" +
            "-fx-font-family: 'Monospace';" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 8 20 8 20;" +
            "-fx-border-color: #666666;" +
            "-fx-border-width: 2px;" +
            "-fx-background-radius: 0;" +
            "-fx-border-radius: 0;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 1);";
            
        String ghostHoverStyle = 
            "-fx-background-color: #555555;" +
            "-fx-text-fill: #bbbbbb;" +
            "-fx-font-family: 'Monospace';" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 8 20 8 20;" +
            "-fx-border-color: #777777;" +
            "-fx-border-width: 2px;" +
            "-fx-background-radius: 0;" +
            "-fx-border-radius: 0;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 8, 0, 0, 1);";
        
        // Clear previous handlers
        actionButton.setOnMouseEntered(null);
        actionButton.setOnMouseExited(null);
        reportButton.setOnMouseEntered(null);
        reportButton.setOnMouseExited(null);
        
        // Set appropriate hover styles
        if (isDead) {
            // Ghost hover styles
            actionButton.setOnMouseEntered(e -> actionButton.setStyle(ghostHoverStyle));
            actionButton.setOnMouseExited(e -> actionButton.setStyle(ghostButtonStyle));
            
            reportButton.setOnMouseEntered(e -> reportButton.setStyle(ghostHoverStyle));
            reportButton.setOnMouseExited(e -> reportButton.setStyle(ghostButtonStyle));
        } else if (isImposter) {
            // Imposter hover styles for action button
            actionButton.setOnMouseEntered(e -> actionButton.setStyle(imposterHoverStyle));
            actionButton.setOnMouseExited(e -> actionButton.setStyle(imposterButtonStyle));
            
            // Regular hover styles for report button
            reportButton.setOnMouseEntered(e -> reportButton.setStyle(hoverButtonStyle));
            reportButton.setOnMouseExited(e -> reportButton.setStyle(baseButtonStyle));
        } else {
            // Crewmate hover styles
            actionButton.setOnMouseEntered(e -> actionButton.setStyle(hoverButtonStyle));
            actionButton.setOnMouseExited(e -> actionButton.setStyle(baseButtonStyle));
            
            reportButton.setOnMouseEntered(e -> reportButton.setStyle(hoverButtonStyle));
            reportButton.setOnMouseExited(e -> reportButton.setStyle(baseButtonStyle));
        }
    }
    
    /**
     * Creates a visual flash effect on a button
     */
    private void flashButton(Button button, String flashStyle) {
        String originalStyle = button.getStyle();
        button.setStyle(flashStyle);
        
        new Thread(() -> {
            try {
                Thread.sleep(200);
                Platform.runLater(() -> button.setStyle(originalStyle));
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }).start();
    }
}
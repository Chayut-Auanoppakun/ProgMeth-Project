package Task;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import java.util.Random;

public class Task5 extends Task {
    private static final int SCENE_WIDTH = 700;
    private static final int SCENE_HEIGHT = 400;
    private static final int FISH_PATH_Y = 250;  // Y-coordinate for fish path - moved lower
    private static final double PLUS_X = SCENE_WIDTH / 2.0;  // X-coordinate for plus sign
    private static final double MIN_SPEED = 100;  // Minimum fish speed (pixels per second)
    private static final double MAX_SPEED = 300;  // Maximum fish speed (pixels per second)
    private static final double CATCH_RANGE = 25;  // Range within which fish can be caught (pixels)
    private static final int TARGET_SCORE = 5;  // Number of fish to catch to complete task
    
    private ImageView fishImageView;
    private Label fishLabel;  // For ASCII fish fallback
    private boolean usingImageFish = true;  // Whether we're using image or ASCII fish
    private double fishSpeed;
    private boolean fishActive = false;
    private int score = 0;
    private int misses = 0;
    private Label scoreLabel;
    private Label targetLabel;
    private Label gameOverLabel;
    private Random random = new Random();
    private AnimationTimer gameLoop;
    private Pane taskContainer;
    
    public Task5(TaskPane parent, String taskId) {
        super(parent, taskId);
        
        // Pixel-art style background
        setStyle("-fx-background-color: #8b6c42; -fx-border-color: #555; -fx-border-width: 2px;");
        
        // Title panel with darker brown background
        HBox titlePanel = new HBox();
        titlePanel.setStyle("-fx-background-color: #6d4e2a; -fx-padding: 10px;");
        titlePanel.setPrefWidth(960);
        titlePanel.setAlignment(Pos.CENTER);
        
        // Task title with pixelated font
        Text title = new Text("Fishing Game");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
        title.setFill(Color.WHITE);
        titlePanel.getChildren().add(title);
        
        // Content panel with pixel-art styling
        VBox contentPanel = new VBox(20);
        contentPanel.setStyle("-fx-background-color: #a88c60; -fx-padding: 20px; -fx-border-color: #6d4e2a; -fx-border-width: 2px;");
        contentPanel.setAlignment(Pos.CENTER);
        contentPanel.setMaxWidth(700);
        
        // Goal instructions
        targetLabel = new Label("GOAL: Catch " + TARGET_SCORE + " fish to complete the task!");
        targetLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 18));
        targetLabel.setTextFill(Color.WHITE);
        
        // Pixel-art style instructions
        Text instructions = new Text("Press SPACE to start | Press C to catch fish when crossing the plus sign");
        instructions.setFont(Font.font("Monospace", FontWeight.NORMAL, 16));
        instructions.setFill(Color.WHITE);
        
        // Task container with pixel-art styling
        taskContainer = new Pane();
        taskContainer.setPrefSize(SCENE_WIDTH, SCENE_HEIGHT);
        taskContainer.setStyle(
            "-fx-background-color: #64553a; " +
            "-fx-padding: 20px; " +
            "-fx-border-color: #444; " +
            "-fx-border-width: 3px; " +
            "-fx-border-radius: 5px;"
        );

        // Create water and sky backgrounds with distinct colors
        Rectangle sky = new Rectangle(0, 0, SCENE_WIDTH, FISH_PATH_Y);
        sky.setFill(Color.rgb(135, 206, 235)); // Sky blue
        taskContainer.getChildren().add(sky);
        
        Rectangle water = new Rectangle(0, FISH_PATH_Y, SCENE_WIDTH, SCENE_HEIGHT - FISH_PATH_Y);
        water.setFill(Color.rgb(0, 105, 148)); // Deeper blue for water
        taskContainer.getChildren().add(water);
        
        // Create horizontal line representing the water surface
        Line waterSurface = new Line(0, FISH_PATH_Y, SCENE_WIDTH, FISH_PATH_Y);
        waterSurface.setStroke(Color.WHITE);
        waterSurface.setStrokeWidth(2);
        waterSurface.getStrokeDashArray().addAll(5d, 3d); // Dashed line for water effect
        taskContainer.getChildren().add(waterSurface);
        
        // Create the plus sign in the middle - positioned lower to match fish
        int crosshairY = FISH_PATH_Y + 30; // Position crosshair to align with swimming fish
        Line plusVertical = new Line(PLUS_X, crosshairY - 20, PLUS_X, crosshairY + 20);
        plusVertical.setStroke(Color.RED);
        plusVertical.setStrokeWidth(3);
        taskContainer.getChildren().add(plusVertical);
        
        Line plusHorizontal = new Line(PLUS_X - 20, crosshairY, PLUS_X + 20, crosshairY);
        plusHorizontal.setStroke(Color.RED);
        plusHorizontal.setStrokeWidth(3);
        taskContainer.getChildren().add(plusHorizontal);
        
        // Create fish image - positioned below the water line
        fishImageView = new ImageView();
        fishImageView.setY(FISH_PATH_Y + 15);  // Below the water line
        fishImageView.setFitHeight(60);
        fishImageView.setFitWidth(80);
        fishImageView.setPreserveRatio(true);
        fishImageView.setVisible(false);  // Initially invisible
        taskContainer.getChildren().add(fishImageView);
        
        // Create ASCII fish as fallback - below water line
        fishLabel = new Label("><(((°>");
        fishLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 24));
        fishLabel.setTextFill(Color.DARKORANGE);
        fishLabel.setLayoutY(FISH_PATH_Y + 20); // Below the water line
        fishLabel.setVisible(false);  // Initially invisible
        taskContainer.getChildren().add(fishLabel);
        
        // Try to load fish image, use ASCII fish as fallback
        try {
            Image fishImage = new Image(getClass().getResourceAsStream("/TaskAsset/Fishing/fish.png"));
            if (fishImage.isError()) {
                throw new Exception("Fish image could not be loaded");
            }
            fishImageView.setImage(fishImage);
            usingImageFish = true;
        } catch (Exception e) {
            System.out.println("Could not load fish image, using ASCII fish instead: " + e.getMessage());
            fishImageView.setVisible(false);
            usingImageFish = false;
        }
        
        // Create score label with pixel-art styling
        scoreLabel = new Label("Score: 0 | Misses: 0");
        scoreLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        scoreLabel.setTextFill(Color.WHITE);
        scoreLabel.setLayoutX(20);
        scoreLabel.setLayoutY(20);
        taskContainer.getChildren().add(scoreLabel);
        
        // Add visual indicator for catch zone - aligned with crosshair
//        int zoneY = FISH_PATH_Y + 20; // Match the crosshair Y position
//        Line catchZoneLeft = new Line(PLUS_X - CATCH_RANGE, zoneY - 40, PLUS_X - CATCH_RANGE, zoneY + 40);
//        catchZoneLeft.setStroke(Color.LIGHTGREEN);
//        catchZoneLeft.setStrokeWidth(1);
//        catchZoneLeft.getStrokeDashArray().addAll(5d, 5d);
//        taskContainer.getChildren().add(catchZoneLeft);
//        
//        Line catchZoneRight = new Line(PLUS_X + CATCH_RANGE, zoneY - 40, PLUS_X + CATCH_RANGE, zoneY + 40);
//        catchZoneRight.setStroke(Color.LIGHTGREEN);
//        catchZoneRight.setStrokeWidth(1);
//        catchZoneRight.getStrokeDashArray().addAll(5d, 5d);
//        taskContainer.getChildren().add(catchZoneRight);
        
        // Create game over label (initially invisible)
        gameOverLabel = new Label("GAME OVER\nPress SPACE to play again");
        gameOverLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 24));
        gameOverLabel.setTextFill(Color.RED);
        
        StackPane gameOverPane = new StackPane(gameOverLabel);
        gameOverPane.setLayoutX(0);
        gameOverPane.setLayoutY(0);
        gameOverPane.setPrefWidth(SCENE_WIDTH);
        gameOverPane.setPrefHeight(SCENE_HEIGHT);
        gameOverPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);"); // Semi-transparent overlay
        gameOverPane.setAlignment(Pos.CENTER);
        gameOverPane.setVisible(false);
        taskContainer.getChildren().add(gameOverPane);

        // Game animation timer
        gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;
            
            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }
                
                double elapsedSeconds = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;
                
                if (fishActive) {
                    // Move fish based on its speed
                    double newX;
                    if (usingImageFish) {
                        newX = fishImageView.getX() + fishSpeed * elapsedSeconds;
                        fishImageView.setX(newX);
                        
                        // Check if fish has moved off screen
                        if (newX > SCENE_WIDTH) {
                            fishActive = false;
                            misses++;
                            updateScoreLabel();
                            checkGameOver();
                            spawnNewFish();
                        }
                    } else {
                        // Use ASCII fish
                        newX = fishLabel.getLayoutX() + fishSpeed * elapsedSeconds;
                        fishLabel.setLayoutX(newX);
                        
                        if (newX > SCENE_WIDTH) {
                            fishActive = false;
                            misses++;
                            updateScoreLabel();
                            checkGameOver();
                            spawnNewFish();
                        }
                    }
                }
            }
        };

        // Enable key handling on the task container
        taskContainer.setFocusTraversable(true);
        taskContainer.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.SPACE) {
                if (!fishActive && misses < 3) {
                    gameOverPane.setVisible(false);
                    spawnNewFish();
                } else if (misses >= 3) {
                    // Reset game
                    score = 0;
                    misses = 0;
                    updateScoreLabel();
                    gameOverPane.setVisible(false);
                    spawnNewFish();
                }
            } else if (event.getCode() == KeyCode.C && fishActive) {
                // Check if fish is near the plus sign (allow some margin for catching)
                double fishCenter;
                if (usingImageFish) {
                    fishCenter = fishImageView.getX() + fishImageView.getFitWidth() / 2;
                } else {
                    fishCenter = fishLabel.getLayoutX() + 40; // Approximate center of ASCII fish
                }
                
                double distance = Math.abs(fishCenter - PLUS_X);
                
                if (distance < CATCH_RANGE) {  // Use the defined catch range constant
                    score++;
                    fishActive = false;
                    updateScoreLabel();
                    
                    // Check for task completion
                    if (score >= TARGET_SCORE) {
                        // Game completed successfully
                        gameLoop.stop();
                        completeTask();
                    } else {
                        spawnNewFish();
                    }
                } else {
                    misses++;
                    updateScoreLabel();
                    checkGameOver();
                }
            }
        });

        // Start the game loop when the task is created
        gameLoop.start();
        
        // Assemble the main container
        VBox mainContainer = new VBox(0);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.getChildren().add(titlePanel);
        
        VBox.setMargin(contentPanel, new Insets(50, 0, 0, 0));
        contentPanel.getChildren().addAll(targetLabel, instructions, taskContainer);
        mainContainer.getChildren().add(contentPanel);
        
        getChildren().addAll(mainContainer, getCloseButton());
        
        // Ensure task container gets focus for keyboard input
        taskContainer.requestFocus();
    }
    
    private void spawnNewFish() {
        if (misses >= 5) {
            return;  // Don't spawn new fish if game is over
        }
        
        // Set random speed
        fishSpeed = MIN_SPEED + random.nextDouble() * (MAX_SPEED - MIN_SPEED);
        
        // Reset fish position to start from left
        if (usingImageFish) {
            fishImageView.setX(-fishImageView.getFitWidth());
            fishImageView.setVisible(true);
            fishLabel.setVisible(false);
        } else {
            fishLabel.setLayoutX(-80);  // Start off-screen
            fishLabel.setVisible(true);
            fishImageView.setVisible(false);
        }
        
        fishActive = true;
    }
    
    private void updateScoreLabel() {
        scoreLabel.setText("Score: " + score + "/" + TARGET_SCORE + " | Misses: " + misses + "/3");
    }
    
    private void checkGameOver() {
        if (misses >= 3) {
            fishActive = false;
            gameOverLabel.getParent().setVisible(true);
        }
    }

    @Override
    protected void initializeTask() {
        // Initialization happens in constructor
    }

    @Override
    public boolean isCompleted() {
        return score >= TARGET_SCORE;
    }

}
package test;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.util.Random;

public class FishingGame extends Application {

    private static final int SCENE_WIDTH = 800;
    private static final int SCENE_HEIGHT = 400;
    private static final int FISH_PATH_Y = 200;  // Y-coordinate for fish path
    private static final double PLUS_X = SCENE_WIDTH / 2.0;  // X-coordinate for plus sign
    private static final double MIN_SPEED = 100;  // Minimum fish speed (pixels per second)
    private static final double MAX_SPEED = 300;  // Maximum fish speed (pixels per second)
    private static final double CATCH_RANGE = 50;  // Range within which fish can be caught (pixels)
    
    private ImageView fishImageView;
    private Label fishLabel;  // For ASCII fish fallback
    private boolean usingImageFish = true;  // Whether we're using image or ASCII fish
    private double fishSpeed;
    private boolean fishActive = false;
    private int score = 0;
    private int misses = 0;
    private Label scoreLabel;
    private Label gameOverLabel;
    private Random random = new Random();
    
    @Override
    public void start(Stage primaryStage) {
        // Create the root pane
        Pane root = new Pane();
        root.setStyle("-fx-background-color: lightblue;");
        
        // Create horizontal line representing the fish path
        Line fishPath = new Line(0, FISH_PATH_Y, SCENE_WIDTH, FISH_PATH_Y);
        fishPath.setStroke(Color.DARKBLUE);
        fishPath.setStrokeWidth(2);
        root.getChildren().add(fishPath);
        
        // Create the plus sign in the middle
        Line plusVertical = new Line(PLUS_X, FISH_PATH_Y - 20, PLUS_X, FISH_PATH_Y + 20);
        plusVertical.setStroke(Color.RED);
        plusVertical.setStrokeWidth(3);
        root.getChildren().add(plusVertical);
        
        Line plusHorizontal = new Line(PLUS_X - 20, FISH_PATH_Y, PLUS_X + 20, FISH_PATH_Y);
        plusHorizontal.setStroke(Color.RED);
        plusHorizontal.setStrokeWidth(3);
        root.getChildren().add(plusHorizontal);
        
        // Create fish image
        fishImageView = new ImageView();
        fishImageView.setY(FISH_PATH_Y - 30);  // Center the fish on the path
        fishImageView.setFitHeight(60);
        fishImageView.setFitWidth(80);
        fishImageView.setPreserveRatio(true);
        fishImageView.setVisible(false);  // Initially invisible
        root.getChildren().add(fishImageView);
        
        // Create ASCII fish as fallback
        fishLabel = new Label("><(((°>");
        fishLabel.setFont(Font.font("Monospace", FontWeight.BOLD, 24));
        fishLabel.setTextFill(Color.DARKORANGE);
        fishLabel.setLayoutY(FISH_PATH_Y - 12);
        fishLabel.setVisible(false);  // Initially invisible
        root.getChildren().add(fishLabel);
        
        // Try to load fish image, use ASCII fish as fallback
        try {
            Image fishImage = new Image(getClass().getResourceAsStream("fish.png"));
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
        
        // Create score label
        scoreLabel = new Label("Score: 0 | Misses: 0");
        scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        scoreLabel.setLayoutX(20);
        scoreLabel.setLayoutY(20);
        root.getChildren().add(scoreLabel);
        
        // Create instructions label
        Label instructionsLabel = new Label("Press SPACE to start | Press C to catch when fish crosses the plus sign");
        instructionsLabel.setFont(Font.font("Arial", 16));
        instructionsLabel.setLayoutX(150);
        instructionsLabel.setLayoutY(SCENE_HEIGHT - 50);
        root.getChildren().add(instructionsLabel);
        
        // Add visual indicator for catch zone
        Line catchZoneLeft = new Line(PLUS_X - CATCH_RANGE, FISH_PATH_Y - 40, PLUS_X - CATCH_RANGE, FISH_PATH_Y + 40);
        catchZoneLeft.setStroke(Color.LIGHTGREEN);
        catchZoneLeft.setStrokeWidth(1);
        catchZoneLeft.getStrokeDashArray().addAll(5d, 5d);
        root.getChildren().add(catchZoneLeft);
        
        Line catchZoneRight = new Line(PLUS_X + CATCH_RANGE, FISH_PATH_Y - 40, PLUS_X + CATCH_RANGE, FISH_PATH_Y + 40);
        catchZoneRight.setStroke(Color.LIGHTGREEN);
        catchZoneRight.setStrokeWidth(1);
        catchZoneRight.getStrokeDashArray().addAll(5d, 5d);
        root.getChildren().add(catchZoneRight);
        
        // Create game over label (initially invisible)
        gameOverLabel = new Label("GAME OVER\nPress SPACE to play again");
        gameOverLabel.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        gameOverLabel.setTextFill(Color.RED);
        
        StackPane gameOverPane = new StackPane(gameOverLabel);
        gameOverPane.setLayoutX(0);
        gameOverPane.setLayoutY(0);
        gameOverPane.setPrefWidth(SCENE_WIDTH);
        gameOverPane.setPrefHeight(SCENE_HEIGHT);
        gameOverPane.setAlignment(Pos.CENTER);
        gameOverPane.setVisible(false);
        root.getChildren().add(gameOverPane);
        
        Scene scene = new Scene(root, SCENE_WIDTH, SCENE_HEIGHT);
        
        // Game animation timer
        AnimationTimer gameLoop = new AnimationTimer() {
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
        
        // Handle key presses
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE) {
                if (!fishActive && misses < 5) {
                    gameOverPane.setVisible(false);
                    spawnNewFish();
                } else if (misses >= 5) {
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
                
                System.out.println("Fish center: " + fishCenter + ", Plus X: " + PLUS_X + ", Distance: " + distance);
                
                if (distance < CATCH_RANGE) {  // Use the defined catch range constant
                    score++;
                    fishActive = false;
                    updateScoreLabel();
                    spawnNewFish();
                    System.out.println("Fish caught! Score: " + score);
                } else {
                    misses++;
                    updateScoreLabel();
                    checkGameOver();
                    System.out.println("Missed! Misses: " + misses);
                }
            }
        });
        
        // Start the game loop
        gameLoop.start();
        
        primaryStage.setTitle("JavaFX Fishing Game");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
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
        scoreLabel.setText("Score: " + score + " | Misses: " + misses);
    }
    
    private void checkGameOver() {
        if (misses >= 5) {
            fishActive = false;
            gameOverLabel.getParent().setVisible(true);
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
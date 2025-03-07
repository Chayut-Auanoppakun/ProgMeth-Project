package Task;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.layout.Pane;

public class Task10 extends Task {
    public Task10(TaskPane parent, String taskId) {
        super(parent, taskId);
        
        // Pixel-art style background
        setStyle("-fx-background-color: #8b6c42; -fx-border-color: #555; -fx-border-width: 2px;");
        
        // Title panel with darker brown background
        HBox titlePanel = new HBox();
        titlePanel.setStyle("-fx-background-color: #6d4e2a; -fx-padding: 10px;");
        titlePanel.setPrefWidth(960);
        titlePanel.setAlignment(Pos.CENTER);
        
        // Task title with pixelated font
        Text title = new Text("Cut Flowers");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
        title.setFill(Color.WHITE);
        titlePanel.getChildren().add(title);
        
        // Content panel with pixel-art styling
        VBox contentPanel = new VBox(20);
        contentPanel.setStyle("-fx-background-color: #a88c60; -fx-padding: 20px; -fx-border-color: #6d4e2a; -fx-border-width: 2px;");
        contentPanel.setAlignment(Pos.CENTER);
        contentPanel.setMaxWidth(700);
        
        // Pixel-art style instructions
        Text instructions = new Text("Cut all the yellow flowers in the garden.");
        instructions.setFont(Font.font("Monospace", FontWeight.NORMAL, 18));
        instructions.setFill(Color.WHITE);
        
        // Task container with pixel-art styling
        Pane taskContainer = new Pane();
        taskContainer.setPrefSize(656, 400);
        taskContainer.setStyle(
            "-fx-background-color: #64553a; " +
            "-fx-padding: 20px; " +
            "-fx-border-color: #444; " +
            "-fx-border-width: 3px; " +
            "-fx-border-radius: 5px;"
        );

        // Background image
        ImageView ground = new ImageView(new Image("/TaskAsset/CutFlower/background.png")); 
        ground.setFitWidth(656);
        ground.setFitHeight(400);
        taskContainer.getChildren().add(ground);

        int flowerCount = 6; // Number of flowers
        double flowerWidth = 60;
        double flowerHeight = 60;
        Random random = new Random();
        AtomicInteger remainingFlowers = new AtomicInteger(flowerCount);

        // List to track flower positions to prevent overlap
        ArrayList<ImageView> placedFlowers = new ArrayList<>();

        for (int i = 0; i < flowerCount; i++) {
            ImageView flower = new ImageView(new Image("/TaskAsset/CutFlower/yellow_flower.png")); 
            flower.setFitWidth(flowerWidth);
            flower.setFitHeight(flowerHeight);

            // Find a non-overlapping position
            boolean positioned;
            int attempts = 0;
            do {
                positioned = true;
                
                // Randomize position with margins
                double x = 100 + random.nextInt((int)(taskContainer.getPrefWidth() - 200 - flowerWidth));
                double y = 100 + random.nextInt((int)(taskContainer.getPrefHeight() - 200 - flowerHeight));
                
                flower.setLayoutX(x);
                flower.setLayoutY(y);

                // Check for overlap with previously placed flowers
                for (ImageView placedFlower : placedFlowers) {
                    if (checkOverlap(flower, placedFlower)) {
                        positioned = false;
                        break;
                    }
                }

                attempts++;
                // Prevent infinite loop
                if (attempts > 100) {
                    break;
                }
            } while (!positioned);

            // Track initial click position
            final double[] offsetX = new double[1];
            final double[] offsetY = new double[1];

//            // Make the flower draggable
//            flower.setOnMousePressed(event -> {
//                offsetX[0] = event.getX();
//                offsetY[0] = event.getY();
//            });
//
//            flower.setOnMouseDragged(event -> {
//                // Calculate new position
//                double newX = flower.getLayoutX() + (event.getX() - offsetX[0]);
//                double newY = flower.getLayoutY() + (event.getY() - offsetY[0]);
//
//                // Boundary checks
//                double minX = 0;
//                double maxX = taskContainer.getWidth() - flower.getFitWidth();
//                double minY = 0;
//                double maxY = taskContainer.getHeight() - flower.getFitHeight();
//
//                // Restrict movement within boundaries
//                newX = Math.max(minX, Math.min(newX, maxX));
//                newY = Math.max(minY, Math.min(newY, maxY));
//
//                flower.setLayoutX(newX);
//                flower.setLayoutY(newY);
//            });

            // Detect when the flower is clicked to cut
            flower.setOnMouseClicked(e -> {
                taskContainer.getChildren().remove(flower);
                if (remainingFlowers.decrementAndGet() == 0) {
                    completeTask();
                }
            });

            // Add flower to tracking list and container
            placedFlowers.add(flower);
            taskContainer.getChildren().add(flower);
        }

        // Assemble the main container
        VBox mainContainer = new VBox(0);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.getChildren().add(titlePanel);
        
        VBox.setMargin(contentPanel, new Insets(50, 0, 0, 0));
        contentPanel.getChildren().addAll(instructions, taskContainer);
        mainContainer.getChildren().add(contentPanel);
        
        getChildren().addAll(mainContainer, getCloseButton());
    }

    // Check if two flowers overlap
    private boolean checkOverlap(ImageView flower1, ImageView flower2) {
        double margin = 10; // Additional margin to prevent too-close placement
        return flower1.getBoundsInParent().intersects(
            flower2.getBoundsInParent().getMinX() - margin, 
            flower2.getBoundsInParent().getMinY() - margin, 
            flower2.getBoundsInParent().getWidth() + 2 * margin, 
            flower2.getBoundsInParent().getHeight() + 2 * margin
        );
    }

    
    @Override
    public boolean isCompleted() {
        return false; // Placeholder
    }
}
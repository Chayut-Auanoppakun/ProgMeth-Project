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

public class Task12 extends Task {
    public Task12(TaskPane parent) {
        super(parent, "task12");
        
        // Pixel-art style background
        setStyle("-fx-background-color: #8b6c42; -fx-border-color: #555; -fx-border-width: 2px;");
        
        // Title panel with darker brown background
        HBox titlePanel = new HBox();
        titlePanel.setStyle("-fx-background-color: #6d4e2a; -fx-padding: 10px;");
        titlePanel.setPrefWidth(960);
        titlePanel.setAlignment(Pos.CENTER);
        
        // Task title with pixelated font
        Text title = new Text("Clean Mirror");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
        title.setFill(Color.WHITE);
        titlePanel.getChildren().add(title);
        
        // Content panel with pixel-art styling
        VBox contentPanel = new VBox(20);
        contentPanel.setStyle("-fx-background-color: #a88c60; -fx-padding: 20px; -fx-border-color: #6d4e2a; -fx-border-width: 2px;");
        contentPanel.setAlignment(Pos.CENTER);
        contentPanel.setMaxWidth(700);
        
        // Pixel-art style instructions
        Text instructions = new Text("Remove all the dirt from the mirror.");
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

        // Mirror image
        ImageView mirror = new ImageView(new Image("/TaskAsset/CleanMirror/mirror.png"));
        mirror.setFitWidth(656);
        mirror.setFitHeight(400);
        mirror.setLayoutX(0);
        mirror.setLayoutY(0);
        taskContainer.getChildren().add(mirror);

        int dirtCount = 6; // Number of dirt spots
        double dirtWidth = 60;
        double dirtHeight = 60;
        Random random = new Random();
        AtomicInteger remainingDirt = new AtomicInteger(dirtCount);

        // List to track dirt positions to prevent overlap
        ArrayList<ImageView> placedDirt = new ArrayList<>();

        for (int i = 0; i < dirtCount; i++) {
            ImageView dirt = new ImageView(new Image("/TaskAsset/CleanMirror/dirt.png"));
            dirt.setFitWidth(dirtWidth);
            dirt.setFitHeight(dirtHeight);

            // Find a non-overlapping position on the mirror
            boolean positioned;
            int attempts = 0;
            do {
                positioned = true;
                
                // Randomize position within mirror bounds
                double mirrorX = mirror.getLayoutX();
                double mirrorY = mirror.getLayoutY();
                double x = mirrorX + 100 + random.nextInt((int)(mirror.getFitWidth() - 200 - dirtWidth));
                double y = mirrorY + 100 + random.nextInt((int)(mirror.getFitHeight() - 200 - dirtHeight));
                
                dirt.setLayoutX(x);
                dirt.setLayoutY(y);

                // Check for overlap with previously placed dirt
                for (ImageView placedDirtSpot : placedDirt) {
                    if (checkOverlap(dirt, placedDirtSpot)) {
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

            // Add dirt to tracking list and container
            placedDirt.add(dirt);
            
            dirt.setOnMouseClicked(e -> {
                taskContainer.getChildren().remove(dirt);
                if (remainingDirt.decrementAndGet() == 0) {
                    completeTask();
                }
            });

            taskContainer.getChildren().add(dirt);
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

    // Check if two dirt spots overlap
    private boolean checkOverlap(ImageView dirt1, ImageView dirt2) {
        double margin = 10; // Additional margin to prevent too-close placement
        return dirt1.getBoundsInParent().intersects(
            dirt2.getBoundsInParent().getMinX() - margin, 
            dirt2.getBoundsInParent().getMinY() - margin, 
            dirt2.getBoundsInParent().getWidth() + 2 * margin, 
            dirt2.getBoundsInParent().getHeight() + 2 * margin
        );
    }

    @Override
    protected void initializeTask() {
        // No specific initialization needed
    }

    @Override
    public boolean isCompleted() {
        return false; // Placeholder
    }
}
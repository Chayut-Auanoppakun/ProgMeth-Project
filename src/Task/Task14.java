package Task;

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

public class Task14 extends Task {
    public Task14(TaskPane parent) {
        super(parent, "task14");
        
        // Pixel-art style background
        setStyle("-fx-background-color: #8b6c42; -fx-border-color: #555; -fx-border-width: 2px;");
        
        // Title panel with darker brown background
        HBox titlePanel = new HBox();
        titlePanel.setStyle("-fx-background-color: #6d4e2a; -fx-padding: 10px;");
        titlePanel.setPrefWidth(960);
        titlePanel.setAlignment(Pos.CENTER);
        
        // Task title with pixelated font
        Text title = new Text("Extinguish Fire");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
        title.setFill(Color.WHITE);
        titlePanel.getChildren().add(title);
        
        // Content panel with pixel-art styling
        VBox contentPanel = new VBox(20);
        contentPanel.setStyle("-fx-background-color: #a88c60; -fx-padding: 20px; -fx-border-color: #6d4e2a; -fx-border-width: 2px;");
        contentPanel.setAlignment(Pos.CENTER);
        contentPanel.setMaxWidth(700);
        
        // Pixel-art style instructions
        Text instructions = new Text("Extinguish all the fires in the room.");
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

        // Background scene
        ImageView scene = new ImageView(new Image("/TaskAsset/Fire/background.png"));
        scene.setFitWidth(656);
        scene.setFitHeight(400);
        taskContainer.getChildren().add(scene);

        int fireCount = 5; // Number of fire spots
        Random random = new Random();
        AtomicInteger remainingFire = new AtomicInteger(fireCount);

        // Fire spots
        for (int i = 0; i < fireCount; i++) {
            ImageView fire = new ImageView(new Image("/TaskAsset/Fire/fire.png"));
            fire.setFitWidth(50);
            fire.setFitHeight(50);

            // Random position within the scene
            double x = 100 + random.nextInt((int)(scene.getFitWidth() - 200));
            double y = 100 + random.nextInt((int)(scene.getFitHeight() - 200));
            
            fire.setLayoutX(x);
            fire.setLayoutY(y);

            fire.setOnMouseClicked(e -> {
                taskContainer.getChildren().remove(fire);
                if (remainingFire.decrementAndGet() == 0) {
                    completeTask();
                }
            });

            taskContainer.getChildren().add(fire);
        }

        // Fire extinguisher
        ImageView extinguisher = new ImageView(new Image("/TaskAsset/Fire/fire_extinguisher.png"));
        extinguisher.setFitWidth(100);
        extinguisher.setFitHeight(50);
        extinguisher.setLayoutX(250);
        extinguisher.setLayoutY(350);
        taskContainer.getChildren().add(extinguisher);
        
        // Assemble the main container
        VBox mainContainer = new VBox(0);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.getChildren().add(titlePanel);
        
        VBox.setMargin(contentPanel, new Insets(50, 0, 0, 0));
        contentPanel.getChildren().addAll(instructions, taskContainer);
        mainContainer.getChildren().add(contentPanel);
        
        getChildren().addAll(mainContainer, getCloseButton());
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
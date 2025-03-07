package Task;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import java.util.concurrent.atomic.AtomicInteger;

public class Task2 extends Task {
    private static int pumpCount = 0;
    private static boolean pumpTaskRunning = false;
    
    public Task2(TaskPane parent, String taskId) {
        super(parent, taskId);
        
        // Pixel-art style background matching the game's aesthetic
        setStyle("-fx-background-color: #8b6c42; -fx-border-color: #555; -fx-border-width: 2px;");
        
        // Pixel-art style title panel
        HBox titlePanel = new HBox();
        titlePanel.setStyle("-fx-background-color: #6d4e2a; -fx-padding: 10px;");
        titlePanel.setPrefWidth(960);
        titlePanel.setAlignment(Pos.CENTER);
        
        // Pixel-art style title text
        Text title = new Text("Pump Toilet");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
        title.setFill(Color.WHITE);
        titlePanel.getChildren().add(title);
        
        // Content panel with pixel-art styling
        VBox contentPanel = new VBox(20);
        contentPanel.setStyle("-fx-background-color: #a88c60; -fx-padding: 20px; -fx-border-color: #6d4e2a; -fx-border-width: 2px;");
        contentPanel.setAlignment(Pos.CENTER);
        contentPanel.setMaxWidth(700);
        
        // Pixel-art style instructions
//        Text instructions = new Text("Hold the button to pump fuel until the tank is full.");
//        instructions.setFont(Font.font("Monospace", FontWeight.NORMAL, 18));
//        instructions.setFill(Color.WHITE);
        
        // Pixel-art style progress bar (vertical)
        javafx.scene.control.ProgressBar progressBar = new javafx.scene.control.ProgressBar(0);
        progressBar.setPrefWidth(200);
        progressBar.setPrefHeight(40);
        progressBar.setRotate(-90);
        progressBar.setStyle(
            "-fx-accent: #ff8c00;" + 
            "-fx-control-inner-background: #6d4e2a;" + 
            "-fx-border-color: #444;" + 
            "-fx-border-width: 2px;"
        );
        
        // Pixel-art style toilet/pump
        ImageView toiletView = new ImageView(new Image("/TaskAsset/PumpToilet/pump_up.png"));
        toiletView.setFitWidth(280);
        toiletView.setFitHeight(440);
        
        // Pixel-art style button for interaction
        Button pumpButton = new Button();
        pumpButton.setStyle("-fx-background-color: transparent;");
        pumpButton.setGraphic(toiletView);
        
        // Interaction logic
        pumpButton.setOnMousePressed(e -> {
            toiletView.setImage(new Image("/TaskAsset/PumpToilet/pump_down.png"));
            toiletPump(progressBar);
        });
        
        pumpButton.setOnMouseReleased(e -> {
            toiletView.setImage(new Image("/TaskAsset/PumpToilet/pump_up.png"));
        });
        
        // Layout with pixel-art border
        HBox layout = new HBox(20);
        layout.setStyle(
            "-fx-background-color: #64553a;" + 
            "-fx-padding: 20px;" + 
            "-fx-border-color: #444;" + 
            "-fx-border-width: 3px;" + 
            "-fx-border-radius: 5px;"
        );
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(progressBar, pumpButton);
        
        // Assemble the main container
        VBox mainContainer = new VBox(0);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.getChildren().add(titlePanel);
        
        VBox.setMargin(contentPanel, new javafx.geometry.Insets(25, 0, 0, 0));
        contentPanel.getChildren().add(layout);
        mainContainer.getChildren().add(contentPanel);
        
        getChildren().addAll(mainContainer, getCloseButton());
    }
    
    public void toiletPump(javafx.scene.control.ProgressBar pump) {
        pumpCount++;
        pump.setProgress((double) pumpCount / 20);
        if (pumpCount >= 20) {
            System.out.println("Task2 Success!");
            pumpCount = 0;
            completeTask();
        } else if (!pumpTaskRunning) {
            pumpTaskRunning = true;
            new Thread(() -> {
                try {
                    while (pumpCount > 0 && pumpTaskRunning) {
                        Thread.sleep(350);
                        pumpCount--;
                        double progress = (double) pumpCount / 20;
                        Platform.runLater(() -> pump.setProgress(progress));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    pumpTaskRunning = false;
                }
            }).start();
        }
    }

    @Override
    protected void initializeTask() {
        // Initialization happens in constructor
    }

    @Override
    public boolean isCompleted() {
        return false; // This is handled by the completeTask method
    }
}
package Task;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
/**
 * Sample implementation of Task 2: Fuel Task
 */
class Task2 extends Task {
    private javafx.scene.control.ProgressBar fuelBar;
    private double fuelLevel = 0;
    private boolean isPouring = false;
    private Thread pumpThread;
    private Button pumpButton;
    
    public Task2(TaskPane parent) {
        super(parent, "task2");
    }
    
    @Override
    protected void initializeTask() {
        // Task title
        Text title = new Text("Refuel Engine");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setFill(Color.WHITE);
        
        // Task instructions
        Text instructions = new Text("Hold the button to pump fuel until the tank is full.");
        instructions.setFill(Color.LIGHTGRAY);
        
        // Fuel progress bar
        fuelBar = new javafx.scene.control.ProgressBar(0);
        fuelBar.setPrefWidth(400);
        fuelBar.setPrefHeight(30);
        fuelBar.setStyle("-fx-accent: #ff8c00;"); // Orange fuel color
        
        // Pump button
        pumpButton = new Button("HOLD TO PUMP");
        pumpButton.setPrefSize(200, 60);
        pumpButton.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold;");
        
        pumpButton.setOnMousePressed(e -> startPumping());
        pumpButton.setOnMouseReleased(e -> stopPumping());
        
        // Put everything together
        VBox content = new VBox(30);
        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(title, instructions, fuelBar, pumpButton);
        
        getChildren().add(content);
    }
    
    private void startPumping() {
        isPouring = true;
        if (pumpThread != null && pumpThread.isAlive()) {
            return; // Thread already running
        }
        
        pumpThread = new Thread(() -> {
            try {
                while (isPouring && fuelLevel < 1.0) {
                    Thread.sleep(100);
                    fuelLevel += 0.02;
                    if (fuelLevel > 1.0) fuelLevel = 1.0;
                    
                    final double level = fuelLevel;
                    Platform.runLater(() -> {
                        fuelBar.setProgress(level);
                        
                        // Task complete when fuel is full
                        if (isCompleted()) {
                            completeTask();
                        }
                    });
                }
            } catch (InterruptedException e) {
                // Thread interrupted, do nothing
            }
        });
        pumpThread.setDaemon(true);
        pumpThread.start();
    }
    
    private void stopPumping() {
        isPouring = false;
        // Fuel leaks if not full
        if (fuelLevel < 1.0) {
            startLeaking();
        }
    }
    
    private void startLeaking() {
        if (pumpThread != null && pumpThread.isAlive()) {
            pumpThread.interrupt();
        }
        
        pumpThread = new Thread(() -> {
            try {
                while (!isPouring && fuelLevel > 0.0) {
                    Thread.sleep(100);
                    fuelLevel -= 0.01;
                    if (fuelLevel < 0.0) fuelLevel = 0.0;
                    
                    final double level = fuelLevel;
                    Platform.runLater(() -> fuelBar.setProgress(level));
                }
            } catch (InterruptedException e) {
                // Thread interrupted, do nothing
            }
        });
        pumpThread.setDaemon(true);
        pumpThread.start();
    }
    
    @Override
    public boolean isCompleted() {
        return fuelLevel >= 1.0;
    }
}

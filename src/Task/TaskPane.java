package Task;

import gui.GameWindow;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import logic.TaskLogic;

/**
 * TaskPane is a manager class that creates and displays the appropriate task
 * GUI based on the event ID passed to it.
 */
public class TaskPane extends StackPane {
    private static TaskPane instance;
    private Task currentTask;

    // Singleton pattern to ensure only one TaskPane exists
    public static TaskPane getInstance() {
        if (instance == null) {
            instance = new TaskPane();
        }
        return instance;
    }

    private TaskPane() {
        // Set the size to match the game window
        setPrefSize(GameWindow.getScreenwidth(), GameWindow.getScreenheight());
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        setStyle("-fx-background-color: rgba(0, 0, 0, 0.7)");
        
        // Center all content by default
        setAlignment(Pos.CENTER);
    }

    /**
     * Opens the appropriate task based on the event ID.
     * 
     * @param eventId    The ID of the event/task to open
     * @param parentPane The parent pane to add this task pane to
     * @return true if a task was opened, false otherwise
     */
    public boolean openTask(String eventId, Pane parentPane) {
        // Clear any existing task
        getChildren().clear();

        // Create the appropriate task based on the event ID
        switch (eventId) {
        case "1"://fix lighting (8 switches)
            currentTask = new Task1(this);
            break;
        case "2"://pump toilet
            currentTask = new Task2(this);
            break;
        case "3"://passcode (fix O2)
            currentTask = new Task3(this);
            break;
        case "4"://dish washing
            currentTask = new Task4(this);
            break;
        case "5"://fishing
            currentTask = new Task5(this);
            break;
        case "6"://dump trash
            currentTask = new Task6(this);
            break;
        case "7"://fold blanket
            currentTask = new Task7(this);
            break;
        case "8"://fix wiring (soldering)
            currentTask = new Task8(this);
            break;
        case "9"://organize bookshelf
            currentTask = new Task9(this);
            break;
        case "10"://collect flower
            currentTask = new Task10(this);
            break;
        case "11"://fire extinguish
            currentTask = new Task11(this);
            break;
        case "12"://clean mirror
            currentTask = new Task12(this);
            break;
        case "13"://chop carrot
            currentTask = new Task13(this);
            break;
        default:
        	break;
        }

        // Add the task to the task pane - it will be centered by default thanks to StackPane
        getChildren().add(currentTask);

        // Add the task pane to the parent (typically the GameWindow)
        if (!parentPane.getChildren().contains(this)) {
            // Remove this instance if it exists elsewhere in the scene graph
            if (this.getParent() != null) {
                ((Pane)this.getParent()).getChildren().remove(this);
            }
            
            parentPane.getChildren().add(this);
            
            // Make the TaskPane fill the parent
            this.prefWidthProperty().bind(parentPane.widthProperty());
            this.prefHeightProperty().bind(parentPane.heightProperty());
            this.maxWidthProperty().bind(parentPane.widthProperty());
            this.maxHeightProperty().bind(parentPane.heightProperty());
        }
        
        // Play the fade-in animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), this);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        return true;
    }

    /**
     * Closes the current task and removes the task pane from its parent.
     * 
     * @param success Whether the task was completed successfully
     */
    public void closeTask(boolean success) {
        if(success) {
            StackPane successOverlay = new StackPane();
            successOverlay.setAlignment(Pos.CENTER);
            
            Rectangle background = new Rectangle(getWidth(), getHeight(), Color.rgb(0, 0, 0, 0.5));
            background.widthProperty().bind(widthProperty());
            background.heightProperty().bind(heightProperty());
            
            Text successText = new Text("Task Success!");
            successText.setFont(Font.font("Arial", FontWeight.BOLD, 30));
            successText.setFill(Color.WHITE);
            successText.setTextAlignment(TextAlignment.CENTER);
    
            successOverlay.getChildren().addAll(background, successText);
            getChildren().add(successOverlay);
            //setOpacity(0.5);
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // Wait 2 seconds
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Platform.runLater(() -> {
                    getChildren().remove(successOverlay);
                    
                    // Add a fade-out animation
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(800), this);
                    fadeOut.setFromValue(1);
                    fadeOut.setToValue(0);
                    fadeOut.play();
                    
                    // Notify the TaskLogic that the task is complete
                    if (currentTask != null) {
                        TaskLogic.completeTask(currentTask.getTaskId());
                    }
                });
            }).start();
        } else {
            // For unsuccessful task completion, just fade out and remove
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), this);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                Pane parent = (Pane) getParent();
                if (parent != null) {
                    parent.getChildren().remove(this);
                }
                
                // Clear and reset this instance for reuse
                getChildren().clear();
                setOpacity(1.0);
            });
            fadeOut.play();
        }
    }

//    private void showTaskSuccessMessage(Pane parentPane) {
//        StackPane successOverlay = new StackPane();
//        successOverlay.setAlignment(Pos.CENTER);
//        
//        Rectangle background = new Rectangle(parentPane.getWidth(), parentPane.getHeight(), Color.rgb(0, 0, 0, 0.5));
//        background.widthProperty().bind(parentPane.widthProperty());
//        background.heightProperty().bind(parentPane.heightProperty());
//        
//        Text successText = new Text("Task Completed Successfully!");
//        successText.setFont(Font.font("Impact", FontWeight.BOLD, 36));
//        successText.setFill(Color.WHITE);
//        successText.setTextAlignment(TextAlignment.CENTER);
//
//        successOverlay.getChildren().addAll(background, successText);
//        parentPane.getChildren().add(successOverlay);
//        
//        // Create a timer to remove the success message after a delay
//        new Thread(() -> {
//            try {
//                Thread.sleep(2000); // Wait 2 seconds
//            } catch (InterruptedException ex) {
//                ex.printStackTrace();
//            }
//            Platform.runLater(() -> parentPane.getChildren().remove(successOverlay));
//        }).start();
//    }
}
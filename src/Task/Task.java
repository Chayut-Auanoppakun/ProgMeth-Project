package Task;

import javafx.geometry.Insets;	
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import logic.SoundLogic;

abstract class Task extends StackPane implements Doable{
    protected TaskPane parentTaskPane;
    protected String taskId;
    private Button closeButton = new Button("X");
    
    public Task(TaskPane parent, String taskId) {
        this.parentTaskPane = parent;
        this.taskId = taskId;

        // Common task setup
        setPrefSize(960, 640);
        setMaxSize(960, 640);
        setMinSize(960, 640);
        setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #555; -fx-border-width: 2px;");

        // Add close button to all tasks
        closeButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-weight: bold;");
        closeButton.setOnAction(e -> parentTaskPane.closeTask(false));

        StackPane.setAlignment(closeButton, Pos.TOP_RIGHT);
        StackPane.setMargin(closeButton, new Insets(10, 10, 0, 0));

        // Initialize the task
    }

    public String getTaskId() {
        return taskId;
    }

    /**
     * Called when the task is completed successfully.
     */
    protected void completeTask() {
        parentTaskPane.closeTask(true);
    }

    /**
     * Initialize task-specific components and logic. Each task must implement this
     * method.
     */

    /**
     * Check if the task has been completed successfully. Each task must implement
     * this method.
     * 
     * @return true if the task is completed, false otherwise
     */
    //public abstract boolean isCompleted();
    
    public Button getCloseButton() {
        return closeButton;
    }
}
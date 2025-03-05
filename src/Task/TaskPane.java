package Task;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
public class TaskPane extends Pane {
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
		setPrefSize(800, 600);
		setStyle("-fx-background-color: rgba(0, 0, 0, 0.7)");
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
		case "1":
			currentTask = new Task1(this);
			break;
		case "2":
			currentTask = new Task2(this);
			break;
		case "3":
			currentTask = new Task3(this);
			break;
		default:
			System.out.println("Unknown task ID: " + eventId);
			return false;
		}

		// Add the task to the task pane
		getChildren().add(currentTask);

		// Add the task pane to the parent (typically the GameWindow)
		if (!parentPane.getChildren().contains(this)) {
			parentPane.getChildren().add(this);

			// Center the task pane in the parent
			setLayoutX((parentPane.getWidth() - getPrefWidth()) / 2);
			setLayoutY((parentPane.getHeight() - getPrefHeight()) / 2);

			// Add a fade-in animation
			FadeTransition fadeIn = new FadeTransition(Duration.millis(200), this);
			fadeIn.setFromValue(0);
			fadeIn.setToValue(1);
			fadeIn.play();
		}

		return true;
	}

	/**
	 * Closes the current task and removes the task pane from its parent.
	 * 
	 * @param success Whether the task was completed successfully
	 */
	public void closeTask(boolean success) {
		// Add a fade-out animation
		FadeTransition fadeOut = new FadeTransition(Duration.millis(200), this);
		fadeOut.setFromValue(1);
		fadeOut.setToValue(0);

		fadeOut.setOnFinished(e -> {
			// Remove from parent
			if (getParent() != null) {
				((Pane) getParent()).getChildren().remove(this);
			}

			// Show success message if needed
			if (success && getParent() != null) {
				showTaskSuccessMessage((Pane) getParent());
			}
		});

		fadeOut.play();

		// Notify the TaskLogic that the task is complete
		if (success && currentTask != null) {
			TaskLogic.completeTask(currentTask.getTaskId());
		}
	}

	private void showTaskSuccessMessage(Pane parentPane) {
		StackPane successOverlay = new StackPane();
		Rectangle background = new Rectangle(parentPane.getWidth(), parentPane.getHeight(), Color.rgb(0, 0, 0, 0.5));
		Text successText = new Text("Task Completed Successfully!");
		successText.setFont(Font.font("Impact", FontWeight.BOLD, 36));
		successText.setFill(Color.WHITE);
		successText.setTextAlignment(TextAlignment.CENTER);

		successOverlay.getChildren().addAll(background, successText);
		parentPane.getChildren().add(successOverlay);

		// Create a timer to remove the success message after a delay
		new Thread(() -> {
			try {
				Thread.sleep(2000); // Wait 2 seconds
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
			Platform.runLater(() -> parentPane.getChildren().remove(successOverlay));
		}).start();
	}
}
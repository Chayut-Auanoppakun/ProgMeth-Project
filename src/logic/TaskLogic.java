package logic;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import gameObjects.eventObject;
import gui.GameWindow;
import Task.TaskPane;
import javafx.scene.layout.Pane;

/**
 * TaskLogic provides utility methods for managing tasks in the game. It handles
 * collision detection with task objects and tracks task completion.
 */
public class TaskLogic {
	// Map to track which tasks have been completed

	/**
	 * Checks if the player is colliding with any event object.
	 * 
	 * @param eventObjects List of event objects to check against
	 * @return The EventID of the collided event, or empty string if no collision
	 */
	public static String isPlayerCollidingWithEvent(List<eventObject> eventObjects) {
		for (eventObject event : eventObjects) {
			double playerPosX = PlayerLogic.getMyPosX();
			double playerPosY = PlayerLogic.getMyPosY();
			double eventPosX = event.getX();
			double eventPosY = event.getY();

			// Define player collision box dimensions
			final double PLAYER_WIDTH = 48;
			final double PLAYER_HEIGHT = 64;

			// Calculate player collision box coordinates
			double playerLeft = playerPosX - (PLAYER_WIDTH / 2);
			double playerRight = playerPosX + (PLAYER_WIDTH / 2);
			double playerTop = playerPosY - (PLAYER_HEIGHT / 2);
			double playerBottom = playerPosY + (PLAYER_HEIGHT / 2);

			// Standard rectangle collision detection
			if (playerRight > eventPosX && playerLeft < eventPosX + event.getWidth() && playerBottom > eventPosY
					&& playerTop < eventPosY + event.getHeight()) {
				if (PlayerLogic.getTasks().contains(Integer.parseInt(event.getEventID()))) {
					return event.getEventID();
				} else {
					//System.out.println("Collided with " + event.getEventID() + " but not in task lisk.");
				}
			}
		}
		return "";
	}

	/**
	 * Attempts to open a task based on the event ID
	 * 
	 * @param eventId    The ID of the event/task to open
	 * @param parentPane The parent pane to add the task to
	 * @return true if task was opened, false otherwise
	 */
	public static boolean openTask(String eventId, Pane parentPane) {
		// Open the task using the TaskPane
		parentPane.setLayoutX(GameWindow.getScreenwidth() * 0.15);
		parentPane.setLayoutY(GameWindow.getScreenheight() * 0.15);
		return TaskPane.getInstance().openTask(eventId, parentPane);
	}

	/**
	 * Marks a task as completed and updates task progress.
	 * 
	 * @param taskId The ID of the completed task
	 */
	public static void completeTask(String taskId) {
		PlayerLogic.getTasks().remove(Integer.parseInt(taskId.substring(4)));
		// Log task completion
		System.out.println("Task " + taskId + " completed!");
		System.out.println(PlayerLogic.getTasks());
	}
}
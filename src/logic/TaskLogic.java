package logic;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import gameObjects.eventObject;
import gui.GameWindow;
import Task.TaskPane;
import javafx.scene.layout.Pane;

/**
 * TaskLogic provides utility methods for managing tasks in the game.
 * It handles collision detection with task objects and tracks task completion.
 */
public class TaskLogic {
    // Map to track which tasks have been completed
    private static Map<String, Boolean> completedTasks = new HashMap<>();
    
    // Counter for total completed tasks
    private static int completedTaskCount = 0;
    private static int totalTaskCount = 0; // This should be set based on the total number of tasks in the game
    
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
            
            // Debug output
//            System.out.println("Checking collision:");
//            System.out.println("Player Position: (" + playerPosX + ", " + playerPosY + ")");
//            System.out.println("Player Box: L=" + playerLeft + ", R=" + playerRight + 
//                           ", T=" + playerTop + ", B=" + playerBottom);
//            System.out.println("Event Position: (" + eventPosX + ", " + eventPosY + ")");
//            System.out.println("Event Size: W=" + event.getWidth() + ", H=" + event.getHeight());
            
            // Standard rectangle collision detection
            if (playerRight > eventPosX && 
                playerLeft < eventPosX + event.getWidth() &&
                playerBottom > eventPosY && 
                playerTop < eventPosY + event.getHeight()) {
                return event.getEventID();
            }
        }
        return "";
    }
    
    /**
     * Attempts to open a task based on the event ID
     * 
     * @param eventId The ID of the event/task to open
     * @param parentPane The parent pane to add the task to
     * @return true if task was opened, false otherwise
     */
    public static boolean openTask(String eventId, Pane parentPane) {
        // Check if this task has already been completed
        if (completedTasks.getOrDefault(eventId, false)) {
            System.out.println("Task " + eventId + " already completed.");
            return false;
        }
        
        // Open the task using the TaskPane
        parentPane.setLayoutX(GameWindow.getScreenwidth()*0.15);
        parentPane.setLayoutY(GameWindow.getScreenheight()*0.15);
        return TaskPane.getInstance().openTask(eventId, parentPane);
    }
    
    /**
     * Marks a task as completed and updates task progress.
     * 
     * @param taskId The ID of the completed task
     */
    public static void completeTask(String taskId) {
        // Only count the task if it wasn't already completed
        if (!completedTasks.getOrDefault(taskId, false)) {
            completedTasks.put(taskId, true);
            completedTaskCount++;
            
            // Log task completion
            System.out.println("Task " + taskId + " completed!");
            System.out.println("Completed " + completedTaskCount + " out of " + totalTaskCount + " tasks");
            
            // Check if all tasks are completed
            checkAllTasksCompleted();
        }
    }
    
    /**
     * Sets the total number of tasks in the game.
     * This should be called during game initialization.
     * 
     * @param count Total number of tasks
     */
    public static void setTotalTaskCount(int count) {
        totalTaskCount = count;
    }
    
    /**
     * Gets the number of completed tasks.
     * 
     * @return Number of completed tasks
     */
    public static int getCompletedTaskCount() {
        return completedTaskCount;
    }
    
    /**
     * Gets the total number of tasks.
     * 
     * @return Total number of tasks
     */
    public static int getTotalTaskCount() {
        return totalTaskCount;
    }
    
    /**
     * Gets the task completion percentage.
     * 
     * @return Completion percentage (0-100)
     */
    public static double getTaskCompletionPercentage() {
        if (totalTaskCount == 0) return 0;
        return (completedTaskCount * 100.0) / totalTaskCount;
    }
    
    /**
     * Checks if all tasks have been completed.
     * This will trigger game events if all tasks are done.
     */
    private static void checkAllTasksCompleted() {
        if (totalTaskCount > 0 && completedTaskCount >= totalTaskCount) {
            System.out.println("All tasks completed!");
            
            // Here you can trigger game events, like:
            // - Crewmate victory
            // - Special animations
            // - Game phase changes
            // GameLogic.crewmateVictory();
        }
    }
    
    /**
     * Resets all task completion status.
     * This should be called when starting a new game.
     */
    public static void resetTasks() {
        completedTasks.clear();
        completedTaskCount = 0;
    }
    
    /**
     * Checks if a specific task has been completed.
     * 
     * @param taskId The ID of the task to check
     * @return true if the task is completed, false otherwise
     */
    public static boolean isTaskCompleted(String taskId) {
        return completedTasks.getOrDefault(taskId, false);
    }
}
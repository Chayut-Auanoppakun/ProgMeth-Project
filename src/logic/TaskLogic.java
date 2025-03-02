package logic;

import java.util.List;

import gameObjects.eventObject;
import gui.MainMenuPane;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;

public class TaskLogic {

	private static int pumpCount = 0;
	private static boolean pumpTaskRunning = false;
	
	public static String isPlayerCollidingWithEvent(List<eventObject> eventObjects) {
	    for (eventObject event : eventObjects) {
	        double playerPosX = PlayerLogic.getMyPosX();
	        double playerPosY = PlayerLogic.getMyPosY();
	        double eventPosX = event.getX();
	        double eventPosY = event.getY();
	        
	        System.out.println("Checking collision:");
	        System.out.println("Player Position: (" + playerPosX + ", " + playerPosY + ")");
	        System.out.println("Event Position: (" + eventPosX + ", " + eventPosY + ")");
	        
	        if (playerPosX < eventPosX + event.getWidth() &&
	            playerPosX + 48 > eventPosX &&
	            playerPosY < eventPosY + event.getHeight() &&
	            playerPosY + 64 > eventPosY) {
	            return event.getEventID();
	        }
	    }
	    return "";
	}


	public static void checkTask1Success(ToggleButton[] switches) {
		for (ToggleButton tb : switches) {
			if (!tb.isSelected()) {
				return; // Task fails if any switch is off
			}
		}
		System.out.println("Task1 Success!"); // Replace with actual success logic
		Parent root = switches[0].getScene().getRoot();
		if (root instanceof MainMenuPane) {
			((MainMenuPane) root).taskClose(true);
		}
	}

	public static void toiletPump(ProgressBar pump) {
		pumpCount++;
		pump.setProgress((double) pumpCount / 20);
		if (pumpCount >= 20) {
			System.out.println("Task2 Success!");
			Parent root = pump.getScene().getRoot();
			if (root instanceof MainMenuPane) {
				((MainMenuPane) root).taskClose(true);
			}
			pumpCount = 0;
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

	public static void fixO2(StackPane O2) {
		System.out.println("Task3 Success!");
		Parent root = O2.getScene().getRoot();
		if (root instanceof MainMenuPane) {
			((MainMenuPane) root).taskClose(true);
		}
	}
}

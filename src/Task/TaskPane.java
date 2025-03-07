package Task;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

import gui.GameWindow;
import gui.MainMenuPane;
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
import logic.ClientLogic;
import logic.GameLogic;
import logic.PlayerLogic;
import logic.ServerLogic;
import logic.SoundLogic;
import logic.State;
import logic.TaskLogic;
import server.ClientInfo;

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
		case "1":// done
			currentTask = new Task1(this, "task" + eventId); // FIX LIGHT
			break;
		case "2":// done
			currentTask = new Task2(this, "task" + eventId); // PUMP TOILET
			break;
		case "3":// done
			currentTask = new Task3(this, "task" + eventId); // PASSCODE
			break;
		case "4":// done
			currentTask = new Task4(this, "task" + eventId); // DISH WASHING
			break;
		case "5":// done
			currentTask = new Task5(this, "task" + eventId); // FISHING
			break;
		case "6":// done
			currentTask = new Task6(this, "task" + eventId); // DUMP TRASH
			break;
		case "7":// done
			currentTask = new Task7(this, "task" + eventId); // Fold blankets
			break;
		case "8":// done
			currentTask = new Task8(this, "task" + eventId); // FIX WIRING
			break;
		case "9":// done
			currentTask = new Task9(this, "task" + eventId); // ORGANIZE BOOKSHELF
			break;
		case "10":// done
			currentTask = new Task10(this, "task" + eventId); // COLECT FLOWER
			break;
		case "11":// done
			currentTask = new Task11(this, "task" + eventId); // FIRE
			break;
		case "12":// done
			currentTask = new Task12(this, "task" + eventId); // MIRROR
			break;
		case "13":// done
			currentTask = new Task13(this, "task" + eventId); // CARROT
			break;
		case "14":// done
			currentTask = new Task3(this, "task" + eventId); // PASSCODE
			break;
		case "15":
			currentTask = new Task4(this, "task" + eventId);
			break;
		case "16":// done
			currentTask = new Task5(this, "task" + eventId);
			break;
		case "17":// done
			currentTask = new Task7(this, "task" + eventId); // Fold blankets
			break;
		case "18":// done
			currentTask = new Task8(this, "task" + eventId); // FIX WIRING
			break;
		case "19":// done
			currentTask = new Task9(this, "task" + eventId); // ORGANIZE BOOKSHELF
			break;
		case "20":// done
			currentTask = new Task10(this, "task" + eventId); // COLECT FLOWER
			break;
		case "21":// done
			currentTask = new Task11(this, "task" + eventId); // FIRE EXTINGUISH
			break;
		case "22":// done
			currentTask = new Task12(this, "task" + eventId); // MIRROR
			break;
		case "99":
			Platform.runLater(() -> {
				System.out.println("Emergency button pressed");
				System.out.println("Is Emergency Meeting Available: " + GameLogic.isEmergencyMeetingAvailable());
				System.out.println("Remaining Cooldown: " + GameLogic.getRemainingEmergencyMeetingCooldown());
				// Check emergency meeting cooldown
				if (GameLogic.isEmergencyMeetingAvailable()) {
					Pane parent = (Pane) getParent();
					if (parent != null) {
						parent.getChildren().remove(this);
					}

					// Call the emergency meeting
					if (GameWindow.getGameWindowInstance() != null) {
						String reporterKey = PlayerLogic.getLocalAddressPort();
						GameWindow.getGameWindowInstance().startEmergencyMeeting(reporterKey, null, 0);
						GameLogic.recordEmergencyMeetingTime(); // Record the time of emergency meeting

						// If we're in server mode, broadcast the meeting to all clients
						if (MainMenuPane.getState() == State.SERVER) {
							// Create meeting data
							JSONObject meetingData = new JSONObject();
							meetingData.put("reporter", reporterKey);
							meetingData.put("reportedPlayer", JSONObject.NULL);
							meetingData.put("reportedCharId", 0);
							meetingData.put("time", System.currentTimeMillis());

							String meetingMessage = "/meeting/" + meetingData.toString();
							byte[] buf = meetingMessage.getBytes(StandardCharsets.UTF_8);

							// Send to all connected clients
							for (ClientInfo clientInfo : ServerLogic.getConnectedClients()) {
								try {
									DatagramPacket packet = new DatagramPacket(buf, buf.length, clientInfo.getAddress(),
											clientInfo.getPort());
									ServerLogic.getServerSocket().send(packet);

									// Send multiple times to reduce chance of packet loss
									for (int i = 0; i < 3; i++) {
										Thread.sleep(50); // Short delay between retransmissions
										ServerLogic.getServerSocket().send(packet);
									}
								} catch (Exception e) {
									System.err.println("Error sending meeting message: " + e.getMessage());
								}
							}
						} else if (MainMenuPane.getState() == State.CLIENT) {
							// Client mode - send meeting request to server
							JSONObject meetingData = new JSONObject();
							meetingData.put("reporter", reporterKey);
							meetingData.put("reportedPlayer", JSONObject.NULL);
							meetingData.put("reportedCharId", 0);
							meetingData.put("time", System.currentTimeMillis());

							String meetingMessage = "/meeting/" + meetingData.toString();
							ClientLogic.sendMessage(meetingMessage, null);
						}
					}
				} else {
					// Show cooldown notification
					int remainingCooldown = GameLogic.getRemainingEmergencyMeetingCooldown();
					GameWindow.getGameWindowInstance()
							.showNotification("Emergency Meeting on cooldown. Wait " + remainingCooldown + " seconds.");
				}
			});

			// Return true to indicate that the task was handled
			return true;
		default:

			// currentTask = new Task12(this);
			break;
		}

		// Add the task to the task pane - it will be centered by default thanks to
		// StackPane
		getChildren().add(currentTask);

		// Add the task pane to the parent (typically the GameWindow)
		if (!parentPane.getChildren().contains(this)) {
			// Remove this instance if it exists elsewhere in the scene graph
			if (this.getParent() != null) {
				((Pane) this.getParent()).getChildren().remove(this);
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
		if (success) {
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
			SoundLogic.playSound("assets/sounds/panel_admin_cardaccept.wav", 0);
			getChildren().add(successOverlay);

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
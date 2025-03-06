package gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;
import logic.GameLogic;
import logic.PlayerLogic;
import logic.SoundLogic;
import logic.TaskLogic;
import server.PlayerInfo;

/**
 * Manages the in-game UI overlay that shows task progress and action buttons.
 * This class handles creating and updating the UI elements that appear during
 * gameplay.
 */
public class OverlayUI extends Pane {
	// UI Components
	private Pane overlayPane;
	private ProgressBar taskProgressBar;
	private Text taskProgressText;
	private Button actionButton;
	private Button reportButton;
	private VBox taskContainer;
	private HBox buttonContainer;

	// State tracking
	private boolean isVisible = false;
	private boolean isInitialized = false;
	private long lastActionTime = 0;
	private long lastReportTime = 0;

	// Reference to game window
	private GameWindow gameWindow;

	/**
	 * Creates a new GameOverlayManager for handling the in-game UI overlay.
	 * 
	 * @param gameWindow Reference to the main GameWindow instance
	 */
	public OverlayUI(GameWindow gameWindow) {
		this.gameWindow = gameWindow;
	}

	private VBox createTaskListDisplay() {
		VBox taskListBox = new VBox(5);
		taskListBox.setAlignment(Pos.CENTER_LEFT);
		taskListBox.setMaxWidth(300);

		// Create stylized backing panel
		Rectangle taskListBg = new Rectangle(300, 150);
		taskListBg.setArcWidth(12);
		taskListBg.setArcHeight(12);
		taskListBg.setFill(Color.rgb(30, 30, 50, 0.85));
		taskListBg.setStroke(Color.rgb(70, 130, 180, 0.8));
		taskListBg.setStrokeWidth(2);

		// Add drop shadow for depth
		DropShadow dropShadow = new DropShadow();
		dropShadow.setRadius(5.0);
		dropShadow.setOffsetX(3.0);
		dropShadow.setOffsetY(3.0);
		dropShadow.setColor(Color.rgb(0, 0, 0, 0.5));
		taskListBg.setEffect(dropShadow);

		// Task list title
		Text taskListTitle = new Text("Remaining Tasks");
		taskListTitle.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
		taskListTitle.setFill(Color.WHITE);

		// Create container for task list content
		VBox content = new VBox(8);
		content.setAlignment(Pos.CENTER_LEFT);
		content.setPadding(new Insets(10));

		// Enhanced task descriptions with more context and emojis
		Map<Integer, String> taskDescriptions = new HashMap<>() {
			{
				put(1, "> Fix Electrical Switches");
				put(2, "> Unclog Toilet Plumbing");
				put(3, "> Enter Passcode");
				put(4, "> Sanitize Kitchen Dishes");
				put(5, "> Fishing Equipment Check");
				put(6, "> Dispose of Waste Bags");
				put(7, "> Make the bed");
				put(8, "> Repair Electrical Wiring");
				put(9, "> Organize books in Library");
				put(10, "> Collect Botanical Samples");
				put(11, "> Fire Extinguish");
				put(12, "> Clean Observatory Mirror");
				put(13, "> Prepare Nutrition Supplies");
			}
		};

		// Create container for task entries
		VBox taskList = new VBox(3);
		taskList.setAlignment(Pos.CENTER_LEFT);

		// Get the player's current tasks
		Set<Integer> playerTasks = PlayerLogic.getTasks();

		if (playerTasks.isEmpty()) {
			// Stylized "All tasks completed" message
			Text noTasksText = new Text("All tasks completed!");
			noTasksText.setFont(Font.font("Monospace", FontWeight.BOLD, 12));
			noTasksText.setFill(Color.LIGHTGREEN);
			taskList.getChildren().add(noTasksText);
		} else {
			// Sort tasks to maintain consistent order
			List<Integer> sortedTasks = new ArrayList<>(playerTasks);
			Collections.sort(sortedTasks);

			for (Integer taskId : sortedTasks) {
				Text taskText = new Text(taskDescriptions.get(taskId));
				taskText.setFont(Font.font("Monospace", FontWeight.NORMAL, 12));

				// Alternate text colors for better readability
				taskText.setFill(taskId % 2 == 0 ? Color.LIGHTBLUE : Color.WHITE);

				taskList.getChildren().add(taskText);
			}
		}

		// Add title and task list to content
		content.getChildren().addAll(taskListTitle, taskList);

		// Create a StackPane to layer the background and content
		StackPane taskListPanel = new StackPane();
		taskListPanel.getChildren().addAll(taskListBg, content);

		// Add to main container
		taskListBox.getChildren().add(taskListPanel);

		return taskListBox;
	}

	/**
	 * Initializes the overlay UI components and adds them to the root node.
	 * 
	 * @param root         The root node to add overlay elements to
	 * @param screenWidth  The width of the game screen
	 * @param screenHeight The height of the game screen
	 */
	public void initialize(Group root, double screenWidth, double screenHeight) {
		if (isInitialized)
			return;

		// Create the overlay pane
		overlayPane = new Pane();
		overlayPane.setPrefSize(screenWidth, screenHeight);
		overlayPane.setMouseTransparent(true); // Let mouse events pass through
		overlayPane.setOpacity(0);
		overlayPane.setVisible(false);

		// Create task progress elements
		createTaskProgressUI(screenWidth);

		// Create action buttons
		createActionButtonsUI(screenWidth, screenHeight);

		// Add all elements to the overlay
		overlayPane.getChildren().addAll(taskContainer, buttonContainer);

		// Add overlay to root
		root.getChildren().add(overlayPane);

		isInitialized = true;
	}

	/**
	 * Creates the task progress UI elements (top-left corner)
	 */
	private void createTaskProgressUI(double screenWidth) {
		// Task progress container
		taskContainer = new VBox(5);
		taskContainer.setAlignment(Pos.CENTER_LEFT);
		taskContainer.setPadding(new Insets(20, 15, 10, 15));
		taskContainer.setMaxWidth(screenWidth * 0.3);

		// Create stylized backing panel
		Rectangle progressBg = new Rectangle(screenWidth * 0.3, 68);
		progressBg.setArcWidth(12);
		progressBg.setArcHeight(12);
		progressBg.setFill(Color.rgb(30, 30, 50, 0.85));
		progressBg.setStroke(Color.rgb(70, 130, 180, 0.8));
		progressBg.setStrokeWidth(2);

		// Add drop shadow for depth
		DropShadow dropShadow = new DropShadow();
		dropShadow.setRadius(5.0);
		dropShadow.setOffsetX(3.0);
		dropShadow.setOffsetY(3.0);
		dropShadow.setColor(Color.rgb(0, 0, 0, 0.5));
		progressBg.setEffect(dropShadow);

		// Progress title
		Text title = new Text("Task Progress");
		title.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
		title.setFill(Color.WHITE);

		// Create a JavaFX ProgressBar
		ProgressBar progressBar = new ProgressBar(0);
		progressBar.setPrefWidth(screenWidth * 0.25);
		progressBar.setPrefHeight(16);
		progressBar.setStyle("-fx-accent: #2196F3;" + // Default blue color
				"-fx-control-inner-background: #e0e0e0;" // Light gray background
		);

		// Store reference to the ProgressBar
		taskProgressBar = progressBar;

		// Progress text
		taskProgressText = new Text("0%");
		taskProgressText.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
		taskProgressText.setFill(Color.LIGHTBLUE);

		// Create layout for progress elements
		HBox progressRow = new HBox(15);
		progressRow.setAlignment(Pos.CENTER_LEFT);
		progressRow.getChildren().addAll(progressBar, taskProgressText);

		// Stack everything in the container
		StackPane progressPanel = new StackPane();

		VBox content = new VBox(8);
		content.setAlignment(Pos.CENTER_LEFT);
		content.setPadding(new Insets(10));
		content.getChildren().addAll(title, progressRow);

		progressPanel.getChildren().addAll(progressBg, content);

		taskContainer.getChildren().add(progressPanel);
		taskContainer.setLayoutX(20);
		taskContainer.setLayoutY(20);
	}

	/**
	 * Creates the action buttons UI (bottom-right corner)
	 */
	private void createActionButtonsUI(double screenWidth, double screenHeight) {
		buttonContainer = new HBox(10);
		buttonContainer.setAlignment(Pos.CENTER);
		buttonContainer.setPadding(new Insets(15));

		// Button styles
		String baseButtonStyle = "-fx-background-color: #1e90ff;" + "-fx-text-fill: white;"
				+ "-fx-font-family: 'Monospace';" + "-fx-font-size: 16px;" + "-fx-font-weight: bold;"
				+ "-fx-padding: 8 20 8 20;" + "-fx-border-color: #87cefa;" + "-fx-border-width: 2px;"
				+ "-fx-background-radius: 0;" + "-fx-border-radius: 0;"
				+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);";

		String hoverButtonStyle = "-fx-background-color: #00bfff;" + "-fx-text-fill: white;"
				+ "-fx-font-family: 'Monospace';" + "-fx-font-size: 16px;" + "-fx-font-weight: bold;"
				+ "-fx-padding: 8 20 8 20;" + "-fx-border-color: #b0e2ff;" + "-fx-border-width: 2px;"
				+ "-fx-background-radius: 0;" + "-fx-border-radius: 0;"
				+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 8, 0, 0, 1);";

		// Create action button
		actionButton = new Button("INTERACT [F]");
		actionButton.setPrefWidth(180);
		actionButton.setStyle(baseButtonStyle);

		// Add hover effects
		actionButton.setOnMouseEntered(e -> {
			if (!"dead".equals(PlayerLogic.getStatus())) {
				actionButton.setStyle(hoverButtonStyle);
			}
		});

		actionButton.setOnMouseExited(e -> {
			if (!"dead".equals(PlayerLogic.getStatus())) {
				actionButton.setStyle(baseButtonStyle);
			}
		});

		actionButton.setOnAction(e -> {
			if (System.currentTimeMillis() - lastActionTime < 250)
				return; // Debounce
			lastActionTime = System.currentTimeMillis();

			if ("imposter".equals(PlayerLogic.getStatus()) && !"dead".equals(PlayerLogic.getStatus())) {
				// For imposters: try to kill
				PlayerInfo target = gameWindow.findClosestKillablePlayer();
				if (target != null) {
					gameWindow.killPlayer(target);
				} else {
					// Flash button if no target
					flashButton(actionButton, "-fx-background-color: #550000;");
				}
			} else {
				// For crewmates: interact with tasks
				gameWindow.interactWithNearbyTask();
			}
		});

		// Create report button
		reportButton = new Button("REPORT [R]");
		reportButton.setPrefWidth(150);
		reportButton.setStyle(baseButtonStyle);

		// Add hover effects
		reportButton.setOnMouseEntered(e -> {
			if (!"dead".equals(PlayerLogic.getStatus())) {
				reportButton.setStyle(hoverButtonStyle);
			}
		});

		reportButton.setOnMouseExited(e -> {
			if (!"dead".equals(PlayerLogic.getStatus())) {
				reportButton.setStyle(baseButtonStyle);
			}
		});

		// Make sure reportButton is properly connected too:
		reportButton.setOnAction(e -> {
			if (System.currentTimeMillis() - lastReportTime < 250)
				return; // Debounce
			lastReportTime = System.currentTimeMillis();

			if (!"dead".equals(PlayerLogic.getStatus())) {
				gameWindow.reportNearbyCorpse();
			}
		});

		// Add buttons to container
		buttonContainer.getChildren().addAll(actionButton, reportButton);

		// Position the container
		buttonContainer.setLayoutX(screenWidth - 350);
		buttonContainer.setLayoutY(screenHeight - 100);
	}

	/**
	 * Shows the overlay with a fade-in animation
	 */
	public void show() {
		if (!isInitialized || isVisible)
			return;

		overlayPane.setVisible(true);

		// Fade in animation
		FadeTransition fadeIn = new FadeTransition(Duration.millis(250), overlayPane);
		fadeIn.setFromValue(0);
		fadeIn.setToValue(1);
		fadeIn.play();

		isVisible = true;
	}

	/**
	 * Hides the overlay with a fade-out animation
	 */
	public void hide() {
		if (!isInitialized || !isVisible)
			return;

		// Fade out animation
		FadeTransition fadeOut = new FadeTransition(Duration.millis(250), overlayPane);
		fadeOut.setFromValue(1);
		fadeOut.setToValue(0);
		fadeOut.setOnFinished(e -> overlayPane.setVisible(false));
		fadeOut.play();

		isVisible = false;
	}

	/**
	 * Updates the task progress display
	 */
	/**
	 * Updates the task progress display using a standard ProgressBar
	 */
	public void updateTaskProgress() {
		if (!isInitialized || !isVisible)
			return;

		// Get current task completion percentage
		double progressValue = GameWindow.getTotalPercentage() / 100;
		String progressText = String.format("%.0f%%", progressValue * 100);

		// Update UI on JavaFX thread
		Platform.runLater(() -> {
			// Update progress text
			taskProgressText.setText(progressText);

			// Update the progress bar
			taskProgressBar.setProgress(progressValue);
			VBox taskListDisplay = createTaskListDisplay();
			if (taskContainer.getChildren().size() > 1) {
				taskContainer.getChildren().remove(1);
			}
			taskContainer.getChildren().add(taskListDisplay);
			// Update progress bar color based on completion
			if (progressValue > 0.8) {
				// Green for high completion
				taskProgressBar.setStyle("-fx-accent: #4CAF50;" + // Green progress
						"-fx-control-inner-background: #e0e0e0;" // Light gray background
				);
				taskProgressText.setFill(Color.rgb(76, 175, 80));
			} else if (progressValue > 0.5) {
				// Yellow for medium completion
				taskProgressBar.setStyle("-fx-accent: #FFC107;" + // Yellow progress
						"-fx-control-inner-background: #e0e0e0;" // Light gray background
				);
				taskProgressText.setFill(Color.rgb(255, 193, 7));
			} else {
				// Blue for low completion
				taskProgressBar.setStyle("-fx-accent: #2196F3;" + // Blue progress
						"-fx-control-inner-background: #e0e0e0;" // Light gray background
				);
				taskProgressText.setFill(Color.rgb(33, 150, 243));
			}
		});
	}

	/**
	 * Updates the buttons based on the player's current role
	 */
	public void updatePlayerRoleUI() {
		if (!isInitialized)
			return;

		Platform.runLater(() -> {
			boolean isImposter = "imposter".equals(PlayerLogic.getStatus());
			boolean isDead = "dead".equals(PlayerLogic.getStatus());

			// Style definitions
			String baseButtonStyle = "-fx-background-color: #1e90ff;" + "-fx-text-fill: white;"
					+ "-fx-font-family: 'Monospace';" + "-fx-font-size: 16px;" + "-fx-font-weight: bold;"
					+ "-fx-padding: 8 20 8 20;" + "-fx-border-color: #87cefa;" + "-fx-border-width: 2px;"
					+ "-fx-background-radius: 0;" + "-fx-border-radius: 0;"
					+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);";

			String imposterButtonStyle = "-fx-background-color: #d32f2f;" + "-fx-text-fill: white;"
					+ "-fx-font-family: 'Monospace';" + "-fx-font-size: 16px;" + "-fx-font-weight: bold;"
					+ "-fx-padding: 8 20 8 20;" + "-fx-border-color: #ff6347;" + "-fx-border-width: 2px;"
					+ "-fx-background-radius: 0;" + "-fx-border-radius: 0;"
					+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);";

			String ghostButtonStyle = "-fx-background-color: #424242;" + "-fx-text-fill: #aaaaaa;"
					+ "-fx-font-family: 'Monospace';" + "-fx-font-size: 16px;" + "-fx-font-weight: bold;"
					+ "-fx-padding: 8 20 8 20;" + "-fx-border-color: #666666;" + "-fx-border-width: 2px;"
					+ "-fx-background-radius: 0;" + "-fx-border-radius: 0;"
					+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 1);";

			// Update action button
			if (isDead) {
				// Ghost appearance
				actionButton.setText("INTERACT [F]");
				actionButton.setStyle(ghostButtonStyle);
				reportButton.setStyle(ghostButtonStyle);
			} else if (isImposter) {
				// Imposter appearance
				actionButton.setText("KILL [F]");
				actionButton.setStyle(imposterButtonStyle);
				reportButton.setStyle(baseButtonStyle);
			} else {
				// Crewmate appearance
				actionButton.setText("INTERACT [F]");
				actionButton.setStyle(baseButtonStyle);
				reportButton.setStyle(baseButtonStyle);
			}

			// Update hover handlers based on new state
			updateButtonHoverHandlers(isImposter, isDead);
		});
	}

	/**
	 * Updates the hover handlers for buttons based on player role
	 */
	private void updateButtonHoverHandlers(boolean isImposter, boolean isDead) {
		// Hover styles
		String hoverButtonStyle = "-fx-background-color: #00bfff;" + "-fx-text-fill: white;"
				+ "-fx-font-family: 'Monospace';" + "-fx-font-size: 16px;" + "-fx-font-weight: bold;"
				+ "-fx-padding: 8 20 8 20;" + "-fx-border-color: #b0e2ff;" + "-fx-border-width: 2px;"
				+ "-fx-background-radius: 0;" + "-fx-border-radius: 0;"
				+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 8, 0, 0, 1);";

		String baseButtonStyle = "-fx-background-color: #1e90ff;" + "-fx-text-fill: white;"
				+ "-fx-font-family: 'Monospace';" + "-fx-font-size: 16px;" + "-fx-font-weight: bold;"
				+ "-fx-padding: 8 20 8 20;" + "-fx-border-color: #87cefa;" + "-fx-border-width: 2px;"
				+ "-fx-background-radius: 0;" + "-fx-border-radius: 0;"
				+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);";

		String imposterButtonStyle = "-fx-background-color: #d32f2f;" + "-fx-text-fill: white;"
				+ "-fx-font-family: 'Monospace';" + "-fx-font-size: 16px;" + "-fx-font-weight: bold;"
				+ "-fx-padding: 8 20 8 20;" + "-fx-border-color: #ff6347;" + "-fx-border-width: 2px;"
				+ "-fx-background-radius: 0;" + "-fx-border-radius: 0;"
				+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);";

		String imposterHoverStyle = "-fx-background-color: #ff1744;" + "-fx-text-fill: white;"
				+ "-fx-font-family: 'Monospace';" + "-fx-font-size: 16px;" + "-fx-font-weight: bold;"
				+ "-fx-padding: 8 20 8 20;" + "-fx-border-color: #ff8a80;" + "-fx-border-width: 2px;"
				+ "-fx-background-radius: 0;" + "-fx-border-radius: 0;"
				+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 8, 0, 0, 1);";

		String ghostButtonStyle = "-fx-background-color: #424242;" + "-fx-text-fill: #aaaaaa;"
				+ "-fx-font-family: 'Monospace';" + "-fx-font-size: 16px;" + "-fx-font-weight: bold;"
				+ "-fx-padding: 8 20 8 20;" + "-fx-border-color: #666666;" + "-fx-border-width: 2px;"
				+ "-fx-background-radius: 0;" + "-fx-border-radius: 0;"
				+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 1);";

		String ghostHoverStyle = "-fx-background-color: #555555;" + "-fx-text-fill: #bbbbbb;"
				+ "-fx-font-family: 'Monospace';" + "-fx-font-size: 16px;" + "-fx-font-weight: bold;"
				+ "-fx-padding: 8 20 8 20;" + "-fx-border-color: #777777;" + "-fx-border-width: 2px;"
				+ "-fx-background-radius: 0;" + "-fx-border-radius: 0;"
				+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 8, 0, 0, 1);";

		// Clear previous handlers
		actionButton.setOnMouseEntered(null);
		actionButton.setOnMouseExited(null);
		reportButton.setOnMouseEntered(null);
		reportButton.setOnMouseExited(null);

		// Set appropriate hover styles
		if (isDead) {
			// Ghost hover styles
			actionButton.setOnMouseEntered(e -> actionButton.setStyle(ghostHoverStyle));
			actionButton.setOnMouseExited(e -> actionButton.setStyle(ghostButtonStyle));

			reportButton.setOnMouseEntered(e -> reportButton.setStyle(ghostHoverStyle));
			reportButton.setOnMouseExited(e -> reportButton.setStyle(ghostButtonStyle));
		} else if (isImposter) {
			// Imposter hover styles for action button
			actionButton.setOnMouseEntered(e -> actionButton.setStyle(imposterHoverStyle));
			actionButton.setOnMouseExited(e -> actionButton.setStyle(imposterButtonStyle));

			// Regular hover styles for report button
			reportButton.setOnMouseEntered(e -> reportButton.setStyle(hoverButtonStyle));
			reportButton.setOnMouseExited(e -> reportButton.setStyle(baseButtonStyle));
		} else {
			// Crewmate hover styles
			actionButton.setOnMouseEntered(e -> actionButton.setStyle(hoverButtonStyle));
			actionButton.setOnMouseExited(e -> actionButton.setStyle(baseButtonStyle));

			reportButton.setOnMouseEntered(e -> reportButton.setStyle(hoverButtonStyle));
			reportButton.setOnMouseExited(e -> reportButton.setStyle(baseButtonStyle));
		}
	}

	public void updateButtonStates() {
		// Skip if overlay isn't visible or player is dead
		if (!isVisible || !isInitialized)
			return;

		boolean playerIsDead = "dead".equalsIgnoreCase(PlayerLogic.getStatus());
		boolean playerIsImposter = "imposter".equalsIgnoreCase(PlayerLogic.getStatus());

		Platform.runLater(() -> {
			// Update action button based on player role and nearby opportunities
			if (playerIsDead) {
				// When dead (ghost), can still do tasks but can't kill/report
				boolean hasTasks = gameWindow.hasNearbyInteractiveTask();
				actionButton.setDisable(!hasTasks);
				reportButton.setDisable(true);

				// Update appearance based on availability
				if (hasTasks) {
					actionButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;"
							+ "-fx-font-family: 'Monospace'; -fx-font-size: 16px; -fx-font-weight: bold;"
							+ "-fx-padding: 8 20 8 20; -fx-border-color: #81C784; -fx-border-width: 2px;"
							+ "-fx-background-radius: 0; -fx-border-radius: 0;"
							+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);");
				} else {
					actionButton.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: #E0E0E0;"
							+ "-fx-font-family: 'Monospace'; -fx-font-size: 16px; -fx-font-weight: bold;"
							+ "-fx-padding: 8 20 8 20; -fx-border-color: #BDBDBD; -fx-border-width: 2px;"
							+ "-fx-background-radius: 0; -fx-border-radius: 0;");
				}
			} else if (playerIsImposter) {
				// Imposters can kill nearby players
				boolean canKill = gameWindow.hasNearbyKillablePlayer();
				actionButton.setDisable(!canKill);

				// Update appearance based on availability
				if (canKill) {
					// Bright red for available kill
					actionButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;"
							+ "-fx-font-family: 'Monospace'; -fx-font-size: 16px; -fx-font-weight: bold;"
							+ "-fx-padding: 8 20 8 20; -fx-border-color: #EF5350; -fx-border-width: 2px;"
							+ "-fx-background-radius: 0; -fx-border-radius: 0;"
							+ "-fx-effect: dropshadow(three-pass-box, rgba(255,0,0,0.5), 8, 0, 0, 0);");
				} else {
					// Dark gray for unavailable kill
					actionButton.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: #E0E0E0;"
							+ "-fx-font-family: 'Monospace'; -fx-font-size: 16px; -fx-font-weight: bold;"
							+ "-fx-padding: 8 20 8 20; -fx-border-color: #BDBDBD; -fx-border-width: 2px;"
							+ "-fx-background-radius: 0; -fx-border-radius: 0;");
				}

				// Update report button
				boolean canReport = gameWindow.hasNearbyCorpse();
				reportButton.setDisable(!canReport);

				if (canReport) {
					// Yellow for available report
					reportButton.setStyle("-fx-background-color: #FFC107; -fx-text-fill: #212121;"
							+ "-fx-font-family: 'Monospace'; -fx-font-size: 16px; -fx-font-weight: bold;"
							+ "-fx-padding: 8 20 8 20; -fx-border-color: #FFD54F; -fx-border-width: 2px;"
							+ "-fx-background-radius: 0; -fx-border-radius: 0;"
							+ "-fx-effect: dropshadow(three-pass-box, rgba(255,193,7,0.5), 8, 0, 0, 0);");
				} else {
					// Gray for unavailable report
					reportButton.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: #E0E0E0;"
							+ "-fx-font-family: 'Monospace'; -fx-font-size: 16px; -fx-font-weight: bold;"
							+ "-fx-padding: 8 20 8 20; -fx-border-color: #BDBDBD; -fx-border-width: 2px;"
							+ "-fx-background-radius: 0; -fx-border-radius: 0;");
				}
			} else {
				// Crewmates can do tasks
				boolean hasTasks = gameWindow.hasNearbyInteractiveTask();
				actionButton.setDisable(!hasTasks);

				if (hasTasks) {
					// Blue for available task
					actionButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;"
							+ "-fx-font-family: 'Monospace'; -fx-font-size: 16px; -fx-font-weight: bold;"
							+ "-fx-padding: 8 20 8 20; -fx-border-color: #64B5F6; -fx-border-width: 2px;"
							+ "-fx-background-radius: 0; -fx-border-radius: 0;"
							+ "-fx-effect: dropshadow(three-pass-box, rgba(33,150,243,0.5), 8, 0, 0, 0);");
				} else {
					// Gray for unavailable task
					actionButton.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: #E0E0E0;"
							+ "-fx-font-family: 'Monospace'; -fx-font-size: 16px; -fx-font-weight: bold;"
							+ "-fx-padding: 8 20 8 20; -fx-border-color: #BDBDBD; -fx-border-width: 2px;"
							+ "-fx-background-radius: 0; -fx-border-radius: 0;");
				}

				// Update report button
				boolean canReport = gameWindow.hasNearbyCorpse();
				reportButton.setDisable(!canReport);

				if (canReport) {
					// Yellow for available report
					reportButton.setStyle("-fx-background-color: #FFC107; -fx-text-fill: #212121;"
							+ "-fx-font-family: 'Monospace'; -fx-font-size: 16px; -fx-font-weight: bold;"
							+ "-fx-padding: 8 20 8 20; -fx-border-color: #FFD54F; -fx-border-width: 2px;"
							+ "-fx-background-radius: 0; -fx-border-radius: 0;"
							+ "-fx-effect: dropshadow(three-pass-box, rgba(255,193,7,0.5), 8, 0, 0, 0);");
				} else {
					// Gray for unavailable report
					reportButton.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: #E0E0E0;"
							+ "-fx-font-family: 'Monospace'; -fx-font-size: 16px; -fx-font-weight: bold;"
							+ "-fx-padding: 8 20 8 20; -fx-border-color: #BDBDBD; -fx-border-width: 2px;"
							+ "-fx-background-radius: 0; -fx-border-radius: 0;");
				}
			}
		});
	}

	/**
	 * Creates a visual flash effect on a button
	 */
	private void flashButton(Button button, String flashStyle) {
		String originalStyle = button.getStyle();
		button.setStyle(flashStyle);

		new Thread(() -> {
			try {
				Thread.sleep(200);
				Platform.runLater(() -> button.setStyle(originalStyle));
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}).start();
	}
}
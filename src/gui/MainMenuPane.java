package gui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import logic.State;

public class MainMenuPane extends Pane {
	private static final String VERSION = "v.3.001";
	private Button newGameButton;
	private Button optionButton;
	private Button loadGameButton;
	private Button helpButton;
	private Button exitButton;
	private VBox menuVBox;
	private boolean isGameOptionsVisible = false;
	private static TextField InputName;
	private static Button hostButton;
	private static Button joinButton;
	private static Button testButton;
	private static Button testButton2;
	private TaskGui activeTaskGui;
	Scene Curscene;
	Stage thisStage;
	private static State thisState = logic.State.IDLE;

	public MainMenuPane(Stage primaryStage, double width, double height) {
		setPrefSize(width, height);
		thisStage = primaryStage;
		Background background;
		try {
			BackgroundImage backgroundImage = new BackgroundImage(
					new Image(getClass().getResourceAsStream("/MainMenu.png")), BackgroundRepeat.NO_REPEAT,
					BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
					new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true));
			background = new Background(backgroundImage);
		} catch (Exception e) {
			background = new Background(new BackgroundFill(Color.web("#1A1A1A"), CornerRadii.EMPTY, Insets.EMPTY));
		}

		Text titleText = new Text("AMONG CEDT " + VERSION);
		titleText.setFont(Font.font("Helvetica", FontWeight.LIGHT, 28));
		titleText.setFill(Color.WHITE);
		titleText.setOpacity(0.9);
		titleText.setTextAlignment(TextAlignment.CENTER);

		newGameButton = createMenuButton("NEW GAME");
		optionButton = createMenuButton("OPTIONS");
		loadGameButton = createMenuButton("ABOUT");
		helpButton = createMenuButton("HELP");
		exitButton = createMenuButton("EXIT");
		testButton = createMenuButton("TEST TASK");
		testButton2 = createMenuButton("Test Setup");

		menuVBox = new VBox(10, titleText, newGameButton, optionButton, loadGameButton, helpButton, testButton,
				testButton2);
		menuVBox.setAlignment(Pos.TOP_LEFT);
		menuVBox.setPadding(new Insets(40, 10, 10, 40));

		Region darkOverlay = new Region();
		darkOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);");
		darkOverlay.prefWidthProperty().bind(primaryStage.widthProperty());
		darkOverlay.prefHeightProperty().bind(primaryStage.heightProperty());

		StackPane overlayPane = new StackPane(darkOverlay);

		GridPane gridPane = new GridPane();
		gridPane.setBackground(background);
		gridPane.add(overlayPane, 0, 0, 1, 2);
		gridPane.add(menuVBox, 0, 0);
		GridPane.setHalignment(menuVBox, HPos.LEFT);
		GridPane.setMargin(exitButton, new Insets(0, 0, 20, 20));
		gridPane.add(exitButton, 0, 1);

		getChildren().add(gridPane);
	}

	private Button createMenuButton(String text) {
		Button button = new Button(text);

		// Set button properties
		button.setPrefWidth(200);
		button.setPrefHeight(40);
		button.setMnemonicParsing(false);

		// Define the button style
		button.setStyle("-fx-background-color: transparent;" + "-fx-text-fill: white;" + "-fx-font-family: 'Helvetica';"
				+ "-fx-font-size: 16px;" + "-fx-font-weight: normal;" + "-fx-alignment: center;" + "-fx-opacity: 0.8;");

		// Define button actions
		if (text.equals("NEW GAME")) {
			button.setOnAction(e -> showGameOptions());
		} else if (text.equals("OPTIONS")) {
			button.setOnAction(e -> System.out.println("Option clicked"));
		} else if (text.equals("ABOUT")) {
			button.setOnAction(e -> System.out.println("ABOUT clicked"));
		} else if (text.equals("HELP")) {
			button.setOnAction(e -> System.out.println("HELP clicked"));
		} else if (text.equals("EXIT")) {
			button.setOnAction(e -> showMainMenu());
		} else if (text.equals("HOST")) {
			button.setOnAction(e -> {
				thisState = logic.State.SERVER;
				openServerGui(thisState);
			});
		} else if (text.equals("JOIN")) {
			button.setOnAction(e -> {
				thisState = logic.State.CLIENT;
				openServerGui(thisState);
			});
		} else if (text.equals("TEST TASK")) {
			button.setOnAction(e -> task(Integer.parseInt(InputName.getText().isEmpty() ? "1" : InputName.getText())));
		} else if (text.equals("Test Setup")) {
			button.setOnAction(e -> {
				MatchSetupPane setup = new MatchSetupPane();
				this.getChildren().add(setup);
			});

		}
		// Add hover effect
		button.setOnMouseEntered(e -> {
			button.setStyle("-fx-background-color: transparent;" + "-fx-text-fill: white;"
					+ "-fx-font-family: 'Helvetica';" + "-fx-font-size: 16px;" + "-fx-font-weight: normal;"
					+ "-fx-alignment: center;" + "-fx-opacity: 1.0;");
		});

		button.setOnMouseExited(e -> {
			button.setStyle("-fx-background-color: transparent;" + "-fx-text-fill: white;"
					+ "-fx-font-family: 'Helvetica';" + "-fx-font-size: 16px;" + "-fx-font-weight: normal;"
					+ "-fx-alignment: center;" + "-fx-opacity: 0.8;");
		});

		return button;
	}

	private void showGameOptions() {
		if (isGameOptionsVisible)
			return;
		isGameOptionsVisible = true;

		exitButton.setText("BACK TO MAIN");

		// Create host and join buttons
		hostButton = createMenuButton("HOST");
		joinButton = createMenuButton("JOIN");
		InputName = new TextField();
		InputName.setPromptText("Enter your name");
		InputName.setPrefSize(200, 40); // Adjust width to be smaller
		InputName.setMaxWidth(200); // Ensure it doesn’t stretch
		InputName.setAlignment(Pos.CENTER);

		// Adjust styles to ensure prompt text is visible
		InputName.setStyle("-fx-background-color: transparent;" + "-fx-border-color: white;" + "-fx-border-width: 2px;"
				+ "-fx-text-fill: white;" + "-fx-font-family: 'Helvetica';" + "-fx-font-size: 16px;"
				+ "-fx-font-weight: normal;" + "-fx-opacity: 1.0;");

		// Ensure prompt text is visible
		InputName.setStyle("-fx-prompt-text-fill: rgba(255,255,255,0.6);");

		// Add hover effect like buttons
		InputName.setOnMouseEntered(e -> InputName
				.setStyle("-fx-background-color: transparent;" + "-fx-border-color: white;" + "-fx-border-width: 2px;"
						+ "-fx-text-fill: white;" + "-fx-font-family: 'Helvetica';" + "-fx-font-size: 16px;"
						+ "-fx-font-weight: normal;" + "-fx-alignment: center;" + "-fx-opacity: 1.0;"));

		InputName.setOnMouseExited(e -> InputName
				.setStyle("-fx-background-color: transparent;" + "-fx-border-color: white;" + "-fx-border-width: 2px;"
						+ "-fx-text-fill: white;" + "-fx-font-family: 'Helvetica';" + "-fx-font-size: 16px;"
						+ "-fx-font-weight: normal;" + "-fx-alignment: center;" + "-fx-opacity: 0.8;"));

		// Initially set opacity to 0 for fade-in effect
		hostButton.setOpacity(0);
		joinButton.setOpacity(0);

		// Remove the NEW GAME button with a fade out effect
		FadeTransition fadeOutNewGame = new FadeTransition(Duration.millis(10), newGameButton);
		fadeOutNewGame.setFromValue(1.0);
		fadeOutNewGame.setToValue(0.0);
		fadeOutNewGame.setOnFinished(event -> {

			// Remove the NEW GAME button but keep others
			menuVBox.getChildren().remove(newGameButton);

			// Add the host and join buttons right after the title
			menuVBox.getChildren().add(1, InputName);
			menuVBox.getChildren().add(2, hostButton);
			menuVBox.getChildren().add(3, joinButton);
			menuVBox.requestFocus();
			// Move other buttons down by adding spacing after join button
			VBox.setMargin(optionButton, new Insets(15, 0, 0, 0));

			// Fade in the HOST and JOIN buttons
			FadeTransition fadeInHost = new FadeTransition(Duration.millis(400), hostButton);
			fadeInHost.setFromValue(0.0);
			fadeInHost.setToValue(0.8);
			fadeInHost.play();

			FadeTransition fadeInJoin = new FadeTransition(Duration.millis(400), joinButton);
			fadeInJoin.setFromValue(0.0);
			fadeInJoin.setToValue(0.8);
			fadeInJoin.play();
		});
		fadeOutNewGame.play();
	}

	private void showMainMenu() { // Linked to Exit button
		if (!isGameOptionsVisible) { // on main menu not selected play
			Platform.exit();
		} else { // Player pressed Play
			isGameOptionsVisible = false;

			// Change BACK button to EXIT
			exitButton.setText("EXIT");
			ServerSelectGui.stopGame();
			for (int i = 0; i < this.getChildren().size(); i++) {
				if (this.getChildren().get(i) instanceof ServerSelectGui) {
					ServerSelectGui sgui = (ServerSelectGui) this.getChildren().get(i);
					this.getChildren().remove(sgui);
				}
			}
			// Find the host and join buttons
			Button hostButton = null;
			Button joinButton = null;
			for (int i = 0; i < menuVBox.getChildren().size(); i++) {
				if (menuVBox.getChildren().get(i) instanceof Button) {
					Button btn = (Button) menuVBox.getChildren().get(i);
					if (btn.getText().equals("HOST")) {
						hostButton = btn;
					} else if (btn.getText().equals("JOIN")) {
						joinButton = btn;
					}
				}
			}

			TextField InputName = null;
			for (int i = 0; i < menuVBox.getChildren().size(); i++) {
				if (menuVBox.getChildren().get(i) instanceof TextField) {
					TextField fid = (TextField) menuVBox.getChildren().get(i);
					if (fid.getPromptText().equals("Enter your name")) {
						InputName = fid;
					}
				}
			}

			// Recreate the NEW GAME button
			newGameButton = createMenuButton("NEW GAME");
			newGameButton.setOpacity(0);

			// Remove margin from options button
			VBox.setMargin(optionButton, new Insets(0, 0, 0, 0));

			// Remove host and join buttons, add NEW GAME button back
			if (InputName != null)
				menuVBox.getChildren().remove(InputName);
			if (hostButton != null)
				menuVBox.getChildren().remove(hostButton);
			if (joinButton != null)
				menuVBox.getChildren().remove(joinButton);
			// Add the NEW GAME button back at position 1 (after title)
			menuVBox.getChildren().add(1, newGameButton);

			// Fade in the NEW GAME button
			FadeTransition fadeInNewGame = new FadeTransition(Duration.millis(400), newGameButton);
			fadeInNewGame.setFromValue(0.0);
			fadeInNewGame.setToValue(0.8);
			fadeInNewGame.play();
		}
	}

	private void openServerGui(State state) {

		ServerSelectGui serverGui = new ServerSelectGui(state, thisStage);

		// Set the width of the ServerGui (adjust as needed)

		// Position it on the right side of the screen
		serverGui.setLayoutX(this.getWidth() - 620);
		serverGui.setLayoutY(100);

		// Add it to the MainMenuPane
		this.getChildren().add(serverGui);
	}

	public static String getPlayerName() {
		return InputName.getText().isEmpty() ? "Player" : InputName.getText();
	}

	public static String getServerName() {
		return InputName.getText().isEmpty() ? "Host" : InputName.getText();
	}

	private void task(int taskId) {
		if (activeTaskGui != null) {
			this.getChildren().remove(activeTaskGui);
		}

		activeTaskGui = new TaskGui(taskId);
		activeTaskGui.setLayoutX(this.getWidth() / 4);
		activeTaskGui.setLayoutY(this.getHeight() / 10);

		this.getChildren().add(activeTaskGui);
	}

	public void taskClose(boolean success) {
		if (activeTaskGui != null) {
			getChildren().remove(activeTaskGui);
			activeTaskGui = null;
		}
		if (success) {
			StackPane successOverlay = new StackPane();
			Rectangle background = new Rectangle(this.getWidth(), this.getHeight(), Color.rgb(0, 0, 0, 0.5)); // 50%
																												// transparent
																												// black
			Text successText = new Text("Task Success!");
			successText.setFont(new Font(30));
			successText.setFill(Color.WHITE);
			successText.setTextAlignment(TextAlignment.CENTER);

			successOverlay.getChildren().addAll(background, successText);
			this.getChildren().add(successOverlay);
			new Thread(() -> {
				try {
					Thread.sleep(2000); // Wait 2 seconds
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Platform.runLater(() -> this.getChildren().remove(successOverlay));
			}).start();
		}
	}

	public static void setNameDisable(boolean disable) {
		InputName.setDisable(disable);
	}

	public static void setHostDisable(boolean disable) {
		hostButton.setDisable(disable);
	}

	public static void setJoinDisable(boolean disable) {
		joinButton.setDisable(disable);
	}

	public static State getState() {
		return thisState;
	}

}
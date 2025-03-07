package gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import logic.GameLogic;
import logic.PlayerLogic;
import server.PlayerInfo;

public class CharacterSelectgui extends VBox {
	private ImageView characterImageView;
	private int curChar = PlayerLogic.getCharID();
	private static final String[] CHARACTER_NAMES = { "Alex", "Casey", "Taylor", "Jordan", "Morgan", "Riley", "Avery",
			"Cameron", "Quinn", "Rowan" };
	private static final String[] CHARACTER_IMAGES = { "/player/profile/01.png", "/player/profile/02.png",
			"/player/profile/03.png", "/player/profile/04.png", "/player/profile/05.png", "/player/profile/06.png",
			"/player/profile/07.png", "/player/profile/08.png", "/player/profile/09.png", "/player/profile/10.png" };
	private Button selectButton;
	private TextField name;

	public CharacterSelectgui(Runnable onCharacterSelectedCallback) {

		// Set fixed size
		setPrefWidth(300);
		setPrefHeight(400);
		setMaxWidth(300);
		setMaxHeight(400);

		setPadding(new Insets(20));
		setSpacing(15);
		setAlignment(Pos.CENTER);

		// Match game UI with darker background and blue accents
		setStyle("-fx-background-color: rgba(30, 30, 50, 0.95);"); // Dark blue-gray background

		// Create border with sharp corners to match pixel art style
		setBorder(new Border(new BorderStroke(Paint.valueOf("#1e90ff"), // Blue border color to match button
				BorderStrokeStyle.SOLID, new CornerRadii(0), // Sharp corners
				new BorderWidths(3))));

		Text title = new Text("Select Character");
		title.setFill(Paint.valueOf("#87cefa")); // Light blue text
		title.setFont(Font.font("Monospace", javafx.scene.text.FontWeight.BOLD, 20)); // Pixel-style font

		// Shadow effect for title text to make it pop
		title.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 3, 0, 0, 0);");

		characterImageView = new ImageView();
		characterImageView.setFitHeight(200);
		characterImageView.setFitWidth(150);
		characterImageView.setPreserveRatio(true);
		Image image = new Image(CHARACTER_IMAGES[curChar]);
		characterImageView.setImage(image);

		// Add a stylized border to the character image
		characterImageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);"
				+ "-fx-border-color: #1e90ff;" + "-fx-border-width: 2px;" + "-fx-background-color: #2a2a3a;");

		// Button styling
		String baseButtonStyle = "-fx-background-color: #1e90ff;" + "-fx-text-fill: white;"
				+ "-fx-font-family: 'Monospace';" + "-fx-font-size: 14px;" + "-fx-font-weight: bold;"
				+ "-fx-padding: 5 15 5 15;" + "-fx-border-color: #87cefa;" + "-fx-border-width: 2px;"
				+ "-fx-background-radius: 0;" + "-fx-border-radius: 0;"
				+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);";

		String hoverButtonStyle = "-fx-background-color: #00bfff;" + "-fx-text-fill: white;"
				+ "-fx-font-family: 'Monospace';" + "-fx-font-size: 12px;" + "-fx-font-weight: bold;"
				+ "-fx-padding: 5 10 5 10;" + "-fx-border-color: #b0e2ff;" + "-fx-border-width: 2px;"
				+ "-fx-background-radius: 0;" + "-fx-border-radius: 0;"
				+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 8, 0, 0, 1);";

		Button leftArrow = new Button("<");
		Button rightArrow = new Button(">");
		selectButton = new Button("Select");

		// Configure name field to match game style
		name = new TextField(CHARACTER_NAMES[curChar]);
		name.setPrefWidth(150);
		name.setEditable(false);
		name.setAlignment(Pos.CENTER);
		name.setStyle("-fx-background-color: #2a2a3a;" + "-fx-text-fill: white;" + "-fx-font-family: 'Monospace';"
				+ "-fx-font-weight: bold;" + "-fx-font-size: 14px;" + "-fx-padding: 5 10 5 10;"
				+ "-fx-border-color: #1e90ff;" + "-fx-border-width: 2px;" + "-fx-background-radius: 0;"
				+ "-fx-border-radius: 0;");

		// Make select button wider to fit status text
		selectButton.setPrefWidth(170);

		// Apply styles to all buttons
		leftArrow.setStyle(baseButtonStyle);
		rightArrow.setStyle(baseButtonStyle);
		selectButton.setStyle(baseButtonStyle);

		// Add hover effects to all buttons
		leftArrow.setOnMouseEntered(e -> leftArrow.setStyle(hoverButtonStyle));
		leftArrow.setOnMouseExited(e -> leftArrow.setStyle(baseButtonStyle));

		rightArrow.setOnMouseEntered(e -> rightArrow.setStyle(hoverButtonStyle));
		rightArrow.setOnMouseExited(e -> rightArrow.setStyle(baseButtonStyle));

		selectButton.setOnMouseEntered(e -> {
			if (!selectButton.isDisabled()) {
				selectButton.setStyle(hoverButtonStyle);
			}
		});
		selectButton.setOnMouseExited(e -> {
			if (!selectButton.isDisabled()) {
				selectButton.setStyle(baseButtonStyle);
			}
		});

		leftArrow.setOnAction(e -> {
			curChar = (curChar > 0) ? curChar - 1 : 0;
			updateCharacterPreview();
		});

		rightArrow.setOnAction(e -> {
			curChar = (curChar < 9) ? curChar + 1 : 9;
			updateCharacterPreview();
		});

		selectButton.setOnAction(e -> {
			PlayerLogic.setCharID(curChar);
		    updateCharacterPreview();
			if (onCharacterSelectedCallback != null) {
				onCharacterSelectedCallback.run();
			}
		});

		HBox buttons = new HBox(10, leftArrow, name, rightArrow);
		buttons.setAlignment(Pos.CENTER);

		VBox vBox = new VBox(15);
		vBox.setAlignment(Pos.CENTER);
		vBox.getChildren().addAll(title, characterImageView, buttons, selectButton);

		getChildren().add(vBox);
		updateCharacterPreview();
	}

	private void updateCharacterPreview() {
		if (curChar >= 0 && curChar < CHARACTER_IMAGES.length) {
			Image image = new Image(CHARACTER_IMAGES[curChar]);
			characterImageView.setImage(image);
			name.setText(CHARACTER_NAMES[curChar]);
		}

		boolean isDuplicate = false;
		for (String key : GameLogic.playerList.keySet()) {
			PlayerInfo info = GameLogic.playerList.get(key);
			if (info.getCharacterID() == curChar) {
				isDuplicate = true;
				break;
			}
		}

		String disabledButtonStyle = "-fx-font-family: 'Monospace';" + "-fx-font-size: 11px;" + "-fx-font-weight: bold;"
				+ "-fx-padding: 5 5 5 5;" + "-fx-background-radius: 0;" + "-fx-border-radius: 0;"
				+ "-fx-border-width: 2px;";

		String baseButtonStyle = "-fx-background-color: #1e90ff;" + "-fx-text-fill: white;"
				+ "-fx-font-family: 'Monospace';" + "-fx-font-size: 14px;" + "-fx-font-weight: bold;"
				+ "-fx-padding: 5 15 5 15;" + "-fx-border-color: #87cefa;" + "-fx-border-width: 2px;"
				+ "-fx-background-radius: 0;" + "-fx-border-radius: 0;"
				+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);";

		if (isDuplicate) {
			selectButton.setDisable(true);
			selectButton.setText("Character Taken");
			selectButton.setStyle(disabledButtonStyle + "-fx-background-color: #8B0000;" + // Dark red for taken
																							// characters
					"-fx-text-fill: #CCCCCC;" + "-fx-border-color: #FF6347;"); // Tomato border
		} else if (PlayerLogic.getCharID() == curChar) {
			selectButton.setDisable(true);
			selectButton.setText("Currently Selected");
			selectButton.setStyle(disabledButtonStyle + "-fx-background-color: #006400;" + // Dark green for selected
																							// character
					"-fx-text-fill: #CCCCCC;" + "-fx-border-color: #4CAF50;"); // Green border
		} else {
			selectButton.setDisable(false);
			selectButton.setText("Select");
			selectButton.setStyle(baseButtonStyle);
		}
	}
}
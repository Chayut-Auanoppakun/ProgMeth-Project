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

public class MatchSetupPane extends VBox {
	private ImageView characterImageView;
	private int curChar = 0;
	private static final String[] CHARACTER_NAMES = { "Alex", "Casey", "Taylor", "Jordan", "Morgan", "Riley", "Avery",
			"Cameron", "Quinn", "Rowan" };
	private static final String[] CHARACTER_IMAGES = { "/player/profile/01.png", "/player/profile/02.png",
			"/player/profile/03.png", "/player/profile/04.png", "/player/profile/05.png", "/player/profile/06.png",
			"/player/profile/07.png", "/player/profile/08.png", "/player/profile/09.png", "/player/profile/10.png" };
	private Button selectButton;
	private TextField name;

	public MatchSetupPane(Runnable onCharacterSelectedCallback) {
		setPadding(new Insets(10));
		setSpacing(10);
		setAlignment(Pos.CENTER);
		setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);"); // Semi-transparent background

		// Create border
		setBorder(new Border(new BorderStroke(Paint.valueOf("gray"), BorderStrokeStyle.SOLID, new CornerRadii(10),
				new BorderWidths(2))));

		Button LeftArrow = new Button("<");
		Button RightArrow = new Button(">");
		selectButton = new Button("Select");
		name = new TextField();
		Text title = new Text();
		title.setText("Select Character");
		title.setFill(Paint.valueOf("white")); // Set text color to white
		title.setFont(Font.font("System", javafx.scene.text.FontWeight.BOLD, 20)); // Set text to bold
		name.setPrefWidth(150); // Set preferred width
		name.setEditable(false); // Make the text field uneditable
		name.setAlignment(Pos.CENTER); // Center the text
		selectButton.setPrefWidth(150);

		// Style buttons
		String buttonStyle = "-fx-background-color: #007ACC; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 15 5 15;";
		LeftArrow.setStyle(buttonStyle);
		RightArrow.setStyle(buttonStyle);
		selectButton.setStyle(buttonStyle);

		LeftArrow.setOnAction(e -> {
			curChar--;
			if (curChar < 0)
				curChar = 0;
			updateCharacterPreview();
			name.setText(CHARACTER_NAMES[curChar]);
		});

		RightArrow.setOnAction(e -> {
			curChar++;
			if (curChar > 9)
				curChar = 9;
			updateCharacterPreview();
			name.setText(CHARACTER_NAMES[curChar]);
		});

		selectButton.setOnAction(e -> {
			PlayerLogic.setCharID(curChar);
			updateCharacterPreview();
			if (onCharacterSelectedCallback != null) {
				onCharacterSelectedCallback.run();
			}
		});

		name.setText(CHARACTER_NAMES[curChar]);
		HBox buttons = new HBox(10, LeftArrow, name, RightArrow);
		buttons.setAlignment(Pos.CENTER);

		characterImageView = new ImageView();
		characterImageView.setFitHeight(200);
		characterImageView.setFitWidth(150);
		Image image = new Image(CHARACTER_IMAGES[curChar]);
		characterImageView.setImage(image);

		VBox vBox = new VBox(10, title, characterImageView, buttons, selectButton);
		vBox.setAlignment(Pos.CENTER);

		getChildren().add(vBox);

		startCharacterUpdateThread(); //this is just debug
		updateCharacterPreview();
	}

	private void updateCharacterPreview() {
		if (curChar >= 0 && curChar < CHARACTER_IMAGES.length) {
			Image image = new Image(CHARACTER_IMAGES[curChar]);
			characterImageView.setImage(image);
		}

		boolean Dup = false;
		for (String key : GameLogic.playerList.keySet()) {
			if (key != PlayerLogic.getLocalAddressPort()) { //check for not ours
				PlayerInfo info = GameLogic.playerList.get(key);
				if (info.getCharacterID() == curChar) {
					Dup = true;
				}
			}
		}
		if (Dup) {
			selectButton.setDisable(true);
			selectButton.setText("Taken");
			selectButton.setStyle(
					"-fx-background-color: #FF6347; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 15 5 15;");
		} else {

			if (PlayerLogic.getCharID() == curChar) {
				selectButton.setDisable(true);
				selectButton.setText("Selected");
				selectButton.setStyle(
						"-fx-background-color: #FF6347; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 15 5 15;");
			} else {
				selectButton.setText("Select");
				selectButton.setDisable(false);

				selectButton.setStyle(
						"-fx-background-color: #007ACC; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 15 5 15;"); // style
			}
		}

	}

	private void startCharacterUpdateThread() {
		Thread thread = new Thread(() -> {
			while (true) {
				System.out.println("OURS" + " : " + PlayerLogic.getCharID());

				for (String key : GameLogic.playerList.keySet()) {
					PlayerInfo info = GameLogic.playerList.get(key);
					int char_id = info.getCharacterID();
					System.out.println(info.getName() + " : " + info.getCharacterID());
				}
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		thread.setDaemon(true);
		thread.start();
	}
}

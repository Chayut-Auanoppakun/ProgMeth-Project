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

public class CharaterSelectgui extends VBox {
    private ImageView characterImageView;
    private int curChar = 0;
    private static final String[] CHARACTER_NAMES = { "Alex", "Casey", "Taylor", "Jordan", "Morgan", "Riley", "Avery",
            "Cameron", "Quinn", "Rowan" };
    private static final String[] CHARACTER_IMAGES = { "/player/profile/01.png", "/player/profile/02.png",
            "/player/profile/03.png", "/player/profile/04.png", "/player/profile/05.png", "/player/profile/06.png",
            "/player/profile/07.png", "/player/profile/08.png", "/player/profile/09.png", "/player/profile/10.png" };
    private Button selectButton;
    private TextField name;
    private Runnable onCharacterSelectedCallback;

    public CharaterSelectgui(Runnable onCharacterSelectedCallback) {
        this.onCharacterSelectedCallback = onCharacterSelectedCallback;
        
        // Set fixed size
        setPrefWidth(300);
        setPrefHeight(400);
        setMaxWidth(300);
        setMaxHeight(400);
        
        setPadding(new Insets(20));
        setSpacing(15);
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: rgba(0, 0, 0, 0.85);"); // More opaque background

        // Create border
        setBorder(new Border(new BorderStroke(Paint.valueOf("white"), BorderStrokeStyle.SOLID, new CornerRadii(10),
                new BorderWidths(3))));

        Text title = new Text("Select Character");
        title.setFill(Paint.valueOf("white")); // Set text color to white
        title.setFont(Font.font("System", javafx.scene.text.FontWeight.BOLD, 20)); // Set text to bold

        characterImageView = new ImageView();
        characterImageView.setFitHeight(200);
        characterImageView.setFitWidth(150);
        characterImageView.setPreserveRatio(true);
        Image image = new Image(CHARACTER_IMAGES[curChar]);
        characterImageView.setImage(image);
        
        Button leftArrow = new Button("<");
        Button rightArrow = new Button(">");
        selectButton = new Button("Select");
        name = new TextField(CHARACTER_NAMES[curChar]);
        
        name.setPrefWidth(150); // Set preferred width
        name.setEditable(false); // Make the text field uneditable
        name.setAlignment(Pos.CENTER); // Center the text
        selectButton.setPrefWidth(150);

        // Style buttons
        String buttonStyle = "-fx-background-color: #007ACC; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 15 5 15;";
        leftArrow.setStyle(buttonStyle);
        rightArrow.setStyle(buttonStyle);
        selectButton.setStyle(buttonStyle);

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

        if (isDuplicate) {
            selectButton.setDisable(true);
            selectButton.setText("Character Taken");
            selectButton.setStyle(
                    "-fx-background-color: #FF6347; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 15 5 15;");
        } else if (PlayerLogic.getCharID() == curChar) {
            selectButton.setDisable(true);
            selectButton.setText("Currently Selected");
            selectButton.setStyle(
                    "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 15 5 15;");
        } else {
            selectButton.setDisable(false);
            selectButton.setText("Select");
            selectButton.setStyle(
                    "-fx-background-color: #007ACC; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 5 15 5 15;");
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

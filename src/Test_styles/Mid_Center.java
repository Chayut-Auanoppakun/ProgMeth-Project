package Test_styles;

import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class Mid_Center extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("The Last of Us Part II - Menu");
        
        // Try to load background image - fallback to dark color if image can't be loaded
        Background background;
        try {
            BackgroundImage backgroundImage = new BackgroundImage(
                new Image(getClass().getResourceAsStream("/background.png")), 
                BackgroundRepeat.NO_REPEAT, 
                BackgroundRepeat.NO_REPEAT, 
                BackgroundPosition.CENTER, 
                new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true)
            );
            background = new Background(backgroundImage);
        } catch (Exception e) {
            background = new Background(new BackgroundFill(Color.web("#1A1A1A"), CornerRadii.EMPTY, Insets.EMPTY));
        }
        
        // Create title text
        Text titleText = new Text("THE LAST OF US PART II");
        titleText.setFont(Font.font("Helvetica", FontWeight.LIGHT, 28));
        titleText.setFill(Color.WHITE);
        titleText.setOpacity(0.9);
        titleText.setTextAlignment(TextAlignment.CENTER);
        
        // Create menu buttons
        Button continueButton = createMenuButton("CONTINUE");
        Button newGameButton = createMenuButton("NEW GAME");
        Button chaptersButton = createMenuButton("CHAPTERS");
        Button loadGameButton = createMenuButton("LOAD GAME");
        Button bonusesButton = createMenuButton("BONUSES");
        Button backButton = createMenuButton("BACK");
        
        // Create VBox for menu items
        VBox menuVBox = new VBox(15);
        menuVBox.getChildren().addAll(
            titleText,
            continueButton,
            newGameButton,
            chaptersButton,
            loadGameButton,
            bonusesButton
        );
        menuVBox.setAlignment(Pos.CENTER);
        
        // Create GridPane as the root layout
        GridPane gridPane = new GridPane();
        gridPane.setBackground(background);
        
        // Add dark overlay that covers the entire scene
        StackPane overlayPane = new StackPane();
        Region darkOverlay = new Region();
        darkOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);");
        darkOverlay.prefWidthProperty().bind(primaryStage.widthProperty());
        darkOverlay.prefHeightProperty().bind(primaryStage.heightProperty());
        overlayPane.getChildren().add(darkOverlay);
        
        // Configure GridPane
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(100);
        gridPane.getColumnConstraints().add(col1);
        
        RowConstraints row1 = new RowConstraints();
        row1.setPercentHeight(90);  // Main content area
        RowConstraints row2 = new RowConstraints();
        row2.setPercentHeight(10);  // Footer area for back button
        gridPane.getRowConstraints().addAll(row1, row2);
        
        // Add overlayPane to cover the entire grid
        gridPane.add(overlayPane, 0, 0, 1, 2);
        
        // Add menu to center of first row
        gridPane.add(menuVBox, 0, 0);
        GridPane.setHalignment(menuVBox, HPos.CENTER);
        GridPane.setMargin(menuVBox, new Insets(0, 0, 0, 0));
        
        // Add back button to bottom-left of second row
        gridPane.add(backButton, 0, 1);
        GridPane.setHalignment(backButton, HPos.LEFT);
        GridPane.setMargin(backButton, new Insets(0, 0, 20, 20));
        
        // Set scene and show stage
        Scene scene = new Scene(gridPane, 900, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    private Button createMenuButton(String text) {
        Button button = new Button(text);
        
        // Set button properties
        button.setPrefWidth(200);
        button.setPrefHeight(40);
        button.setMnemonicParsing(false);
        
        // Define the button style
        button.setStyle(
            "-fx-background-color: transparent;" +
            "-fx-text-fill: white;" +
            "-fx-font-family: 'Helvetica';" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: normal;" +
            "-fx-alignment: center;" +
            "-fx-opacity: 0.8;"
        );
        
        // Define button actions
        if (text.equals("CONTINUE")) {
            button.setOnAction(e -> System.out.println("Continue clicked"));
        } else if (text.equals("NEW GAME")) {
            button.setOnAction(e -> System.out.println("New Game clicked"));
        } else if (text.equals("CHAPTERS")) {
            button.setOnAction(e -> System.out.println("Chapters clicked"));
        } else if (text.equals("LOAD GAME")) {
            button.setOnAction(e -> System.out.println("Load Game clicked"));
        } else if (text.equals("BONUSES")) {
            button.setOnAction(e -> System.out.println("Bonuses clicked"));
        } else if (text.equals("BACK")) {
            button.setOnAction(e -> System.out.println("Back clicked"));
        }
        
        // Add hover effect
        button.setOnMouseEntered(e -> {
            button.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: white;" +
                "-fx-font-family: 'Helvetica';" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: normal;" +
                "-fx-alignment: center;" +
                "-fx-opacity: 1.0;"
            );
        });
        
        button.setOnMouseExited(e -> {
            button.setStyle(
                "-fx-background-color: transparent;" +
                "-fx-text-fill: white;" +
                "-fx-font-family: 'Helvetica';" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: normal;" +
                "-fx-alignment: center;" +
                "-fx-opacity: 0.8;"
            );
        });
        
        return button;
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
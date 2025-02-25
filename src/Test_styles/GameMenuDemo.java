package Test_styles;

import javafx.animation.FadeTransition;
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
import javafx.util.Duration;

public class GameMenuDemo extends Application {
    private static String version = "v.1";
    private Button newGameButton;
    private Button optionButton;
    private Button loadGameButton;
    private Button helpButton;
    private Button exitButton;
    private VBox menuVBox;
    private boolean isGameOptionsVisible = false;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("AMONG CEDT " + version);

        // Try to load background image - fallback to dark color if image can't be loaded
        Background background;
        try {
            BackgroundImage backgroundImage = new BackgroundImage(
                    new Image(getClass().getResourceAsStream("/background.jpg")), BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
                    new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, true, true));
            background = new Background(backgroundImage);
        } catch (Exception e) {
            background = new Background(new BackgroundFill(Color.web("#1A1A1A"), CornerRadii.EMPTY, Insets.EMPTY));
        }

        // Create title text
        Text titleText = new Text("AMONG CEDT " + version);
        titleText.setFont(Font.font("Helvetica", FontWeight.LIGHT, 28));
        titleText.setFill(Color.WHITE);
        titleText.setOpacity(0.9);
        titleText.setTextAlignment(TextAlignment.CENTER);

        // Create menu buttons
        newGameButton = createMenuButton("NEW GAME");
        optionButton = createMenuButton("OPTIONS");
        loadGameButton = createMenuButton("ABOUT");
        helpButton = createMenuButton("HELP");
        exitButton = createMenuButton("EXIT");

        // Update VBox for menu items
        menuVBox = new VBox(10);
        menuVBox.getChildren().addAll(titleText, newGameButton, optionButton, loadGameButton, helpButton);
        menuVBox.setAlignment(Pos.TOP_LEFT);

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
        row1.setPercentHeight(90); // Main content area
        RowConstraints row2 = new RowConstraints();
        row2.setPercentHeight(10); // Footer area for back button
        gridPane.getRowConstraints().addAll(row1, row2);

        // Add overlayPane to cover the entire grid
        gridPane.add(overlayPane, 0, 0, 1, 2);
        // Add menu to top-left of first row
        gridPane.add(menuVBox, 0, 0);
        GridPane.setHalignment(menuVBox, HPos.LEFT);
        GridPane.setValignment(menuVBox, javafx.geometry.VPos.TOP);
        GridPane.setMargin(menuVBox, new Insets(40, 10, 10, 40)); // Adjust the margin as needed

        // Add back button to bottom-left of second row
        gridPane.add(exitButton, 0, 1);
        GridPane.setHalignment(exitButton, HPos.LEFT);
        GridPane.setMargin(exitButton, new Insets(0, 0, 20, 20));

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
        button.setStyle("-fx-background-color: transparent;" + 
                        "-fx-text-fill: white;" + 
                        "-fx-font-family: 'Helvetica';" +
                        "-fx-font-size: 16px;" + 
                        "-fx-font-weight: normal;" + 
                        "-fx-alignment: center;" + 
                        "-fx-opacity: 0.8;");

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
            button.setOnAction(e -> System.out.println("Host game clicked"));
        } else if (text.equals("JOIN")) {
            button.setOnAction(e -> System.out.println("Join game clicked"));
        }
        // Add hover effect
        button.setOnMouseEntered(e -> {
            button.setStyle("-fx-background-color: transparent;" + 
                           "-fx-text-fill: white;" +
                           "-fx-font-family: 'Helvetica';" + 
                           "-fx-font-size: 16px;" + 
                           "-fx-font-weight: normal;" +
                           "-fx-alignment: center;" + 
                           "-fx-opacity: 1.0;");
        });

        button.setOnMouseExited(e -> {
            button.setStyle("-fx-background-color: transparent;" + 
                           "-fx-text-fill: white;" +
                           "-fx-font-family: 'Helvetica';" + 
                           "-fx-font-size: 16px;" + 
                           "-fx-font-weight: normal;" +
                           "-fx-alignment: center;" + 
                           "-fx-opacity: 0.8;");
        });

        return button;
    }
    
    private void showGameOptions() {
        if (isGameOptionsVisible) return;
        isGameOptionsVisible = true;
        
        exitButton.setText("BACK TO MAIN");
        
        // Create host and join buttons
        Button hostButton = createMenuButton("HOST");
        Button joinButton = createMenuButton("JOIN");
        
        // Initially set opacity to 0 for fade-in effect
        hostButton.setOpacity(0);
        joinButton.setOpacity(0);

        // Store original positions of other buttons (index in the VBox)
        int optionButtonIndex = menuVBox.getChildren().indexOf(optionButton);
        int loadGameButtonIndex = menuVBox.getChildren().indexOf(loadGameButton);
        int helpButtonIndex = menuVBox.getChildren().indexOf(helpButton);
        
        // Remove the NEW GAME button with a fade out effect
        FadeTransition fadeOutNewGame = new FadeTransition(Duration.millis(10), newGameButton);
        fadeOutNewGame.setFromValue(1.0);
        fadeOutNewGame.setToValue(0.0);
        fadeOutNewGame.setOnFinished(event -> {
            // Get the title text
            Text titleText = (Text) menuVBox.getChildren().get(0);
            
            // Remove the NEW GAME button but keep others
            menuVBox.getChildren().remove(newGameButton);
            
            // Add the host and join buttons right after the title
            menuVBox.getChildren().add(1, hostButton);
            menuVBox.getChildren().add(2, joinButton);
            
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
    
    private void showMainMenu() {
        if (!isGameOptionsVisible) return;
        isGameOptionsVisible = false;
        
        // Change BACK button to EXIT
        exitButton.setText("EXIT");
        
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
        
        // Recreate the NEW GAME button
        newGameButton = createMenuButton("NEW GAME");
        newGameButton.setOpacity(0);
        
        // Remove margin from options button
        VBox.setMargin(optionButton, new Insets(0, 0, 0, 0));
        
        // Remove host and join buttons, add NEW GAME button back
        if (hostButton != null) menuVBox.getChildren().remove(hostButton);
        if (joinButton != null) menuVBox.getChildren().remove(joinButton);
        
        // Add the NEW GAME button back at position 1 (after title)
        menuVBox.getChildren().add(1, newGameButton);
        
        // Fade in the NEW GAME button
        FadeTransition fadeInNewGame = new FadeTransition(Duration.millis(400), newGameButton);
        fadeInNewGame.setFromValue(0.0);
        fadeInNewGame.setToValue(0.8);
        fadeInNewGame.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
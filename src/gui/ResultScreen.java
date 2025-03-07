package gui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import logic.GameLogic;
import logic.GameLogic.GameResult;
import logic.PlayerLogic;
import server.PlayerInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Displays the final results of the game showing which team won (Impostors or Crewmates).
 * The design maintains the pixelated/retro style used throughout the game.
 */
public class ResultScreen extends StackPane {
    private static final double ANIMATION_DURATION = 5000; // Total time for animations in ms
    private GameWindow gameWindow;
    private Group root;
    private GameResult gameResult;

    /**
     * Creates a new ResultScreen with the specified game result
     * 
     * @param root The Group root node of the main game scene
     * @param gameWindow Reference to the main GameWindow
     * @param result The result of the game (CREWMATE_WIN or IMPOSTER_WIN)
     */
    public ResultScreen(Group root, GameWindow gameWindow, GameResult result) {
        this.root = root;
        this.gameWindow = gameWindow;
        this.gameResult = result;
        
        // Set full-screen size
        setPrefSize(gameWindow.getWidth(), gameWindow.getHeight());
        setMaxSize(gameWindow.getWidth(), gameWindow.getHeight());
        
        // Create and display the results screen
        createResultScreen();
    }
    
    /**
     * Create and show the results screen with all UI elements and animations
     */
    private void createResultScreen() {
        // Create background with semi-transparent gradient similar to game start
        Rectangle background = new Rectangle(0, 0, getWidth(), getHeight());
        
        // Create a semi-transparent gradient similar to gamestart
        if (gameResult == GameResult.IMPOSTER_WIN) {
            // Semi-transparent dark red gradient for Impostor win
            background.setFill(new RadialGradient(
                0, 0, getWidth() / 2, getHeight() / 2,
                Math.max(getWidth(), getHeight()) / 2, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(50, 0, 0, 0.85)), // Dark red center
                new Stop(1, Color.rgb(20, 0, 0, 0.90)) // Darker edges
            ));
        } else {
            // Semi-transparent dark blue gradient for Crewmate win
            background.setFill(new RadialGradient(
                0, 0, getWidth() / 2, getHeight() / 2,
                Math.max(getWidth(), getHeight()) / 2, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(20, 20, 50, 0.85)), // Dark blue center
                new Stop(1, Color.rgb(10, 10, 30, 0.90)) // Darker edges
            ));
        }
        
        // Create main content container
        BorderPane contentPane = new BorderPane();
        contentPane.setPrefSize(getWidth(), getHeight());
        
        // Create result title with winner announcement
        VBox titleBox = createTitleSection();
        contentPane.setTop(titleBox);
        BorderPane.setAlignment(titleBox, Pos.CENTER);
        BorderPane.setMargin(titleBox, new Insets(80, 0, 20, 0));

        // Create character display section
        HBox charactersBox = createCharactersSection();
        contentPane.setCenter(charactersBox);
        
        // Create buttons section
        HBox buttonsBox = createButtonsSection();
        contentPane.setBottom(buttonsBox);
        BorderPane.setMargin(buttonsBox, new Insets(0, 0, 80, 0));
        
        // Create star field in the background
        Pane starField = createStarField();
        
        // Add all elements
        getChildren().addAll(background, starField, contentPane);
        
        // Start with low opacity for fade-in
        setOpacity(0);
        
        // Fade in the result screen
        FadeTransition fadeIn = new FadeTransition(Duration.millis(2000), this);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
        
        // Play sound based on winner
        if (gameResult == GameResult.IMPOSTER_WIN) {
            logic.SoundLogic.playSound("assets/sounds/impostor_win.wav", 0);
        } else {
            logic.SoundLogic.playSound("assets/sounds/crewmate_win.wav", 0);
        }
    }
    
    /**
     * Creates the title section of the result screen
     */
    private VBox createTitleSection() {
        VBox titleBox = new VBox(30);
        titleBox.setAlignment(Pos.CENTER);
        
        // Main title with pixelated font
        Text gameOverText = new Text("GAME OVER");
        gameOverText.setFont(Font.font("Monospace", FontWeight.BOLD, 64));
        gameOverText.setFill(Color.WHITE);
        gameOverText.setStroke(Color.BLACK);
        gameOverText.setStrokeWidth(2);
        gameOverText.setTextAlignment(TextAlignment.CENTER);
        
        // Create a glow effect
        Glow glow = new Glow(0.8);
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.BLACK);
        shadow.setRadius(10);
        shadow.setSpread(0.6);
        gameOverText.setEffect(glow);
        
        // Winner announcement with pixelated font
        Text winnerText = new Text();
        if (gameResult == GameResult.IMPOSTER_WIN) {
            winnerText.setText("IMPOSTORS WIN");
            winnerText.setFill(Color.rgb(255, 50, 50));
            
            // Create pulsing animation for text
            Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(winnerText.fillProperty(), Color.rgb(255, 50, 50))),
                new KeyFrame(Duration.seconds(1), new KeyValue(winnerText.fillProperty(), Color.rgb(180, 0, 0))),
                new KeyFrame(Duration.seconds(2), new KeyValue(winnerText.fillProperty(), Color.rgb(255, 50, 50)))
            );
            pulse.setCycleCount(Timeline.INDEFINITE);
            pulse.play();
        } else {
            winnerText.setText("CREWMATES WIN");
            winnerText.setFill(Color.rgb(50, 150, 255));
            
            // Create pulsing animation for text
            Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(winnerText.fillProperty(), Color.rgb(50, 150, 255))),
                new KeyFrame(Duration.seconds(1), new KeyValue(winnerText.fillProperty(), Color.rgb(0, 80, 180))),
                new KeyFrame(Duration.seconds(2), new KeyValue(winnerText.fillProperty(), Color.rgb(50, 150, 255)))
            );
            pulse.setCycleCount(Timeline.INDEFINITE);
            pulse.play();
        }
        winnerText.setFont(Font.font("Monospace", FontWeight.BOLD, 48));
        winnerText.setStroke(Color.BLACK);
        winnerText.setStrokeWidth(2);
        winnerText.setTextAlignment(TextAlignment.CENTER);
        
        titleBox.getChildren().addAll(gameOverText, winnerText);
        
        // Add entrance animation
        ScaleTransition scaleTitle = new ScaleTransition(Duration.millis(1000), titleBox);
        scaleTitle.setFromX(0.2);
        scaleTitle.setFromY(0.2);
        scaleTitle.setToX(1.0);
        scaleTitle.setToY(1.0);
        scaleTitle.setInterpolator(Interpolator.EASE_OUT);
        scaleTitle.play();
        
        return titleBox;
    }
    
    /**
     * Creates the characters section that displays character icons for the winning team only
     */
    private HBox createCharactersSection() {
        HBox charactersBox = new HBox(50);
        charactersBox.setAlignment(Pos.CENTER);
        
        // Lists to hold characters from winning team
        List<PlayerInfo> winningTeamPlayers = new ArrayList<>();
        
        // Check local player team
        boolean localPlayerIsImpostor = false;
        boolean localPlayerOnWinningTeam = false;
        
        if ("imposter".equals(PlayerLogic.getStatus()) || (PlayerLogic.isWasImposter() && "dead".equals(PlayerLogic.getStatus()))) {
            localPlayerIsImpostor = true;
        }
        
        // Check if local player is on winning team
        if ((gameResult == GameResult.IMPOSTER_WIN && localPlayerIsImpostor) ||
            (gameResult == GameResult.CREWMATE_WIN && !localPlayerIsImpostor)) {
            localPlayerOnWinningTeam = true;
        }
        
        // Collect players on winning team
        for (String key : GameLogic.playerList.keySet()) {
            PlayerInfo player = GameLogic.playerList.get(key);
            if (player != null) {
                boolean isPlayerImpostor = "imposter".equals(player.getStatus()) || 
                    ("dead".equals(player.getStatus()) && player.getClass().getSimpleName().contains("Imposter"));
                
                // Add player to winning team list if they match the winning team
                if ((gameResult == GameResult.IMPOSTER_WIN && isPlayerImpostor) ||
                    (gameResult == GameResult.CREWMATE_WIN && !isPlayerImpostor)) {
                    winningTeamPlayers.add(player);
                }
            }
        }
        
        // Create winning team display box
        String teamName = (gameResult == GameResult.IMPOSTER_WIN) ? "IMPOSTORS" : "CREWMATES";
        boolean isImpostorTeam = (gameResult == GameResult.IMPOSTER_WIN);
        Integer localPlayerCharId = localPlayerOnWinningTeam ? PlayerLogic.getCharID() : null;
        
        VBox winningTeamBox = createTeamBox(teamName, winningTeamPlayers, isImpostorTeam, localPlayerCharId);
        
        // Add floating/bobbing animation to the box
        addFloatingAnimation(winningTeamBox);
        
        // Add highlight to the winning team
        addWinningGlow(winningTeamBox);
        
        charactersBox.getChildren().add(winningTeamBox);
        return charactersBox;
    }
    
    /**
     * Creates a team box displaying character icons and team name
     */
    private VBox createTeamBox(String teamName, List<PlayerInfo> players, boolean isImpostorTeam, Integer localPlayerCharId) {
        VBox teamBox = new VBox(15);
        teamBox.setAlignment(Pos.CENTER);
        teamBox.setPadding(new Insets(20));
        
        // Make panel size responsive to player count
        int width = Math.max(320, Math.min(650, 250 + players.size() * 120));
        
        // Background panel with team color - semitransparent like gamestart
        Rectangle teamBg = new Rectangle(width, 400);
        teamBg.setArcWidth(15);
        teamBg.setArcHeight(15);
        
        if (isImpostorTeam) {
            teamBg.setFill(Color.rgb(80, 20, 20, 0.7));
            teamBg.setStroke(Color.rgb(180, 50, 50));
        } else {
            teamBg.setFill(Color.rgb(20, 40, 80, 0.7));
            teamBg.setStroke(Color.rgb(50, 100, 200));
        }
        teamBg.setStrokeWidth(3);
        
        // Add drop shadow for depth
        DropShadow dropShadow = new DropShadow();
        dropShadow.setRadius(15);
        dropShadow.setOffsetX(5);
        dropShadow.setOffsetY(5);
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.6));
        teamBg.setEffect(dropShadow);
        
        // Team name text
        Text teamText = new Text(teamName + " WIN!");
        teamText.setFont(Font.font("Monospace", FontWeight.BOLD, 32));
        teamText.setFill(isImpostorTeam ? Color.rgb(255, 50, 50) : Color.rgb(50, 150, 255));
        teamText.setStroke(Color.BLACK);
        teamText.setStrokeWidth(1);
        
        // Create flow container for character icons - use FlowPane to handle overflow better
        FlowPane charactersFlow = new FlowPane(15, 15);
        charactersFlow.setAlignment(Pos.CENTER);
        charactersFlow.setPadding(new Insets(10));
        charactersFlow.setPrefWrapLength(width - 40);
        
        // Add local player if on this team
        if (localPlayerCharId != null) {
            VBox playerBox = createPlayerBox(PlayerLogic.getName(), localPlayerCharId, true);
            charactersFlow.getChildren().add(playerBox);
        }
        
        // Add other players
        for (PlayerInfo player : players) {
            if (player != null) {
                VBox playerBox = createPlayerBox(player.getName(), player.getCharacterID(), false);
                charactersFlow.getChildren().add(playerBox);
            }
        }
        
        // Stack the background and content
        StackPane boxPane = new StackPane();
        
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.getChildren().addAll(teamText, charactersFlow);
        
        boxPane.getChildren().addAll(teamBg, contentBox);
        teamBox.getChildren().add(boxPane);
        
        return teamBox;
    }
    
    /**
     * Creates a player box with character image and name
     */
    private VBox createPlayerBox(String name, int charId, boolean isLocalPlayer) {
        VBox playerBox = new VBox(10);
        playerBox.setAlignment(Pos.CENTER);
        
        // Create character image
        String profilePath = String.format("/player/profile/%02d.png", (charId + 1));
        ImageView characterView = new ImageView();
        
        try {
            // Debug for path
            System.out.println("Loading character image from: " + profilePath);
            
            // Try to load image
            Image charImage = new Image(getClass().getResourceAsStream(profilePath));
            
            if (charImage.isError()) {
                throw new Exception("Image failed to load: " + charImage.getException().getMessage());
            }
            
            characterView.setImage(charImage);
            characterView.setFitWidth(100);
            characterView.setFitHeight(100);
            characterView.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("Error loading character image: " + e.getMessage());
            e.printStackTrace();
            
            // Try alternate path format
            try {
                // Try a different path format (without leading slash)
                String altPath = String.format("player/profile/%02d.png", (charId + 1));
                System.out.println("Trying alternate path: " + altPath);
                Image altImage = new Image(ClassLoader.getSystemResourceAsStream(altPath));
                characterView.setImage(altImage);
            } catch (Exception e2) {
                System.err.println("Alternative path also failed: " + e2.getMessage());
                
                // Create colored circle as fallback
                Circle placeholder = new Circle(40);
                placeholder.setFill(Color.rgb(50, 50, 70));
                placeholder.setStroke(Color.WHITE);
                placeholder.setStrokeWidth(2);
                
                StackPane placeholderPane = new StackPane(placeholder);
                playerBox.getChildren().add(placeholderPane);
                return playerBox; // Return early since we added fallback directly
            }
        }
        
        // Add border to character image
        StackPane imageContainer = new StackPane();
        Rectangle imageBorder = new Rectangle(105, 105);
        imageBorder.setArcWidth(10);
        imageBorder.setArcHeight(10);
        imageBorder.setFill(Color.TRANSPARENT);
        imageBorder.setStroke(isLocalPlayer ? Color.GOLD : Color.WHITE);
        imageBorder.setStrokeWidth(2);
        
        imageContainer.getChildren().addAll(imageBorder, characterView);
        
        // Create name label
        Text nameText = new Text(shortenName(name));
        nameText.setFont(Font.font("Monospace", FontWeight.BOLD, 14));
        nameText.setFill(Color.WHITE);
        nameText.setTextAlignment(TextAlignment.CENTER);
        
        playerBox.getChildren().addAll(imageContainer, nameText);
        
        // Add glow for local player
        if (isLocalPlayer) {
            DropShadow glow = new DropShadow();
            glow.setColor(Color.GOLD);
            glow.setRadius(10);
            glow.setSpread(0.5);
            imageContainer.setEffect(glow);
        }
        
        return playerBox;
    }
    
    /**
     * Shortens long player names to fit in the UI
     */
    private String shortenName(String name) {
        if (name.length() > 10) {
            return name.substring(0, 8) + "...";
        }
        return name;
    }
    
    /**
     * Creates the buttons section at the bottom of the screen
     */
    private HBox createButtonsSection() {
        HBox buttonsBox = new HBox(30);
        buttonsBox.setAlignment(Pos.CENTER);
        
        // Main menu button
        Button mainMenuButton = createStyledButton("MAIN MENU", () -> {
            // Handle returning to main menu
            fadeOutAndClose(() -> {
                // Code to return to main menu goes here
                Platform.runLater(() -> {
                    // This would need to be implemented in GameWindow class
                    if (gameWindow != null) {
                        //gameWindow.returnToMainMenu();
                    }
                });
            });
        });
        
        // Quit button
        Button quitButton = createStyledButton("QUIT", () -> {
            // Handle quit game
            fadeOutAndClose(() -> {
                Platform.exit();
            });
        });
        
        buttonsBox.getChildren().addAll(mainMenuButton, quitButton);
        return buttonsBox;
    }
    
    /**
     * Creates a styled button with hover effects
     */
    private Button createStyledButton(String text, Runnable action) {
        Button button = new Button(text);
        
        // Apply pixelated style to match game aesthetic
        button.setStyle("-fx-background-color: #1e90ff; " + 
                        "-fx-text-fill: white; " +
                        "-fx-font-family: 'Monospace'; " +
                        "-fx-font-size: 16px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 12 30 12 30; " +
                        "-fx-border-color: #87cefa; " +
                        "-fx-border-width: 3px; " +
                        "-fx-background-radius: 0; " +
                        "-fx-border-radius: 0; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);");
        
        // Hover effects
        button.setOnMouseEntered(e -> 
            button.setStyle("-fx-background-color: #00bfff; " + 
                           "-fx-text-fill: white; " +
                           "-fx-font-family: 'Monospace'; " +
                           "-fx-font-size: 16px; " +
                           "-fx-font-weight: bold; " +
                           "-fx-padding: 12 30 12 30; " +
                           "-fx-border-color: #b0e2ff; " +
                           "-fx-border-width: 3px; " +
                           "-fx-background-radius: 0; " +
                           "-fx-border-radius: 0; " +
                           "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 8, 0, 0, 1);")
        );
        
        button.setOnMouseExited(e -> 
            button.setStyle("-fx-background-color: #1e90ff; " + 
                           "-fx-text-fill: white; " +
                           "-fx-font-family: 'Monospace'; " +
                           "-fx-font-size: 16px; " +
                           "-fx-font-weight: bold; " +
                           "-fx-padding: 12 30 12 30; " +
                           "-fx-border-color: #87cefa; " +
                           "-fx-border-width: 3px; " +
                           "-fx-background-radius: 0; " +
                           "-fx-border-radius: 0; " +
                           "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);")
        );
        
        // Set action when clicked
        button.setOnAction(e -> {
            if (action != null) {
                action.run();
            }
        });
        
        return button;
    }
    
    /**
     * Creates a star field in the background
     */
    private Pane createStarField() {
        Pane starField = new Pane();
        starField.setPrefSize(getWidth(), getHeight());
        
        Random random = new Random();
        
        // Add stars of different sizes
        for (int i = 0; i < 200; i++) {
            Circle star = new Circle(random.nextDouble() * 1.5 + 0.5); // Random size between 0.5 and 2
            
            // Random position
            star.setCenterX(random.nextDouble() * getWidth());
            star.setCenterY(random.nextDouble() * getHeight());
            
            // Randomize color slightly
            double blueTint = random.nextDouble() * 0.3;
            star.setFill(Color.rgb(255 - (int)(blueTint * 50), 255 - (int)(blueTint * 50), 255));
            
            // Add subtle glow effect
            Glow glow = new Glow(0.8);
            star.setEffect(glow);
            
            // Add twinkling animation to some stars
            if (random.nextBoolean()) {
                Timeline twinkle = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(star.opacityProperty(), 0.3 + random.nextDouble() * 0.7)),
                    new KeyFrame(Duration.seconds(random.nextDouble() * 2 + 1), 
                                new KeyValue(star.opacityProperty(), 0.3 + random.nextDouble() * 0.7))
                );
                twinkle.setCycleCount(Timeline.INDEFINITE);
                twinkle.setAutoReverse(true);
                twinkle.play();
            }
            
            starField.getChildren().add(star);
        }
        
        return starField;
    }
    
    /**
     * Adds a floating/bobbing animation to a node
     */
    private void addFloatingAnimation(javafx.scene.Node node) {
        TranslateTransition float1 = new TranslateTransition(Duration.seconds(2), node);
        float1.setByY(-10);
        float1.setCycleCount(Timeline.INDEFINITE);
        float1.setAutoReverse(true);
        float1.play();
    }
    
    /**
     * Adds a glowing highlight to the winning team
     */
    private void addWinningGlow(javafx.scene.Node node) {
        // Create pulsing glow
        DropShadow glow = new DropShadow();
        
        if (gameResult == GameResult.IMPOSTER_WIN) {
            glow.setColor(Color.rgb(255, 50, 50, 0.8));
        } else {
            glow.setColor(Color.rgb(50, 150, 255, 0.8));
        }
        
        glow.setRadius(20);
        glow.setSpread(0.4);
        node.setEffect(glow);
        
        // Create pulsing animation
        Timeline pulseGlow = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 20)),
            new KeyFrame(Duration.seconds(1.5), new KeyValue(glow.radiusProperty(), 30)),
            new KeyFrame(Duration.seconds(3), new KeyValue(glow.radiusProperty(), 20))
        );
        pulseGlow.setCycleCount(Timeline.INDEFINITE);
        pulseGlow.play();
        
        // Add scaling animation
        ScaleTransition scale = new ScaleTransition(Duration.seconds(1.5), node);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.05);
        scale.setToY(1.05);
        scale.setCycleCount(Timeline.INDEFINITE);
        scale.setAutoReverse(true);
        scale.play();
    }
    
    /**
     * Fades out the result screen and executes an action after
     */
    private void fadeOutAndClose(Runnable onFinished) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(1000), this);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> {
            // Remove from parent
            if (getParent() instanceof Group) {
                ((Group) getParent()).getChildren().remove(this);
            } else if (getParent() instanceof Pane) {
                ((Pane) getParent()).getChildren().remove(this);
            }
            
            // Execute callback
            if (onFinished != null) {
                onFinished.run();
            }
        });
        fadeOut.play();
    }
}
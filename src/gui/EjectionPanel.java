package gui;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import logic.SoundLogic;

/**
 * A panel that displays when a player is ejected after voting.
 */
public class EjectionPanel extends StackPane {
    
    private final String ejectedPlayerName;
    private final boolean wasImpostor;
    private final int ejectedCharId;
    private final Group root;
    private final double screenWidth;
    private final double screenHeight;
    
    /**
     * Creates a new ejection panel
     * 
     * @param root The root Group to add this panel to
     * @param ejectedPlayerName The name of the ejected player
     * @param wasImpostor Whether the ejected player was an impostor
     * @param ejectedCharId The character ID of the ejected player
     * @param screenWidth The width of the game screen
     * @param screenHeight The height of the game screen
     */
    public EjectionPanel(Group root, String ejectedPlayerName, boolean wasImpostor, 
                         int ejectedCharId, double screenWidth, double screenHeight) {
        this.ejectedPlayerName = ejectedPlayerName;
        this.wasImpostor = wasImpostor;
        this.ejectedCharId = ejectedCharId;
        this.root = root;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        
        // Set up the panel
        setPrefSize(screenWidth, screenHeight);
        setMaxSize(screenWidth, screenHeight);
        
        // Create the panel content
        buildEjectionPanel();
        
        // Add to root
        root.getChildren().add(this);
        
        // Play ejection sound
        if (wasImpostor) {
            SoundLogic.playSound("assets/sounds/impostor_eject.wav", 0);
        } else {
            SoundLogic.playSound("assets/sounds/crewmate_eject.wav", 0);
        }
    }
    
    /**
     * Builds the ejection panel UI
     */
    private void buildEjectionPanel() {
        // Create a full-screen dark overlay
        Rectangle overlay = new Rectangle(0, 0, screenWidth, screenHeight);
        overlay.setFill(Color.rgb(0, 0, 0, 0.85));
        
        // Create content container styled to match CharacterSelectGui
        VBox contentBox = new VBox(20);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPrefWidth(500);
        contentBox.setPrefHeight(420);
        contentBox.setMaxWidth(500);
        contentBox.setMaxHeight(420);
        contentBox.setPadding(new Insets(25));
        
        // Use the same dark blue-gray background as CharacterSelectGui
        contentBox.setStyle("-fx-background-color: rgba(30, 30, 50, 0.95);");
        
        // Create border with same blue accent color
        contentBox.setBorder(new Border(new BorderStroke(
            Paint.valueOf("#1e90ff"), // Blue border color
            BorderStrokeStyle.SOLID, 
            new CornerRadii(0), // Sharp corners
            new BorderWidths(3))
        ));
        
        // Create title text
        Text titleText = new Text(ejectedPlayerName + " was ejected");
        titleText.setFill(Paint.valueOf("#ff6347")); // Tomato red
        titleText.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
        titleText.setTextAlignment(TextAlignment.CENTER);
        
        // Shadow effect to make it pop
        titleText.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 3, 0, 0, 0);");
        
        // Character image
        VBox characterBox = createCharacterDisplay();
        
        // Create impostor status text
        Text statusText = new Text(getStatusText());
        statusText.setFont(Font.font("Monospace", FontWeight.BOLD, 22));
        statusText.setFill(wasImpostor ? Color.rgb(255, 100, 100) : Color.rgb(100, 200, 255));
        statusText.setTextAlignment(TextAlignment.CENTER);
        
        // Continue button
        Button continueButton = new Button("CONTINUE");
        
        // Match the button style from CharacterSelectGui
        String baseButtonStyle = "-fx-background-color: #1e90ff;" + 
            "-fx-text-fill: white;" +
            "-fx-font-family: 'Monospace';" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 10 20 10 20;" +
            "-fx-border-color: #87cefa;" +
            "-fx-border-width: 2px;" +
            "-fx-background-radius: 0;" +
            "-fx-border-radius: 0;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);";
        
        String hoverButtonStyle = "-fx-background-color: #00bfff;" +
            "-fx-text-fill: white;" +
            "-fx-font-family: 'Monospace';" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 10 20 10 20;" +
            "-fx-border-color: #b0e2ff;" +
            "-fx-border-width: 2px;" +
            "-fx-background-radius: 0;" +
            "-fx-border-radius: 0;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 8, 0, 0, 1);";
        
        continueButton.setStyle(baseButtonStyle);
        continueButton.setPrefWidth(200);
        
        // Add hover effects
        continueButton.setOnMouseEntered(e -> continueButton.setStyle(hoverButtonStyle));
        continueButton.setOnMouseExited(e -> continueButton.setStyle(baseButtonStyle));
        
        // Button action to close the panel
        continueButton.setOnAction(e -> closePanel());
        
        // Add all elements to the content box
        contentBox.getChildren().addAll(titleText, characterBox, statusText, continueButton);
        
        // Add elements to the panel
        getChildren().addAll(overlay, contentBox);
        
        // Start with zero opacity and scale for entrance animation
        setOpacity(0);
        contentBox.setScaleX(0.7);
        contentBox.setScaleY(0.7);
        
        // Create entrance animations
        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), this);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(800), contentBox);
        scaleUp.setFromX(0.7);
        scaleUp.setFromY(0.7);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);
        
        // Play animations
        fadeIn.play();
        scaleUp.play();
        
        // Create pulsing animation for the title
        ScaleTransition pulse = new ScaleTransition(Duration.millis(1000), titleText);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.05);
        pulse.setToY(1.05);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.play();
    }
    
    /**
     * Creates a character display for the ejected player
     * @return A VBox containing the character display
     */
    private VBox createCharacterDisplay() {
        VBox charBox = new VBox(10);
        charBox.setAlignment(Pos.CENTER);
        charBox.setPadding(new Insets(10));
        
        // Load character profile image
        String profilePath = String.format("/player/profile/%02d.png", (ejectedCharId + 1));
        try {
            Image profileImage = new Image(getClass().getResourceAsStream(profilePath));
            ImageView profileView = new ImageView(profileImage);
            
            // Set size matching CharacterSelectGui
            profileView.setFitWidth(150);
            profileView.setFitHeight(150);
            profileView.setPreserveRatio(true);
            
            // Add styling to match CharacterSelectGui
            profileView.setStyle(
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);" +
                "-fx-border-color: #1e90ff;" +
                "-fx-border-width: 2px;" +
                "-fx-background-color: #2a2a3a;"
            );
            
            // Add a space background behind the character
            Rectangle spaceBackground = new Rectangle(200, 200);
            spaceBackground.setFill(Color.rgb(0, 0, 30));
            spaceBackground.setStroke(Color.rgb(30, 50, 100));
            spaceBackground.setStrokeWidth(2);
            spaceBackground.setArcWidth(10);
            spaceBackground.setArcHeight(10);
            
            // Create an ejection animation showing character flying off
            StackPane animationPane = new StackPane();
            animationPane.getChildren().addAll(spaceBackground, profileView);
            
            // Add the animation pane to the character box
            charBox.getChildren().add(animationPane);
            
            // Create a sliding off animation for the character
            Timeline slideOffTimeline = new Timeline();
            KeyFrame frame1 = new KeyFrame(Duration.seconds(0), 
                new javafx.animation.KeyValue(profileView.translateXProperty(), 0));
            KeyFrame frame2 = new KeyFrame(Duration.seconds(1.5), 
                new javafx.animation.KeyValue(profileView.translateXProperty(), 300));
            slideOffTimeline.getKeyFrames().addAll(frame1, frame2);
            slideOffTimeline.setDelay(Duration.seconds(2));
            slideOffTimeline.play();
        
        } catch (Exception e) {
            System.err.println("Error loading profile image: " + e.getMessage());
            
            // Fallback
            Text fallbackText = new Text(ejectedPlayerName);
            fallbackText.setFont(Font.font("Monospace", FontWeight.BOLD, 18));
            fallbackText.setFill(Color.WHITE);
            
            charBox.getChildren().add(fallbackText);
        }
        
        return charBox;
    }
    
    /**
     * Gets the status text based on whether the player was an impostor
     * @return The status text
     */
    private String getStatusText() {
        if (wasImpostor) {
            return ejectedPlayerName + " was an Impostor.";
        } else {
            return ejectedPlayerName + " was not an Impostor.";
        }
    }
    
    /**
     * Closes this panel with a fade-out animation
     */
    private void closePanel() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(500), this);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> {
            if (root != null) {
                root.getChildren().remove(this);
            }
        });
        fadeOut.play();
    }
}
package gui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

/**
 * AboutPanel displays information about the game including version, developers, 
 * and other credits.
 */
public class AboutPanel extends ScrollPane {
private boolean isVisible = false;
private VBox contentBox;
public AboutPanel() {
// Set up the scrollable panel to match window dimensions
setPrefWidth(570);
setPrefHeight(480); // Adjusted to fit typical window height
setMaxWidth(570);
setMaxHeight(480);
 setHbarPolicy(ScrollBarPolicy.NEVER);
 setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
 
 // Style the ScrollPane to match the game's theme
 setStyle(
     "-fx-background: transparent;" +
     "-fx-background-color: rgba(30, 30, 50, 0.95);" +
     "-fx-border-color: #1e90ff;" +
     "-fx-border-width: 2px;" +
     "-fx-padding: 0;"
 );
 
 // Create main content container
 contentBox = new VBox(15);
 contentBox.setAlignment(Pos.TOP_CENTER);
 contentBox.setPadding(new Insets(25));
 contentBox.setBackground(new Background(new BackgroundFill(
         Color.rgb(30, 30, 50, 0.95), 
         new CornerRadii(0), 
         Insets.EMPTY)));
 
 // Ensure content width matches ScrollPane
 contentBox.setPrefWidth(450);
 contentBox.setMaxWidth(450);
 
 // Shadow effect for depth
 DropShadow shadow = new DropShadow();
 shadow.setRadius(10);
 shadow.setOffsetX(3);
 shadow.setOffsetY(3);
 shadow.setColor(Color.rgb(0, 0, 0, 0.6));
 setEffect(shadow);
 
 // Title text
 Text titleText = new Text("AMONG CEDT");
 titleText.setFont(Font.font("Monospace", FontWeight.BOLD, 36));
 titleText.setFill(Color.WHITE);
 titleText.setTextAlignment(TextAlignment.CENTER);
 
 // Version text
 Text versionText = new Text("v.3.001");
 versionText.setFont(Font.font("Monospace", FontWeight.NORMAL, 18));
 versionText.setFill(Color.LIGHTGRAY);
 versionText.setTextAlignment(TextAlignment.CENTER);
 
 // Description text
 Text descText = new Text(
         "Among CEDT is a multiplayer social deduction game inspired by " +
         "popular titles in the genre. Players work together to complete " +
         "tasks while trying to identify impostors among the crew."
 );
 descText.setFont(Font.font("Monospace", FontWeight.NORMAL, 16));
 descText.setFill(Color.WHITE);
 descText.setTextAlignment(TextAlignment.CENTER);
 descText.setWrappingWidth(430);
 
 // Separator
 Region separator = new Region();
 separator.setPrefHeight(1);
 separator.setMinHeight(1);
 separator.setMaxHeight(1);
 separator.setBackground(new Background(new BackgroundFill(
         Color.rgb(100, 150, 255, 0.5), 
         new CornerRadii(0), 
         Insets.EMPTY)));
 separator.setPrefWidth(430);
 
 // Developers section
 Text developersTitle = new Text("DEVELOPMENT TEAM");
 developersTitle.setFont(Font.font("Monospace", FontWeight.BOLD, 20));
 developersTitle.setFill(Color.rgb(100, 200, 255)); // Light blue
 developersTitle.setTextAlignment(TextAlignment.CENTER);
 
 Text developersText = new Text(
         "Lead Developer: [Developer Name]\n" +
         "Graphics Design: [Artist Name]\n" +
         "Sound Design: [Sound Engineer Name]\n" +
         "Additional Programming: [Team Members]"
 );
 developersText.setFont(Font.font("Monospace", FontWeight.NORMAL, 16));
 developersText.setFill(Color.WHITE);
 developersText.setTextAlignment(TextAlignment.CENTER);
 
 // Credits section
 Text creditsTitle = new Text("SPECIAL THANKS");
 creditsTitle.setFont(Font.font("Monospace", FontWeight.BOLD, 20));
 creditsTitle.setFill(Color.rgb(100, 200, 255)); // Light blue
 creditsTitle.setTextAlignment(TextAlignment.CENTER);
 
 Text creditsText = new Text(
         "Faculty Advisors: [Names]\n" +
         "Playtesters: [Names]\n" +
         "And all CEDT students who provided feedback!"
 );
 creditsText.setFont(Font.font("Monospace", FontWeight.NORMAL, 16));
 creditsText.setFill(Color.WHITE);
 creditsText.setTextAlignment(TextAlignment.CENTER);
 
 // Legal text
 Text legalText = new Text(
         "© 2023 CEDT Students. All rights reserved.\n" +
         "This is an educational project and is not intended for commercial use."
 );
 legalText.setFont(Font.font("Monospace", FontWeight.NORMAL, 12));
 legalText.setFill(Color.LIGHTGRAY);
 legalText.setTextAlignment(TextAlignment.CENTER);
 
 // Close button
 Button closeButton = new Button("CLOSE");
 styleButton(closeButton);
 closeButton.setOnAction(e -> hide());
 
 // Add all elements to the panel
 contentBox.getChildren().addAll(
     titleText,
     versionText,
     descText,
     separator,
     developersTitle,
     developersText,
     new Region() {{ setMinHeight(10); }},
     creditsTitle,
     creditsText,
     new Region() {{ setMinHeight(10); }},
     legalText,
     closeButton
 );
 
 // Set the content to the ScrollPane
 this.setContent(contentBox);
 
 // Initially invisible
 this.setOpacity(0);
 this.setVisible(false);
}
    
    /**
     * Applies consistent styling to buttons
     */
    private void styleButton(Button button) {
        String baseStyle = "-fx-background-color: #1e90ff;" +
                           "-fx-text-fill: white;" +
                           "-fx-font-family: 'Monospace';" +
                           "-fx-font-size: 16px;" +
                           "-fx-font-weight: bold;" +
                           "-fx-padding: 8 20 8 20;" +
                           "-fx-border-color: #87cefa;" +
                           "-fx-border-width: 2px;" +
                           "-fx-background-radius: 0;" +
                           "-fx-border-radius: 0;" +
                           "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);";
        
        String hoverStyle = "-fx-background-color: #00bfff;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-family: 'Monospace';" +
                            "-fx-font-size: 16px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-padding: 8 20 8 20;" +
                            "-fx-border-color: #b0e2ff;" +
                            "-fx-border-width: 2px;" +
                            "-fx-background-radius: 0;" +
                            "-fx-border-radius: 0;" +
                            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 8, 0, 0, 1);";
        
        button.setStyle(baseStyle);
        button.setPrefWidth(180);
        
        // Add hover effects
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));
    }
    
    /**
     * Shows the panel with a fade-in animation
     */
    public void show() {
        if (isVisible) return;
        
        // Make sure we're on the JavaFX thread
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::show);
            return;
        }
        
        setVisible(true);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), this);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
        
        isVisible = true;
    }
    
    /**
     * Hides the panel with a fade-out animation
     */
    public void hide() {
        if (!isVisible) return;
        
        // Make sure we're on the JavaFX thread
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::hide);
            return;
        }
        
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), this);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            setVisible(false);
            // Make sure panel is fully hidden
            setOpacity(0);
        });
        fadeOut.play();
        
        isVisible = false;
    }
}
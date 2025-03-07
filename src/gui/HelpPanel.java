package gui;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
/**

HelpPanel provides game instructions, controls, and tips for players.
*/
public class HelpPanel extends VBox {
private boolean isVisible = false;
public HelpPanel() {
// Set up the panel with styling that matches the game's theme
setPrefWidth(670);
setPrefHeight(480); // Adjusted to fit typical window height
setMaxWidth(670);
setMaxHeight(480);
setPadding(new Insets(25));
 setSpacing(15);
 setAlignment(Pos.TOP_CENTER);
 
 // Dark blue-gray background matching game theme
 setBackground(new Background(new BackgroundFill(
         Color.rgb(30, 30, 50, 0.95), 
         new CornerRadii(0), 
         Insets.EMPTY)));
 
 // Blue border to match game theme
 setBorder(new Border(new BorderStroke(
         Color.rgb(30, 144, 255), 
         BorderStrokeStyle.SOLID, 
         new CornerRadii(0), 
         new BorderWidths(3))));
 
 // Shadow effect for depth
 DropShadow shadow = new DropShadow();
 shadow.setRadius(10);
 shadow.setOffsetX(3);
 shadow.setOffsetY(3);
 shadow.setColor(Color.rgb(0, 0, 0, 0.6));
 setEffect(shadow);
 
 // Title text
 Text titleText = new Text("GAME HELP");
 titleText.setFont(Font.font("Monospace", FontWeight.BOLD, 36));
 titleText.setFill(Color.WHITE);
 titleText.setTextAlignment(TextAlignment.CENTER);
 
 // Create tab pane for different help topics
 TabPane tabPane = new TabPane();
 tabPane.setPrefHeight(350); // Leave room for title and close button
 tabPane.setMaxHeight(350);
 tabPane.setStyle(
     "-fx-background-color: transparent;" +
     "-fx-tab-min-height: 30px;" +
     "-fx-tab-max-height: 30px;"
 );
 
 // Ensure only vertical scrolling in tab content
 tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
 
 // Add tabs for different help sections
 Tab controlsTab = createControlsTab();
 Tab rolesTab = createRolesTab();
 Tab gameplayTab = createGameplayTab();
 Tab tasksTab = createTasksTab();
 
 tabPane.getTabs().addAll(controlsTab, rolesTab, gameplayTab, tasksTab);
 VBox.setVgrow(tabPane, Priority.ALWAYS);
 
 // Close button
 Button closeButton = new Button("CLOSE");
 styleButton(closeButton);
 closeButton.setOnAction(e -> hide());
 
 // Add all elements to the panel
 getChildren().addAll(
     titleText,
     tabPane,
     closeButton
 );
 
 // Initially invisible
 setOpacity(0);
 setVisible(false);
}

    private Tab createControlsTab() {
        Tab tab = new Tab("CONTROLS");
        styleTab(tab);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setBackground(new Background(new BackgroundFill(
                Color.rgb(40, 40, 60, 0.8), 
                new CornerRadii(0), 
                Insets.EMPTY)));
        
        Text controlsTitle = new Text("KEYBOARD CONTROLS");
        controlsTitle.setFont(Font.font("Monospace", FontWeight.BOLD, 22));
        controlsTitle.setFill(Color.rgb(100, 200, 255)); // Light blue
        
        VBox controlsList = new VBox(10);
        
        addControlItem(controlsList, "W", "Move Up");
        addControlItem(controlsList, "A", "Move Left");
        addControlItem(controlsList, "S", "Move Down");
        addControlItem(controlsList, "D", "Move Right");
        addControlItem(controlsList, "F", "Interact / Kill (if Impostor)");
        addControlItem(controlsList, "R", "Report Body");
        addControlItem(controlsList, "C", "Toggle Collision View (Debug)");
        
        Text additionalInfo = new Text(
            "During meetings, use the mouse to vote and chat with other players."
        );
        additionalInfo.setFont(Font.font("Monospace", FontWeight.NORMAL, 16));
        additionalInfo.setFill(Color.LIGHTGRAY);
        additionalInfo.setWrappingWidth(600);
        
        content.getChildren().addAll(controlsTitle, controlsList, new Region() {{ setMinHeight(20); }}, additionalInfo);
        
        ScrollPane scrollPane = createStyledScrollPane(content);
        tab.setContent(scrollPane);
        
        return tab;
    }
    
    private Tab createRolesTab() {
        Tab tab = new Tab("ROLES");
        styleTab(tab);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setBackground(new Background(new BackgroundFill(
                Color.rgb(40, 40, 60, 0.8), 
                new CornerRadii(0), 
                Insets.EMPTY)));
        
        // Crewmate section
        Text crewTitle = new Text("CREWMATE");
        crewTitle.setFont(Font.font("Monospace", FontWeight.BOLD, 22));
        crewTitle.setFill(Color.rgb(100, 200, 255)); // Light blue
        
        Text crewInfo = new Text(
            "As a Crewmate, your goal is to complete\nall tasks and identify the Impostors.\n\n" +
            "• Complete all tasks assigned to you\n" +
            "• Report dead bodies when you find them\n" +
            "• Use emergency meetings to discuss suspicious behavior\n" +
            "• Vote to eject Impostors from the crew\n" +
            "• Work with other Crewmates to win"
        );
        crewInfo.setFont(Font.font("Monospace", FontWeight.NORMAL, 16));
        crewInfo.setFill(Color.WHITE);
        crewInfo.setWrappingWidth(600);
        
        // Impostor section
        Text imposterTitle = new Text("IMPOSTOR");
        imposterTitle.setFont(Font.font("Monospace", FontWeight.BOLD, 22));
        imposterTitle.setFill(Color.rgb(255, 100, 100)); // Red
        
        Text imposterInfo = new Text(
            "As an Impostor, your goal is to eliminate the Crewmates without being caught.\n\n" +
            "• Eliminate Crewmates by pressing F when close to them\n" +
            "• Blend in by pretending to do tasks\n" +
            "• Create alibis and avoid suspicion\n" +
            "• Use reports strategically to create confusion\n" +
            "• Work with fellow Impostors to outnumber the crew"
        );
        imposterInfo.setFont(Font.font("Monospace", FontWeight.NORMAL, 16));
        imposterInfo.setFill(Color.WHITE);
        imposterInfo.setWrappingWidth(600);
        
        // Ghost section
        Text ghostTitle = new Text("GHOST");
        ghostTitle.setFont(Font.font("Monospace", FontWeight.BOLD, 22));
        ghostTitle.setFill(Color.rgb(180, 180, 255)); // Light purple
        
        Text ghostInfo = new Text(
            "When you're eliminated, you become a Ghost.\n\n" +
            "• As a Crewmate Ghost, you can still complete tasks to help your team\n" +
            "• Ghost players can pass through walls and move freely\n" +
            "• Only other ghosts can see your messages in chat\n" +
            "• You can observe the gameplay\nbut cannot interact with living players"
        );
        ghostInfo.setFont(Font.font("Monospace", FontWeight.NORMAL, 16));
        ghostInfo.setFill(Color.WHITE);
        ghostInfo.setWrappingWidth(600);
        
        content.getChildren().addAll(crewTitle, crewInfo, new Region() {{ setMinHeight(20); }}, 
                                    imposterTitle, imposterInfo, new Region() {{ setMinHeight(20); }},
                                    ghostTitle, ghostInfo);
        
        ScrollPane scrollPane = createStyledScrollPane(content);
        tab.setContent(scrollPane);
        
        return tab;
    }
    
    private Tab createGameplayTab() {
        Tab tab = new Tab("GAMEPLAY");
        styleTab(tab);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setBackground(new Background(new BackgroundFill(
                Color.rgb(40, 40, 60, 0.8), 
                new CornerRadii(0), 
                Insets.EMPTY)));
        
        // Game Flow
        Text phaseTitle = new Text("GAME PHASES");
        phaseTitle.setFont(Font.font("Monospace", FontWeight.BOLD, 22));
        phaseTitle.setFill(Color.rgb(100, 200, 255)); // Light blue
        
        Text phaseInfo = new Text(
            "1. Preparation Phase\n" +
            "   • Select your character\n" +
            "   • Wait for all players to be ready\n\n" +
            "2. Game Start\n" +
            "   • Roles are assigned (Crewmate or Impostor)\n" +
            "   • Players are teleported to starting positions\n\n" +
            "3. Main Game\n" +
            "   • Crewmates complete tasks to fill the task bar\n" +
            "   • Impostors eliminate Crewmates\n" +
            "   • Players can report bodies or call emergency meetings\n\n" +
            "4. Meetings\n" +
            "   • Discuss who might be an Impostor\n" +
            "   • Vote to eject a suspected player\n" +
            "   • Skip voting if unsure\n\n" +
            "5. Game End\n" +
            "   • Crewmates win by completing all tasks or ejecting all Impostors\n" +
            "   • Impostors win by eliminating enough Crewmates\nto equal their numbers"
        );
        phaseInfo.setFont(Font.font("Monospace", FontWeight.NORMAL, 16));
        phaseInfo.setFill(Color.WHITE);
        phaseInfo.setWrappingWidth(600);
        
        // Tips section
        Text tipsTitle = new Text("GAMEPLAY TIPS");
        tipsTitle.setFont(Font.font("Monospace", FontWeight.BOLD, 22));
        tipsTitle.setFill(Color.rgb(100, 200, 255)); // Light blue
        
        Text tipsInfo = new Text(
            "• Stay with other players to avoid\nbeing eliminated when alone\n" +
            "• Watch for players who aren't doing tasks or are moving suspiciously\n" +
            "• Don't falsely accuse without evidence\n" +
            "• Pay attention to where other players were during meetings\n" +
            "• As Impostor, wait for opportunities when players are isolated\n" +
            "• Don't rush to report bodies - observe who's nearby first\n" +
            "• Communicate clearly during meetings"
        );
        tipsInfo.setFont(Font.font("Monospace", FontWeight.NORMAL, 16));
        tipsInfo.setFill(Color.WHITE);
        tipsInfo.setWrappingWidth(600);
        
        content.getChildren().addAll(phaseTitle, phaseInfo, new Region() {{ setMinHeight(20); }}, 
                                    tipsTitle, tipsInfo);
        
        ScrollPane scrollPane = createStyledScrollPane(content);
        tab.setContent(scrollPane);
        
        return tab;
    }
    
    private Tab createTasksTab() {
        Tab tab = new Tab("TASKS");
        styleTab(tab);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setBackground(new Background(new BackgroundFill(
                Color.rgb(40, 40, 60, 0.8), 
                new CornerRadii(0), 
                Insets.EMPTY)));
        
        Text tasksTitle = new Text("COMPLETING TASKS");
        tasksTitle.setFont(Font.font("Monospace", FontWeight.BOLD, 22));
        tasksTitle.setFill(Color.rgb(100, 200, 255)); // Light blue
        
        Text tasksIntro = new Text(
            "Tasks are represented by interactive objects around the map. " +
            "Approach a task and press F to interact with it.\nEach task has unique instructions. " +
            "Here are some common tasks you'll encounter:"
        );
        tasksIntro.setFont(Font.font("Monospace", FontWeight.NORMAL, 16));
        tasksIntro.setFill(Color.WHITE);
        tasksIntro.setWrappingWidth(600);
        
        VBox tasksList = new VBox(15);
        
        addTaskItem(tasksList, "Fix the Lights", 
            "Toggle switches until all lights are on to restore power.");
        
        addTaskItem(tasksList, "Security Code", 
            "Enter the correct 4-digit code displayed in the hint.");
            
        addTaskItem(tasksList, "Fishing", 
            "Press C when fish cross the center mark to catch them.");
            
        addTaskItem(tasksList, "Take Out Trash", 
            "Drag trash bags to the bin to dispose of them.");
            
        addTaskItem(tasksList, "Clean Dishes", 
            "Move the sponge over the dirty dish until it's clean.");
            
        addTaskItem(tasksList, "Extinguish Fires", 
            "Drag the fire extinguisher to put out all visible fires.");
            
        addTaskItem(tasksList, "Tidy Bookshelf", 
            "Arrange books in their correct positions on the shelves.");
        
        Text emergencyInfo = new Text(
            "EMERGENCY BUTTON: Located in the center of the map,\npress F to call an emergency meeting."
        );
        emergencyInfo.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        emergencyInfo.setFill(Color.rgb(255, 200, 100)); // Orange
        emergencyInfo.setWrappingWidth(600);
        
        content.getChildren().addAll(tasksTitle, tasksIntro, new Region() {{ setMinHeight(10); }}, 
                                    tasksList, new Region() {{ setMinHeight(20); }}, emergencyInfo);
        
        ScrollPane scrollPane = createStyledScrollPane(content);
        tab.setContent(scrollPane);
        
        return tab;
    }
    
    private void addControlItem(VBox container, String key, String action) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        
        Text keyText = new Text(key);
        keyText.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        keyText.setFill(Color.WHITE);
        
        // Create a key box with styling
        StackPane keyBox = new javafx.scene.layout.StackPane();
        keyBox.setBackground(new Background(new BackgroundFill(
                Color.rgb(60, 60, 100, 0.8), 
                new CornerRadii(4), 
                Insets.EMPTY)));
        keyBox.setBorder(new Border(new BorderStroke(
                Color.rgb(100, 150, 255, 0.8), 
                BorderStrokeStyle.SOLID, 
                new CornerRadii(4), 
                new BorderWidths(1))));
        keyBox.setPadding(new Insets(5, 10, 5, 10));
        keyBox.getChildren().add(keyText);
        keyBox.setPrefWidth(50);
        keyBox.setMaxWidth(50);
        keyBox.setAlignment(Pos.CENTER);
        
        Text actionText = new Text(action);
        actionText.setFont(Font.font("Monospace", FontWeight.NORMAL, 16));
        actionText.setFill(Color.LIGHTGRAY);
        
        item.getChildren().addAll(keyBox, actionText);
        container.getChildren().add(item);
    }
    
    private void addTaskItem(VBox container, String taskName, String description) {
        VBox taskItem = new VBox(5);
        
        Text nameText = new Text(taskName);
        nameText.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
        nameText.setFill(Color.rgb(100, 200, 255)); // Light blue
        
        Text descText = new Text(description);
        descText.setFont(Font.font("Monospace", FontWeight.NORMAL, 14));
        descText.setFill(Color.WHITE);
        descText.setWrappingWidth(600);
        
        taskItem.getChildren().addAll(nameText, descText);
        container.getChildren().add(taskItem);
    }
    
    private ScrollPane createStyledScrollPane(VBox content) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle(
            "-fx-background: transparent;" +
            "-fx-background-color: transparent;" +
            "-fx-padding: 0;" +
            "-fx-border-color: transparent;"
        );
        
        return scrollPane;
    }
    
    private void styleTab(Tab tab) {
        // Custom styling for tabs to match game aesthetic
        tab.setStyle(
            "-fx-background-color: #1e374d;" +
            "-fx-text-base-color: white;" +
            "-fx-font-family: 'Monospace';" +
            "-fx-font-weight: bold;" +
            "-fx-padding: 3px 10px;"
        );
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
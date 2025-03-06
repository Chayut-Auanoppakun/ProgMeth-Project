package Task;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

class Task1 extends Task {
    private int switchesOn = 0;
    private final int totalSwitches = 8;
    
    public Task1(TaskPane parent) {
        super(parent, "task1");
        
        // Set pixelated style background with brown color matching the interior walls
        setStyle("-fx-background-color: #8b6c42; -fx-border-color: #555; -fx-border-width: 2px;");
        
        // Title panel with darker brown background
        HBox titlePanel = new HBox();
        titlePanel.setStyle("-fx-background-color: #6d4e2a; -fx-padding: 10px;");
        titlePanel.setPrefWidth(960);
        titlePanel.setAlignment(Pos.CENTER);
        
        // Task title with pixelated font
        Text title = new Text("Fix the Lights");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
        title.setFill(Color.WHITE);
        titlePanel.getChildren().add(title);
        
        // Panel for task content with slightly lighter brown
        VBox contentPanel = new VBox(20);
        contentPanel.setStyle("-fx-background-color: #a88c60; -fx-padding: 20px; -fx-border-color: #6d4e2a; -fx-border-width: 2px;");
        contentPanel.setAlignment(Pos.CENTER);
        contentPanel.setMaxWidth(700);
        
        // Task instructions 
        Text instructions = new Text("Turn on all the switches to restore power.");
        instructions.setFont(Font.font("Monospace", FontWeight.NORMAL, 18));
        instructions.setFill(Color.WHITE);
        
        // Create a pixelated monitor/control panel image as background for switches
        ImageView controlPanel = new ImageView();
        try {
            // Try to load from resources - fallback to colored panel if not available
            Image panelImage = new Image(getClass().getResourceAsStream("/TaskAsset/controlpanel.png"));
            controlPanel.setImage(panelImage);
            controlPanel.setFitWidth(500);
            controlPanel.setPreserveRatio(true);
        } catch (Exception e) {
            // Fallback to a colored rectangle if image loading fails
            controlPanel = null;
        }
        
        // Create a grid of switches
        GridPane switchGrid = new GridPane();
        switchGrid.setHgap(20);
        switchGrid.setVgap(20);
        switchGrid.setAlignment(Pos.CENTER);
        switchGrid.setStyle("-fx-background-color: #64553a; -fx-padding: 20px; -fx-border-color: #444; -fx-border-width: 3px; -fx-border-radius: 5px;");
        
        // Randomly turn on some switches at the start
        java.util.Random random = new java.util.Random();
        boolean[] initialStates = new boolean[totalSwitches];
        switchesOn = 0; // Ensure counter starts at zero
        
        for (int i = 0; i < 3; i++) { // Start with 3 switches on
            int index = random.nextInt(totalSwitches);
            // Only increment counter if this switch wasn't already turned on
            if (!initialStates[index]) {
                initialStates[index] = true;
                switchesOn++;
            } else {
                // Try again if this switch was already on
                i--;
            }
        }
        
        // Create the switches with pixelated style
        for (int i = 0; i < totalSwitches; i++) {
            final int switchIndex = i;
            Button switchBtn = new Button(initialStates[i] ? "ON" : "OFF");
            switchBtn.setPrefSize(80, 40);
            
            // Apply pixelated style to match game aesthetic
            if (initialStates[i]) {
                switchBtn.setStyle("-fx-background-color: #4CAF50; " + 
                                  "-fx-text-fill: white; " +
                                  "-fx-font-family: 'Monospace'; " +
                                  "-fx-font-weight: bold; " +
                                  "-fx-font-size: 16px; " +
                                  "-fx-border-color: #2e7d32; " +
                                  "-fx-border-width: 3px; " +
                                  "-fx-background-radius: 0; " +
                                  "-fx-border-radius: 0;");
            } else {
                switchBtn.setStyle("-fx-background-color: #f44336; " + 
                                  "-fx-text-fill: white; " +
                                  "-fx-font-family: 'Monospace'; " +
                                  "-fx-font-weight: bold; " +
                                  "-fx-font-size: 16px; " +
                                  "-fx-border-color: #d32f2f; " +
                                  "-fx-border-width: 3px; " +
                                  "-fx-background-radius: 0; " +
                                  "-fx-border-radius: 0;");
            }
            
            // Hover effects matching pixel art style
            switchBtn.setOnMouseEntered(e -> {
                String baseStyle = switchBtn.getText().equals("ON") ? 
                    "-fx-background-color: #66BB6A; " : "-fx-background-color: #EF5350; ";
                    
                switchBtn.setStyle(baseStyle + 
                                  "-fx-text-fill: white; " +
                                  "-fx-font-family: 'Monospace'; " +
                                  "-fx-font-weight: bold; " +
                                  "-fx-font-size: 16px; " +
                                  "-fx-border-color: " + (switchBtn.getText().equals("ON") ? "#388E3C" : "#C62828") + "; " +
                                  "-fx-border-width: 3px; " +
                                  "-fx-background-radius: 0; " +
                                  "-fx-border-radius: 0;");
            });
            
            switchBtn.setOnMouseExited(e -> {
                String baseStyle = switchBtn.getText().equals("ON") ? 
                    "-fx-background-color: #4CAF50; " : "-fx-background-color: #f44336; ";
                    
                switchBtn.setStyle(baseStyle + 
                                  "-fx-text-fill: white; " +
                                  "-fx-font-family: 'Monospace'; " +
                                  "-fx-font-weight: bold; " +
                                  "-fx-font-size: 16px; " +
                                  "-fx-border-color: " + (switchBtn.getText().equals("ON") ? "#2e7d32" : "#d32f2f") + "; " +
                                  "-fx-border-width: 3px; " +
                                  "-fx-background-radius: 0; " +
                                  "-fx-border-radius: 0;");
            });
            
            switchBtn.setOnAction(e -> {
                boolean isOn = switchBtn.getText().equals("ON");
                if (isOn) {
                    switchBtn.setText("OFF");
                    switchBtn.setStyle("-fx-background-color: #f44336; " + 
                                      "-fx-text-fill: white; " +
                                      "-fx-font-family: 'Monospace'; " +
                                      "-fx-font-weight: bold; " +
                                      "-fx-font-size: 16px; " +
                                      "-fx-border-color: #d32f2f; " +
                                      "-fx-border-width: 3px; " +
                                      "-fx-background-radius: 0; " +
                                      "-fx-border-radius: 0;");
                    switchesOn--;
                } else {
                    switchBtn.setText("ON");
                    switchBtn.setStyle("-fx-background-color: #4CAF50; " + 
                                      "-fx-text-fill: white; " +
                                      "-fx-font-family: 'Monospace'; " +
                                      "-fx-font-weight: bold; " +
                                      "-fx-font-size: 16px; " +
                                      "-fx-border-color: #2e7d32; " +
                                      "-fx-border-width: 3px; " +
                                      "-fx-background-radius: 0; " +
                                      "-fx-border-radius: 0;");
                    switchesOn++;
                }
                
                // Check if all switches are on
                if (switchesOn == totalSwitches) {
                    completeTask();
                }
            });
            
            switchGrid.add(switchBtn, i % 4, i / 4);
        }
        
        // Add the control panel as the background for the switches, if loaded
        if (controlPanel != null) {
            contentPanel.getChildren().addAll(instructions, controlPanel, switchGrid);
        } else {
            contentPanel.getChildren().addAll(instructions, switchGrid);
        }
        
        // Put everything together
        VBox mainContainer = new VBox(0); // No spacing in the VBox
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.getChildren().add(titlePanel);
        
        // Add the content panel with explicit margin to move it down
        VBox.setMargin(contentPanel, new javafx.geometry.Insets(50, 0, 0, 0)); // 50px top margin
        mainContainer.getChildren().add(contentPanel);
        
        getChildren().add(mainContainer);
        getChildren().add(getCloseButton());
    }

    @Override
    protected void initializeTask() {
        // Task initialization happens in the constructor
    }

    @Override
    public boolean isCompleted() {
        return switchesOn == totalSwitches;
    }
}
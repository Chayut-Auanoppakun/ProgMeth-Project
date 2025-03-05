package Task;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

class Task1 extends Task {
    private int switchesOn = 0;
    private final int totalSwitches = 8;
    private boolean[] switchStates;
    private Button[] switchButtons;
    
    public Task1(TaskPane parent) {
        super(parent, "task1");
    }
    
    @Override
    protected void initializeTask() {
        // Task title
        Text title = new Text("Fix the Lights");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setFill(Color.WHITE);
        
        // Task instructions
        Text instructions = new Text("Turn on all the switches to restore power.");
        instructions.setFill(Color.LIGHTGRAY);
        
        // Create a grid of switches
        javafx.scene.layout.GridPane switchGrid = new javafx.scene.layout.GridPane();
        switchGrid.setHgap(20);
        switchGrid.setVgap(20);
        switchGrid.setAlignment(Pos.CENTER);
        
        // Initialize switch states
        switchStates = new boolean[totalSwitches];
        switchButtons = new Button[totalSwitches];
        
        // Randomly turn on some switches at the start
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 3; i++) { // Start with 3 switches on
            int index = random.nextInt(totalSwitches);
            switchStates[index] = true;
            switchesOn++;
        }
        
        // Create the switches
        for (int i = 0; i < totalSwitches; i++) {
            final int switchIndex = i;
            switchButtons[i] = new Button(switchStates[i] ? "ON" : "OFF");
            switchButtons[i].setPrefSize(80, 40);
            
            if (switchStates[i]) {
                switchButtons[i].setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            } else {
                switchButtons[i].setStyle("-fx-background-color: #555; -fx-text-fill: white;");
            }
            
            switchButtons[i].setOnAction(e -> toggleSwitch(switchIndex));
            
            switchGrid.add(switchButtons[i], i % 4, i / 4);
        }
        
        // Put everything together
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(title, instructions, switchGrid);
        
        getChildren().add(content);
    }
    
    /**
     * Toggles a switch on or off
     * @param index The index of the switch to toggle
     */
    private void toggleSwitch(int index) {
        if (index < 0 || index >= totalSwitches) return;
        
        // Toggle the switch state
        switchStates[index] = !switchStates[index];
        
        // Update the switch appearance
        if (switchStates[index]) {
            switchButtons[index].setText("ON");
            switchButtons[index].setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            switchesOn++;
        } else {
            switchButtons[index].setText("OFF");
            switchButtons[index].setStyle("-fx-background-color: #555; -fx-text-fill: white;");
            switchesOn--;
        }
        
        // Check if all switches are on
        if (isCompleted()) {
            completeTask();
        }
    }
    
    @Override
    public boolean isCompleted() {
        return switchesOn == totalSwitches;
    }
}

package Task;

import java.util.Random;	
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import logic.SoundLogic;
import javafx.application.Platform;

class Task3 extends Task {
    private final String correctCode = generateRandomCode();
    private StringBuilder enteredCode = new StringBuilder();
    private Text displayText;
    private Text codeHint;
    
    public Task3(TaskPane parent, String taskId) {
        super(parent, taskId);
        
        // Pixel-art style background
        setStyle("-fx-background-color: #8b6c42; -fx-border-color: #555; -fx-border-width: 2px;");
        
        // Title panel with darker brown background
        HBox titlePanel = new HBox();
        titlePanel.setStyle("-fx-background-color: #6d4e2a; -fx-padding: 10px;");
        titlePanel.setPrefWidth(960);
        titlePanel.setAlignment(Pos.CENTER);
        
        // Task title with pixelated font
        Text title = new Text("Enter Security Code");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
        title.setFill(Color.WHITE);
        titlePanel.getChildren().add(title);
        
        // Content panel with pixel-art styling
        VBox contentPanel = new VBox(20);
        contentPanel.setStyle("-fx-background-color: #a88c60; -fx-padding: 20px; -fx-border-color: #6d4e2a; -fx-border-width: 2px;");
        contentPanel.setAlignment(Pos.CENTER);
        contentPanel.setMaxWidth(700);
        
        // Pixel-art style instructions
        Text instructions = new Text("Enter the 4-digit security code.");
        instructions.setFont(Font.font("Monospace", FontWeight.NORMAL, 18));
        instructions.setFill(Color.WHITE);
        
        // Code hint (initially hidden)
        codeHint = new Text("Hint: " + correctCode);
        codeHint.setFont(Font.font("Monospace", FontWeight.NORMAL, 16));
        codeHint.setFill(Color.LIGHTGRAY);
        codeHint.setVisible(false);
        
        // Hint button with pixel-art styling
        Button hintButton = new Button("Show Hint");
        hintButton.setStyle(
            "-fx-background-color: #1976d2; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-font-family: 'Monospace'; " +
            "-fx-border-color: #115293; " +
            "-fx-border-width: 2px;"
        );
        hintButton.setOnAction(e -> codeHint.setVisible(true));
        
        // Display for entered code with pixel-art styling
        displayText = new Text("_ _ _ _");
        displayText.setFont(Font.font("Monospace", FontWeight.BOLD, 36));
        displayText.setFill(Color.LIGHTGREEN);
        
        // Create keypad with pixel-art styling
        javafx.scene.layout.GridPane keypad = new javafx.scene.layout.GridPane();
        keypad.setHgap(10);
        keypad.setVgap(10);
        keypad.setAlignment(Pos.CENTER);
        keypad.setStyle(
            "-fx-background-color: #64553a; " +
            "-fx-padding: 20px; " +
            "-fx-border-color: #444; " +
            "-fx-border-width: 3px; " +
            "-fx-border-radius: 5px;"
        );
        
        // Add number buttons (1-9)
        for (int i = 1; i <= 9; i++) {
            final String num = String.valueOf(i);
            Button numButton = createKeypadButton(num);
            keypad.add(numButton, (i - 1) % 3, (i - 1) / 3);
        }
        
        // Add special buttons (Clear, 0, Enter)
        Button clearButton = new Button("Clear");
        clearButton.setPrefSize(80, 60);
        clearButton.setStyle(
            "-fx-background-color: #d32f2f; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-font-family: 'Monospace'; " +
            "-fx-border-color: #9A1818; " +
            "-fx-border-width: 2px;"
        );
        clearButton.setOnAction(e -> {
        	SoundLogic.playSound("assets/sounds/panel_enterID01.wav", 0);
            enteredCode.setLength(0);
            updateDisplay();
        });
        
        Button zeroButton = createKeypadButton("0");
        
        Button enterButton = new Button("Enter");
        enterButton.setPrefSize(80, 60);
        enterButton.setStyle(
            "-fx-background-color: #388e3c; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-font-family: 'Monospace'; " +
            "-fx-border-color: #2E7D32; " +
            "-fx-border-width: 2px;"
        );
        enterButton.setOnAction(e -> {
            if (enteredCode.toString().equals(correctCode)) {
                displayText.setText("ACCESS GRANTED");
                displayText.setFill(Color.GREEN);
                
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        Platform.runLater(() -> completeTask());
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }).start();
            } else {
                displayText.setText("ACCESS DENIED");
                displayText.setFill(Color.RED);
                
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        Platform.runLater(() -> {
                            enteredCode.setLength(0);
                            updateDisplay();
                        });
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }
        });
        
        keypad.add(clearButton, 0, 3);
        keypad.add(zeroButton, 1, 3);
        keypad.add(enterButton, 2, 3);
        
        // Assemble the main container
        VBox mainContainer = new VBox(0);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.getChildren().add(titlePanel);
        
        VBox.setMargin(contentPanel, new javafx.geometry.Insets(50, 0, 0, 0));
        contentPanel.getChildren().addAll(instructions, hintButton, codeHint, displayText, keypad);
        mainContainer.getChildren().add(contentPanel);
        
        getChildren().addAll(mainContainer, getCloseButton());
    }
    
    private Button createKeypadButton(String text) {
        Button button = new Button(text);
        button.setPrefSize(80, 60);
        button.setStyle(
            "-fx-background-color: #424242; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 18px; " +
            "-fx-font-weight: bold; " +
            "-fx-font-family: 'Monospace'; " +
            "-fx-border-color: #1E1E1E; " +
            "-fx-border-width: 2px;"
        );
        
        button.setOnAction(e -> {
            if (enteredCode.length() < 4) {
                enteredCode.append(text);
                updateDisplay();
            }
        });
        
        return button;
    }
    
    private void updateDisplay() {
        StringBuilder display = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i < enteredCode.length()) {
                display.append(enteredCode.charAt(i));
            } else {
                display.append("_");
            }
            if (i < 3) {
                display.append(" ");
            }
        }
        displayText.setText(display.toString());
        displayText.setFill(Color.LIGHTGREEN);
    }
    
    private String generateRandomCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    

    @Override
    public boolean isCompleted() {
        return false;
    }
}
package Task;

import javafx.scene.text.Text;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;


/**
 * Sample implementation of Task 3: Keypad Code Task
 */
class Task3 extends Task {
    private final String correctCode = generateRandomCode();
    private StringBuilder enteredCode = new StringBuilder();
    private Text displayText;
    private boolean codeCorrect = false;
    
    public Task3(TaskPane parent) {
        super(parent, "task3");
    }
    
    @Override
    protected void initializeTask() {
        // Task title
        Text title = new Text("Enter Security Code");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setFill(Color.WHITE);
        
        // Code hint
        Text codeHint = new Text("Hint: " + correctCode);
        codeHint.setFill(Color.LIGHTGRAY);
        
        // Display for entered code
        displayText = new Text("_ _ _ _");
        displayText.setFont(Font.font("Monospace", FontWeight.BOLD, 36));
        displayText.setFill(Color.LIGHTGREEN);
        
        // Create keypad
        javafx.scene.layout.GridPane keypad = new javafx.scene.layout.GridPane();
        keypad.setHgap(10);
        keypad.setVgap(10);
        keypad.setAlignment(Pos.CENTER);
        
        // Add number buttons (1-9)
        for (int i = 1; i <= 9; i++) {
            final String num = String.valueOf(i);
            Button numButton = createKeypadButton(num);
            keypad.add(numButton, (i - 1) % 3, (i - 1) / 3);
        }
        
        // Add special buttons (Clear, 0, Enter)
        Button clearButton = new Button("Clear");
        clearButton.setPrefSize(80, 60);
        clearButton.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold;");
        clearButton.setOnAction(e -> clearCode());
        
        Button zeroButton = createKeypadButton("0");
        
        Button enterButton = new Button("Enter");
        enterButton.setPrefSize(80, 60);
        enterButton.setStyle("-fx-background-color: #388e3c; -fx-text-fill: white; -fx-font-weight: bold;");
        enterButton.setOnAction(e -> checkCode());
        
        // Add bottom row
        keypad.add(clearButton, 0, 3);
        keypad.add(zeroButton, 1, 3);
        keypad.add(enterButton, 2, 3);
        
        // Put everything together
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(title, codeHint, displayText, keypad);
        
        getChildren().add(content);
    }
    
    private Button createKeypadButton(String text) {
        Button button = new Button(text);
        button.setPrefSize(80, 60);
        button.setStyle("-fx-background-color: #424242; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        
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
    
    private void clearCode() {
        enteredCode.setLength(0);
        updateDisplay();
    }
    
    private void checkCode() {
        if (enteredCode.toString().equals(correctCode)) {
            displayText.setText("ACCESS GRANTED");
            displayText.setFill(Color.GREEN);
            codeCorrect = true;
            
            // Complete task after a delay
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
            
            // Reset after delay
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
    }
    
    private String generateRandomCode() {
        java.util.Random random = new java.util.Random();
        StringBuilder code = new StringBuilder();
        
        for (int i = 0; i < 4; i++) {
            code.append(random.nextInt(10));
        }
        
        return code.toString();
    }
    
    @Override
    public boolean isCompleted() {
        return codeCorrect;
    }
}


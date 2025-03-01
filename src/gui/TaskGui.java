package gui;

import java.util.Random;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import logic.TaskLogic;

public class TaskGui extends Pane{
	private int taskId;
	public TaskGui(int taskId) {
        this.taskId = taskId;
        this.setPrefWidth(1300);
        this.setPrefHeight(900);
        this.setStyle("-fx-background-color: #1e1e1e;"); // Dark background

        // Create Label
        Label label = new Label("Task " + taskId);
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-font-weight: bold;");

        // Container for the Label
        VBox labelBox = new VBox(5, label);
        labelBox.setPadding(new Insets(10));
        labelBox.setStyle("-fx-border-color: #555; -fx-border-width: 2px;");

        // Close Button (X)
        Button closeButton = new Button("X");
        closeButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-weight: bold; -fx-border-radius: 5px;");
        closeButton.setOnAction(e -> {
            if (this.getParent() instanceof Pane parent) {
                parent.getChildren().remove(this); // Remove this TaskPane
            }
        });

        // Position the close button at the top-right
        closeButton.setLayoutX(this.getPrefWidth() - 40); // Adjust based on button width
        closeButton.setLayoutY(10); // Margin from the top

        // Add components to the Pane
        this.getChildren().addAll(labelBox, closeButton);
		
		
		switch(taskId) {
		case 1:
			task1();
			break;
		case 2:
			task2();
			break;
		case 3:
			task3();
			break;
		default:
			break;
		}
	}
	public void task1() {
	    BorderPane root = new BorderPane();
	    root.prefWidthProperty().bind(this.widthProperty());
	    root.prefHeightProperty().bind(this.heightProperty());

	    VBox leftSwitches = new VBox();
	    VBox rightSwitches = new VBox();
	    leftSwitches.setAlignment(Pos.CENTER);
	    rightSwitches.setAlignment(Pos.CENTER);

	    HBox switchContainer = new HBox(50);
	    switchContainer.setAlignment(Pos.CENTER);

	    Image switchOnImage = new Image("/FixLight/On.png");
	    Image switchOffImage = new Image("/FixLight/Off.png");

	    ToggleButton[] switches = new ToggleButton[8];

	    for (int i = 0; i < 8; i++) {
	        ImageView switchIcon = new ImageView(switchOffImage);

	        // Dynamically scale switch size based on window size
	        switchIcon.fitWidthProperty().bind(this.widthProperty().multiply(0.1));
	        switchIcon.fitHeightProperty().bind(this.heightProperty().multiply(0.1));

	        switches[i] = new ToggleButton();
	        switches[i].setGraphic(switchIcon);
	        switches[i].setStyle("-fx-background-color: transparent; -fx-border: none;");

	        switches[i].selectedProperty().addListener((obs, oldVal, newVal) -> {
	            switchIcon.setImage(newVal ? switchOnImage : switchOffImage);
	            TaskLogic.checkTask1Success(switches);
	        });

	        if (i < 4) {
	            leftSwitches.getChildren().add(switches[i]);
	        } else {
	            rightSwitches.getChildren().add(switches[i]);
	        }
	    }

	    switchContainer.getChildren().addAll(leftSwitches, rightSwitches);

	    // Wrap in StackPane to ensure proper centering
	    StackPane centerPane = new StackPane(switchContainer);
	    centerPane.setAlignment(Pos.CENTER);
	    root.setCenter(centerPane);

	    // Randomly turn on 4 switches
	    Random random = new Random();
	    for (int i = 0; i < 4; i++) {
	        int index = random.nextInt(8);
	        if (!switches[index].isSelected()) {
	            switches[index].setSelected(true);
	        }
	    }

	    this.getChildren().add(root);
	}


	private void task2() {
        // Progress Bar
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(500);
        progressBar.setPrefHeight(50);
        progressBar.setRotate(-90);
        
     
        Image imageUp = new Image("/PumpToilet/pump_up.png");
        Image imageDown = new Image("/PumpToilet/pump_down.png");
        ImageView imageView = new ImageView(imageUp);
        imageView.setFitWidth(480);  
        imageView.setFitHeight(320);
        
        // Button to Fill Progress Bar
        Button fillButton = new Button();
        fillButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        fillButton.setGraphic(imageView);
        fillButton.setOnMousePressed(e -> imageView.setImage(imageDown));
        fillButton.setOnMouseReleased(e -> {
            imageView.setImage(imageUp);
            TaskLogic.toiletPump(progressBar);
        });

        // Layout
        HBox layout = new HBox(10,progressBar, fillButton);
        layout.setPadding(new Insets(100,10,10,100));
        layout.setAlignment(Pos.CENTER);
        this.getChildren().add(layout);
	}
	
	private void task3() {
	    StackPane root = new StackPane(); // Center everything
	    root.setPrefSize(this.getPrefWidth(), this.getPrefHeight());

	    VBox panel = new VBox(15);
	    panel.setPadding(new Insets(40));
	    panel.setAlignment(Pos.CENTER);
	    panel.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: white; -fx-border-width: 2px; -fx-border-radius: 10px; -fx-padding: 20px;");

	    Label monitor = new Label("Enter Password:");
	    monitor.setStyle("-fx-font-size: 18px; -fx-text-fill: white;");

	    StringBuilder enteredPassword = new StringBuilder();
	    Random random = new Random();
	    String correctPassword = String.valueOf(1000 + random.nextInt(9000)); // 4-digit random password

	    Label passwordHint = new Label("Today's Password");
	    passwordHint.setStyle("-fx-font-size: 14px; -fx-text-fill: lightgray;");
	    passwordHint.setOnMouseClicked(e -> passwordHint.setText("Password: " + correctPassword));

	    GridPane keypad = new GridPane();
	    keypad.setHgap(10);
	    keypad.setVgap(10);
	    keypad.setAlignment(Pos.CENTER);

	    // Number buttons
	    int[][] layout = {
	        {1, 2, 3},
	        {4, 5, 6},
	        {7, 8, 9}
	    };

	    for (int row = 0; row < layout.length; row++) {
	        for (int col = 0; col < layout[row].length; col++) {
	            int num = layout[row][col];
	            Button numButton = new Button(String.valueOf(num));
	            numButton.setPrefSize(80, 60);
	            numButton.setStyle("-fx-font-size: 18px;");
	            numButton.setOnAction(e -> {
	                enteredPassword.append(numButton.getText());
	                monitor.setText("Enter Password: " + enteredPassword);
	            });

	            keypad.add(numButton, col, row);
	        }
	    }

	    // Special buttons (Clear, 0, Enter)
	    Button clearButton = new Button("Clear");
	    clearButton.setPrefSize(80, 60);
	    clearButton.setStyle("-fx-font-size: 16px; -fx-background-color: red; -fx-text-fill: white;");
	    clearButton.setOnAction(e -> {
	        enteredPassword.setLength(0);
	        monitor.setText("Enter Password:");
	        monitor.setStyle("-fx-text-fill: white;");
	    });

	    Button zeroButton = new Button("0");
	    zeroButton.setPrefSize(80, 60);
	    zeroButton.setStyle("-fx-font-size: 18px;");
	    zeroButton.setOnAction(e -> {
	        enteredPassword.append("0");
	        monitor.setText("Enter Password: " + enteredPassword);
	    });

	    Button enterButton = new Button("Enter");
	    enterButton.setPrefSize(80, 60);
	    enterButton.setStyle("-fx-font-size: 16px; -fx-background-color: green; -fx-text-fill: white;");
	    enterButton.setOnAction(e -> {
	        if (enteredPassword.toString().equals(correctPassword)) {
	            monitor.setText("O2 Fixed");
	            monitor.setStyle("-fx-text-fill: green;");
	            Timeline timeline = new Timeline(
	                new KeyFrame(Duration.seconds(1), event -> {
	                    TaskLogic.fixO2(root);
	                })
	            );
	            timeline.setCycleCount(1);
	            timeline.play();
	        } else {
	            monitor.setText("Wrond Password!");
	            monitor.setStyle("-fx-text-fill: red;");

	            // Reset after 1 second
	            Timeline timeline = new Timeline(
	                new KeyFrame(Duration.seconds(1), event -> {
	                    monitor.setText("Enter Password:");
	                    monitor.setStyle("-fx-text-fill: white;");
	                    enteredPassword.setLength(0);
	                })
	            );
	            timeline.setCycleCount(1);
	            timeline.play();
	        }
	    });

	    // Add the last row separately
	    HBox bottomRow = new HBox(10, clearButton, zeroButton, enterButton);
	    bottomRow.setAlignment(Pos.CENTER);

	    panel.getChildren().addAll(passwordHint, monitor, keypad, bottomRow);
	    
	    root.getChildren().add(panel); // Center in StackPane
	    this.getChildren().add(root);
	}

}
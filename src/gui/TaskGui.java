package gui;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
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
		case 4:
			//task4();
		case 5:
			//task5();
		case 6:
			//task6();
		case 7:
			//task7();
		case 8:
			task8();
			break;
		case 9:
			//task9();
		case 10:
			task10();
			break;
		case 11:
			//task11();
		case 12:
			task12();
			break;
		case 13:
			task13();
		case 14:
			//task14();
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

	    Image switchOnImage = new Image("/TaskAsset/FixLight/On.png");
	    Image switchOffImage = new Image("/TaskAsset/FixLight/Off.png");

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
        
     
        Image imageUp = new Image("/TaskAsset/PumpToilet/pump_up.png");
        Image imageDown = new Image("/TaskAsset/PumpToilet/pump_down.png");
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

	private void task8() {
	    ImageView wireLeft = new ImageView(new Image("TaskAsset/Soldering/leftwire.png"));
	    wireLeft.setFitWidth(200);
	    wireLeft.setFitHeight(100);
	    wireLeft.setLayoutX(0);
	    wireLeft.setLayoutY(200);

	    ImageView wireRight = new ImageView(new Image("TaskAsset/Soldering/rightwire.png"));
	    wireRight.setFitWidth(200);
	    wireRight.setFitHeight(100);
	    wireRight.setLayoutX(400);
	    wireRight.setLayoutY(200);

	    ImageView solderingIron = new ImageView(new Image("TaskAsset/Soldering/solderingmat.png"));
	    solderingIron.setFitWidth(100);
	    solderingIron.setFitHeight(100);
	    solderingIron.setLayoutX(230);
	    solderingIron.setLayoutY(100);

	    ImageView solderWire = new ImageView(new Image("TaskAsset/Soldering/finishsoldering.png"));
	    solderWire.setFitWidth(50);
	    solderWire.setFitHeight(50);
	    solderWire.setLayoutX(700);
	    solderWire.setLayoutY(500);

	    solderingIron.setOnMousePressed(event -> {
	        PauseTransition solderTime = new PauseTransition(Duration.seconds(2));
	        solderTime.setOnFinished(e -> {
	            // Remove wireLeft, wireRight, solderingIron, and solderWire
	            this.getChildren().removeAll(wireLeft, wireRight, solderingIron, solderWire);

	            // Add the final soldered wire
	            ImageView solderedWire = new ImageView(new Image("TaskAsset/Soldering/finishsoldering.png"));
	            solderedWire.setFitWidth(680);
	            solderedWire.setFitHeight(150);
	            solderedWire.setLayoutX(0);  // Centered properly
	            solderedWire.setLayoutY(200);
	            this.getChildren().add(solderedWire);
	        });
	        solderTime.play();
	    });

	    this.getChildren().addAll(wireLeft, wireRight, solderingIron, solderWire);
	}

	
	//TASK10 Cut Flower
	private void task10() {
		    ImageView ground = new ImageView(new Image("/TaskAsset/CutFlower/background.png")); 
		    ground.setFitWidth(800); // Adjust as needed
		    ground.setFitHeight(500); // Adjust as needed

		    int flowerCount = 6; // Number of flowers
		    Random random = new Random();
		    AtomicInteger remainingFlowers = new AtomicInteger(flowerCount);
		    this.getChildren().add(ground);
		    for (int i = 0; i < flowerCount; i++) {
		        ImageView flower = new ImageView(new Image("/TaskAsset/CutFlower/yellow_flower.png")); 
		        flower.setFitWidth(60);
		        flower.setFitHeight(60);

		        // Random position
		        double x = 100 + random.nextInt(400);
		        double y = 100 + random.nextInt(400);
		        flower.setLayoutX(x);
		        flower.setLayoutY(y);

		        flower.setOnMouseClicked(e -> {
		            this.getChildren().remove(flower);
		            if (remainingFlowers.decrementAndGet() == 0) {
		                // Close the pane when all flowers are removed
		                Pane parent = (Pane) this.getParent();
		                if (parent != null) {
		                    parent.getChildren().remove(this);
		                }
		            }
		        });

		        this.getChildren().add(flower);
		    }

		    
		}
	
	
	//clean mirror
	private void task12() {
	    // Load images
	    Image mirrorImage = new Image("/TaskAsset/CleanMirror/mirror.png"); // Change path as needed
	    Image dirtImage = new Image("/TaskAsset/CleanMirror/dirt.png"); // Change path as needed

	    // Mirror image
	    ImageView mirror = new ImageView(mirrorImage);
	    mirror.setFitWidth(500);  // Set appropriate size
	    mirror.setFitHeight(500);
	    mirror.setLayoutX(10);    // Adjust position as needed
	    mirror.setLayoutY(10);

	    int dirtCount = 6; // Number of dirt spots
	    Random random = new Random();
	    AtomicInteger remainingDirt = new AtomicInteger(dirtCount);

	    this.getChildren().add(mirror); // Add mirror first so dirt appears in front

	    for (int i = 0; i < dirtCount; i++) {
	        ImageView dirt = new ImageView(dirtImage);
	        dirt.setFitWidth(60);  // Adjust dirt image size
	        dirt.setFitHeight(60);

	        // Random position
	        double x = 100 + random.nextInt(200);
	        double y = 100 + random.nextInt(200);
	        dirt.setLayoutX(x);
	        dirt.setLayoutY(y);

	        dirt.setOnMouseClicked(e -> {
	            this.getChildren().remove(dirt);
	            if (remainingDirt.decrementAndGet() == 0) {
	                // Close the pane when all dirt images are removed
	                ((Pane) this.getParent()).getChildren().remove(this);
	            }
	        });

	        this.getChildren().add(dirt);
	    }
	}
	
	//TASK13 Cut Carrot
		public void task13() {
			// Background (Chopping board)
		    ImageView board = new ImageView(new Image("/TaskAsset/CutCarrot/choppingboard.png"));
		    board.setFitWidth(600);
		    board.setFitHeight(400);
		    board.setLayoutX(0);
		    board.setLayoutY(0);
		    
		    // Carrot
		    ImageView carrot = new ImageView(new Image("/TaskAsset/CutCarrot/carrot.png"));
		    carrot.setFitWidth(200);
		    carrot.setFitHeight(80);
		    carrot.setLayoutX(300);
		    carrot.setLayoutY(300);
		    
		    // Knife
		    ImageView knife = new ImageView(new Image("/TaskAsset/CutCarrot/knife.png"));
		    knife.setFitWidth(100);
		    knife.setFitHeight(50);
		    knife.setLayoutX(250);
		    knife.setLayoutY(250);
		    
		    knife.setOnMouseDragged((MouseEvent event) -> {
		        knife.setLayoutX(event.getSceneX() - knife.getFitWidth() / 2);
		        knife.setLayoutY(event.getSceneY() - knife.getFitHeight() / 2);
		    });
		    //cut carrot into five pieces to finish task
		    knife.setOnMouseReleased(event -> {
		        if (knife.getBoundsInParent().intersects(carrot.getBoundsInParent())) {
		            this.getChildren().remove(carrot);
		            for (int i = 0; i < 5; i++) {
		                ImageView carrotPiece = new ImageView(new Image("/TaskAsset/CutCarrot/choppedcarrot.png"));
		                carrotPiece.setFitWidth(40);
		                carrotPiece.setFitHeight(80);
		                carrotPiece.setLayoutX(200 + i * 40);
		                carrotPiece.setLayoutY(200);
		                this.getChildren().add(carrotPiece);
		            }
		        }
		    });
		    
		    this.getChildren().addAll(board, carrot, knife);
		
		}
	
}
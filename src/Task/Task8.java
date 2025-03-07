package Task;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class Task8 extends Task {
    public Task8(TaskPane parent) {
        super(parent, "task8");
        
        // Pixel-art style background
        setStyle("-fx-background-color: #8b6c42; -fx-border-color: #555; -fx-border-width: 2px;");
        
        // Title panel with darker brown background
        HBox titlePanel = new HBox();
        titlePanel.setStyle("-fx-background-color: #6d4e2a; -fx-padding: 10px;");
        titlePanel.setPrefWidth(960);
        titlePanel.setAlignment(Pos.CENTER);
        
        // Task title with pixelated font
        Text title = new Text("Soldering Wires");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
        title.setFill(Color.WHITE);
        titlePanel.getChildren().add(title);
        
        // Content panel with pixel-art styling
        VBox contentPanel = new VBox(20);
        contentPanel.setStyle("-fx-background-color: #a88c60; -fx-padding: 20px; -fx-border-color: #6d4e2a; -fx-border-width: 2px;");
        contentPanel.setAlignment(Pos.CENTER);
        contentPanel.setMaxWidth(700);
        
        // Pixel-art style instructions
        Text instructions = new Text("Click the soldering iron to connect the wires.");
        instructions.setFont(Font.font("Monospace", FontWeight.NORMAL, 18));
        instructions.setFill(Color.WHITE);
        
        // Task container with pixel-art styling
        VBox taskContainer = new VBox();
        taskContainer.setPrefSize(600, 400);
        taskContainer.setStyle(
            "-fx-background-color: #64553a; " +
            "-fx-padding: 20px; " +
            "-fx-border-color: #444; " +
            "-fx-border-width: 3px; " +
            "-fx-border-radius: 5px;"
        );

        // Wire
        ImageView wire = new ImageView(new Image("TaskAsset/Soldering/wire.png"));
        wire.setFitWidth(300);
        wire.setFitHeight(150);
        wire.setLayoutX(50);
        wire.setLayoutY(150);
 
        // Soldering iron
        ImageView solderingIron = new ImageView(new Image("TaskAsset/Soldering/solderingmat.png"));
        solderingIron.setFitWidth(150);
        solderingIron.setFitHeight(150);
        solderingIron.setLayoutX(266);
        solderingIron.setLayoutY(50);

        // Soldering interaction
        solderingIron.setOnMousePressed(event -> {
            PauseTransition solderTime = new PauseTransition(Duration.seconds(2));
            solderTime.setOnFinished(e -> {
                // Remove previous elements
                taskContainer.getChildren().removeAll(wire, solderingIron);
                
                // Create soldered wire
                ImageView solderedWire = new ImageView(new Image("TaskAsset/Soldering/fixedwire.png"));
                solderedWire.setFitWidth(300);
                solderedWire.setFitHeight(150);
                solderedWire.setLayoutX(50);
                solderedWire.setLayoutY(150);
                
                taskContainer.getChildren().add(solderedWire);
                completeTask();
            });
            solderTime.play();
        });

        // Add elements to the task container
        taskContainer.setAlignment(Pos.TOP_CENTER);
        taskContainer.setPadding(new Insets(25,0,0,0));
        taskContainer.getChildren().addAll(wire, solderingIron);

        // Assemble the main container
        VBox mainContainer = new VBox(0);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.getChildren().add(titlePanel);
        
        VBox.setMargin(contentPanel, new javafx.geometry.Insets(50, 0, 0, 0));
        contentPanel.getChildren().addAll(instructions, taskContainer);
        mainContainer.getChildren().add(contentPanel);
        
        getChildren().addAll(mainContainer, getCloseButton());
    }

    @Override
    protected void initializeTask() {
        // Initialization happens in constructor
    }

    @Override
    public boolean isCompleted() {
        return false; // This is handled by the completeTask method
    }
}
package Task;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class Task7 extends Task {
    public Task7(TaskPane parent, String taskId) {
        super(parent, taskId);
        
        // Pixel-art style background
        setStyle("-fx-background-color: #8b6c42; -fx-border-color: #555; -fx-border-width: 2px;");
        
        // Title panel with darker brown background
        HBox titlePanel = new HBox();
        titlePanel.setStyle("-fx-background-color: #6d4e2a; -fx-padding: 10px;");
        titlePanel.setPrefWidth(960);
        titlePanel.setAlignment(Pos.CENTER);
        
        // Task title with pixelated font
        Text title = new Text("Make the Bed");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
        title.setFill(Color.WHITE);
        titlePanel.getChildren().add(title);
        
        // Content panel with pixel-art styling
        VBox contentPanel = new VBox(20);
        contentPanel.setStyle("-fx-background-color: #a88c60; -fx-padding: 20px; -fx-border-color: #6d4e2a; -fx-border-width: 2px;");
        contentPanel.setAlignment(Pos.CENTER);
        contentPanel.setMaxWidth(700);
        
        // Pixel-art style instructions
        Text instructions = new Text("Click to fold the blanket completely.");
        instructions.setFont(Font.font("Monospace", FontWeight.NORMAL, 18));
        instructions.setFill(Color.WHITE);
        
        // Task container with pixel-art styling
        VBox taskContainer = new VBox(20);
        taskContainer.setStyle(
            "-fx-background-color: #64553a; " +
            "-fx-padding: 20px; " +
            "-fx-border-color: #444; " +
            "-fx-border-width: 3px; " +
            "-fx-border-radius: 5px; " +
            "-fx-alignment: center;"
        );
        
        // Load images
        ImageView unfoldedBlanket = new ImageView(new Image("/TaskAsset/Makeabed/bedblanket.png"));
        unfoldedBlanket.setFitWidth(200);
        unfoldedBlanket.setFitHeight(200);

        ImageView halfFoldedBlanket = new ImageView(new Image("/TaskAsset/Makeabed/half.png"));
        halfFoldedBlanket.setFitWidth(200);
        halfFoldedBlanket.setFitHeight(200);
        halfFoldedBlanket.setVisible(false);

        ImageView fullyFoldedBlanket = new ImageView(new Image("/TaskAsset/Makeabed/foldblanket.png"));
        fullyFoldedBlanket.setFitWidth(200);
        fullyFoldedBlanket.setFitHeight(200);
        fullyFoldedBlanket.setVisible(false);
        
        ImageView doneFoldingBlanket = new ImageView(new Image("/TaskAsset/Makeabed/done.png"));
        doneFoldingBlanket.setFitWidth(200);
        doneFoldingBlanket.setFitHeight(200);
        doneFoldingBlanket.setVisible(false);

        // Handle click events
        unfoldedBlanket.setOnMouseClicked(event -> {
            taskContainer.getChildren().remove(unfoldedBlanket);
            halfFoldedBlanket.setVisible(true);
            taskContainer.getChildren().add(halfFoldedBlanket);
        });

        halfFoldedBlanket.setOnMouseClicked(event -> {
            taskContainer.getChildren().remove(halfFoldedBlanket);
            fullyFoldedBlanket.setVisible(true);
            taskContainer.getChildren().add(fullyFoldedBlanket);
        });

        fullyFoldedBlanket.setOnMouseClicked(event -> {
            taskContainer.getChildren().remove(fullyFoldedBlanket);
            doneFoldingBlanket.setVisible(true);
            taskContainer.getChildren().add(doneFoldingBlanket);
            completeTask();
        });
        

        // Initial blanket
        taskContainer.getChildren().add(unfoldedBlanket);

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
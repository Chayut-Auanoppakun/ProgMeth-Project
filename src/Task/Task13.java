package Task;

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

public class Task13 extends Task {
    public Task13(TaskPane parent, String taskId) {
        super(parent, taskId);
        
        // Pixel-art style background
        setStyle("-fx-background-color: #8b6c42; -fx-border-color: #555; -fx-border-width: 2px;");
        
        // Title panel with darker brown background
        HBox titlePanel = new HBox();
        titlePanel.setStyle("-fx-background-color: #6d4e2a; -fx-padding: 10px;");
        titlePanel.setPrefWidth(960);
        titlePanel.setAlignment(Pos.CENTER);
        
        // Task title with pixelated font
        Text title = new Text("Chopping Vegetables");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
        title.setFill(Color.WHITE);
        titlePanel.getChildren().add(title);
        
        // Content panel with pixel-art styling
        VBox contentPanel = new VBox(20);
        contentPanel.setStyle("-fx-background-color: #a88c60; -fx-padding: 20px; -fx-border-color: #6d4e2a; -fx-border-width: 2px;");
        contentPanel.setAlignment(Pos.CENTER);
        contentPanel.setMaxWidth(700);
        
        // Pixel-art style instructions
        Text instructions = new Text("Chop the carrot into pieces.");
        instructions.setFont(Font.font("Monospace", FontWeight.NORMAL, 18));
        instructions.setFill(Color.WHITE);
        
        // Task container with pixel-art styling
        Pane taskContainer = new Pane();
        taskContainer.setPrefSize(656, 400);
        taskContainer.setStyle(
            "-fx-background-color: #64553a; " +
            "-fx-padding: 20px; " +
            "-fx-border-color: #444; " +
            "-fx-border-width: 3px; " +
            "-fx-border-radius: 5px;"
        );

        // Chopping board
        ImageView board = new ImageView(new Image("/TaskAsset/Chopping/choppingboard.png"));
        board.setFitWidth(656);
        board.setFitHeight(400);
        board.setLayoutX(0);
        board.setLayoutY(0);
        taskContainer.getChildren().add(board);

        // Carrot
        ImageView carrot = new ImageView(new Image("/TaskAsset/Chopping/carrot.png"));
        carrot.setFitWidth(267);
        carrot.setFitHeight(200);
        carrot.setLayoutX(175);
        carrot.setLayoutY(100);
        taskContainer.getChildren().add(carrot);

        // Knife
        ImageView knife = new ImageView(new Image("/TaskAsset/Chopping/knife.png"));
        knife.setFitWidth(100);
        knife.setFitHeight(100);
        knife.setLayoutX(225);
        knife.setLayoutY(350);
        taskContainer.getChildren().add(knife);

        // Track initial click position
        final double[] offsetX = new double[1];
        final double[] offsetY = new double[1];

        // Make knife draggable
        knife.setOnMousePressed(event -> {
            offsetX[0] = event.getX();
            offsetY[0] = event.getY();
        });

        knife.setOnMouseDragged(event -> {
            // Calculate new position
            double newX = knife.getLayoutX() + (event.getX() - offsetX[0]);
            double newY = knife.getLayoutY() + (event.getY() - offsetY[0]);

            // Boundary checks
            double minX = 0;
            double maxX = taskContainer.getWidth() - knife.getFitWidth();
            double minY = 0;
            double maxY = taskContainer.getHeight() - knife.getFitHeight();

            // Restrict movement within boundaries
            newX = Math.max(minX, Math.min(newX, maxX));
            newY = Math.max(minY, Math.min(newY, maxY));

            knife.setLayoutX(newX);
            knife.setLayoutY(newY);
        });

        // Cut carrot interaction
        knife.setOnMouseReleased(event -> {
            // Check if knife intersects with carrot
            if (knife.getBoundsInParent().intersects(carrot.getBoundsInParent())) {
                // Remove whole carrot
                taskContainer.getChildren().remove(carrot);

                // Create carrot pieces
                for (int i = 0; i < 5; i++) {
                    ImageView carrotPiece = new ImageView(new Image("/TaskAsset/Chopping/choppedcarrot.png"));
                    carrotPiece.setFitWidth(80);
                    carrotPiece.setFitHeight(48);
                    carrotPiece.setLayoutX(175 + i * 55);
                    carrotPiece.setLayoutY(120);
                    taskContainer.getChildren().add(carrotPiece);
                }

                // Remove knife after cutting
                taskContainer.getChildren().remove(knife);
                completeTask();
            }
        });

        // Assemble the main container
        VBox mainContainer = new VBox(0);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.getChildren().add(titlePanel);
        
        VBox.setMargin(contentPanel, new Insets(50, 0, 0, 0));
        contentPanel.getChildren().addAll(instructions, taskContainer);
        mainContainer.getChildren().add(contentPanel);
        
        getChildren().addAll(mainContainer, getCloseButton());
    }

    

    @Override
    public boolean isCompleted() {
        return false; // Placeholder
    }
}
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
import javafx.scene.layout.Pane;
import java.util.ArrayList;
import java.util.List;

public class Task6 extends Task {
    private int depositedBags = 0;

    public Task6(TaskPane parent) {
        super(parent, "task6");
        
        // Pixel-art style background
        setStyle("-fx-background-color: #8b6c42; -fx-border-color: #555; -fx-border-width: 2px;");
        
        // Title panel with darker brown background
        HBox titlePanel = new HBox();
        titlePanel.setStyle("-fx-background-color: #6d4e2a; -fx-padding: 10px;");
        titlePanel.setPrefWidth(960);
        titlePanel.setAlignment(Pos.CENTER);
        
        // Task title with pixelated font
        Text title = new Text("Take Out Trash");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
        title.setFill(Color.WHITE);
        titlePanel.getChildren().add(title);
        
        // Content panel with pixel-art styling
        VBox contentPanel = new VBox(20);
        contentPanel.setStyle("-fx-background-color: #a88c60; -fx-padding: 20px; -fx-border-color: #6d4e2a; -fx-border-width: 2px;");
        contentPanel.setAlignment(Pos.CENTER);
        contentPanel.setMaxWidth(700);
        
        // Pixel-art style instructions
        Text instructions = new Text("Drag all 3 trash bags into the bin.");
        instructions.setFont(Font.font("Monospace", FontWeight.NORMAL, 18));
        instructions.setFill(Color.WHITE);

        // Task container with pixel-art styling
        Pane taskContainer = new Pane();
        taskContainer.setPrefSize(600, 400);
        taskContainer.setStyle(
            "-fx-background-color: #64553a; " +
            "-fx-padding: 20px; " +
            "-fx-border-color: #444; " +
            "-fx-border-width: 3px; " +
            "-fx-border-radius: 5px;"
        );

        // Trash bin
        ImageView trashBin = new ImageView(new Image("/TaskAsset/Trash/bin.png"));
        trashBin.setFitWidth(300);
        trashBin.setFitHeight(400);
        trashBin.setLayoutX(300);
        trashBin.setLayoutY(0);

        // Create 3 trash bags
        List<ImageView> trashBags = new ArrayList<>();
        int[] yPositions = {150, 75, 0};
        for (int i = 2; i >= 0; i--) {
            ImageView trashBag = new ImageView(new Image("/TaskAsset/Trash/trashbag.png"));
            trashBag.setFitWidth(200);
            trashBag.setFitHeight(250);
            trashBag.setLayoutY(yPositions[i]);
            trashBag.setLayoutX(50);
            
            // Track initial click position
            final double[] offsetX = new double[1];
            final double[] offsetY = new double[1];

            // Make the trash bag draggable
            final ImageView currentBag = trashBag;
            currentBag.setOnMousePressed(event -> {
                offsetX[0] = event.getX();
                offsetY[0] = event.getY();
            });

            currentBag.setOnMouseDragged(event -> {
                // Calculate new position
                double newX = currentBag.getLayoutX() + (event.getX() - offsetX[0]);
                double newY = currentBag.getLayoutY() + (event.getY() - offsetY[0]);

                // Boundary checks
                double minX = 0;
                double maxX = taskContainer.getWidth() - currentBag.getFitWidth();
                double minY = 0;
                double maxY = taskContainer.getHeight() - currentBag.getFitHeight();

                // Restrict movement within boundaries
                newX = Math.max(minX, Math.min(newX, maxX));
                newY = Math.max(minY, Math.min(newY, maxY));

                currentBag.setLayoutX(newX);
                currentBag.setLayoutY(newY);
            });

            // Detect when the trash bag is placed inside the bin
            currentBag.setOnMouseReleased(event -> {
                if (intersects(currentBag, trashBin)) {
                    taskContainer.getChildren().remove(currentBag);
                    depositedBags++;
                    
                    if (depositedBags == 3) {
                        completeTask();
                    }
                }
            });

            trashBags.add(currentBag);
        }

        // Add elements to the task container
        taskContainer.getChildren().add(trashBin);
        taskContainer.getChildren().addAll(trashBags);

        // Assemble the main container
        VBox mainContainer = new VBox(0);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.getChildren().add(titlePanel);
        
        VBox.setMargin(contentPanel, new javafx.geometry.Insets(50, 0, 0, 0));
        contentPanel.getChildren().addAll(instructions, taskContainer);
        mainContainer.getChildren().add(contentPanel);
        
        getChildren().addAll(mainContainer, getCloseButton());
    }

    // Custom intersection method for more precise hit detection
    private boolean intersects(ImageView obj1, ImageView obj2) {
        double obj1Left = obj1.getLayoutX();
        double obj1Right = obj1Left + obj1.getFitWidth();
        double obj1Top = obj1.getLayoutY();
        double obj1Bottom = obj1Top + obj1.getFitHeight();

        double obj2Left = obj2.getLayoutX();
        double obj2Right = obj2Left + obj2.getFitWidth();
        double obj2Top = obj2.getLayoutY();
        double obj2Bottom = obj2Top + obj2.getFitHeight();

        return !(obj1Right < obj2Left || 
                 obj1Left > obj2Right || 
                 obj1Bottom < obj2Top || 
                 obj1Top > obj2Bottom);
    }

    @Override
    protected void initializeTask() {}

    @Override
    public boolean isCompleted() {
        return depositedBags == 3;
    }
}
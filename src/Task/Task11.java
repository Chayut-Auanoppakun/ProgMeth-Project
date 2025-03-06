package Task;

import javafx.application.Platform;
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
import java.util.Random;

public class Task11 extends Task {
    private int extinguishedFires = 0;
    private final List<ImageView> fires = new ArrayList<>();
    private boolean completeTaskCalled=false;
    
    public Task11(TaskPane parent) {
        super(parent, "task11");
        
        setStyle("-fx-background-color: #8b6c42; -fx-border-color: #555; -fx-border-width: 2px;");
        
        HBox titlePanel = new HBox();
        titlePanel.setStyle("-fx-background-color: #6d4e2a; -fx-padding: 10px;");
        titlePanel.setPrefWidth(960);
        titlePanel.setAlignment(Pos.CENTER);
        
        Text title = new Text("Extinguish the Fires");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
        title.setFill(Color.WHITE);
        titlePanel.getChildren().add(title);
        
        VBox contentPanel = new VBox(20);
        contentPanel.setStyle("-fx-background-color: #a88c60; -fx-padding: 20px; -fx-border-color: #6d4e2a; -fx-border-width: 2px;");
        contentPanel.setAlignment(Pos.CENTER);
        contentPanel.setMaxWidth(700);
        
        Text instructions = new Text("Drag the fire extinguisher to put out all 5 fires.");
        instructions.setFont(Font.font("Monospace", FontWeight.NORMAL, 18));
        instructions.setFill(Color.WHITE);
        
        Pane taskContainer = new Pane();
        taskContainer.setPrefSize(656, 400);
        taskContainer.setStyle("-fx-background-color: #64553a; -fx-padding: 20px; -fx-border-color: #444; -fx-border-width: 3px; -fx-border-radius: 5px;");
        
        ImageView background = new ImageView(new Image("/TaskAsset/Fire/background.png"));
        background.setFitWidth(656);
        background.setFitHeight(400);
        
        ImageView extinguisher = new ImageView(new Image("/TaskAsset/Fire/fire_extinguisher.png"));
        extinguisher.setFitWidth(110);
        extinguisher.setFitHeight(100);
        extinguisher.setLayoutX(50);
        extinguisher.setLayoutY(250);
        
        final double[] offsetX = new double[1];
        final double[] offsetY = new double[1];
        
        extinguisher.setOnMousePressed(event -> {
            offsetX[0] = event.getX();
            offsetY[0] = event.getY();
        });

        extinguisher.setOnMouseDragged(event -> {
            double newX = extinguisher.getLayoutX() + (event.getX() - offsetX[0]);
            double newY = extinguisher.getLayoutY() + (event.getY() - offsetY[0]);
            newX = Math.max(0, Math.min(newX, taskContainer.getWidth() - extinguisher.getFitWidth()));
            newY = Math.max(0, Math.min(newY, taskContainer.getHeight() - extinguisher.getFitHeight()));
            extinguisher.setLayoutX(newX);
            extinguisher.setLayoutY(newY);
            
            List<ImageView> removedFires = new ArrayList<>();
            for (ImageView fire : fires) {
                if (intersects(extinguisher, fire)) {
                    taskContainer.getChildren().remove(fire);
                    removedFires.add(fire);
                    extinguishedFires++;
                }
            }
            fires.removeAll(removedFires);
            
            if (extinguishedFires >= 5 && !completeTaskCalled) {
            	completeTask();
            	completeTaskCalled=true;
            }
        });
        
        Random rand = new Random();
        for (int i = 0; i < 5; i++) {
            boolean overlapping;
            final ImageView[] fireHolder = new ImageView[1]; // Use an array to hold the ImageView
            do {
                fireHolder[0] = new ImageView(new Image("/TaskAsset/Fire/fire.png"));
                fireHolder[0].setFitWidth(69);
                fireHolder[0].setFitHeight(100);
                fireHolder[0].setLayoutX(rand.nextInt(500));
                fireHolder[0].setLayoutY(rand.nextInt(300));
                overlapping = fires.stream().anyMatch(existing -> intersects(fireHolder[0], existing));
            } while (overlapping);
            fires.add(fireHolder[0]); // Add fire after ensuring no overlap
        }

        
        taskContainer.getChildren().add(background);
        taskContainer.getChildren().addAll(fires);
        taskContainer.getChildren().add(extinguisher);
        
        VBox mainContainer = new VBox(0);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.getChildren().add(titlePanel);
        
        VBox.setMargin(contentPanel, new javafx.geometry.Insets(50, 0, 0, 0));
        contentPanel.getChildren().addAll(instructions, taskContainer);
        mainContainer.getChildren().add(contentPanel);
        
        getChildren().addAll(mainContainer, getCloseButton());
    }

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
        return extinguishedFires >= 5;
    }
}

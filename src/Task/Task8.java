package Task;

import javafx.animation.PauseTransition;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class Task8 extends Pane {  // Extend Pane to use getChildren()

    public Task8() {  // Correct Constructor (Same name as class, no return type)
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
                solderedWire.setLayoutX(0);  // Position correctly
                solderedWire.setLayoutY(200);
                this.getChildren().add(solderedWire);
            });
            solderTime.play();
        });

        this.getChildren().addAll(wireLeft, wireRight, solderingIron, solderWire);
    }
}

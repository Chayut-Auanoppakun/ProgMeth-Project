package Task;

import javafx.animation.PauseTransition;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

public class Task8 extends Pane {  // Extend Pane to use getChildren()
    
    public Task8() {  // Constructor to initialize components
        // Create wire images
        ImageView wireLeft = new ImageView(new Image("file:wire_left.png"));
        wireLeft.setFitWidth(100);
        wireLeft.setFitHeight(50);
        wireLeft.setLayoutX(400);
        wireLeft.setLayoutY(400);

        ImageView wireRight = new ImageView(new Image("file:wire_right.png"));
        wireRight.setFitWidth(100);
        wireRight.setFitHeight(50);
        wireRight.setLayoutX(600);
        wireRight.setLayoutY(400);

        // Create soldering iron image
        ImageView solderingIron = new ImageView(new Image("file:soldering_iron.png"));
        solderingIron.setFitWidth(100);
        solderingIron.setFitHeight(100);
        solderingIron.setLayoutX(300);
        solderingIron.setLayoutY(500);

        // Create solder wire image
        ImageView solderWire = new ImageView(new Image("file:solder_wire.png"));
        solderWire.setFitWidth(50);
        solderWire.setFitHeight(50);
        solderWire.setLayoutX(700);
        solderWire.setLayoutY(500);

        // Soldering event when pressing the soldering iron
        solderingIron.setOnMousePressed(event -> {
            PauseTransition solderTime = new PauseTransition(Duration.seconds(5));
            solderTime.setOnFinished(e -> {
                this.getChildren().removeAll(wireLeft, wireRight);

                // Add soldered wire image
                ImageView solderedWire = new ImageView(new Image("file:soldered_wire.png"));
                solderedWire.setFitWidth(200);
                solderedWire.setFitHeight(50);
                solderedWire.setLayoutX(500);
                solderedWire.setLayoutY(400);
                
                this.getChildren().add(solderedWire);
            });
            solderTime.play();
        });

        // Add all elements to the scene
        this.getChildren().addAll(wireLeft, wireRight, solderingIron, solderWire);
    }
}

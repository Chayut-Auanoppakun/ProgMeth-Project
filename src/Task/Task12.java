package Task;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class Task12 extends Pane {  // Extend Pane to use getChildren()
    
    public Task12() {  // Constructor to initialize the UI
        Image mirrorImage = new Image("/TaskAsset/CleanMirror/mirror.png"); // Change path as needed
        Image dirtImage = new Image("/TaskAsset/CleanMirror/dirt.png"); // Change path as needed

        // Mirror image
        ImageView mirror = new ImageView(mirrorImage);
        mirror.setFitWidth(500);  // Set appropriate size
        mirror.setFitHeight(500);
        mirror.setLayoutX(10);    // Adjust position as needed
        mirror.setLayoutY(10);
        
        this.getChildren().add(mirror); // Add mirror first so dirt appears in front

        int dirtCount = 6; // Number of dirt spots
        Random random = new Random();
        AtomicInteger remainingDirt = new AtomicInteger(dirtCount);

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
                    Pane parent = (Pane) this.getParent();
                    if (parent != null) {
                        parent.getChildren().remove(this);
                    }
                }
            });

            this.getChildren().add(dirt);
        }
    }
}

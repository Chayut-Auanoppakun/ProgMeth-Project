package Task;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class Task10 extends Pane {  // Extend Pane to use getChildren()
    
    public Task10() {  // Constructor to initialize the UI
        // Background image
        ImageView ground = new ImageView(new Image("/TaskAsset/CutFlower/background.png")); 
        ground.setFitWidth(800);
        ground.setFitHeight(500);
        this.getChildren().add(ground); // Add background first

        int flowerCount = 6; // Number of flowers
        Random random = new Random();
        AtomicInteger remainingFlowers = new AtomicInteger(flowerCount);

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
}

package Task;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class Task14 extends Pane{
	public void task14() {
	    // Load images
	    Image sceneImage = new Image("/TaskAsset/Fire/background.png");
	    Image fireImage = new Image("/TaskAsset/Fire/fire.png");
	    Image extinguisherImage = new Image("/TaskAsset/Fire/fire_extinguisher.png");

	    // Background scene
	    ImageView scene = new ImageView(sceneImage);
	    scene.setFitWidth(600);
	    scene.setFitHeight(400);
	    scene.setLayoutX(0);
	    scene.setLayoutY(0);

	    int fireCount = 5; // Number of fire spots
	    Random random = new Random();
	    AtomicInteger remainingFire = new AtomicInteger(fireCount);

	    this.getChildren().add(scene); // Add scene first so fire spots appear in front

	    // Fire spots
	    for (int i = 0; i < fireCount; i++) {
	        ImageView fire = new ImageView(fireImage);
	        fire.setFitWidth(50);  // Adjust fire image size
	        fire.setFitHeight(50);

	        // Random position
	        double x = 50 + random.nextInt(500);
	        double y = 50 + random.nextInt(300);
	        fire.setLayoutX(x);
	        fire.setLayoutY(y);

	        fire.setOnMouseClicked(e -> {
	            this.getChildren().remove(fire);
	            if (remainingFire.decrementAndGet() == 0) {
	                // Task is completed when all fire spots are extinguished
	                ((Pane) this.getParent()).getChildren().remove(this);
	            }
	        });

	        this.getChildren().add(fire);
	    }

	    // Fire extinguisher
	    ImageView extinguisher = new ImageView(extinguisherImage);
	    extinguisher.setFitWidth(100);
	    extinguisher.setFitHeight(50);
	    extinguisher.setLayoutX(300);
	    extinguisher.setLayoutY(300);

	    this.getChildren().add(extinguisher);
	}
}

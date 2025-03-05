package Task;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class Task6 extends Pane {
    public Task6() {

        // Load images
        ImageView trashBin = new ImageView(new Image("/TaskAsset/Trash/bin.png"));
        trashBin.setFitWidth(200);
        trashBin.setFitHeight(300);
        trashBin.setLayoutX(500);
        trashBin.setLayoutY(300);

        ImageView trashBag = new ImageView(new Image("/TaskAsset/Trash/trashbag.png"));
        trashBag.setFitWidth(150);
        trashBag.setFitHeight(200);
        trashBag.setLayoutX(300);
        trashBag.setLayoutY(400);

        // Track initial click position
        final double[] offsetX = new double[1];
        final double[] offsetY = new double[1];

        // Make the trash bag draggable
        trashBag.setOnMousePressed(event -> {
            offsetX[0] = event.getSceneX() - trashBag.getLayoutX();
            offsetY[0] = event.getSceneY() - trashBag.getLayoutY();
        });

        trashBag.setOnMouseDragged(event -> {
            trashBag.setLayoutX(event.getSceneX() - offsetX[0]);
            trashBag.setLayoutY(event.getSceneY() - offsetY[0]);
        });

        // Detect when the trash bag is placed inside the bin
        trashBag.setOnMouseReleased(event -> {
            // Corrected the line to use 'this' to refer to the current Pane
            if (trashBag.getBoundsInParent().intersects(trashBin.getBoundsInParent())) {
                this.getChildren().remove(trashBag); // Remove trash bag from the pane
            }
        });

        // Add elements to the pane
        this.getChildren().addAll(trashBin, trashBag);
    }
}

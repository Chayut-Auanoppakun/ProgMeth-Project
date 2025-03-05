package Task;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class Task7 extends Pane {
    private void task7() {
        // Load images
        ImageView unfoldedBlanket = new ImageView(new Image("/TaskAsset/Makeabed/bedblanket.png"));
        unfoldedBlanket.setFitWidth(200);
        unfoldedBlanket.setFitHeight(200);
        unfoldedBlanket.setLayoutX(0);
        unfoldedBlanket.setLayoutY(0);

        Image halfFoldedImage = new Image("/TaskAsset/Makeabed/half.png");
        ImageView halfFoldedBlanket = new ImageView(halfFoldedImage);
        halfFoldedBlanket.setFitWidth(200);
        halfFoldedBlanket.setFitHeight(200);
        halfFoldedBlanket.setLayoutX(0);
        halfFoldedBlanket.setLayoutY(0);
        halfFoldedBlanket.setVisible(false); // Initially invisible

        Image fullyFoldedImage = new Image("/TaskAsset/Makeabed/foldblanket.png");
        ImageView fullyFoldedBlanket = new ImageView(fullyFoldedImage);
        fullyFoldedBlanket.setFitWidth(200);
        fullyFoldedBlanket.setFitHeight(200);
        fullyFoldedBlanket.setLayoutX(0);
        fullyFoldedBlanket.setLayoutY(0);
        fullyFoldedBlanket.setVisible(false); // Initially invisible

        // Handle click event on the unfolded blanket
        unfoldedBlanket.setOnMouseClicked(event -> {
            this.getChildren().remove(unfoldedBlanket); // Remove the unfolded blanket from the pane
            halfFoldedBlanket.setVisible(true); // Show the half-folded blanket
        });

        // Handle click event on the half-folded blanket
        halfFoldedBlanket.setOnMouseClicked(event -> {
            this.getChildren().remove(halfFoldedBlanket); // Remove the half-folded blanket from the pane
            fullyFoldedBlanket.setVisible(true); // Show the fully folded blanket
        });

        // Handle click event on the fully folded blanket
        fullyFoldedBlanket.setOnMouseClicked(event -> {
            this.getChildren().remove(fullyFoldedBlanket); // Remove the fully folded blanket from the pane
            Pane parent = (Pane) this.getParent();
            if (parent != null) {
                parent.getChildren().remove(this); // Close pane
            }
        });

        // Add elements to the pane
        this.getChildren().addAll(unfoldedBlanket, halfFoldedBlanket, fullyFoldedBlanket);
    }
}

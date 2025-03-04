package Task;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

public class Task13 extends Pane {  // Extend Pane to allow getChildren()

    public Task13() {  // Correct constructor
        // Background (Chopping board)
        ImageView board = new ImageView(new Image("file:chopping_board.png"));
        board.setFitWidth(600);
        board.setFitHeight(400);
        board.setLayoutX(350);
        board.setLayoutY(200);

        // Carrot
        ImageView carrot = new ImageView(new Image("file:carrot.png"));
        carrot.setFitWidth(200);
        carrot.setFitHeight(80);
        carrot.setLayoutX(550);
        carrot.setLayoutY(300);

        // Knife
        ImageView knife = new ImageView(new Image("file:knife.png"));
        knife.setFitWidth(100);
        knife.setFitHeight(50);
        knife.setLayoutX(500);
        knife.setLayoutY(500);

        knife.setOnMouseDragged((MouseEvent event) -> {
            knife.setLayoutX(event.getSceneX() - knife.getFitWidth() / 2);
            knife.setLayoutY(event.getSceneY() - knife.getFitHeight() / 2);
        });

        // Cut carrot into five pieces when knife intersects carrot
        knife.setOnMouseReleased(event -> {
            if (knife.getBoundsInParent().intersects(carrot.getBoundsInParent())) {
                this.getChildren().remove(carrot);  // Remove whole carrot

                // Create carrot pieces
                for (int i = 0; i < 5; i++) {
                    ImageView carrotPiece = new ImageView(new Image("file:carrot_piece.png"));
                    carrotPiece.setFitWidth(40);
                    carrotPiece.setFitHeight(80);
                    carrotPiece.setLayoutX(550 + i * 42);  // Spacing for pieces
                    carrotPiece.setLayoutY(300);
                    this.getChildren().add(carrotPiece);
                }

                // Optionally remove knife after cutting
                this.getChildren().remove(knife);
            }
        });

        this.getChildren().addAll(board, carrot, knife);
    }
}

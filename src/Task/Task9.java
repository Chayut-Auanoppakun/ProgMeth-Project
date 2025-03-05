package Task;

import java.util.concurrent.atomic.AtomicInteger;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class Task9 extends Pane {
	private void task9() {
	    ImageView bookshelf = new ImageView(new Image("/TaskAsset/TidyBookshelf/bookshelf.png")); 
	    bookshelf.setFitWidth(800); // Adjust as needed
	    bookshelf.setFitHeight(500); // Adjust as needed

	    int bookCount = 3; // Number of books to place
	    AtomicInteger remainingBooks = new AtomicInteger(bookCount);
	    this.getChildren().add(bookshelf);

	    // Define the empty shelf positions
	    ImageView emptyShelf1 = new ImageView(new Image("/TaskAsset/TidyBookshelf/emptyshelf.png"));
	    emptyShelf1.setFitWidth(100);
	    emptyShelf1.setFitHeight(100);
	    emptyShelf1.setLayoutX(150); // Position of empty shelf 1 on the bookshelf
	    emptyShelf1.setLayoutY(150);
	    this.getChildren().add(emptyShelf1);

	    ImageView emptyShelf2 = new ImageView(new Image("/TaskAsset/TidyBookshelf/emptyshelf.png"));
	    emptyShelf2.setFitWidth(100);
	    emptyShelf2.setFitHeight(100);
	    emptyShelf2.setLayoutX(300); // Position of empty shelf 2 on the bookshelf
	    emptyShelf2.setLayoutY(150);
	    this.getChildren().add(emptyShelf2);

	    ImageView emptyShelf3 = new ImageView(new Image("/TaskAsset/TidyBookshelf/emptyshelf.png"));
	    emptyShelf3.setFitWidth(100);
	    emptyShelf3.setFitHeight(100);
	    emptyShelf3.setLayoutX(450); // Position of empty shelf 3 on the bookshelf
	    emptyShelf3.setLayoutY(150);
	    this.getChildren().add(emptyShelf3);

	    for (int i = 0; i < bookCount; i++) {
	        ImageView book = new ImageView(new Image("/TaskAsset/TidyBookshelf/book.png")); 
	        book.setFitWidth(60);
	        book.setFitHeight(100);

	        // Initial position for the books
	        double x = 100 + (i * 70);
	        double y = 400; // Starting position for the books
	        book.setLayoutX(x);
	        book.setLayoutY(y);

	        book.setOnMouseDragged(e -> {
	            book.setLayoutX(e.getSceneX() - book.getFitWidth() / 2);
	            book.setLayoutY(e.getSceneY() - book.getFitHeight() / 2);
	        });

	        book.setOnMouseReleased(e -> {
	            if (book.getBoundsInParent().intersects(emptyShelf1.getBoundsInParent()) ||
	                book.getBoundsInParent().intersects(emptyShelf2.getBoundsInParent()) ||
	                book.getBoundsInParent().intersects(emptyShelf3.getBoundsInParent())) {

	                // Snap the book to the empty shelf
	                ImageView targetShelf = null;
	                if (book.getBoundsInParent().intersects(emptyShelf1.getBoundsInParent())) {
	                    targetShelf = emptyShelf1;
	                } else if (book.getBoundsInParent().intersects(emptyShelf2.getBoundsInParent())) {
	                    targetShelf = emptyShelf2;
	                } else if (book.getBoundsInParent().intersects(emptyShelf3.getBoundsInParent())) {
	                    targetShelf = emptyShelf3;
	                }

	                if (targetShelf != null) {
	                    double shelfX = targetShelf.getLayoutX() + (targetShelf.getFitWidth() - book.getFitWidth()) / 2;
	                    double shelfY = targetShelf.getLayoutY() + (targetShelf.getFitHeight() - book.getFitHeight()) / 2;
	                    book.setLayoutX(shelfX);
	                    book.setLayoutY(shelfY);
	                }

	                if (remainingBooks.decrementAndGet() == 0) {
	                    // Close the pane when all books are placed on the empty shelves
	                    Pane parent = (Pane) this.getParent();
	                    if (parent != null) {
	                        parent.getChildren().remove(this);
	                    }
	                }
	            } else {
	                // Return the book to its original position if not placed on an empty shelf
	                book.setLayoutX(x);
	                book.setLayoutY(y);
	            }
	        });

	        this.getChildren().add(book);
	    }
	}
}

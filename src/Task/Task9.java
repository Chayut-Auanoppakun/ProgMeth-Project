package Task;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.geometry.Insets;
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

public class Task9 extends Task {
    public Task9(TaskPane parent, String taskId) {
        super(parent, taskId);
        
        // Pixel-art style background
        setStyle("-fx-background-color: #8b6c42; -fx-border-color: #555; -fx-border-width: 2px;");
        
        // Title panel with darker brown background
        HBox titlePanel = new HBox();
        titlePanel.setStyle("-fx-background-color: #6d4e2a; -fx-padding: 10px;");
        titlePanel.setPrefWidth(960);
        titlePanel.setAlignment(Pos.CENTER);
        
        // Task title with pixelated font
        Text title = new Text("Tidy Bookshelf");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
        title.setFill(Color.WHITE);
        titlePanel.getChildren().add(title);
        
        // Content panel with pixel-art styling
        VBox contentPanel = new VBox(20);
        contentPanel.setStyle("-fx-background-color: #a88c60; -fx-padding: 20px; -fx-border-color: #6d4e2a; -fx-border-width: 2px;");
        contentPanel.setAlignment(Pos.CENTER);
        contentPanel.setMaxWidth(700);
        
        // Pixel-art style instructions
        Text instructions = new Text("Drag books to their correct shelves.");
        instructions.setFont(Font.font("Monospace", FontWeight.NORMAL, 18));
        instructions.setFill(Color.WHITE);
        
        // Task container with pixel-art styling
        Pane taskContainer = new Pane();
        taskContainer.setPrefSize(600, 400);
        taskContainer.setStyle(
            "-fx-background-color: #64553a; " +
            "-fx-padding: 20px; " +
            "-fx-border-color: #444; " +
            "-fx-border-width: 3px; " +
            "-fx-border-radius: 5px;"
        );

        // Bookshelf background - centered in the container
        ImageView bookshelf = new ImageView(new Image("/TaskAsset/TidyBookshelf/bookshelf.png")); 
        bookshelf.setFitWidth(440); // Adjusted based on image
        bookshelf.setFitHeight(350); // Adjusted based on image
        bookshelf.setPreserveRatio(true);
        bookshelf.setLayoutX(150); // Position on the right side
        bookshelf.setLayoutY(25);
        taskContainer.getChildren().add(bookshelf);

        int bookCount = 3; // Number of books to place
        AtomicInteger remainingBooks = new AtomicInteger(bookCount);

        // Define the empty shelf positions - based on your image
        ImageView[] emptyShelves = new ImageView[3];
        
        // Position the empty shelves as shown in the reference image
        // First shelf - top row
        emptyShelves[0] = new ImageView(new Image("/TaskAsset/TidyBookshelf/emptyshelf.png"));
        emptyShelves[0].setFitWidth(30);
        emptyShelves[0].setFitHeight(70);
        emptyShelves[0].setLayoutX(330); // Position in the right side of top shelf
        emptyShelves[0].setLayoutY(100);
        
        // Second shelf - middle row
        emptyShelves[1] = new ImageView(new Image("/TaskAsset/TidyBookshelf/emptyshelf.png"));
        emptyShelves[1].setFitWidth(30);
        emptyShelves[1].setFitHeight(70);
        emptyShelves[1].setLayoutX(270); // Position in the middle shelf
        emptyShelves[1].setLayoutY(160);
        
        // Third shelf - bottom row
        emptyShelves[2] = new ImageView(new Image("/TaskAsset/TidyBookshelf/emptyshelf.png"));
        emptyShelves[2].setFitWidth(30);
        emptyShelves[2].setFitHeight(70);
        emptyShelves[2].setLayoutX(390); // Position in the right side of bottom shelf
        emptyShelves[2].setLayoutY(240);
        
        // Add empty shelves to the container
        for (int i = 0; i < 3; i++) {
            taskContainer.getChildren().add(emptyShelves[i]);
        }

        // Books positioning - on the left side as shown in the reference image
        for (int i = 0; i < bookCount; i++) {
            ImageView book = new ImageView(new Image("/TaskAsset/TidyBookshelf/book.png")); 
            book.setFitWidth(25);
            book.setFitHeight(75);
            
            // Position books on the left side in a staggered pattern
            double x = 75; // Left side of container
            double y = 175 + (i * 70); // Staggered vertically
            
            // Adjust positions to match the reference image
            if (i == 0) {
                x = 90;
                y = 20;
            } else if (i == 1) {
                x = 75;
                y = 120;
            } else if (i == 2) {
                x = 90;
                y = 200;
            }
            
            book.setLayoutX(x);
            book.setLayoutY(y);

            // Track initial click position
            final double[] offsetX = new double[1];
            final double[] offsetY = new double[1];

            // Make the book draggable
            book.setOnMousePressed(event -> {
                offsetX[0] = event.getX();
                offsetY[0] = event.getY();
                book.toFront(); // Bring to front when dragging
            });

            book.setOnMouseDragged(event -> {
                // Calculate new position
                double newX = book.getLayoutX() + (event.getX() - offsetX[0]);
                double newY = book.getLayoutY() + (event.getY() - offsetY[0]);

                // Boundary checks
                double minX = 0;
                double maxX = taskContainer.getWidth() - book.getFitWidth();
                double minY = 0;
                double maxY = taskContainer.getHeight() - book.getFitHeight();

                // Restrict movement within boundaries
                newX = Math.max(minX, Math.min(newX, maxX));
                newY = Math.max(minY, Math.min(newY, maxY));

                book.setLayoutX(newX);
                book.setLayoutY(newY);
            });

            // Detect when the book is placed on a shelf
            book.setOnMouseReleased(event -> {
                // Check intersection with any empty shelf
                for (int j = 0; j < emptyShelves.length; j++) {
                    if (book.getBoundsInParent().intersects(emptyShelves[j].getBoundsInParent())) {
                        // Snap the book to the center of the empty shelf
                        double shelfX = emptyShelves[j].getLayoutX() + 
                                        (emptyShelves[j].getFitWidth() - book.getFitWidth()) / 2;
                        double shelfY = emptyShelves[j].getLayoutY();
                        
                        book.setLayoutX(shelfX);
                        book.setLayoutY(shelfY);

                        if (remainingBooks.decrementAndGet() == 0) {
                            completeTask();
                        }
                        return; // Exit after successful placement
                    }
                }
            });

            taskContainer.getChildren().add(book);
        }

        // Assemble the main container
        VBox mainContainer = new VBox(0);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.getChildren().add(titlePanel);
        
        VBox.setMargin(contentPanel, new Insets(50, 0, 0, 0));
        contentPanel.getChildren().addAll(instructions, taskContainer);
        mainContainer.getChildren().add(contentPanel);
        
        getChildren().addAll(mainContainer, getCloseButton());
    }



    @Override
    public boolean isCompleted() {
        return false; // Handled by the completeTask() method
    }
}
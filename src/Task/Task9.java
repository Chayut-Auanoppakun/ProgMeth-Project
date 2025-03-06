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
    public Task9(TaskPane parent) {
        super(parent, "task9");
        
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

        // Bookshelf background
        ImageView bookshelf = new ImageView(new Image("/TaskAsset/TidyBookshelf/bookshelf.png")); 
        bookshelf.setFitWidth(600);
        bookshelf.setFitHeight(400);
        taskContainer.getChildren().add(bookshelf);

        int bookCount = 3; // Number of books to place
        AtomicInteger remainingBooks = new AtomicInteger(bookCount);

        // Define the empty shelf positions
        ImageView[] emptyShelves = new ImageView[3];
        double[] shelfXPositions = {200, 420, 640}; // Spread out across the bookshelf
        
        for (int i = 0; i < 3; i++) {
            emptyShelves[i] = new ImageView(new Image("/TaskAsset/TidyBookshelf/emptyshelf.png"));
            emptyShelves[i].setFitWidth(150);
            emptyShelves[i].setFitHeight(150);
            emptyShelves[i].setLayoutX(shelfXPositions[i]-190);
            emptyShelves[i].setLayoutY(250);
            taskContainer.getChildren().add(emptyShelves[i]);
        }

        // Books positioning
        for (int i = 0; i < bookCount; i++) {
            ImageView book = new ImageView(new Image("/TaskAsset/TidyBookshelf/book.png")); 
            book.setFitWidth(100);
            book.setFitHeight(150);

            // Spread out initial book positions
            double x = 100 + (i * 150);
            double y = 15; // Lower on the screen
            book.setLayoutX(x);
            book.setLayoutY(y);

            // Track initial click position
            final double[] offsetX = new double[1];
            final double[] offsetY = new double[1];

            // Make the book draggable
            book.setOnMousePressed(event -> {
                offsetX[0] = event.getX();
                offsetY[0] = event.getY();
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
                        double shelfY = emptyShelves[j].getLayoutY() + 
                                        (emptyShelves[j].getFitHeight() - book.getFitHeight()) / 2;
                        
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
    protected void initializeTask() {
        // No specific initialization needed
    }

    @Override
    public boolean isCompleted() {
        return false; // Placeholder
    }
}
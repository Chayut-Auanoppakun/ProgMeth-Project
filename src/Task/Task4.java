package Task;

import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.layout.StackPane;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

public class Task4 extends Task {
    private Timeline washingTimeline;
    private Timeline spongeFollowTimeline;

    private double mouseX = 0.00;
    private double mouseY = 0.00;

    public Task4(TaskPane parent, String taskId) {
        super(parent, taskId);
        
        // Set pixelated style background with brown color matching the interior walls
        setStyle("-fx-background-color: #8b6c42; -fx-border-color: #555; -fx-border-width: 2px;");
        
        // Title panel with darker brown background
        HBox titlePanel = new HBox();
        titlePanel.setStyle("-fx-background-color: #6d4e2a; -fx-padding: 10px;");
        titlePanel.setPrefWidth(960);
        titlePanel.setAlignment(Pos.CENTER);
        
        // Task title with pixelated font
        Text title = new Text("Dish Washing");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 28));
        title.setFill(Color.WHITE);
        titlePanel.getChildren().add(title);
        
        // Panel for task content with slightly lighter brown
        VBox contentPanel = new VBox(20);
        contentPanel.setStyle("-fx-background-color: #a88c60; -fx-padding: 20px; -fx-border-color: #6d4e2a; -fx-border-width: 2px;");
        contentPanel.setAlignment(Pos.CENTER);
        contentPanel.setMaxWidth(700);
        
        // Task instructions 
        Text instructions = new Text("Clean the dish by moving the sponge.");
        instructions.setFont(Font.font("Monospace", FontWeight.NORMAL, 18));
        instructions.setFill(Color.WHITE);

        StackPane root = new StackPane();
        root.setPrefSize(getPrefWidth(), getPrefHeight());
        root.setStyle("-fx-background-color: #64553a; -fx-padding: 20px; -fx-border-color: #444; -fx-border-width: 3px; -fx-border-radius: 5px;");

        ImageView dishView = new ImageView(new Image("/TaskAsset/DishWashing/dirty_dish.png"));
        dishView.setFitWidth(400);
        dishView.setFitHeight(400);

        Image[] dishStages = {
            new Image("/TaskAsset/DishWashing/dirty_dish.png"),
            new Image("/TaskAsset/DishWashing/mid_clean_dish2.png"),
            new Image("/TaskAsset/DishWashing/mid_clean_dish1.png"),
            new Image("/TaskAsset/DishWashing/clean_dish.png")
        };

        ImageView spongeView = new ImageView(new Image("/TaskAsset/DishWashing/sponge.png"));
        spongeView.setFitWidth(100);
        spongeView.setFitHeight(100);
        spongeView.setTranslateX(-250);
        spongeView.setVisible(false);

        ImageView spongeClickable = new ImageView(new Image("/TaskAsset/DishWashing/sponge.png"));
        spongeClickable.setFitWidth(100);
        spongeClickable.setFitHeight(100);
        spongeClickable.setTranslateX(-250);

        spongeClickable.setOnMouseClicked(e -> {
            spongeView.setVisible(true);
            spongeClickable.setVisible(false);
        });
        
        root.setOnMouseMoved(e -> {
            mouseX = e.getX();
            mouseY = e.getY();
        });

        final int[] cleanProgress = {0};

        spongeFollowTimeline = new Timeline(new KeyFrame(Duration.millis(10), e -> {
            if (spongeView.isVisible() && root.getScene() != null) {
                double targetX = mouseX - root.getWidth() / 2;
                double targetY = mouseY - root.getHeight() / 2;

                spongeView.setTranslateX(spongeView.getTranslateX() + (targetX - spongeView.getTranslateX()) * 0.2);
                spongeView.setTranslateY(spongeView.getTranslateY() + (targetY - spongeView.getTranslateY()) * 0.2);
            }
        }));
        spongeFollowTimeline.setCycleCount(Timeline.INDEFINITE);
        spongeFollowTimeline.play();

        washingTimeline = new Timeline(new KeyFrame(Duration.millis(1000), e -> {
            if (spongeView.isVisible() && dishView.getBoundsInParent().intersects(spongeView.getBoundsInParent())) {
                if (cleanProgress[0] < dishStages.length - 1) {
                    cleanProgress[0]++;
                    dishView.setImage(dishStages[cleanProgress[0]]);
                }
                if (cleanProgress[0] == dishStages.length - 1) {
                    washingTimeline.stop();
                    spongeFollowTimeline.stop();
                    new Timeline(new KeyFrame(Duration.seconds(1), event -> {
                        completeTask();
                    })).play();
                }
            }
        }));
        washingTimeline.setCycleCount(Timeline.INDEFINITE);
        washingTimeline.play();

        root.getChildren().addAll(dishView, spongeClickable, spongeView);

        // Assemble the main container
        VBox mainContainer = new VBox(0);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.getChildren().add(titlePanel);
        
        VBox.setMargin(contentPanel, new javafx.geometry.Insets(25, 0, 0, 0));
        contentPanel.getChildren().addAll(instructions, root);
        mainContainer.getChildren().add(contentPanel);
        
        getChildren().addAll(mainContainer, getCloseButton());
    }

    @Override
    protected void initializeTask() {
        // Initialization happens in constructor
    }

    @Override
    public boolean isCompleted() {
        return false; // This is handled by the completeTask method
    }
}
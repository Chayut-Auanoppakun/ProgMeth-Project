package Test_Code;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;

public class PLayerAnimation extends Application {
    private static final int FRAME_WIDTH = 48;
    private static final int FRAME_HEIGHT = 75;
    private static final int SPRITE_COLUMNS = 6;
    private static final int SPRITE_ROWS = 2;
    private static final int ANIMATION_SPEED = 150; // milliseconds per frame
    
    private double playerX = 100;
    private double playerSpeed = 5;
    private boolean moving = false;
    
    private Animation animation;
    private ImageView player;
    private int directionRow = 1; // Default facing right
    
    @Override
    public void start(Stage primaryStage) {
        Pane root = new Pane();
        root.setStyle("-fx-background-color: green;");
        
        Image spriteSheet = new Image("/player/01.png");
        player = new ImageView(spriteSheet);
        player.setViewport(new Rectangle2D(0, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT));
        player.setX(playerX);
        player.setY(200);
        
        animation = new Transition() {
            {
                setCycleDuration(Duration.millis(SPRITE_COLUMNS * ANIMATION_SPEED));
                setInterpolator(Interpolator.LINEAR);
                setCycleCount(INDEFINITE);
            }
            
            @Override
            protected void interpolate(double frac) {
                int index = (int) (frac * SPRITE_COLUMNS);
                player.setViewport(new Rectangle2D(index * FRAME_WIDTH, directionRow * FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT));
            }
        };
        animation.play();
        animation.stop();
        
        Scene scene = new Scene(root, 600, 400);
        root.getChildren().add(player);
        
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.A) {
                directionRow = 0; // Walk left
                playerX -= playerSpeed;
                player.setX(playerX);
                if (!moving) {
                    animation.play();
                    moving = true;
                }
            } else if (event.getCode() == KeyCode.D) {
                directionRow = 1; // Walk right
                playerX += playerSpeed;
                player.setX(playerX);
                if (!moving) {
                    animation.play();
                    moving = true;
                }
            }
        });
        
        scene.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.A || event.getCode() == KeyCode.D) {
                animation.stop();
                moving = false;
            }
        });
        
        primaryStage.setTitle("Green Room Game");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}

package Test_styles;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class GifDemo extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("GIF Demo");

        // Load the GIF image
        Image gifImage = new Image(getClass().getResourceAsStream("/amongus.gif"));
        
        // Create an ImageView to display the GIF
        ImageView imageView = new ImageView(gifImage);

        // Create a layout and add the ImageView
        StackPane root = new StackPane();
        root.getChildren().add(imageView);

        // Set the scene and show the stage
        Scene scene = new Scene(root, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

package application;

import gui.MainMenuPane;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;


public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {

        double aspectRatio = 16.0 / 9.0;
        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        double screenHeight = Screen.getPrimary().getBounds().getHeight();

        double width = screenWidth;
        double height = width / aspectRatio;

        if (height > screenHeight) {
            height = screenHeight;
            width = height * aspectRatio;
        }
        

        MainMenuPane mainMenu = new MainMenuPane(primaryStage,900,600);
        Scene scene = new Scene(mainMenu, 900, 600);
        primaryStage.setTitle("Among CEDT");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
        
        
    }

    public static void main(String[] args) {
        launch(args);
    }

}

package application;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import client.GameClient;
import server.GameServer;
import server.PlayerInfo;

public class GameWindow {
    private Stage gameStage;
    private Canvas canvas;
    private GraphicsContext gc;
    private AnimationTimer timer;

    public void start(Stage stage) {
        this.gameStage = stage;
        canvas = new Canvas(600, 600);
        gc = canvas.getGraphicsContext2D();

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

                if (Main.getState() == 1) { // Server Mode
                    // Draw server position in red
                    gc.setFill(Color.RED);
                    String serverKey = GameServer.getLocalAddressPort();
					gc.fillOval(GameServer.getServerX(), GameServer.getServerY(), 20, 20);
                    

                    // Draw client positions in green
                    gc.setFill(Color.GREEN);
                    for (String key : GameServer.getPlayerPositions().keySet()) {
                        if (!key.equals(serverKey)) {
                            PlayerInfo playerInfo = GameServer.getPlayerPositions().get(key);
                            gc.fillOval(playerInfo.getX(), playerInfo.getY(), 20, 20);
                        }
                    }
                } else if (Main.getState() == 2) { // Client Mode
                    // Draw client position in red
                    gc.setFill(Color.RED);
                    gc.fillOval(GameClient.getClientX(), GameClient.getClientY(), 20, 20);

                    // Draw other player positions in green and server in blue
                    for (String key : GameClient.getPlayerPositions().keySet()) {
                        PlayerInfo playerInfo = GameClient.getPlayerPositions().get(key);

                        if (key.equals(GameClient.getLocalAddressPort())) {
                            gc.setFill(Color.RED); // Current player (you) - Client
                        } else if (key.equals(GameServer.getLocalAddressPort())) {
                            gc.setFill(Color.BLUE); // Server
                        } else {
                            gc.setFill(Color.GREEN); // Other players
                        }

                        // Draw player
                        gc.fillOval(playerInfo.getX(), playerInfo.getY(), 20, 20);
                    }
                }
            }
        };
        timer.start();

        Group root = new Group();
        root.getChildren().add(canvas);
        Scene scene = new Scene(root, 600, 600);

        // Handle user input
        scene.setOnKeyPressed(this::handleKeyPress);
        scene.setOnKeyReleased(this::handleKeyRelease);

        gameStage.setScene(scene);
        gameStage.setTitle("Game Window");
        gameStage.show();

        gameStage.setOnCloseRequest(event -> {
            timer.stop();
            Platform.runLater(() -> Main.settoGamedisable(false)); // Re-enable the toGameButton in Main
        });
    }

    private void handleKeyPress(KeyEvent event) {
        double deltaX = 0, deltaY = 0;
        switch (event.getCode()) {
            case W -> deltaY = -5;
            case S -> deltaY = 5;
            case A -> deltaX = -5;
            case D -> deltaX = 5;
            default -> {}
        }
        if (Main.getState() == 1) { // Server
            GameServer.setDelta(deltaX, deltaY);
        } else if (Main.getState() == 2) { // Client
            GameClient.setDelta(deltaX, deltaY);
        }
    }

    private void handleKeyRelease(KeyEvent event) {
        double deltaX = 0, deltaY = 0;
        switch (event.getCode()) {
            case W, S -> deltaY = 0;
            case A, D -> deltaX = 0;
            default -> {}
        }
        if (Main.getState() == 1) { // Server
            GameServer.setDelta(deltaX, deltaY);
        } else if (Main.getState() == 2) { // Client
            GameClient.setDelta(deltaX, deltaY);
        }
    }
}

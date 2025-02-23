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
import javafx.scene.image.Image;
import client.GameClient;
import server.GameServer;
import server.PlayerInfo;

public class GameWindow {
    private Stage gameStage;
    private Canvas canvas;
    private GraphicsContext gc;
    private AnimationTimer timer;
    private Image background;
    
    // Camera position tracking
    private double backgroundX = 0;
    private double backgroundY = 0;
    
    // Screen dimensions
    private final double screenWidth = 600;
    private final double screenHeight = 600;
    
    // Movement flags
    private boolean up, down, left, right;
    private final double speed = 10;

    public void start(Stage stage) {
        this.gameStage = stage;
        canvas = new Canvas(screenWidth, screenHeight);
        gc = canvas.getGraphicsContext2D();
        
        // Load background image
        try {
            background = new Image(getClass().getResourceAsStream("/background.png"));
            System.out.println("Background loaded successfully");
        } catch (Exception e) {
            System.err.println("Failed to load background image: " + e.getMessage());
            e.printStackTrace();
        }

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
                render();
            }
        };
        timer.start();

        Group root = new Group();
        root.getChildren().add(canvas);
        Scene scene = new Scene(root, screenWidth, screenHeight);

        scene.setOnKeyPressed(this::handleKeyPress);
        scene.setOnKeyReleased(this::handleKeyRelease);

        gameStage.setScene(scene);
        gameStage.setTitle("Game Window");
        gameStage.show();

        gameStage.setOnCloseRequest(event -> {
            timer.stop();
            Platform.runLater(() -> Main.settoGamedisable(false));
        });
    }

    private void update() {
        // Calculate movement delta
        double dx = 0, dy = 0;
        if (up) dy -= speed;
        if (down) dy += speed;
        if (left) dx -= speed;
        if (right) dx += speed;

        // Send movement input
        if (dx != 0 || dy != 0) {
            if (Main.getState() == 1) { // Server
                GameServer.setDelta(dx, dy);
            } else if (Main.getState() == 2) { // Client
                GameClient.setDelta(dx, dy);
            }
        }

        // Update camera position based on current player position
        updateCamera();
    }

    private void updateCamera() {
        double playerX, playerY;
        String localKey;
        
        if (Main.getState() == 1) { // Server
            playerX = GameServer.getServerX();
            playerY = GameServer.getServerY();
        } else { // Client
            localKey = GameClient.getLocalAddressPort();
            PlayerInfo playerInfo = GameClient.getPlayerPositions().get(localKey);
            if (playerInfo != null) {
                playerX = playerInfo.getX();
                playerY = playerInfo.getY();
            } else {
                return; // No position data yet
            }
        }

        // Calculate ideal camera position (centered on current player)
        double idealBackgroundX = playerX - screenWidth / 2;
        double idealBackgroundY = playerY - screenHeight / 2;
        
        // Clamp camera to map bounds
        if (background != null) {
            backgroundX = Math.max(0, Math.min(idealBackgroundX, background.getWidth() - screenWidth));
            backgroundY = Math.max(0, Math.min(idealBackgroundY, background.getHeight() - screenHeight));
        }
    }

    private void render() {
        gc.clearRect(0, 0, screenWidth, screenHeight);

        // Draw background
        if (background != null) {
            gc.drawImage(background, -backgroundX, -backgroundY);
        }

        if (Main.getState() == 1) { // Server Mode
            String serverKey = GameServer.getLocalAddressPort();
            
            // Draw server position
            gc.setFill(Color.RED);
            double serverScreenX = GameServer.getServerX() - backgroundX;
            double serverScreenY = GameServer.getServerY() - backgroundY;
            gc.fillOval(serverScreenX, serverScreenY, 20, 20);

            // Draw clients
            gc.setFill(Color.GREEN);
            for (String key : GameServer.getPlayerPositions().keySet()) {
                if (!key.equals(serverKey)) {
                    PlayerInfo playerInfo = GameServer.getPlayerPositions().get(key);
                    double playerScreenX = playerInfo.getX() - backgroundX;
                    double playerScreenY = playerInfo.getY() - backgroundY;
                    gc.fillOval(playerScreenX, playerScreenY, 20, 20);
                }
            }
        } else if (Main.getState() == 2) { // Client Mode
            // Draw all players from client's perspective
            for (String key : GameClient.getPlayerPositions().keySet()) {
                PlayerInfo playerInfo = GameClient.getPlayerPositions().get(key);
                double playerScreenX = playerInfo.getX() - backgroundX;
                double playerScreenY = playerInfo.getY() - backgroundY;

                if (key.equals(GameClient.getLocalAddressPort())) {
                    gc.setFill(Color.RED); // Current client
                } else if (key.equals(GameServer.getLocalAddressPort())) {
                    gc.setFill(Color.BLUE); // Server
                } else {
                    gc.setFill(Color.GREEN); // Other clients
                }
                
                gc.fillOval(playerScreenX, playerScreenY, 20, 20);
            }
        }
    }

    private void handleKeyPress(KeyEvent event) {
        switch (event.getCode()) {
            case W -> up = true;
            case S -> down = true;
            case A -> left = true;
            case D -> right = true;
            default -> {}
        }
    }

    private void handleKeyRelease(KeyEvent event) {
        switch (event.getCode()) {
            case W -> up = false;
            case S -> down = false;
            case A -> left = false;
            case D -> right = false;
            default -> {}
        }
    }
}
package application;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.image.Image;

import java.util.HashSet;
import java.util.Set;

import Logic.GameLogic;
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
    private final double screenWidth = 1920;
    private final double screenHeight = 1080;
    
    // Movement flags
    private boolean up, down, left, right;
    private final double speed = 10;
    private Set<KeyCode> pressedKeys = new HashSet<>();
    
    private long lastTime = System.nanoTime();
    private int frames = 0;
    private long fpsUpdateTime = System.nanoTime();
    private int fps = 0;
    private GameClient gameClient; // Declare it at the top

    public GameWindow(GameClient gameClient) {  // Modify constructor
        this.gameClient = gameClient;
    }

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
                // Calculate FPS
                frames++;
                if (now - fpsUpdateTime >= 1_000_000_000) { // One second has passed
                    fps = frames;
                    frames = 0;
                    fpsUpdateTime = now;
                }

                update();
                render();
                displayFPS();
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
    
    private void displayFPS() {
        gc.setFill(Color.WHITE);
        gc.setFont(new Font("Arial", 20));
        gc.fillText("FPS: " + fps, 10, 20);
    }

    private void update() {
     
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
            PlayerInfo playerInfo = GameClient.getplayerList().get(localKey);
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
            for (String key : GameServer.getplayerList().keySet()) {
                if (!key.equals(serverKey)) {
                    PlayerInfo playerInfo = GameServer.getplayerList().get(key);
                    double playerScreenX = playerInfo.getX() - backgroundX;
                    double playerScreenY = playerInfo.getY() - backgroundY;
                    gc.fillOval(playerScreenX, playerScreenY, 20, 20);
                }
            }
        } else if (Main.getState() == 2) { // Client Mode
            // Draw all players from client's perspective
            for (String key : GameClient.getplayerList().keySet()) {
                PlayerInfo playerInfo = GameClient.getplayerList().get(key);
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
        pressedKeys.add(event.getCode());
        updateMovement();
    }

    private void handleKeyRelease(KeyEvent event) {
        pressedKeys.remove(event.getCode());
        updateMovement();
    }

    private void updateMovement() {
        double deltaX = 0, deltaY = 0;

        if (pressedKeys.contains(KeyCode.W)) {
            deltaY -= 5;
        }
        if (pressedKeys.contains(KeyCode.S)) {
            deltaY += 5;
        }
        if (pressedKeys.contains(KeyCode.A)) {
            deltaX -= 5;
        }
        if (pressedKeys.contains(KeyCode.D)) {
            deltaX += 5;
        }
        if (pressedKeys.contains(KeyCode.R)) {
//        	if(dead player nearby) {
//        		
//        	}
        	if (Main.getState() == 1) { // Server
        		GameLogic.reportDeadBody(Main.getServerName());
            } else if (Main.getState() == 2) { // Client
                	GameLogic.reportDeadBody(Main.getPlayerName());
                }
        }
        if (pressedKeys.contains(KeyCode.V)) {
//        	if(isImposter) {
//        		
//        	}
        	if (Main.getState() == 1) { // Server
        		GameLogic.imposterVentEnter(Main.getServerName());
            } else if (Main.getState() == 2) { // Client
                	GameLogic.imposterVentEnter(Main.getPlayerName());
                }
        }
        
        
        if (Main.getState() == 1) { // Server
            GameServer.setDelta(deltaX, deltaY);
        } else if (Main.getState() == 2) { // Client
            GameClient.setDelta(deltaX, deltaY);
        }
    }
    
    
}
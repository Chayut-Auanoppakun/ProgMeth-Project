package gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import logic.ClientLogic;
import logic.ServerLogic;
import org.mapeditor.core.Map;
import org.mapeditor.core.MapLayer;
import org.mapeditor.core.ObjectGroup;
import org.mapeditor.core.Tile;
import org.mapeditor.core.TileLayer;
import org.mapeditor.io.TMXMapReader;
import org.mapeditor.view.MapRenderer;
import org.mapeditor.view.OrthogonalRenderer;
import org.mapeditor.view.IsometricRenderer;
import org.mapeditor.view.HexagonalRenderer;
import server.PlayerInfo;

public class GameWindow {
    private Stage gameStage;
    private Canvas canvas;
    private GraphicsContext gc;
    private AnimationTimer timer;
    
    // TMX Map variables
    private static final String MAP_FILE = "assets/map.tmx";
    private static final int MAX_CACHE_SIZE = 4000; // Increased for complex maps
    private HashMap<String, WritableImage> tileImageCache = new HashMap<>();
    private java.util.LinkedList<String> tileCacheOrder = new java.util.LinkedList<>();
    private Map map;
    private MapRenderer renderer;
    
    // List to store collision objects
    private List<CollisionObject> collisionObjects = new ArrayList<>();

    // Player position tracking (now serving as camera position)
    private double viewportX = 0;
    private double viewportY = 0;

    // Screen dimensions
    private final double screenWidth = 800;
    private final double screenHeight = 600;

    // Movement flags
    private Set<KeyCode> pressedKeys = new HashSet<>();

    // FPS tracking
    private int frames = 0;
    private long fpsUpdateTime = System.nanoTime();
    private int fps = 0;
    
    // Player size
    private final double PLAYER_RADIUS = 10;
    private final double CAMERA_ZOOM = 0.50; // 15% of screen (much closer than before)

    public void start(Stage stage) {
        this.gameStage = stage;
        canvas = new Canvas(screenWidth, screenHeight);
        gc = canvas.getGraphicsContext2D();

        // Initialize TMX map
        try {
            System.out.println("Attempting to read the map...");
            TMXMapReader mapReader = new TMXMapReader();
            File file = new File(MAP_FILE);
            URL url = file.toURI().toURL();
            map = mapReader.readMap(url);
            renderer = createRenderer(map);
            System.out.println("Map loaded successfully.");
            
            // Initialize collision objects
            loadCollisionObjects();
        } catch (Exception e) {
            System.out.println("Error while reading the map:\n" + e.getMessage());
            e.printStackTrace();
            // Continue with a null map - will need to handle this case in rendering
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
        gameStage.setTitle("TMX Map Game Window");
        gameStage.show();

        gameStage.setOnCloseRequest(event -> {
            timer.stop();
            Platform.runLater(() -> ServerGui.settoGamedisable(false));
        });
    }
    
    private void loadCollisionObjects() {
        collisionObjects.clear();
        
        // Look for object layers that might contain collision objects
        for (MapLayer layer : map.getLayers()) {
            if (layer instanceof ObjectGroup) {
                ObjectGroup objectGroup = (ObjectGroup) layer;
                
                // Check if this layer is meant for collisions (by name convention or property)
                if (objectGroup.getName().toLowerCase().contains("collision") || 
                    "true".equals(objectGroup.getProperties().getProperty("collision"))) {
                    
                    for (org.mapeditor.core.MapObject object : objectGroup.getObjects()) {
                        collisionObjects.add(new CollisionObject(
                            object.getX(), 
                            object.getY(), 
                            object.getWidth(), 
                            object.getHeight()
                        ));
                    }
                }
            }
        }
        
        System.out.println("Loaded " + collisionObjects.size() + " collision objects");
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
        double playerX = 0, playerY = 0;
        String localKey;

        // Get the current player position
        if (ServerGui.getState() == 1) { // Server
            playerX = ServerLogic.getServerX();
            playerY = ServerLogic.getServerY();
        } else { // Client
            localKey = ClientLogic.getLocalAddressPort();
            PlayerInfo playerInfo = ClientLogic.getplayerList().get(localKey);
            if (playerInfo != null) {
                playerX = playerInfo.getX();
                playerY = playerInfo.getY();
            } else {
                return; // No position data yet
            }
        }

        // Calculate ideal camera position (much closer to player)
        double idealViewportX = playerX - screenWidth * CAMERA_ZOOM;
        double idealViewportY = playerY - screenHeight * CAMERA_ZOOM;

        // Smoothly interpolate camera position for less jerky movement
        double smoothFactor = 0.5; // Adjust value for smoother/faster camera
        viewportX = viewportX + (idealViewportX - viewportX) * smoothFactor;
        viewportY = viewportY + (idealViewportY - viewportY) * smoothFactor;

        // Clamp camera to map bounds
        if (map != null) {
            int mapPixelWidth = map.getWidth() * map.getTileWidth();
            int mapPixelHeight = map.getHeight() * map.getTileHeight();
            viewportX = Math.max(0, Math.min(viewportX, mapPixelWidth - screenWidth));
            viewportY = Math.max(0, Math.min(viewportY, mapPixelHeight - screenHeight));
        }
    }
    private void render() {
        gc.clearRect(0, 0, screenWidth, screenHeight);

        // Draw TMX map
        if (map != null) {
            drawMap(gc, viewportX, viewportY, screenWidth, screenHeight);
        } else {
            // Fallback: Draw a gray background if no map is loaded
            gc.setFill(Color.DARKGRAY);
            gc.fillRect(0, 0, screenWidth, screenHeight);
        }

        // Draw players
        renderPlayers();
        
        // Debug: Draw collision objects
        renderCollisionObjects();
    }
    
    private void renderCollisionObjects() {
//         Uncomment this section for debugging collision objects
//        /*
        gc.setStroke(Color.RED);
        gc.setGlobalAlpha(0.5);
        for (CollisionObject obj : collisionObjects) {
            gc.strokeRect(
                obj.x - viewportX,
                obj.y - viewportY,
                obj.width,
                obj.height
            );
        }
        gc.setGlobalAlpha(1.0);
//        */
    }

    private void renderPlayers() {
        if (ServerGui.getState() == 1) { // Server Mode
            String serverKey = ServerLogic.getLocalAddressPort();

            // Draw server position
            gc.setFill(Color.RED);
            double serverScreenX = ServerLogic.getServerX() - viewportX;
            double serverScreenY = ServerLogic.getServerY() - viewportY;
            gc.fillOval(serverScreenX - PLAYER_RADIUS, serverScreenY - PLAYER_RADIUS, PLAYER_RADIUS * 2, PLAYER_RADIUS * 2);

            // Draw clients
            gc.setFill(Color.GREEN);
            for (String key : ServerLogic.getplayerList().keySet()) {
                if (!key.equals(serverKey)) {
                    PlayerInfo playerInfo = ServerLogic.getplayerList().get(key);
                    double playerScreenX = playerInfo.getX() - viewportX;
                    double playerScreenY = playerInfo.getY() - viewportY;
                    gc.fillOval(playerScreenX - PLAYER_RADIUS, playerScreenY - PLAYER_RADIUS, PLAYER_RADIUS * 2, PLAYER_RADIUS * 2);
                }
            }
        } else if (ServerGui.getState() == 2) { // Client Mode
            // Draw all players from client's perspective
            for (String key : ClientLogic.getplayerList().keySet()) {
                PlayerInfo playerInfo = ClientLogic.getplayerList().get(key);
                double playerScreenX = playerInfo.getX() - viewportX;
                double playerScreenY = playerInfo.getY() - viewportY;

                if (key.equals(ClientLogic.getLocalAddressPort())) {
                    gc.setFill(Color.RED); // Current client
                } else if (key.equals(ServerLogic.getLocalAddressPort())) {
                    gc.setFill(Color.BLUE); // Server
                } else {
                    gc.setFill(Color.GREEN); // Other clients
                }

                gc.fillOval(playerScreenX - PLAYER_RADIUS, playerScreenY - PLAYER_RADIUS, PLAYER_RADIUS * 2, PLAYER_RADIUS * 2);
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
        double movementSpeed = 5;

        if (pressedKeys.contains(KeyCode.W)) {
            deltaY -= movementSpeed;
        }
        if (pressedKeys.contains(KeyCode.S)) {
            deltaY += movementSpeed;
        }
        if (pressedKeys.contains(KeyCode.A)) {
            deltaX -= movementSpeed;
        }
        if (pressedKeys.contains(KeyCode.D)) {
            deltaX += movementSpeed;
        }

        // Get current player position
        double currentX, currentY;
        if (ServerGui.getState() == 1) { // Server
            currentX = ServerLogic.getServerX();
            currentY = ServerLogic.getServerY();
        } else { // Client
            String localKey = ClientLogic.getLocalAddressPort();
            PlayerInfo playerInfo = ClientLogic.getplayerList().get(localKey);
            if (playerInfo == null) {
                return; // No position data yet
            }
            currentX = playerInfo.getX();
            currentY = playerInfo.getY();
        }

        // Calculate new position
        double newX = currentX + deltaX;
        double newY = currentY + deltaY;
        
        // Check for collisions and adjust movement
        if (wouldCollide(currentX, currentY, newX, newY)) {
            // Try horizontal movement only
            if (Math.abs(deltaX) > 0 && !wouldCollide(currentX, currentY, newX, currentY)) {
                newY = currentY; // Allow only horizontal movement
            } 
            // Try vertical movement only
            else if (Math.abs(deltaY) > 0 && !wouldCollide(currentX, currentY, currentX, newY)) {
                newX = currentX; // Allow only vertical movement
            }
            // If both directions collide, don't move
            else {
                newX = currentX;
                newY = currentY;
            }
        }
        
        // Calculate the adjusted deltas
        deltaX = newX - currentX;
        deltaY = newY - currentY;

        // Send the adjusted movement
        if (ServerGui.getState() == 1) { // Server
            ServerLogic.setDelta(deltaX, deltaY);
        } else if (ServerGui.getState() == 2) { // Client
            ClientLogic.setDelta(deltaX, deltaY);
        }
    }
    
    private boolean wouldCollide(double currentX, double currentY, double newX, double newY) {
        // Check collision with each collision object
        for (CollisionObject obj : collisionObjects) {
            // Simple circle vs rectangle collision
            double closestX = Math.max(obj.x, Math.min(newX, obj.x + obj.width));
            double closestY = Math.max(obj.y, Math.min(newY, obj.y + obj.height));
            
            double distanceX = newX - closestX;
            double distanceY = newY - closestY;
            
            if ((distanceX * distanceX + distanceY * distanceY) <= (PLAYER_RADIUS * PLAYER_RADIUS)) {
                return true; // Collision detected
            }
        }
        
        return false; // No collision
    }

    // TMX Map rendering methods from the first example
    private void drawMap(GraphicsContext gc, double viewportX, double viewportY, double viewportWidth, double viewportHeight) {
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, viewportWidth, viewportHeight);

        for (MapLayer layer : map.getLayers()) {
            if (layer instanceof TileLayer) {
                drawTileLayer(gc, (TileLayer) layer, viewportX, viewportY, viewportWidth, viewportHeight);
            } else if (layer instanceof ObjectGroup) {
                drawObjectGroup(gc, (ObjectGroup) layer, viewportX, viewportY);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void drawTileLayer(GraphicsContext gc, TileLayer layer, double viewportX, double viewportY, double viewportWidth, double viewportHeight) {
        // Calculate the visible tile range
        int startX = Math.max(0, (int)(viewportX / map.getTileWidth()));
        int startY = Math.max(0, (int)(viewportY / map.getTileHeight()));
        int endX = Math.min(layer.getWidth(), (int)((viewportX + viewportWidth) / map.getTileWidth()) + 2);
        int endY = Math.min(layer.getHeight(), (int)((viewportY + viewportHeight) / map.getTileHeight()) + 2);
        
        // Only process tiles in the viewport
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                Tile tile = layer.getTileAt(x, y);
                
                if (tile == null) {
                    continue;
                }

                int tileX = x * map.getTileWidth();
                int tileY = y * map.getTileHeight();
                
                // Get flip flags
                boolean flipHorizontally = layer.isFlippedHorizontally(x, y);
                boolean flipVertically = layer.isFlippedVertically(x, y);
                boolean flipDiagonally = layer.isFlippedDiagonally(x, y);

                // Create a composite key that includes flip information to avoid conflicts
                String cacheKey = tile.getId() + "_" + layer.getName() + "_" + x + "_" + y + "_" 
                    + flipHorizontally + "_" + flipVertically + "_" + flipDiagonally;

                // Check if the image is already in the cache
                WritableImage writableImage = tileImageCache.get(cacheKey);
                if (writableImage == null) {
                    BufferedImage bufferedImage = (BufferedImage) tile.getImage();
                    if (bufferedImage != null) {
                        try {
                            int width = bufferedImage.getWidth();
                            int height = bufferedImage.getHeight();
                            
                            // Create a new writable image for the tile
                            writableImage = new WritableImage(width, height);
                            PixelWriter pw = writableImage.getPixelWriter();

                            int[] pixels = new int[width * height];
                            bufferedImage.getRGB(0, 0, width, height, pixels, 0, width);

                            for (int i = 0; i < height; i++) {
                                for (int j = 0; j < width; j++) {
                                    int pixel = pixels[i * width + j];
                                    pw.setArgb(j, i, pixel);
                                }
                            }

                            tileImageCache.put(cacheKey, writableImage);
                            manageCache(cacheKey);
                        } catch (Exception e) {
                            System.err.println("Error processing tile at (" + x + "," + y + ") in layer " + layer.getName());
                            e.printStackTrace();
                            continue;
                        }
                    } else {
                        continue;
                    }
                } else {
                    manageCache(cacheKey);
                }

                try {
                    // Save the current state of the graphics context
                    gc.save();
                    
                    // Translate to the position where the tile should be drawn (adjusted for viewport)
                    gc.translate(tileX - viewportX, tileY - viewportY);
                    
                    // Apply transformations based on flip flags
                    if (flipDiagonally) {
                        // Flip diagonally means rotate 90 degrees and then flip horizontally
                        gc.translate(map.getTileWidth(), 0);
                        gc.rotate(90);
                        
                        // Adjust the flipping flags for the remaining transformations
                        boolean temp = flipHorizontally;
                        flipHorizontally = flipVertically;
                        flipVertically = temp;
                    }
                    
                    // Apply horizontal flip
                    if (flipHorizontally) {
                        gc.translate(map.getTileWidth(), 0);
                        gc.scale(-1, 1);
                    }
                    
                    // Apply vertical flip
                    if (flipVertically) {
                        gc.translate(0, map.getTileHeight());
                        gc.scale(1, -1);
                    }
                    
                    // Draw the tile at the origin (0,0) since we've already translated
                    gc.drawImage(writableImage, 0, 0, map.getTileWidth(), map.getTileHeight());
                    
                    // Restore the graphics context to its previous state
                    gc.restore();
                } catch (Exception e) {
                    System.err.println("Error drawing tile at (" + x + "," + y + ") in layer " + layer.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    private void manageCache(String cacheKey) {
        // If we already have this tile, move it to the front of the queue
        if (tileImageCache.containsKey(cacheKey)) {
            tileCacheOrder.remove(cacheKey);
            tileCacheOrder.addFirst(cacheKey);
            return;
        }
        
        // Check if we need to remove the oldest entry
        if (tileImageCache.size() >= MAX_CACHE_SIZE) {
            String oldestKey = tileCacheOrder.removeLast();
            tileImageCache.remove(oldestKey);
        }
        
        // Add the new tile to the front of the queue
        tileCacheOrder.addFirst(cacheKey);
    }
    
    private void drawObjectGroup(GraphicsContext gc, ObjectGroup objectGroup, double viewportX, double viewportY) {
        // Only draw objects for debugging - exclude collision layers to avoid visual clutter
        if (objectGroup.getName().toLowerCase().contains("collision") || 
            "true".equals(objectGroup.getProperties().getProperty("collision"))) {
            return;
        }
        
        gc.setStroke(Color.RED);
        for (org.mapeditor.core.MapObject object : objectGroup.getObjects()) {
            // Only draw objects that are at least partially visible in the viewport
            if (isObjectVisible(object, viewportX, viewportY, screenWidth, screenHeight)) {
                double x = object.getX() - viewportX;
                double y = object.getY() - viewportY;
                gc.strokeRect(x, y, object.getWidth(), object.getHeight());
            }
        }
    }

    private boolean isObjectVisible(org.mapeditor.core.MapObject object, double viewportX, double viewportY, 
                                    double viewportWidth, double viewportHeight) {
        // Check if the object intersects with the viewport
        return (object.getX() + object.getWidth() > viewportX &&
                object.getX() < viewportX + viewportWidth &&
                object.getY() + object.getHeight() > viewportY &&
                object.getY() < viewportY + viewportHeight);
    }

    private static MapRenderer createRenderer(Map map) {
        switch (map.getOrientation()) {
        case ORTHOGONAL:
            return new OrthogonalRenderer(map);
        case ISOMETRIC:
            return new IsometricRenderer(map);
        case HEXAGONAL:
            return new HexagonalRenderer(map);
        default:
            return null;
        }
    }
    
    // Helper class to store collision objects
    private static class CollisionObject {
        public double x, y, width, height;
        
        public CollisionObject(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
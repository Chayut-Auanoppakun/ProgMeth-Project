package gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.math.RoundingMode;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameWindow {
	private Stage gameStage;
	private Canvas canvas;
	private GraphicsContext gc;
	private AnimationTimer timer;

	// TMX Map variables
	private static final String MAP_FILE = "assets/map.tmx";
	private static final int MAX_CACHE_SIZE = 1000; // Increased for complex maps
	private ConcurrentHashMap<String, WritableImage> tileImageCache = new ConcurrentHashMap<>();

	private java.util.concurrent.ConcurrentLinkedDeque<String> tileCacheOrder = new java.util.concurrent.ConcurrentLinkedDeque<>();
	private ExecutorService threadPool;
	private Map map;
	private MapRenderer renderer;
	private long lastUpdate = 0; // Track the last update time
	private double playerX = 1300; // Example player X position
	private double playerY = 2175; // Example player Y position
	private double speed = 100; // Movement speed in units per second
	// List to store collision objects
	private List<CollisionObject> collisionObjects = new CopyOnWriteArrayList<>();
	private ConcurrentHashMap<String, ConcurrentLinkedQueue<CollisionObject>> spatialGrid;

	private final int GRID_CELL_SIZE = 128; // Size of each grid cell, adjust based on game scale

	// Player position tracking (now serving as camera position)
	private double viewportX = 0;
	private double viewportY = 0;

	// Screen dimensions
	private final double screenWidth = 1080;
	private final double screenHeight = 720;

	// Movement flags
	private Set<KeyCode> pressedKeys = new HashSet<>();

	// FPS tracking
	private int frames = 0;
	private long fpsUpdateTime = System.nanoTime();
	private int fps = 0;

	// Player size
	private final double PLAYER_RADIUS = 10;
	private final double CAMERA_ZOOM = 1.8; // 15% of screen (much closer than before)

	public void start(Stage stage) {
		this.gameStage = stage;
		canvas = new Canvas(screenWidth, screenHeight);
		gc = canvas.getGraphicsContext2D();
		threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
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
				updateMovement(now);
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
		gameStage.setTitle("AMONG CEDT");
		gameStage.show();

		gameStage.setOnCloseRequest(event -> {
			timer.stop();
			threadPool.shutdown();
			try {
				if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
					threadPool.shutdownNow();
				}
			} catch (InterruptedException e) {
				threadPool.shutdownNow();
			}
			Platform.runLater(() -> ServerGui.settoGamedisable(false));
		});
	}

	private void loadCollisionObjects() {
		collisionObjects.clear();
		final List<CollisionObject> tempCollisionObjects = Collections.synchronizedList(new ArrayList<>());

		for (MapLayer layer : map.getLayers()) {
			if (layer instanceof ObjectGroup) {
				ObjectGroup objectGroup = (ObjectGroup) layer;

				if (objectGroup.getName().toLowerCase().contains("collision")
						|| "true".equals(objectGroup.getProperties().getProperty("collision"))) {

					for (org.mapeditor.core.MapObject object : objectGroup.getObjects()) {
						double x = object.getX();
						double y = object.getY();
						double width = object.getWidth();
						double height = object.getHeight();

						// Debug: Print collision object coordinates
						System.out.println(
								"Collision Object: x=" + x + ", y=" + y + ", width=" + width + ", height=" + height);

						tempCollisionObjects.add(new CollisionObject(x, y, width, height));
					}
				}
			}
		}

		collisionObjects.addAll(tempCollisionObjects);
		initializeSpatialGrid();

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
		double playerX = 0;
		double playerY = 0;
		if (ServerGui.getState() == 1) {
			playerX = ServerLogic.getServerX();
			// Get the player's current X position
			playerY = ServerLogic.getServerY(); // Get the player's current Y position
		} else {
			playerX = ClientLogic.getClientX();
			playerY = ClientLogic.getClientY();
		}

		// Calculate ideal camera position (keeping the player centered)
		double cameraCenterX = playerX - (screenWidth / 2) / CAMERA_ZOOM;
		double cameraCenterY = playerY - (screenHeight / 2) / CAMERA_ZOOM;

		// Smoothly interpolate the camera position
		double smoothFactor = 0.05; // Adjust smoothness of camera following
		viewportX += (cameraCenterX - viewportX) * smoothFactor;
		viewportY += (cameraCenterY - viewportY) * smoothFactor;

		// Clamp the camera position to the map boundaries
		int mapPixelWidth = map.getWidth() * map.getTileWidth();
		int mapPixelHeight = map.getHeight() * map.getTileHeight();

		// Clamp based on zoom (camera must stay within bounds even when zoomed)
		viewportX = Math.max(0, Math.min(viewportX, mapPixelWidth - screenWidth / CAMERA_ZOOM));
		viewportY = Math.max(0, Math.min(viewportY, mapPixelHeight - screenHeight / CAMERA_ZOOM));
	}

	private void render() {
		gc.clearRect(0, 0, screenWidth, screenHeight);

		// Scale the graphics context for zoom
		gc.save();
		gc.scale(CAMERA_ZOOM, CAMERA_ZOOM); // Zoom in or out

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

		gc.restore();
	}

	private void renderCollisionObjects() {
//         Uncomment this section for debugging collision objects
//        /*
		gc.setStroke(Color.RED);
		gc.setGlobalAlpha(0.5);
		for (CollisionObject obj : collisionObjects) {
			gc.strokeRect(obj.x - viewportX, obj.y - viewportY, obj.width, obj.height);
		}
		gc.setGlobalAlpha(1.0);
//        */
	}

	private void renderPlayers() {
	    // Define FOV rendering radius (larger than tile rendering to prevent pop-in)
	    final int FOV_RADIUS = 1000; // Adjust this value as needed
	    
	    if (ServerGui.getState() == 1) { // Server Mode
	        String serverKey = ServerLogic.getLocalAddressPort();
	        double serverScreenX = ServerLogic.getServerX() - viewportX;
	        double serverScreenY = ServerLogic.getServerY() - viewportY; // Fixed variable name
	        
	        // Draw server position
	        gc.setFill(Color.RED);
	        gc.fillOval(serverScreenX - PLAYER_RADIUS, serverScreenY - PLAYER_RADIUS, PLAYER_RADIUS * 2,
	                PLAYER_RADIUS * 2);

	        // Draw collision bounding box (48 width, 64 height centered on player)
	        gc.setStroke(Color.YELLOW);
	        double collisionBoxX = serverScreenX - 24; // Half of 48 (width)
	        double collisionBoxY = serverScreenY - 32; // Half of 64 (height)
	        gc.strokeRect(collisionBoxX, collisionBoxY, 48, 64);

	        // Draw the bottom 20-pixel collision area
	        gc.setStroke(Color.CYAN); // Use a different color for the collision area
	        double collisionAreaY = serverScreenY + 12; // Bottom 20 pixels (64 - 20 = 44, 44 / 2 = 22, 32 - 22 = 10)
	        gc.strokeRect(collisionBoxX, collisionAreaY, 48, 20);
	        
	        // Draw clients using FOV logic
	        gc.setFill(Color.GREEN);
	        for (String key : ServerLogic.getplayerList().keySet()) {
	            if (!key.equals(serverKey)) {
	                PlayerInfo playerInfo = ServerLogic.getplayerList().get(key);
	                
	                // Check if player is within FOV
	                double distX = playerInfo.getX() - ServerLogic.getServerX();
	                double distY = playerInfo.getY() - ServerLogic.getServerY();
	                double distance = Math.sqrt(distX * distX + distY * distY);
	                
	                if (distance <= FOV_RADIUS) {
	                    double playerScreenX = playerInfo.getX() - viewportX;
	                    double playerScreenY = playerInfo.getY() - viewportY;
	                    gc.fillOval(playerScreenX - PLAYER_RADIUS, playerScreenY - PLAYER_RADIUS, 
	                            PLAYER_RADIUS * 2, PLAYER_RADIUS * 2);
	                }
	            }
	        }
	    } else if (ServerGui.getState() == 2) { // Client Mode
	        // Draw all players from client's perspective with FOV logic
	        String localKey = ClientLogic.getLocalAddressPort();
	        PlayerInfo localPlayer = ClientLogic.getplayerList().get(localKey);
	        
	        if (localPlayer != null) {
	            double playerScreenX = localPlayer.getX() - viewportX;
	            double playerScreenY = localPlayer.getY() - viewportY;

	            // Draw the local player
	            gc.setFill(Color.RED); // Current client
	            gc.fillOval(playerScreenX - PLAYER_RADIUS, playerScreenY - PLAYER_RADIUS, 
	                    PLAYER_RADIUS * 2, PLAYER_RADIUS * 2);

	            // Draw collision bounding box
	            gc.setStroke(Color.YELLOW);
	            double collisionBoxX = playerScreenX - 24;
	            double collisionBoxY = playerScreenY - 32;
	            gc.strokeRect(collisionBoxX, collisionBoxY, 48, 64);

	            // Draw the bottom 20-pixel collision area
	            gc.setStroke(Color.CYAN);
	            double collisionAreaY = playerScreenY + 12;
	            gc.strokeRect(collisionBoxX, collisionAreaY, 48, 20);
	            
	            // Draw other players
	            for (String key : ClientLogic.getplayerList().keySet()) {
	                if (!key.equals(localKey)) {
	                    PlayerInfo playerInfo = ClientLogic.getplayerList().get(key);
	                    
	                    // Check if player is within FOV
	                    double distX = playerInfo.getX() - localPlayer.getX();
	                    double distY = playerInfo.getY() - localPlayer.getY();
	                    double distance = Math.sqrt(distX * distX + distY * distY);
	                    
	                    if (distance <= FOV_RADIUS) {
	                        double otherPlayerScreenX = playerInfo.getX() - viewportX;
	                        double otherPlayerScreenY = playerInfo.getY() - viewportY;
	                        
	                        if (key.equals(ServerLogic.getLocalAddressPort())) {
	                            gc.setFill(Color.BLUE); // Server
	                        } else {
	                            gc.setFill(Color.GREEN); // Other clients
	                        }
	                        
	                        gc.fillOval(otherPlayerScreenX - PLAYER_RADIUS, otherPlayerScreenY - PLAYER_RADIUS, 
	                                PLAYER_RADIUS * 2, PLAYER_RADIUS * 2);
	                    }
	                }
	            }
	        }
	    }
	}
	private void handleKeyPress(KeyEvent event) {
		pressedKeys.add(event.getCode());
		// System.out.println("Key pressed: " + event.getCode()); // Debug output
	}

	private void handleKeyRelease(KeyEvent event) {
		pressedKeys.remove(event.getCode());
		// System.out.println("Key released: " + event.getCode()); // Debug output
	}

	private void updateMovement(long now) {
		if (lastUpdate == 0) {
			lastUpdate = now;
			return;
		}

		// Calculate delta time (time since last frame in seconds)
		double deltaTime = (now - lastUpdate) / 1_000_000_000.0;
		lastUpdate = now;

		// Cap delta time to avoid huge jumps after lag
		if (deltaTime > 0.1) {
			deltaTime = 0.1;
		}

		// Calculate movement
		double dx = 0, dy = 0;
		if (pressedKeys.contains(KeyCode.W)) {
			dy -= speed * deltaTime; // Move up
		}
		if (pressedKeys.contains(KeyCode.S)) {
			dy += speed * deltaTime; // Move down
		}
		if (pressedKeys.contains(KeyCode.A)) {
			dx -= speed * deltaTime; // Move left
		}
		if (pressedKeys.contains(KeyCode.D)) {
			dx += speed * deltaTime; // Move right
		}

		// Move horizontally
		if (dx != 0) {
			if (!checkCollision(playerX + dx, playerY)) {
				playerX += dx;
			}
		}

		// Move vertically
		if (dy != 0) {
			if (!checkCollision(playerX, playerY + dy)) {
				playerY += dy;
			}
		}

		// Send the updated position to the server or client
		sendPositionUpdate(playerX, playerY);
	}

	private void sendPositionUpdate(double x, double y) {
		if (ServerGui.getState() == 1) { // Server
			ServerLogic.setPosition(Math.floor(x), Math.floor(y));
		} else if (ServerGui.getState() == 2) { // Client
			ClientLogic.setPosition(Math.floor(x), Math.floor(y));
		}
	}

	// Optimized collision check using spatial grid
	private boolean checkCollision(double x, double y) {
		double playerLeft = x - 24; // Half of 48 (width)
		double playerRight = x + 24; // Half of 48 (width)
		double playerBottom = y + 32; // Bottom of the player's bounding box (64 height)
		double playerFeetTop = playerBottom - 20; // Top of the 20-pixel tall collision area

		int startGridX = (int) (playerLeft / GRID_CELL_SIZE);
		int startGridY = (int) (playerFeetTop / GRID_CELL_SIZE);
		int endGridX = (int) (playerRight / GRID_CELL_SIZE);
		int endGridY = (int) (playerBottom / GRID_CELL_SIZE);

		for (int gridY = startGridY; gridY <= endGridY; gridY++) {
			for (int gridX = startGridX; gridX <= endGridX; gridX++) {
				String key = gridX + ":" + gridY;
				ConcurrentLinkedQueue<CollisionObject> cellObjects = spatialGrid.get(key);

				if (cellObjects != null) {
					for (CollisionObject obj : cellObjects) {
						double objLeft = obj.x;
						double objRight = obj.x + obj.width;
						double objTop = obj.y;
						double objBottom = obj.y + obj.height;

						// Check for collision with the bottom 20 pixels of the player
						if (playerRight > objLeft && playerLeft < objRight && playerBottom > objTop
								&& playerFeetTop < objBottom) {
							System.out.println("Collision with object at (" + obj.x + ", " + obj.y + ")");
							return true; // Collision detected
						}
					}
				}
			}
		}

		return false; // No collision
	}

	// TMX Map rendering methods from the first example
	private void drawMap(GraphicsContext gc, double viewportX, double viewportY, double viewportWidth,
			double viewportHeight) {
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
	private void drawTileLayer(GraphicsContext gc, TileLayer layer, double viewportX, double viewportY,
			double viewportWidth, double viewportHeight) {
// Calculate the visible tile range
		int startX = Math.max(0, (int) (viewportX / map.getTileWidth()));
		int startY = Math.max(0, (int) (viewportY / map.getTileHeight()));
		int endX = Math.min(layer.getWidth(), (int) ((viewportX + viewportWidth) / map.getTileWidth() + 2));
		int endY = Math.min(layer.getHeight(), (int) ((viewportY + viewportHeight) / map.getTileHeight() + 2));

// Prepare tiles that need to be processed
		List<TileRenderTask> renderTasks = new ArrayList<>();

// First pass: identify tiles and prepare rendering tasks
		for (int y = startY; y < endY; y++) {
			for (int x = startX; x < endX; x++) {
				Tile tile = layer.getTileAt(x, y);
				if (tile == null)
					continue;

				boolean flipHorizontally = layer.isFlippedHorizontally(x, y);
				boolean flipVertically = layer.isFlippedVertically(x, y);
				boolean flipDiagonally = layer.isFlippedDiagonally(x, y);

				String cacheKey = tile.getId() + "_" + layer.getName() + "_" + x + "_" + y + "_" + flipHorizontally
						+ "_" + flipVertically + "_" + flipDiagonally;

				WritableImage cachedImage = tileImageCache.get(cacheKey);
				if (cachedImage == null) {
					// Need to process this tile
					renderTasks.add(new TileRenderTask(tile, x, y, cacheKey, layer, flipHorizontally, flipVertically,
							flipDiagonally));
				} else {
					// Update cache order for existing image
					manageCache(cacheKey);
				}
			}
		}

// Second pass: process new tiles in parallel
		if (!renderTasks.isEmpty()) {
			try {
				List<CompletableFuture<Void>> futures = new ArrayList<>();

				for (TileRenderTask task : renderTasks) {
					CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
						processTileImage(task.tile, task.cacheKey);
					}, threadPool);
					futures.add(future);
				}

// Wait for all image processing to complete
				CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
			} catch (Exception e) {
				System.err.println("Error in parallel tile processing: " + e.getMessage());
				e.printStackTrace();
			}
		}

// Final pass: draw all tiles (must be on JavaFX thread)
		for (int y = startY; y < endY; y++) {
			for (int x = startX; x < endX; x++) {
				Tile tile = layer.getTileAt(x, y);
				if (tile == null)
					continue;

				boolean flipHorizontally = layer.isFlippedHorizontally(x, y);
				boolean flipVertically = layer.isFlippedVertically(x, y);
				boolean flipDiagonally = layer.isFlippedDiagonally(x, y);

				String cacheKey = tile.getId() + "_" + layer.getName() + "_" + x + "_" + y + "_" + flipHorizontally
						+ "_" + flipVertically + "_" + flipDiagonally;

				WritableImage writableImage = tileImageCache.get(cacheKey);
				if (writableImage != null) {
					drawTile(gc, writableImage, x, y, viewportX, viewportY, flipHorizontally, flipVertically,
							flipDiagonally);
				}
			}
		}
	}

	private void drawTile(GraphicsContext gc, WritableImage writableImage, int x, int y, double viewportX,
			double viewportY, boolean flipHorizontally, boolean flipVertically, boolean flipDiagonally) {
		int tileX = x * map.getTileWidth();
		int tileY = y * map.getTileHeight();

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
			System.err.println("Error drawing tile at (" + x + "," + y + ")");
			e.printStackTrace();
		}
	}

	private void processTileImage(Tile tile, String cacheKey) {
		BufferedImage bufferedImage = (BufferedImage) tile.getImage();
		if (bufferedImage == null)
			return;

		int width = bufferedImage.getWidth();
		int height = bufferedImage.getHeight();

		// Create WritableImage on the JavaFX thread
		Platform.runLater(() -> {
			WritableImage writableImage = new WritableImage(width, height);
			PixelWriter pw = writableImage.getPixelWriter();

			int[] pixels = new int[width * height];
			bufferedImage.getRGB(0, 0, width, height, pixels, 0, width);

			for (int i = 0; i < height; i++) {
				for (int j = 0; j < width; j++) {
					int pixel = pixels[i * width + j];
					pw.setArgb(j, i, pixel);
				}
			}

			// Add to cache
			tileImageCache.put(cacheKey, writableImage);
			manageCache(cacheKey);
		});
	}

	private void manageCache(String cacheKey) {
		// If we already have this tile, move it to the front of the queue
		if (tileImageCache.containsKey(cacheKey)) {
			tileCacheOrder.remove(cacheKey);
			tileCacheOrder.addFirst(cacheKey);
			return;
		}

		// Check if we need to remove the oldest entries
		while (tileImageCache.size() >= MAX_CACHE_SIZE) {
			String oldestKey = tileCacheOrder.pollLast();
			if (oldestKey != null) {
				tileImageCache.remove(oldestKey);
			} else {
				break;
			}
		}

		// Add the new tile to the front of the queue
		tileCacheOrder.addFirst(cacheKey);
	}

	private void drawObjectGroup(GraphicsContext gc, ObjectGroup objectGroup, double viewportX, double viewportY) {
		// Only draw objects for debugging - exclude collision layers to avoid visual
		// clutter
		if (objectGroup.getName().toLowerCase().contains("collision")
				|| "true".equals(objectGroup.getProperties().getProperty("collision"))) {
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
		return (object.getX() + object.getWidth() > viewportX && object.getX() < viewportX + viewportWidth
				&& object.getY() + object.getHeight() > viewportY && object.getY() < viewportY + viewportHeight);
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

	private static class TileRenderTask {
		public Tile tile;
		public int x, y;
		public String cacheKey;
		public TileLayer layer;
		public boolean flipHorizontally, flipVertically, flipDiagonally;

		public TileRenderTask(Tile tile, int x, int y, String cacheKey, TileLayer layer, boolean flipHorizontally,
				boolean flipVertically, boolean flipDiagonally) {
			this.tile = tile;
			this.x = x;
			this.y = y;
			this.cacheKey = cacheKey;
			this.layer = layer;
			this.flipHorizontally = flipHorizontally;
			this.flipVertically = flipVertically;
			this.flipDiagonally = flipDiagonally;
		}
	}

	private void initializeSpatialGrid() {
		spatialGrid = new ConcurrentHashMap<>();

		for (CollisionObject obj : collisionObjects) {
			int startGridX = (int) (obj.x / GRID_CELL_SIZE);
			int startGridY = (int) (obj.y / GRID_CELL_SIZE);
			int endGridX = (int) ((obj.x + obj.width) / GRID_CELL_SIZE);
			int endGridY = (int) ((obj.y + obj.height) / GRID_CELL_SIZE);

			// Debug: Print grid cells for each collision object
			System.out.println("Collision Object Grid Cells: x=" + obj.x + ", y=" + obj.y + ", width=" + obj.width
					+ ", height=" + obj.height + ", gridX=" + startGridX + "-" + endGridX + ", gridY=" + startGridY
					+ "-" + endGridY);

			for (int gridY = startGridY; gridY <= endGridY; gridY++) {
				for (int gridX = startGridX; gridX <= endGridX; gridX++) {
					String key = gridX + ":" + gridY;
					spatialGrid.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>()).add(obj);
				}
			}
		}
	}
}
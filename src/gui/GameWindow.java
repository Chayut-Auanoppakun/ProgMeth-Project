package gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import logic.ClientLogic;
import logic.GameLogic;
import logic.PlayerLogic;
import logic.ServerLogic;
import logic.SoundLogic;
import logic.TaskLogic;

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
import gameObjects.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import application.Main;

import org.w3c.dom.Element;

public class GameWindow {
	private Stage gameStage;
	private Canvas canvas;
	private GraphicsContext gc;
	private AnimationTimer timer;

	// TMX Map variables
	private static final String MAP_FILE = "assets/map.tmx";
	private static final int MAX_CACHE_SIZE = 2000; // Increased for complex maps
	private ConcurrentHashMap<String, WritableImage> tileImageCache = new ConcurrentHashMap<>();

	private java.util.concurrent.ConcurrentLinkedDeque<String> tileCacheOrder = new java.util.concurrent.ConcurrentLinkedDeque<>();
	private ExecutorService threadPool;
	private Map map;
	private Group root;
	private MapRenderer renderer;
	private long lastUpdate = 0; // Track the last update time
	private double playerX = 1010; // Starting Position
	private double playerY = 3616; // Starting Position
	private double speed = 120; // Movement speed in units per second
	// List to store collision objects
	private List<gameObjects.CollisionObject> collisionObjects = new CopyOnWriteArrayList<>();
	private List<gameObjects.eventObject> eventObjects = new CopyOnWriteArrayList<>();
	private ConcurrentHashMap<String, ImageView> playerSpriteCache = new ConcurrentHashMap<>();

	private ConcurrentHashMap<String, ConcurrentLinkedQueue<CollisionObject>> spatialGrid;
	private static boolean showCollision = false;
	private static long lastCollisionChanged = 0;
	private static long lastFpressed = 0;
	private static boolean hasGameStarted;
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
	private ImageView playerIMG;
	private Animation animation;

	private static final int FRAME_WIDTH = 48;
	private static final int FRAME_HEIGHT = 75;
	private static final int SPRITE_COLUMNS = 6;
	private static final int ANIMATION_SPEED = 150; // milliseconds per frame

	private TaskGui activeTaskGui;

	public void start(Stage stage) {
		this.gameStage = stage;

		canvas = new Canvas(screenWidth, screenHeight);
		gc = canvas.getGraphicsContext2D();
		threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		collisionObjects = new CopyOnWriteArrayList<>();
		// Initialize TMX map
		try {
			System.out.println("Attempting to read the map...");
			TMXMapReader mapReader = new TMXMapReader();
			File file = new File(MAP_FILE);
			URL url = file.toURI().toURL();
			map = mapReader.readMap(url);
			renderer = createRenderer(map);
			System.out.println("Map loaded successfully.");

			//If server gen random player char
			if (MainMenuPane.getState().equals(logic.State.SERVER)) {
				Random random = new Random();
				int newChar = 0;
				while (true) {
					 newChar = random.nextInt(9);
					boolean dup = false;
					for (String key : GameLogic.playerList.keySet())
					{
						PlayerInfo info = GameLogic.playerList.get(key);
						 if (info.getCharacterID() == newChar) {
							 dup = true;
						 }
					}
					if (!dup) {
						break;
					}
				}
				System.out.println("CHAR = " + newChar);
				PlayerLogic.setCharID(newChar);
			}
			loadPlayerimg();
			loadCollisionObjects();
			loadEventObjects();
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
				keylogger();
				updateMovement(now);
				update();
				render();
				displayFPS();
				if (PlayerLogic.getMoving()) {
					animation.play();
				} else {
					animation.stop();
				}
				for (String key : GameLogic.playerList.keySet()) {
					try {
						SoundLogic.checkAndPlayWalkingSounds(GameLogic.playerList.get(key));
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		timer.start();

		root = new Group();
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
			Platform.runLater(() -> ServerSelectGui.settoGamedisable(false));
		});
	}

	private void loadPlayerimg() {
		// Load the sprite sheet
		String playerPath[] = { "/player/01.png", "/player/02.png", "/player/03.png", "/player/04.png",
				"/player/05.png", "/player/06.png", "/player/07.png", "/player/08.png", "/player/09.png",
				"/player/10.png" };
		Image spriteSheet = new Image(getClass().getResourceAsStream(playerPath[PlayerLogic.getCharID()]));
		if (spriteSheet.isError()) {
			System.err.println("Error loading player sprite sheet: " + spriteSheet.getException().getMessage());
			return;
		}

		// Initialize the ImageView for the player
		playerIMG = new ImageView(spriteSheet);
		playerIMG.setViewport(new Rectangle2D(0, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT));
		playerIMG.setX(PlayerLogic.getMyPosX() - viewportX);
		playerIMG.setY(PlayerLogic.getMyPosY() - viewportY);

		// Set up the animation
		animation = new Transition() {
			{
				setCycleDuration(Duration.millis(SPRITE_COLUMNS * ANIMATION_SPEED));
				setInterpolator(Interpolator.LINEAR);
				setCycleCount(INDEFINITE);
			}

			@Override
			protected void interpolate(double frac) {
				int index = (int) (frac * SPRITE_COLUMNS);
				if (PlayerLogic.getDirection() == 1) {
					playerIMG.setViewport(
							new Rectangle2D(index * FRAME_WIDTH, 1 * FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT));
				} else if (PlayerLogic.getDirection() == 2) {
					playerIMG.setViewport(
							new Rectangle2D(index * FRAME_WIDTH, 0 * FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT));
				}
			}
		};

		// Start the animation if the player is moving
		if (PlayerLogic.getMoving()) {
			animation.play();
		} else {
			animation.stop();
			setPlayerIdleFrame();
		}
	}

	private void setPlayerIdleFrame() {
		if (PlayerLogic.getDirection() == 1) {
			playerIMG.setViewport(new Rectangle2D(5 * FRAME_WIDTH, 1 * FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT));
		} else {
			playerIMG.setViewport(new Rectangle2D(5 * FRAME_WIDTH, 0 * FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT));
		}

	}

	private void loadCollisionObjects() {
		collisionObjects.clear();
		final List<gameObjects.CollisionObject> tempCollisionObjects = Collections.synchronizedList(new ArrayList<>());

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

	private void loadEventObjects() {
		eventObjects.clear();
		final List<gameObjects.eventObject> tempEventObjects = Collections.synchronizedList(new ArrayList<>());

		for (MapLayer layer : map.getLayers()) {
			if (layer instanceof ObjectGroup) {
				ObjectGroup objectGroup = (ObjectGroup) layer;

				if (objectGroup.getName().toLowerCase().contains("eventtrigger")
						|| "true".equals(objectGroup.getProperties().getProperty("eventtrigger"))) {

					for (org.mapeditor.core.MapObject object : objectGroup.getObjects()) {
						double x = object.getX();
						double y = object.getY();
						double width = object.getWidth();
						double height = object.getHeight();
						String id = object.getProperties().getProperty("Event_ID");
						System.out.println("Event Object: x=" + x + ", y=" + y + ", width=" + width + ", height="
								+ height + " Event_ID = " + id);
						tempEventObjects.add(new eventObject(x, y, width, height, id));
						// System.out.println("Object ID: " + id + ", " + propertyName + ": " +
						// propertyValue);
					}
				}
			}
		}
		eventObjects.addAll(tempEventObjects);
		System.out.println("Loaded " + collisionObjects.size() + " event objects");

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
		double playerX = PlayerLogic.getMyPosX();
		double playerY = PlayerLogic.getMyPosY();

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

		// Debug: Draw collision objects
		if (showCollision) {
			renderCollisionObjects();
		}
		gc.restore();
	}

	private void renderCollisionObjects() {
//         Uncomment this section for debugging collision objects
//        /*
		gc.setStroke(Color.RED);
		gc.setGlobalAlpha(0.5);
		for (CollisionObject obj : collisionObjects) {
			gc.strokeRect(obj.getX() - viewportX, obj.getY() - viewportY, obj.getWidth(), obj.getHeight());
		}
		gc.setGlobalAlpha(1.0);
//        */
	}

	private void renderPlayers() {
		String OurKey = PlayerLogic.getLocalAddressPort();
		// Render other players

		for (String key : GameLogic.playerList.keySet()) {
			if (!key.equals(OurKey)) {
				PlayerInfo playerInfo = GameLogic.playerList.get(key);
				renderOtherPlayer(playerInfo);
			}
		}

		// Render the local player
		double localPlayerScreenX = PlayerLogic.getMyPosX() - viewportX;
		double localPlayerScreenY = PlayerLogic.getMyPosY() - viewportY;

		// Draw the local player using the playerIMG sprite
		if (playerIMG != null) {
			// Configure snapshot parameters to support transparency
			SnapshotParameters params = new SnapshotParameters();
			params.setFill(Color.TRANSPARENT); // Set the background to transparent

			// Take a snapshot of the playerIMG with transparency
			Image playerImage = playerIMG.snapshot(params, null);

			// Draw the player image
			gc.drawImage(playerImage, localPlayerScreenX - FRAME_WIDTH / 2, localPlayerScreenY - FRAME_HEIGHT / 2 - 5);
		} else {
			// Fallback: Draw a circle if the sprite is not loaded
			gc.setFill(Color.RED);
			gc.fillOval(localPlayerScreenX - PLAYER_RADIUS, localPlayerScreenY - PLAYER_RADIUS, PLAYER_RADIUS * 2,
					PLAYER_RADIUS * 2);
		}

		// Draw collision bounding box for the local player
		gc.setStroke(Color.YELLOW);
		double collisionBoxX = localPlayerScreenX - 24; // Half of 48 (width)
		double collisionBoxY = localPlayerScreenY - 32; // Half of 64 (height)
		gc.strokeRect(collisionBoxX, collisionBoxY, 48, 64);

		// Draw the bottom 20-pixel collision area for the local player
		gc.setStroke(Color.CYAN);
		double collisionAreaY = localPlayerScreenY + 12; // Bottom 20 pixels (64 - 20 = 44, 44 / 2 = 22, 32 - 22 = 10)
		gc.strokeRect(collisionBoxX, collisionAreaY, 48, 20);

	}

	private void renderOtherPlayer(PlayerInfo playerInfo) {
		final int FOV_RADIUS = 1000; // Adjust this value as needed
		double distX = playerInfo.getX() - PlayerLogic.getMyPosX();
		double distY = playerInfo.getY() - PlayerLogic.getMyPosY();
		double distance = Math.sqrt(distX * distX + distY * distY);

		if (distance <= FOV_RADIUS) {
			double playerScreenX = playerInfo.getX() - viewportX;
			double playerScreenY = playerInfo.getY() - viewportY;

			// Get or create the player's ImageView
			ImageView otherPlayerIMG = playerSpriteCache.computeIfAbsent(playerInfo.toString(), id -> {
				ImageView imgView = new ImageView(new Image("/player/01.png")); // Replace with player-specific sprite
																				// if needed
				imgView.setViewport(new Rectangle2D(0, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT));
				return imgView;
			});

			// Set the sprite direction based on the player's last direction
			int direction = playerInfo.isMoving() ? playerInfo.getDirection() : playerInfo.getDirection();
			int frameIndex = playerInfo.isMoving()
					? (int) ((System.currentTimeMillis() / ANIMATION_SPEED) % SPRITE_COLUMNS)
					: 0; // Stop animation when not moving

			if (direction == 1) { // Left
				otherPlayerIMG.setViewport(
						new Rectangle2D(frameIndex * FRAME_WIDTH, 1 * FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT));
			} else if (direction == 2) { // Right
				otherPlayerIMG.setViewport(
						new Rectangle2D(frameIndex * FRAME_WIDTH, 0 * FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT));
			}

			// Configure snapshot parameters to support transparency
			SnapshotParameters params = new SnapshotParameters();
			params.setFill(Color.TRANSPARENT); // Set the background to transparent

			// Take a snapshot of the playerIMG with transparency
			Image playerImage = otherPlayerIMG.snapshot(params, null);

			// Draw the player image
			gc.drawImage(playerImage, playerScreenX - FRAME_WIDTH / 2, playerScreenY - FRAME_HEIGHT / 2 - 5);
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
		int Direction = PlayerLogic.getDirection();
		boolean moved = false;
		if (pressedKeys.contains(KeyCode.W)) {
			dy -= speed * deltaTime; // Move up
		}
		if (pressedKeys.contains(KeyCode.S)) {
			dy += speed * deltaTime; // Move down
		}
		if (pressedKeys.contains(KeyCode.A)) {
			dx -= speed * deltaTime; // Move left
			Direction = 1;

		}
		if (pressedKeys.contains(KeyCode.D)) {
			dx += speed * deltaTime; // Move right
			Direction = 2;
		}

		// Move horizontally
		if (dx != 0) {
			if (!checkCollision(playerX + dx, playerY)) {
				playerX += dx;
				moved = true;
			}
		}

		// Move vertically
		if (dy != 0) {
			if (!checkCollision(playerX, playerY + dy)) {
				playerY += dy;
				moved = true;
			}
		}
//		playerY += dy;
//		playerX += dx;
//		moved = true;
		// Send the updated position to the server or client
		PlayerLogic.isMoving(moved, Direction);
		sendPositionUpdate(playerX, playerY);

		if (moved) {
			if (animation.getStatus() != Animation.Status.RUNNING) {
				animation.play();
			}
		} else {
			if (animation.getStatus() == Animation.Status.RUNNING) {
				animation.stop();
				setPlayerIdleFrame();
			}
		}
	}

	private void keylogger() {
		if (pressedKeys.contains(KeyCode.C)) {
			if (System.currentTimeMillis() - lastCollisionChanged > 250) {
				lastCollisionChanged = System.currentTimeMillis();
				if (!showCollision)
					showCollision = true;
				else
					showCollision = false;
			}
		}
		if (pressedKeys.contains(KeyCode.F)) {
			if (System.currentTimeMillis() - lastFpressed > 250) {
				lastFpressed = System.currentTimeMillis();
				System.out.println("F pressed");
				System.out.println(TaskLogic.isPlayerCollidingWithEvent(eventObjects));
			}
		}

		if (pressedKeys.contains(KeyCode.V)) {
			if (System.currentTimeMillis() - lastFpressed > 250) {
				lastFpressed = System.currentTimeMillis();
				System.out.println("V pressed");
				MatchSetupPane setup = new MatchSetupPane(this::onCharacterSelected);

				root.getChildren().add(setup);
			}
		}

	}

	public void onCharacterSelected() {
		System.out.println("Character selected! Updating game state...");
		loadPlayerimg();
	}

	private void sendPositionUpdate(double x, double y) {
		if (ServerSelectGui.getState().equals(logic.State.SERVER)) { // Server
			PlayerLogic.setPosition(Math.floor(x), Math.floor(y));
		} else if (ServerSelectGui.getState().equals(logic.State.CLIENT)) { // Client
			PlayerLogic.setPosition(Math.floor(x), Math.floor(y));
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
						double objLeft = obj.getX();
						double objRight = obj.getX() + obj.getWidth();
						double objTop = obj.getY();
						double objBottom = obj.getY() + obj.getHeight();

						// Check for collision with the bottom 20 pixels of the player
						if (playerRight > objLeft && playerLeft < objRight && playerBottom > objTop
								&& playerFeetTop < objBottom) {
							System.out.println("Collision with object at (" + obj.getX() + ", " + obj.getY() + ")");
							return true; // Collision detected
						}
					}
				}
			}
		}

		return false; // No collision
	}

	private void drawMap(GraphicsContext gc, double viewportX, double viewportY, double viewportWidth,
			double viewportHeight) {
		gc.setFill(Color.BLACK);
		gc.fillRect(0, 0, viewportWidth, viewportHeight);

// Render layers that should appear below the player
		for (MapLayer layer : map.getLayers()) {
// Skip layers that should appear above the player
			if (layer.getName().equalsIgnoreCase("Foreground") || layer.getName().equalsIgnoreCase("SuperForeground")) {
				continue; // Skip these layers for now
			}

			if (layer instanceof TileLayer) {
				drawTileLayer(gc, (TileLayer) layer, viewportX, viewportY, viewportWidth, viewportHeight);
			} else if (layer instanceof ObjectGroup) {
				drawObjectGroup(gc, (ObjectGroup) layer, viewportX, viewportY);
			}
		}

// Render the player
		renderPlayers();

// Render layers that should appear above the player
		for (MapLayer layer : map.getLayers()) {
			// Only render layers that should appear above the player
			if (layer.getName().equalsIgnoreCase("Foreground") || layer.getName().equalsIgnoreCase("SuperForeground")) {

				if (layer instanceof TileLayer) {
					drawTileLayer(gc, (TileLayer) layer, viewportX, viewportY, screenWidth, screenHeight);
				} else if (layer instanceof ObjectGroup) {
					drawObjectGroup(gc, (ObjectGroup) layer, viewportX, viewportY);
				}
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
		int tileWidth = map.getTileWidth();
		int tileHeight = map.getTileHeight();

		try {
// Save the current state
			gc.save();
			gc.translate(tileX - viewportX, tileY - viewportY);

			if (flipDiagonally) {
				// Option 1: Rotate 90° counterclockwise (or -90°) and flip vertically
				gc.rotate(-90);
				gc.translate(-tileHeight, 0); // Move back into view after rotation

				// Swap width and height
				int temp = tileWidth;
				tileWidth = tileHeight;
				tileHeight = temp;

				// Diagonal flip changes the meaning of horizontal and vertical flips
				boolean tempFlip = flipHorizontally;
				flipHorizontally = !flipVertically; // Notice the negation
				flipVertically = tempFlip;
			}

// Apply horizontal flip
			if (flipHorizontally) {
				gc.translate(tileWidth, 0);
				gc.scale(-1, 1);
			}

// Apply vertical flip
			if (flipVertically) {
				gc.translate(0, tileHeight);
				gc.scale(1, -1);
			}

// Draw the tile
			gc.drawImage(writableImage, 0, 0, map.getTileWidth(), map.getTileHeight());

// Restore the state
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
			int startGridX = (int) (obj.getX() / GRID_CELL_SIZE);
			int startGridY = (int) (obj.getY() / GRID_CELL_SIZE);
			int endGridX = (int) ((obj.getX() + obj.getWidth()) / GRID_CELL_SIZE);
			int endGridY = (int) ((obj.getY() + obj.getHeight()) / GRID_CELL_SIZE);

			// Debug: Print grid cells for each collision object
			System.out.println("Collision Object Grid Cells: x=" + obj.getX() + ", y=" + obj.getY() + ", width="
					+ obj.getWidth() + ", height=" + obj.getHeight() + ", gridX=" + startGridX + "-" + endGridX
					+ ", gridY=" + startGridY + "-" + endGridY);

			for (int gridY = startGridY; gridY <= endGridY; gridY++) {
				for (int gridX = startGridX; gridX <= endGridX; gridX++) {
					String key = gridX + ":" + gridY;
					spatialGrid.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>()).add(obj);
				}
			}
		}
	}
}
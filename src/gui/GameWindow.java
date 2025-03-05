package gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.Transition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import logic.*;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import gameObjects.*;

public class GameWindow {
	// === JavaFX Components ===
	private Stage gameStage;
	private Group root;
	private Pane taskContainer;
	private Canvas canvas;
	private GraphicsContext gc;
	private PrepGui prepPhaseGui;
	private static GameWindow gameWindowInstance;

	// === Game State ===
	private static boolean showCollision = false;
	private static long lastCollisionChanged = 0;
	private static long lastFpressed = 0;

	// === Player Properties ===
	private static double playerX = 980; // Starting Position
	private static double playerY = 3616; // Starting Position
	private static double speed = 120; // Movement speed in units per second
	private ImageView playerIMG;
	private Animation animation;

	// === Player Rendering and Animation ===
	private static final int FRAME_WIDTH = 48;
	private static final int FRAME_HEIGHT = 75;
	private static final int SPRITE_COLUMNS = 6;
	private static final int ANIMATION_SPEED = 150; // milliseconds per frame

	// === Camera & Viewport ===
	private static double viewportX = 1010;
	private static double viewportY = 3616;
	private static final double screenWidth = 1080 * 1.2;
	private static final double screenHeight = 720 * 1.2;
	private static final double CAMERA_ZOOM = 1.8 * 1.2; // 15% of screen (much closer than before)

	// === Map & Rendering ===
	private static final String MAP_FILE = "assets/map.tmx";
	private Map map;
	private MapRenderer renderer;
	private long lastUpdate = 0; // Track the last update time
	private static int PossibleSpawnsX[] = { 1681, 1711, 1742, 1774, 1774, 1809, 1809, 1840, 1840, 1840, 1840, 1807,
			1807, 1777, 1777, 1744, 1711, 1679, 1679, 1646, 1646, 1615, 1615, 1615, 1615, 1646, 1646, 1679 };
	private static int PossibleSpawnsY[] = { 1491, 1491, 1491, 1491, 1526, 1526, 1556, 1556, 1589, 1618, 1651, 1651,
			1687, // 0-27
			1686, 1714, 1714, 1714, 1714, 1684, 1684, 1652, 1652, 1621, 1590, 1557, 1557, 1526, 1526 };

	// === Collision & Events ===
	private List<gameObjects.CollisionObject> collisionObjects = new CopyOnWriteArrayList<>();
	private List<gameObjects.eventObject> eventObjects = new CopyOnWriteArrayList<>();

	// === Spatial Grid System ===
	private int GRID_CELL_SIZE = 128; // Size of each grid cell, adjust based on game scale
	private ConcurrentHashMap<String, ConcurrentLinkedQueue<CollisionObject>> spatialGrid;

	// === Tile Caching ===
	private static final int MAX_CACHE_SIZE = 2000; // Increased for complex maps
	private ConcurrentHashMap<String, WritableImage> tileImageCache = new ConcurrentHashMap<>();
	private ConcurrentLinkedDeque<String> tileCacheOrder = new ConcurrentLinkedDeque<>();

	// === Player Sprite Caching ===
	private ConcurrentHashMap<String, ImageView> playerSpriteCache = new ConcurrentHashMap<>();
	private HashMap<String, Integer> cachedCharacterIDs = new HashMap<>();

	// === Movement & Input ===
	private Set<KeyCode> pressedKeys = new HashSet<>();

	// === FPS Tracking ===
	private int frames = 0;
	private long fpsUpdateTime = System.nanoTime();
	private int fps = 0;

	// === Threading ===
	private ExecutorService threadPool;

	// === Animation Timer ===
	private AnimationTimer timer;

	// === Player Size ===
	private final double PLAYER_RADIUS = 10;

	// === Buttons and ui ===
	private Button characterSelectButton;
	private CharaterSelectgui characterSelectGui;
	private boolean characterSelectVisible = false;
	private boolean PrepEnd = false;

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

			// Gen char for own player
			Random random = new Random();
			int newChar = 0;
			while (true) {
				newChar = random.nextInt(9);
				boolean dup = false;
				for (String key : GameLogic.playerList.keySet()) {
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

				if (GameLogic.isPrepEnded() != PrepEnd) { // RUN AFTER PREP END ONCE
					System.out.println("HIDE PREP");
					PrepEnd = GameLogic.isPrepEnded();
					updateUIForPrepPhase();
					GameLogic.autoImposterCount(); // For now, automatically set imposter count to be 1/4 of player size
					if (MainMenuPane.getState().equals(logic.State.SERVER)) {
						ServerLogic.randomizeImposters();
					}
					triggerGameStartTransition(); // this auto teleports us
				}

				if (!GameLogic.isPrepEnded() && prepPhaseGui != null) {
					updatePrepPhasePlayerCount();
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
		setupCharacterSelectButton();
		initializePrepPhaseUI();
		taskContainer = new Pane();
		root.getChildren().add(taskContainer);
		gameWindowInstance = this;

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

	private void setupCharacterSelectButton() {
		characterSelectButton = new Button("Select Character");
		characterSelectButton.setStyle("-fx-background-color: #1e90ff; " + // Bright blue to match the UI theme
				"-fx-text-fill: white; " + // White text for contrast
				"-fx-font-size: 14px; " + // Clear readable font size
				"-fx-padding: 5 15 5 15; " + // Comfortable padding
				"-fx-border-color: #87cefa; " + // Light blue border
				"-fx-border-width: 2px; " + // Visible border width
				"-fx-background-radius: 0; " + // Sharp corners for pixel art style
				"-fx-border-radius: 0; " + // Sharp corners for border
				"-fx-font-family: 'Monospace'; " + // Pixelated/game-like font
				"-fx-font-weight: bold; " + // Bold text
				"-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);" // Subtle shadow
		);

		// To make it look more interactive, you could also add hover effects:
		characterSelectButton.setOnMouseEntered(e -> characterSelectButton.setStyle("-fx-background-color: #00bfff; " + // Brighter
																														// blue
																														// on
																														// hover
				"-fx-text-fill: white; " + "-fx-font-size: 14px; " + "-fx-padding: 5 15 5 15; "
				+ "-fx-border-color: #b0e2ff; " + // Lighter border on hover
				"-fx-border-width: 2px; " + "-fx-background-radius: 0; " + "-fx-border-radius: 0; "
				+ "-fx-font-family: 'Monospace'; " + "-fx-font-weight: bold; "
				+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 8, 0, 0, 1);" // Enhanced shadow
		));

		characterSelectButton.setOnMouseExited(e -> characterSelectButton.setStyle("-fx-background-color: #1e90ff; "
				+ "-fx-text-fill: white; " + "-fx-font-size: 14px; " + "-fx-padding: 5 15 5 15; "
				+ "-fx-border-color: #87cefa; " + "-fx-border-width: 2px; " + "-fx-background-radius: 0; "
				+ "-fx-border-radius: 0; " + "-fx-font-family: 'Monospace'; " + "-fx-font-weight: bold; "
				+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);"));
		characterSelectButton.setLayoutX(10);
		characterSelectButton.setLayoutY(28);

		characterSelectButton.setOnAction(e -> {
			showCharacterSelectGui();
		});

		root.getChildren().add(characterSelectButton);
	}

	private void showCharacterSelectGui() {
		// Hide the button first
		characterSelectButton.setVisible(false);

		if (characterSelectGui == null) {
			try {
				characterSelectGui = new CharaterSelectgui(this::onCharacterSelected);

				// Set the initial position completely off-screen
				// Don't use translateX here - we'll animate it later
				characterSelectGui.setLayoutX(-300);
				characterSelectGui.setLayoutY(50);

				characterSelectGui.setVisible(true);

				// Add a close button to the character selection GUI
				Button closeButton = new Button("X");
				closeButton.setStyle("-fx-background-color: #cc0000;" + "-fx-text-fill: white;"
						+ "-fx-font-family: 'Monospace';" + "-fx-font-size: 12px;" + "-fx-font-weight: bold;"
						+ "-fx-padding: 2 8 2 8;" + "-fx-border-color: #ff6347;" + "-fx-border-width: 2px;"
						+ "-fx-background-radius: 0;" + "-fx-border-radius: 0;"
						+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 3, 0, 0, 1);");

				// Add hover effect for close button
				closeButton.setOnMouseEntered(e -> closeButton.setStyle("-fx-background-color: #ff0000;"
						+ "-fx-text-fill: white;" + "-fx-font-family: 'Monospace';" + "-fx-font-size: 12px;"
						+ "-fx-font-weight: bold;" + "-fx-padding: 2 8 2 8;" + "-fx-border-color: #ff6347;"
						+ "-fx-border-width: 2px;" + "-fx-background-radius: 0;" + "-fx-border-radius: 0;"
						+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 5, 0, 0, 1);"));

				closeButton.setOnMouseExited(e -> closeButton.setStyle("-fx-background-color: #cc0000;"
						+ "-fx-text-fill: white;" + "-fx-font-family: 'Monospace';" + "-fx-font-size: 12px;"
						+ "-fx-font-weight: bold;" + "-fx-padding: 2 8 2 8;" + "-fx-border-color: #ff6347;"
						+ "-fx-border-width: 2px;" + "-fx-background-radius: 0;" + "-fx-border-radius: 0;"
						+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 3, 0, 0, 1);"));
				closeButton.setOnAction(e -> hideCharacterSelectGui());

				closeButton.setLayoutX(260);
				closeButton.setLayoutY(10);
				characterSelectGui.getChildren().add(closeButton);

				root.getChildren().add(characterSelectGui);

				characterSelectGui.toFront();

			} catch (Exception e) {
				System.err.println("Error creating character select GUI:");
				e.printStackTrace();
				return;
			}
		} else {
			// If the GUI already exists, just make sure it's at the starting position
			characterSelectGui.setLayoutX(-300);
			characterSelectGui.setVisible(true);
			characterSelectGui.toFront();
		}

		try {
			TranslateTransition slideIn = new TranslateTransition(Duration.millis(350), characterSelectGui);
			slideIn.setFromX(0); // Start at current position (which is -300 due to layoutX)
			slideIn.setToX(300); // Move 300 pixels to the right
			slideIn.setOnFinished(event -> {
			});
			slideIn.play();

			characterSelectVisible = true;
		} catch (Exception e) {
			System.err.println("Error in slide animation:");
			e.printStackTrace();
		}
	}

	private void hideCharacterSelectGui() {
		if (characterSelectGui != null && characterSelectGui.isVisible()) {
			// Create and play the slide-out animation
			TranslateTransition slideOut = new TranslateTransition(Duration.millis(350), characterSelectGui);
			slideOut.setFromX(300); // Start at current position (after sliding in)
			slideOut.setToX(0); // Move back to initial position
			slideOut.setOnFinished(e -> {
				// Reset translateX and make the panel invisible
				characterSelectGui.setTranslateX(0);
				characterSelectGui.setVisible(false);
				characterSelectButton.setVisible(true);
			});
			slideOut.play();

			characterSelectVisible = false;
		} else {
			// If for some reason the GUI isn't visible, just show the button
			characterSelectButton.setVisible(true);
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
		// Create a list to store all players (including local player) for sorting by
		// y-position
		List<PlayerRenderInfo> playersToRender = new ArrayList<>();

		// Add the local player to the list
		playersToRender.add(new PlayerRenderInfo("local", PlayerLogic.getMyPosX(), PlayerLogic.getMyPosY(), null, // For
																													// local
																													// player,
																													// we'll
																													// use
																													// playerIMG
																													// directly
				PlayerLogic.getCharID()));

		// Add other players to the list
		for (String key : GameLogic.playerList.keySet()) {
			PlayerInfo playerInfo = GameLogic.playerList.get(key);

			// Check if player is within FOV radius before adding to render list
			final int FOV_RADIUS = 1000; // Adjust as needed
			double distX = playerInfo.getX() - PlayerLogic.getMyPosX();
			double distY = playerInfo.getY() - PlayerLogic.getMyPosY();
			double distance = Math.sqrt(distX * distX + distY * distY);

			if (distance <= FOV_RADIUS) {
				playersToRender.add(new PlayerRenderInfo(key, playerInfo.getX(), playerInfo.getY(), playerInfo,
						playerInfo.getCharacterID()));
			}
		}

		// Sort players by Y position (higher Y values render on top/later)
		Collections.sort(playersToRender, Comparator.comparingDouble(PlayerRenderInfo::getY));

		// Render all players in sorted order
		for (PlayerRenderInfo player : playersToRender) {
			if (player.getKey().equals("local")) {
				renderLocalPlayer(player.getX(), player.getY());
			} else {
				renderOtherPlayer(player.getInfo());
			}
		}
	}

	// Helper class to store player information for rendering
	private class PlayerRenderInfo {
		private String key;
		private double x;
		private double y;
		private PlayerInfo info;
		private int charID;

		public PlayerRenderInfo(String key, double x, double y, PlayerInfo info, int charID) {
			this.key = key;
			this.x = x;
			this.y = y;
			this.info = info;
			this.charID = charID;
		}

		public String getKey() {
			return key;
		}

		public double getX() {
			return x;
		}

		public double getY() {
			return y;
		}

		public PlayerInfo getInfo() {
			return info;
		}

		public int getCharID() {
			return charID;
		}
	}

	private void renderLocalPlayer(double x, double y) {
		double localPlayerScreenX = x - viewportX;
		double localPlayerScreenY = y - viewportY;

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

		if (showCollision) {
			// Draw collision bounding box for the local player
			gc.setStroke(Color.YELLOW);
			double collisionBoxX = localPlayerScreenX - 24; // Half of 48 (width)
			double collisionBoxY = localPlayerScreenY - 32; // Half of 64 (height)
			gc.strokeRect(collisionBoxX, collisionBoxY, 48, 64);

			// Draw the bottom 20-pixel collision area for the local player
			gc.setStroke(Color.CYAN);
			double collisionAreaY = localPlayerScreenY + 12; // Bottom 20 pixels (64 - 20 = 44, 44 / 2 = 22, 32 - 22 =
																// 10)
			gc.strokeRect(collisionBoxX, collisionAreaY, 48, 20);
		}
	}

	private void renderOtherPlayer(PlayerInfo playerInfo) {
		final int FOV_RADIUS = 1000; // Adjust this value as needed
		double distX = playerInfo.getX() - PlayerLogic.getMyPosX();
		double distY = playerInfo.getY() - PlayerLogic.getMyPosY();
		double distance = Math.sqrt(distX * distX + distY * distY);

		if (distance <= FOV_RADIUS) {
			double playerScreenX = playerInfo.getX() - viewportX;
			double playerScreenY = playerInfo.getY() - viewportY;

			String playerID = playerInfo.toString(); // Unique key per player

			// Check if character ID has changed
			int char_id = playerInfo.getCharacterID();
			if (char_id == 99)
				char_id = 0; // Default to 0 if not set

			if (!playerSpriteCache.containsKey(playerID)
					|| playerInfo.getCharacterID() != cachedCharacterIDs.getOrDefault(playerID, -1)) {
				String[] CHARACTER_IMAGES = { "/player/01.png", "/player/02.png", "/player/03.png", "/player/04.png",
						"/player/05.png", "/player/06.png", "/player/07.png", "/player/08.png", "/player/09.png",
						"/player/10.png" };

				ImageView imgView = new ImageView(new Image(getClass().getResourceAsStream(CHARACTER_IMAGES[char_id])));
				// Explicitly initialize with frame 5 (idle frame)
				imgView.setViewport(new Rectangle2D(5 * FRAME_WIDTH, 0, FRAME_WIDTH, FRAME_HEIGHT));

				playerSpriteCache.put(playerID, imgView);
				cachedCharacterIDs.put(playerID, char_id); // Store the latest character ID
			}

			ImageView otherPlayerIMG = playerSpriteCache.get(playerID);

			// Set the frame based on movement state
			int direction = playerInfo.getDirection();
			int frameIndex;

			// Always use frame 5 (idle frame) when the player is not moving
			if (playerInfo.isMoving()) {
				frameIndex = (int) ((System.currentTimeMillis() / ANIMATION_SPEED) % SPRITE_COLUMNS);
			} else {
				frameIndex = 5; // Always use frame 5 for stationary players
			}

			// Update viewport based on direction and frame
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
		// moved = true;
		// Send the updated position to the server or client
		PlayerLogic.isMoving(moved, Direction);
		PlayerLogic.setPosition(playerX, playerY);

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
				System.out.println("F pressed - checking for interactive objects");

				// Check for event collisions
				String eventId = TaskLogic.isPlayerCollidingWithEvent(eventObjects);

				// If we're colliding with an event object
				if (!eventId.isEmpty()) {
					System.out.println("Interacting with event: " + eventId);

					// Attempt to open the task
					boolean taskOpened = TaskLogic.openTask(eventId, taskContainer);

					if (taskOpened) {
						System.out.println("Task opened: " + eventId);
						// You might want to disable player movement here or add other effects
						// PlayerLogic.setMovementEnabled(false);
					} else {
						System.out.println("Failed to open task: " + eventId);
						// Maybe show a notification that this task is already completed
						if (TaskLogic.isTaskCompleted(eventId)) {
							showTaskCompletedMessage();
						}
					}
				} else {
					System.out.println("No interactive object nearby");
					// Maybe show a hint message that there's nothing to interact with
				}
			}
		}
	}

	private void showTaskCompletedMessage() {
		// Create and display a brief overlay message
		Text message = new Text("Task Already Completed");
		message.setFont(Font.font("Arial", FontWeight.BOLD, 20));
		message.setFill(Color.WHITE);
		message.setStroke(Color.BLACK);
		message.setStrokeWidth(1);

		StackPane messagePane = new StackPane(message);
		messagePane
				.setStyle("-fx-background-color: rgba(0, 100, 0, 0.7); -fx-padding: 10px; -fx-background-radius: 5px;");
		messagePane.setLayoutX((screenWidth - 250) / 2);
		messagePane.setLayoutY(screenHeight * 0.7);
		messagePane.setOpacity(0);

		root.getChildren().add(messagePane);

		// Fade in
		FadeTransition fadeIn = new FadeTransition(Duration.millis(200), messagePane);
		fadeIn.setFromValue(0);
		fadeIn.setToValue(1);
		fadeIn.play();

		// Hold then fade out
		new Thread(() -> {
			try {
				Thread.sleep(1500);
				Platform.runLater(() -> {
					FadeTransition fadeOut = new FadeTransition(Duration.millis(200), messagePane);
					fadeOut.setFromValue(1);
					fadeOut.setToValue(0);
					fadeOut.setOnFinished(e -> root.getChildren().remove(messagePane));
					fadeOut.play();
				});
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}

	public void onCharacterSelected() {
		System.out.println("Character selected! Updating game state...");
		loadPlayerimg();
	}

	// Maintain backwards compatibility with original method
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
							// System.out.println("Collision with object at (" + obj.getX() + ", " +
							// obj.getY() + ")");
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
	// Reverted to the original optimization with fixes for missing tiles
	private void drawTileLayer(GraphicsContext gc, TileLayer layer, double viewportX, double viewportY,
			double viewportWidth, double viewportHeight) {
		// Only render visible layers
		if (!layer.isVisible()) {
			return;
		}

		// Calculate the visible tile range with sufficient buffer
		int tileWidth = map.getTileWidth();
		int tileHeight = map.getTileHeight();
		int startX = Math.max(0, (int) (viewportX / tileWidth) - 2);
		int startY = Math.max(0, (int) (viewportY / tileHeight) - 2);
		int endX = Math.min(layer.getWidth(), (int) ((viewportX + viewportWidth) / tileWidth) + 3);
		int endY = Math.min(layer.getHeight(), (int) ((viewportY + viewportHeight) / tileHeight) + 3);

		// IMPORTANT: Use the original logic for drawing tiles
		// This preserves the exact behavior of your implementation
		for (int y = startY; y < endY; y++) {
			for (int x = startX; x < endX; x++) {
				Tile tile = layer.getTileAt(x, y);
				if (tile == null)
					continue;

				boolean flipHorizontally = layer.isFlippedHorizontally(x, y);
				boolean flipVertically = layer.isFlippedVertically(x, y);
				boolean flipDiagonally = layer.isFlippedDiagonally(x, y);

				// Include tileset information in the cache key to handle tiles with same ID
				// from different tilesets
				int tileSetId = tile.getTileSet().hashCode(); // Get a unique identifier for the tileset
				String cacheKey = tileSetId + "_" + tile.getId() + "_" + (flipHorizontally ? "h" : "")
						+ (flipVertically ? "v" : "") + (flipDiagonally ? "d" : "");

				// Get or create the tile image
				WritableImage tileImage = getTileImage(tile, cacheKey);

				if (tileImage != null) {
					// Draw the tile with the EXACT same transformation logic as original
					int tileX = x * tileWidth;
					int tileY = y * tileHeight;

					gc.save();
					gc.translate(tileX - viewportX, tileY - viewportY);

					if (flipDiagonally) {
						gc.rotate(-90);
						gc.translate(-tileHeight, 0);

						// Correct handling of diagonal flip with other flips
						boolean tempFlip = flipHorizontally;
						flipHorizontally = !flipVertically;
						flipVertically = tempFlip;
					}

					if (flipHorizontally) {
						gc.translate(tileWidth, 0);
						gc.scale(-1, 1);
					}

					if (flipVertically) {
						gc.translate(0, tileHeight);
						gc.scale(1, -1);
					}

					gc.drawImage(tileImage, 0, 0);
					gc.restore();
				}
			}
		}
	}

	private WritableImage getTileImage(Tile tile, String cacheKey) {
		// First check if we have this tile in cache
		WritableImage tileImage = tileImageCache.get(cacheKey);

		if (tileImage == null) {
			// Not in cache, create a new image
			BufferedImage bufferedImage = (BufferedImage) tile.getImage();
			if (bufferedImage == null)
				return null;

			int width = bufferedImage.getWidth();
			int height = bufferedImage.getHeight();

			// Create a new writable image (no transformations, those happen at draw time)
			tileImage = new WritableImage(width, height);
			PixelWriter pw = tileImage.getPixelWriter();

			// Get the pixel data
			int[] pixels = new int[width * height];
			bufferedImage.getRGB(0, 0, width, height, pixels, 0, width);

			// Write the pixels directly (no transformations)
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					pw.setArgb(x, y, pixels[y * width + x]);
				}
			}

			// Add to cache
			synchronized (tileImageCache) {
				// Only add if another thread hasn't already added it
				if (!tileImageCache.containsKey(cacheKey)) {
					// Manage cache size
					if (tileImageCache.size() >= MAX_CACHE_SIZE) {
						// Remove oldest entries
						synchronized (tileCacheOrder) {
							int toRemove = 50; // Remove in batches for better performance
							for (int i = 0; i < toRemove && !tileCacheOrder.isEmpty(); i++) {
								String oldKey = tileCacheOrder.pollLast();
								if (oldKey != null) {
									tileImageCache.remove(oldKey);
								}
							}
						}
					}

					tileImageCache.put(cacheKey, tileImage);
					tileCacheOrder.addFirst(cacheKey);
				} else {
					// If another thread added it while we were creating it, use that one
					tileImage = tileImageCache.get(cacheKey);
					// Update access order
					synchronized (tileCacheOrder) {
						tileCacheOrder.remove(cacheKey);
						tileCacheOrder.addFirst(cacheKey);
					}
				}
			}
		} else {
			// Update access order for existing image
			synchronized (tileCacheOrder) {
				tileCacheOrder.remove(cacheKey);
				tileCacheOrder.addFirst(cacheKey);
			}
		}

		return tileImage;
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

	private void initializeSpatialGrid() {
		// Adjust grid cell size based on average object size for better performance
		// Using smaller cells for denser maps, larger cells for sparse maps
		GRID_CELL_SIZE = calculateOptimalGridSize();

		spatialGrid = new ConcurrentHashMap<>();

		for (CollisionObject obj : collisionObjects) {
			int startGridX = (int) (obj.getX() / GRID_CELL_SIZE);
			int startGridY = (int) (obj.getY() / GRID_CELL_SIZE);
			int endGridX = (int) ((obj.getX() + obj.getWidth()) / GRID_CELL_SIZE);
			int endGridY = (int) ((obj.getY() + obj.getHeight()) / GRID_CELL_SIZE);

			for (int gridY = startGridY; gridY <= endGridY; gridY++) {
				for (int gridX = startGridX; gridX <= endGridX; gridX++) {
					String key = gridX + ":" + gridY;
					spatialGrid.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>()).add(obj);
				}
			}
		}

		System.out.println(
				"Spatial grid initialized with cell size: " + GRID_CELL_SIZE + ", total cells: " + spatialGrid.size());
	}

	private int calculateOptimalGridSize() {
		if (collisionObjects.isEmpty()) {
			return 128; // Default size
		}

		// Calculate average object size
		double totalWidth = 0;
		double totalHeight = 0;

		for (CollisionObject obj : collisionObjects) {
			totalWidth += obj.getWidth();
			totalHeight += obj.getHeight();
		}

		double avgWidth = totalWidth / collisionObjects.size();
		double avgHeight = totalHeight / collisionObjects.size();

		// Estimate object density
		int mapWidth = map.getWidth() * map.getTileWidth();
		int mapHeight = map.getHeight() * map.getTileHeight();
		double mapArea = mapWidth * mapHeight;
		double objArea = collisionObjects.size() * avgWidth * avgHeight;
		double density = objArea / mapArea;

		// Adjust grid size based on density
		// Higher density = smaller cells, Lower density = larger cells
		if (density > 0.4) {
			return 64; // Very dense map
		} else if (density > 0.2) {
			return 96; // Medium density
		} else {
			return 128; // Sparse map
		}
	}

	private void initializePrepPhaseUI() {
		// Initialize the prep phase UI
		prepPhaseGui = new PrepGui(ServerSelectGui.getState());

		// Position it in the center of the screen
		prepPhaseGui.setLayoutX((screenWidth - 480) / 2); // Assuming 400px width for the UI
		prepPhaseGui.setLayoutY(screenHeight * 0.85); // Assuming 150px height for the UI

		// Add it to the root but keep it hidden initially
		root.getChildren().add(prepPhaseGui);

		// Show it if we're in prep phase
		if (!GameLogic.isPrepEnded()) {
			prepPhaseGui.show();
			updatePrepPhasePlayerCount(); // Update player count
		} else {
			prepPhaseGui.hide();
		}
	}

	private void updateUIForPrepPhase() {
		Platform.runLater(() -> {
			if (!GameLogic.isPrepEnded()) {
				if (prepPhaseGui != null) {
					prepPhaseGui.show();
				}
				if (characterSelectButton != null) {
					characterSelectButton.setVisible(true);
				}
			} else {
				if (prepPhaseGui != null) {
					prepPhaseGui.hide();
				}
				if (characterSelectButton != null) {
					characterSelectButton.setVisible(false);
				}
				if (characterSelectGui != null && characterSelectVisible) {
					hideCharacterSelectGui();
				}
			}
		});
	}

	private void updatePrepPhasePlayerCount() {
		if (prepPhaseGui != null) {
			// Calculate total player count (including server)
			int playerCount = 1; // Server/local player
			if (GameLogic.playerList != null) {
				playerCount += GameLogic.playerList.size();
			}
			prepPhaseGui.updatePlayerCount(playerCount, 10); // 10 is max players
		}
	}

	public static void TeleportToStart() {
		Random random = new Random();
		int select = random.nextInt(27);
		PlayerLogic.setPosition(PossibleSpawnsX[select], PossibleSpawnsY[select]);
		playerX = PlayerLogic.getMyPosX();
		playerY = PlayerLogic.getMyPosY();
		viewportX = playerX - (screenWidth / 2) / CAMERA_ZOOM;
		viewportY = playerY - (screenHeight / 2) / CAMERA_ZOOM;
	}

	private void showGameStartTransition(boolean isGameStarting) {
		// Create a black overlay
		Rectangle blackOverlay = new Rectangle(0, 0, screenWidth, screenHeight);
		blackOverlay.setFill(Color.BLACK);
		blackOverlay.setOpacity(0); // Start transparent

		// Create the role text
		Text roleText = new Text();

		// Set text and color based on role
		boolean isImposter = "imposter".equals(PlayerLogic.getStatus());
		if (isGameStarting) {
			if (isImposter) {
				roleText.setText("IMPOSTOR");
				roleText.setFill(Color.rgb(255, 0, 0)); // Bright red text for impostor
			} else {
				roleText.setText("CREWMATE");
				roleText.setFill(Color.rgb(40, 122, 255)); // Bright blue text for crewmate
			}
		} else {
			roleText.setText("EMERGENCY MEETING!");
			roleText.setFill(Color.WHITE);
		}

		// Style text with pixelated font
		roleText.setFont(Font.font("Impact", FontWeight.BOLD, 64));
		roleText.setStyle("-fx-stroke: black; -fx-stroke-width: 2;");
		roleText.setTextAlignment(TextAlignment.CENTER);

		// Container for the role text positioned at the top
		StackPane textContainer = new StackPane(roleText);
		textContainer.setAlignment(Pos.TOP_CENTER);
		textContainer.setPadding(new Insets(50, 0, 0, 0));
		textContainer.setPrefSize(screenWidth, screenHeight);
		textContainer.setOpacity(0);

		// Create a player model display in the center using profile/frontal image
		ImageView playerSprite = null;

		try {
			// Get the appropriate profile image based on character ID
			int charImageIndex = PlayerLogic.getCharID() + 1;
			String profilePath;

			// Handle special case for character 10 which doesn't have a leading zero
			if (charImageIndex == 10) {
				profilePath = "/player/profile/10.png";
			} else {
				profilePath = "/player/profile/0" + charImageIndex + ".png";
			}

			Image profileImage = new Image(getClass().getResourceAsStream(profilePath));
			playerSprite = new ImageView(profileImage);

			// Make the profile image larger for display
			playerSprite.setFitWidth(120);
			playerSprite.setFitHeight(120);
			playerSprite.setPreserveRatio(true);

			// Add a slight bounce animation
			TranslateTransition bounce = new TranslateTransition(Duration.millis(800), playerSprite);
			bounce.setFromY(0);
			bounce.setToY(-20);
			bounce.setCycleCount(Animation.INDEFINITE);
			bounce.setAutoReverse(true);
			bounce.play();
		} catch (Exception e) {
			System.err.println("Error loading player profile image: " + e.getMessage());
		}

		// Create subtitle text describing the role
		Text subtitleText = new Text();
		if (isImposter) {
			subtitleText.setText("Sabotage and eliminate the crew");
		} else {
			subtitleText.setText("Complete tasks and find the impostor");
		}
		subtitleText.setFont(Font.font("Arial", FontWeight.BOLD, 24));
		subtitleText.setFill(Color.WHITE);
		subtitleText.setTextAlignment(TextAlignment.CENTER);

		// Create a container for the player sprite, subtitle, and other players
		HBox mainPlayerRow = new HBox(30);
		mainPlayerRow.setAlignment(Pos.CENTER);

		// Create a container for player profile and subtitle text
		VBox playerInfoBox = new VBox(15);
		playerInfoBox.setAlignment(Pos.CENTER);

		if (playerSprite != null) {
			playerInfoBox.getChildren().add(playerSprite);
		}

		playerInfoBox.getChildren().add(subtitleText);

		// Add your player to the main row
		mainPlayerRow.getChildren().add(playerInfoBox);

		// Create a separator line
		Line separator = new Line(0, 0, 0, 150);
		separator.setStroke(Color.GRAY);
		separator.setStrokeWidth(2);

		// Create a container for other player profiles
		VBox otherPlayersBox = new VBox(10);
		otherPlayersBox.setAlignment(Pos.CENTER);

		// Add title based on role
		Text otherPlayersTitle = new Text();
		if (isImposter) {
			otherPlayersTitle.setText("YOUR TEAMMATE" + (GameLogic.getImposterCount() > 1 ? "S" : ""));
			otherPlayersTitle.setFill(Color.RED);
		} else {
			otherPlayersTitle.setText("YOUR CREWMATES");
			otherPlayersTitle.setFill(Color.LIGHTBLUE);
		}
		otherPlayersTitle.setFont(Font.font("Arial", FontWeight.BOLD, 18));
		otherPlayersBox.getChildren().add(otherPlayersTitle);

		// Create a flow pane for other player profiles
		FlowPane otherPlayersPane = new FlowPane(10, 10);
		otherPlayersPane.setAlignment(Pos.CENTER);
		otherPlayersPane.setPrefWrapLength(350); // Control the width of the flow pane

		// Add appropriate players based on role
		boolean hasOtherPlayers = false;

		if (isImposter) {
			// For imposters: show all other imposters
			for (String key : GameLogic.playerList.keySet()) {
				PlayerInfo playerInfo = GameLogic.playerList.get(key);
				if ("imposter".equals(playerInfo.getStatus())) {
					VBox profileBox = createPlayerProfileBox(playerInfo);
					otherPlayersPane.getChildren().add(profileBox);
					hasOtherPlayers = true;
				}
			}

			// If there are no other imposters (solo imposter)
			if (!hasOtherPlayers) {
				Text soloText = new Text("YOU ARE THE LONE IMPOSTOR");
				soloText.setFont(Font.font("Arial", FontWeight.BOLD, 16));
				soloText.setFill(Color.RED);
				otherPlayersPane.getChildren().add(soloText);
			}
		} else {
			// For crewmates: show all players
			for (String key : GameLogic.playerList.keySet()) {
				PlayerInfo playerInfo = GameLogic.playerList.get(key);
				VBox profileBox = createPlayerProfileBox(playerInfo);
				otherPlayersPane.getChildren().add(profileBox);
				hasOtherPlayers = true;
			}

			// Add server player (host) if not already included
			try {
				String serverKey = InetAddress.getLocalHost().getHostAddress() + ":"
						+ ServerLogic.getServerSocket().getLocalPort();
				if (!PlayerLogic.getLocalAddressPort().equals(serverKey)) {
					VBox hostProfileBox = createHostProfileBox();
					otherPlayersPane.getChildren().add(hostProfileBox);
					hasOtherPlayers = true;
				}
			} catch (Exception e) {
				System.err.println("Error adding host profile: " + e.getMessage());
			}
		}

		otherPlayersBox.getChildren().add(otherPlayersPane);

		// Only add separator and other players if there are any to display
		if (hasOtherPlayers) {
			mainPlayerRow.getChildren().addAll(separator, otherPlayersBox);
		}

		// Set the main player container properties
		mainPlayerRow.setPrefSize(screenWidth, screenHeight);
		mainPlayerRow.setOpacity(0);

		// Add everything to root (order matters for layering)
		root.getChildren().addAll(blackOverlay, textContainer, mainPlayerRow);

		// Animate fade in
		FadeTransition fadeInOverlay = new FadeTransition(Duration.millis(400), blackOverlay);
		fadeInOverlay.setFromValue(0);
		fadeInOverlay.setToValue(1);

		FadeTransition fadeInText = new FadeTransition(Duration.millis(600), textContainer);
		fadeInText.setFromValue(0);
		fadeInText.setToValue(1);
		fadeInText.setDelay(Duration.millis(400));

		FadeTransition fadeInPlayers = new FadeTransition(Duration.millis(800), mainPlayerRow);
		fadeInPlayers.setFromValue(0);
		fadeInPlayers.setToValue(1);
		fadeInPlayers.setDelay(Duration.millis(800));

		// Animate fade out
		FadeTransition fadeOutPlayers = new FadeTransition(Duration.millis(400), mainPlayerRow);
		fadeOutPlayers.setFromValue(1);
		fadeOutPlayers.setToValue(0);
		fadeOutPlayers.setDelay(Duration.seconds(4.0));

		FadeTransition fadeOutText = new FadeTransition(Duration.millis(400), textContainer);
		fadeOutText.setFromValue(1);
		fadeOutText.setToValue(0);
		fadeOutText.setDelay(Duration.seconds(4.4));

		FadeTransition fadeOutOverlay = new FadeTransition(Duration.millis(400), blackOverlay);
		fadeOutOverlay.setFromValue(1);
		fadeOutOverlay.setToValue(0);
		fadeOutOverlay.setDelay(Duration.seconds(4.8));

		// Remove everything when done
		fadeOutOverlay.setOnFinished(e -> {
			root.getChildren().removeAll(blackOverlay, textContainer, mainPlayerRow);
		});

		// Play the animations
		fadeInOverlay.play();
		fadeInText.play();
		fadeInPlayers.play();
		fadeOutPlayers.play();
		fadeOutText.play();
		fadeOutOverlay.play();

		// Play appropriate sound effect
		String soundFile = "assets/sounds/Roundstart_MAIN.wav";
		SoundLogic.playSound(soundFile, 0); // Using 0 for full volume

		// Background operations: teleport the player during the black screen
		if (isGameStarting) {
			TeleportToStart();
		}
	}

	/**
	 * Creates a player profile box showing their character image and name
	 */
	private VBox createPlayerProfileBox(PlayerInfo playerInfo) {
		VBox profileBox = new VBox(5);
		profileBox.setAlignment(Pos.CENTER);

		try {
			// Calculate profile image index (1-indexed in file path)
			int charImageIndex = playerInfo.getCharacterID() + 1;
			String profilePath;

			// Handle special case for character 10 which doesn't have a leading zero
			if (charImageIndex == 10) {
				profilePath = "/player/profile/10.png";
			} else {
				profilePath = "/player/profile/0" + charImageIndex + ".png";
			}

			// Create profile image
			ImageView profileImage = new ImageView(new Image(getClass().getResourceAsStream(profilePath)));
			profileImage.setFitWidth(70);
			profileImage.setFitHeight(70);

			// Add red border for imposters if the local player is also an imposter
			if ("imposter".equals(PlayerLogic.getStatus()) && "imposter".equals(playerInfo.getStatus())) {
				Rectangle border = new Rectangle(74, 74);
				border.setFill(Color.TRANSPARENT);
				border.setStroke(Color.RED);
				border.setStrokeWidth(2);

				StackPane imageWithBorder = new StackPane(profileImage, border);
				profileBox.getChildren().add(imageWithBorder);
			} else {
				profileBox.getChildren().add(profileImage);
			}

			// Create name label
			Text nameText = new Text(playerInfo.getName());
			nameText.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
			nameText.setFill(Color.WHITE);

			profileBox.getChildren().add(nameText);
		} catch (Exception e) {
			System.err.println("Error creating profile for player " + playerInfo.getName() + ": " + e.getMessage());

			// Fallback text if image fails to load
			Text fallbackText = new Text(playerInfo.getName());
			fallbackText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
			fallbackText.setFill(Color.WHITE);
			profileBox.getChildren().add(fallbackText);
		}

		return profileBox;
	}

	/**
	 * Creates a profile box for the host player
	 */
	private VBox createHostProfileBox() {
		VBox hostProfileBox = new VBox(5);
		hostProfileBox.setAlignment(Pos.CENTER);

		try {
			// Create host profile image
			int hostCharImageIndex = PlayerLogic.getCharID() + 1;
			String hostProfilePath;
			if (hostCharImageIndex == 10) {
				hostProfilePath = "/player/profile/10.png";
			} else {
				hostProfilePath = "/player/profile/0" + hostCharImageIndex + ".png";
			}
			ImageView hostProfileImage = new ImageView(new Image(getClass().getResourceAsStream(hostProfilePath)));
			hostProfileImage.setFitWidth(70);
			hostProfileImage.setFitHeight(70);

			// Create host name label
			Text hostNameText = new Text(MainMenuPane.getServerName());
			hostNameText.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
			hostNameText.setFill(Color.WHITE);

			hostProfileBox.getChildren().addAll(hostProfileImage, hostNameText);
		} catch (Exception e) {
			System.err.println("Error creating host profile: " + e.getMessage());

			// Fallback text if image fails to load
			Text fallbackText = new Text(MainMenuPane.getServerName());
			fallbackText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
			fallbackText.setFill(Color.WHITE);
			hostProfileBox.getChildren().add(fallbackText);
		}

		return hostProfileBox;
	}

	// Add this method to GameWindow class to make it callable from other classes
	public static void triggerGameStartTransition() {
		if (gameWindowInstance != null) {
			Platform.runLater(() -> gameWindowInstance.showGameStartTransition(true));
		}
	}

}
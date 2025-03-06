package gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
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
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
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
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import logic.*;

import org.json.JSONObject;
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

import server.ClientInfo;
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
	private OverlayUI overlayManager;
	// === Game State ===
	private static boolean showCollision = false;
	private static long lastCollisionChanged = 0;
	private static long lastFpressed = 0;
	private static long lastRpressed = 0;

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
	private MeetingUI activeMeetingUI;
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
	private CharacterSelectgui characterSelectGui;
	private boolean characterSelectVisible = false;
	private boolean PrepEnd = false;

	public void start(Stage stage) {
		this.gameStage = stage;
		gameWindowInstance = this;
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

					PrepEnd = GameLogic.isPrepEnded();
					updateUIForPrepPhase();
					GameLogic.autoImposterCount(); // For now, automatically set imposter count to be 1/4 of player size
					//System.out.println(GameLogic.getImposterCount());
					if (MainMenuPane.getState().equals(logic.State.SERVER)) {
						ServerLogic.randomizeImposters();
					}
					triggerGameStartTransition(); // this auto teleports us
				}

				if (!GameLogic.isPrepEnded() && prepPhaseGui != null) {
					updatePrepPhasePlayerCount();
				}
				if (GameLogic.isPrepEnded() && overlayManager != null) {
					overlayManager.updateTaskProgress();
					overlayManager.updatePlayerRoleUI();
					overlayManager.updateButtonStates();
				}
				keylogger();
				updateMovement(now);
				update();
				render();
				displayFPS();
				checkPlayerStateChange();

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
		overlayManager = new OverlayUI(this);
		overlayManager.initialize(root, screenWidth, screenHeight);
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
				characterSelectGui = new CharacterSelectgui(this::onCharacterSelected);

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
						//System.out.println(
						//		"Collision Object: x=" + x + ", y=" + y + ", width=" + width + ", height=" + height);

						tempCollisionObjects.add(new CollisionObject(x, y, width, height));
					}
				}
			}
		}

		collisionObjects.addAll(tempCollisionObjects);
		initializeSpatialGrid();

		//System.out.println("Loaded " + collisionObjects.size() + " collision objects");
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
						//System.out.println("Event Object: x=" + x + ", y=" + y + ", width=" + width + ", height="
						//		+ height + " Event_ID = " + id);
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

	private void renderPlayers(boolean renderGhost) {
		// Determine if local player is dead
		boolean localPlayerIsDead = "dead".equalsIgnoreCase(PlayerLogic.getStatus());

		// Create a list to store all players (including local player) for sorting by
		// y-position
		List<PlayerRenderInfo> playersToRender = new ArrayList<>();

		// Only add the local player if it matches the ghost/non-ghost filter
		if (renderGhost == localPlayerIsDead) {
			// For ghost rendering, check if we already have a corpse for the local player
			String localPlayerKey = PlayerLogic.getLocalAddressPort();
			boolean hasCorpse = GameLogic.corpseList.containsKey(localPlayerKey);

			// When rendering ghosts, only add if the player has moved from their corpse
			// position
			if (!renderGhost || !hasCorpse || (hasCorpse
					&& (Math.abs(PlayerLogic.getMyPosX() - GameLogic.corpseList.get(localPlayerKey).getX()) > 10 || Math
							.abs(PlayerLogic.getMyPosY() - GameLogic.corpseList.get(localPlayerKey).getY()) > 10))) {
				playersToRender.add(new PlayerRenderInfo("local", PlayerLogic.getMyPosX(), PlayerLogic.getMyPosY(),
						null, PlayerLogic.getCharID()));
			}
		}

		// Add other players to the list if they match the filter criteria
		for (String key : GameLogic.playerList.keySet()) {
			PlayerInfo playerInfo = GameLogic.playerList.get(key);
			boolean playerIsDead = "dead".equalsIgnoreCase(playerInfo.getStatus());

			// Only add players that match our renderGhost criteria
			if (renderGhost == playerIsDead) {
				// For ghost rendering, check if we already have a corpse for this player
				boolean hasCorpse = GameLogic.corpseList.containsKey(key);

				// When rendering ghosts, only add if the player has moved from their corpse
				// position
				if (!renderGhost || !hasCorpse
						|| (hasCorpse && (Math.abs(playerInfo.getX() - GameLogic.corpseList.get(key).getX()) > 10
								|| Math.abs(playerInfo.getY() - GameLogic.corpseList.get(key).getY()) > 10))) {

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
			}
		}

		// If there are no players to render, return early
		if (playersToRender.isEmpty()) {
			return;
		}

		// Sort players by Y position (higher Y values render on top/later)
		Collections.sort(playersToRender, Comparator.comparingDouble(PlayerRenderInfo::getY));

		// Get the GraphicsContext
		GraphicsContext gc = canvas.getGraphicsContext2D();
		double originalOpacity = gc.getGlobalAlpha();
		double ghostOpacity = 0.5; // 50% opacity for ghosts/dead players

		// Render all players in sorted order
		for (PlayerRenderInfo player : playersToRender) {
			// If we're rendering ghosts, apply the ghost opacity
			if (renderGhost) {
				gc.setGlobalAlpha(ghostOpacity);
			}

			if (player.getKey().equals("local")) {
				renderLocalPlayer(player.getX(), player.getY());
			} else {
				renderOtherPlayer(player.getInfo());
			}
		}

		// Ensure opacity is reset to original value
		gc.setGlobalAlpha(originalOpacity);
	}

	private void renderCorpses() {
		// Skip if there are no corpses
		if (GameLogic.corpseList.isEmpty()) {
			return;
		}

		Image bloodImage = null;
		try {
			bloodImage = new Image(getClass().getResourceAsStream("/player/blood.png"));
		} catch (Exception e) {
			System.err.println("Error loading blood image: " + e.getMessage());
		}

		final int HEAD_WIDTH = 48;
		final int HEAD_HEIGHT = 48;

		// Iterate through all corpses
		for (String corpseKey : GameLogic.corpseList.keySet()) {
			Corpse corpse = GameLogic.corpseList.get(corpseKey);

			if (corpse.isFound()) {
				continue;
			}

			// Calculate the screen position of the corpse
			double corpseScreenX = corpse.getX() - viewportX;
			double corpseScreenY = corpse.getY() - viewportY;

			// Check if the corpse is visible on screen
			if (corpseScreenX > -50 && corpseScreenX < screenWidth + 50 && corpseScreenY > -50
					&& corpseScreenY < screenHeight + 50) {

				// Draw blood under the head
				if (bloodImage != null) {
					// Position blood image under the head
					gc.drawImage(bloodImage, corpseScreenX - bloodImage.getWidth() / 2,
							corpseScreenY - bloodImage.getHeight() / 2 + 33);
				}

				// Get character ID and render the head
				int charID = corpse.getCharacterID();

				String playerPath = String.format("/player/profile/%02d.png", (charID + 1));
				try {
					// Load character sprite sheet
					Image spriteSheet = new Image(getClass().getResourceAsStream(playerPath));

					if (!spriteSheet.isError()) {
						// Extract just the head portion of the sprite
						// This assumes the head is in the top portion of the sprite
						Rectangle2D headRegion = new Rectangle2D(0, 0, HEAD_WIDTH, HEAD_HEIGHT);

						// Create an ImageView to help with extracting and manipulating the image
						ImageView headView = new ImageView(spriteSheet);
						headView.setViewport(headRegion);

						// Convert to snapshotable image for rendering
						SnapshotParameters params = new SnapshotParameters();
						params.setFill(Color.TRANSPARENT);
						Image headImage = headView.snapshot(params, null);

						// Draw the head at corpse position
						gc.drawImage(headImage, corpseScreenX - HEAD_WIDTH / 2, corpseScreenY - HEAD_HEIGHT / 2);

					} else {
						// Fallback if sprite couldn't be loaded
						gc.setFill(Color.RED);
						gc.fillOval(corpseScreenX - 20, corpseScreenY - 20, 40, 40);
						gc.setStroke(Color.BLACK);
						gc.setLineWidth(2);
						gc.strokeOval(corpseScreenX - 20, corpseScreenY - 20, 40, 40);
					}

				} catch (Exception e) {
					System.err.println("Error rendering corpse head: " + e.getMessage());

					// Fallback drawing
					gc.setFill(Color.RED);
					gc.fillOval(corpseScreenX - 20, corpseScreenY - 20, 40, 40);
					gc.setStroke(Color.BLACK);
					gc.setLineWidth(2);
					gc.strokeOval(corpseScreenX - 20, corpseScreenY - 20, 40, 40);
				}
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

			// Special handling for dead players (ghosts)
			if ("dead".equalsIgnoreCase(PlayerLogic.getStatus())) {
				// For dead players, use a static frame but still respect direction
				if (PlayerLogic.getDirection() == 1) { // Left
					playerIMG
							.setViewport(new Rectangle2D(5 * FRAME_WIDTH, 1 * FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT));
				} else { // Right
					playerIMG
							.setViewport(new Rectangle2D(5 * FRAME_WIDTH, 0 * FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT));
				}
			}

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
			double collisionAreaY = localPlayerScreenY + 12; // Bottom 20 pixels
			gc.strokeRect(collisionBoxX, collisionAreaY, 48, 20);
		}
	}

	// Update the renderOtherPlayer method for non-local player ghosts
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

			// Set the frame based on movement state and whether player is dead
			int direction = playerInfo.getDirection();
			int frameIndex;

			// For dead players (ghosts), use a static frame but respect direction
			if ("dead".equalsIgnoreCase(playerInfo.getStatus())) {
				frameIndex = 0; // Static pose for ghosts
			}
			// For alive players, animate normally
			else if (playerInfo.isMoving()) {
				frameIndex = (int) ((System.currentTimeMillis() / ANIMATION_SPEED) % SPRITE_COLUMNS);
			} else {
				frameIndex = 5; // Always use frame 5 for stationary alive players
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

	// Update the updateMovement method to handle player turns even if dead
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
			moved = true;
		}
		if (pressedKeys.contains(KeyCode.S)) {
			dy += speed * deltaTime; // Move down
			moved = true;
		}
		if (pressedKeys.contains(KeyCode.A)) {
			dx -= speed * deltaTime; // Move left
			Direction = 1;
			moved = true;
		}
		if (pressedKeys.contains(KeyCode.D)) {
			dx += speed * deltaTime; // Move right
			Direction = 2;
			moved = true;
		}

		if (!PlayerLogic.getStatus().equals("dead")) { // if alive check for collision
			// Move horizontally
			if (dx != 0) {
				if (!checkCollision(playerX + dx, playerY)) {
					playerX += dx;
				} else {
					moved = false; // Hit collision, didn't actually move
				}
			}

			// Move vertically
			if (dy != 0) {
				if (!checkCollision(playerX, playerY + dy)) {
					playerY += dy;
				} else {
					moved = false; // Hit collision, didn't actually move
				}
			}
		} else { // if dead - no collision checks
			playerX += dx;
			playerY += dy;
		}

		// Send the updated position to the server or client
		PlayerLogic.isMoving(moved, Direction);
		PlayerLogic.setPosition(playerX, playerY);

		// Handle animation updates for alive players only
		if (!PlayerLogic.getStatus().equals("dead")) {
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
		} else {
			// For dead players, always stop animation but still update direction
			if (animation.getStatus() == Animation.Status.RUNNING) {
				animation.stop();
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
		if (pressedKeys.contains(KeyCode.F)) { // Task for player and Kill for Imposter
			if (System.currentTimeMillis() - lastFpressed > 250) {
				lastFpressed = System.currentTimeMillis();
				if (PlayerLogic.getStatus().equals("crewmate") || PlayerLogic.getStatus().equals("dead")) {
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
				} else { // For Imposter kill code
					PlayerInfo closestTarget = findClosestKillablePlayer();
					if (closestTarget != null) {
						// Trigger kill method
						killPlayer(closestTarget);
					} else {
						System.out.println("No players in kill range");
					}
				}
			}
		}
		if (pressedKeys.contains(KeyCode.R)) {
			if (System.currentTimeMillis() - lastRpressed > 250) {
				lastRpressed = System.currentTimeMillis();
				reportNearbyCorpse(); // Call the existing method to report corpses
			}
		}
	}

	public PlayerInfo findClosestKillablePlayer() {
		// Kill range (adjust as needed)
		final double KILL_RANGE = 100.0;

		PlayerInfo closestPlayer = null;
		double closestDistance = Double.MAX_VALUE;

		for (String key : GameLogic.playerList.keySet()) {
			PlayerInfo playerInfo = GameLogic.playerList.get(key);

			// Skip dead players, imposters, and the local player
			if ("dead".equals(playerInfo.getStatus()) || "imposter".equals(playerInfo.getStatus())
					|| key.equals(PlayerLogic.getLocalAddressPort())) {
				continue;
			}

			// Calculate distance
			double dx = playerInfo.getX() - PlayerLogic.getMyPosX();
			double dy = playerInfo.getY() - PlayerLogic.getMyPosY();
			double distance = Math.sqrt(dx * dx + dy * dy);

			// Update closest player if within range
			if (distance <= KILL_RANGE && distance < closestDistance) {
				closestPlayer = playerInfo;
				closestDistance = distance;
			}
		}

		return closestPlayer;
	}

	public void killPlayer(PlayerInfo target) {
		// Validate kill
		if (target == null) {
			System.out.println("GAMEWINDOW: Invalid kill attempt - target is null");
			return;
		}

		if (!"crewmate".equals(target.getStatus())) {
			// System.out.println("GAMEWINDOW: Invalid kill attempt - target is not a
			// crewmate: " + target.getStatus());
			return;
		}

		//System.out.println("GAMEWINDOW: Attempting to kill player: " + target.getName());

		try {
			String killedPlayerKey = target.getAddress().getHostAddress() + ":" + target.getPort();
			String killerKey = PlayerLogic.getLocalAddressPort();

			System.out.println("GAMEWINDOW: Kill details - Victim: " + killedPlayerKey + ", Killer: " + killerKey);

			target.setStatus("dead");
			Corpse corpse = new Corpse(target);
			GameLogic.corpseList.put(killedPlayerKey, corpse);
			System.out.println("GAMEWINDOW: Created local corpse at " + corpse.getX() + "," + corpse.getY());

			// Construct the report payload
			JSONObject killReport = new JSONObject();
			killReport.put("killedPlayer", killedPlayerKey);
			killReport.put("reporter", killerKey);
			killReport.put("killLocation", new double[] { target.getX(), target.getY() });

			// Send kill report to server
			String message = "/kill/" + killReport.toString();
			String killSoundPath = "assets/sounds/impostor_kill.wav";
			SoundLogic.playSound(killSoundPath, 0);

			// Create kill animation
			createKillAnimation(target);

			// Use appropriate logic to send the message based on if we're server or client
			if (MainMenuPane.getState().equals(State.CLIENT)) {
				ClientLogic.sendMessage(message, ServerSelectGui.getLogArea());
			} else if (MainMenuPane.getState().equals(State.SERVER)) {
				// If server, directly call server logic
				ServerLogic.handleKillReport(killedPlayerKey, killerKey, ServerSelectGui.getLogArea());
			}

			System.out.println("GAMEWINDOW: Kill report sent for player: " + target.getName());
		} catch (Exception e) {
			System.err.println("GAMEWINDOW ERROR: Error sending kill report: " + e.getMessage());
			e.printStackTrace();
		}
	}

	// TODO REFINE
	private void createKillAnimation(PlayerInfo target) {
		try {
			// Load the blood image rather than using a colored rectangle
			Image bloodImage = new Image(getClass().getResourceAsStream("/player/blood.png"));

			if (bloodImage.isError()) {
				System.err.println("Error loading blood image: " + bloodImage.getException().getMessage());
				return;
			}

			// Create an ImageView for the blood splatter
			ImageView bloodSplatter = new ImageView(bloodImage);
			bloodSplatter.setFitWidth(96); // Adjust size for better visual impact
			bloodSplatter.setFitHeight(96);
			bloodSplatter.setPreserveRatio(true);

			// Calculate the screen position accounting for zoom and viewport
			// We need to transform the world coordinates to screen coordinates
			double worldX = target.getX();
			double worldY = target.getY();

			// Apply viewport transformation and camera zoom
			double screenX = (worldX - viewportX) * CAMERA_ZOOM;
			double screenY = (worldY - viewportY) * CAMERA_ZOOM;

			// Center the blood splatter on the player
			screenX -= bloodSplatter.getFitWidth() / 2;
			screenY -= bloodSplatter.getFitHeight() / 2;

			// Position the blood splatter
			bloodSplatter.setLayoutX(screenX);
			bloodSplatter.setLayoutY(screenY);

			// Add a subtle pulse effect for more impact
			ScaleTransition pulse = new ScaleTransition(Duration.millis(300), bloodSplatter);
			pulse.setFromX(0.8);
			pulse.setFromY(0.8);
			pulse.setToX(1.2);
			pulse.setToY(1.2);
			pulse.setCycleCount(1);

			// Add to root for visibility
			root.getChildren().add(bloodSplatter);

			// Play the pulse animation
			pulse.play();

			// Fade out animation after the pulse
			FadeTransition fadeOut = new FadeTransition(Duration.millis(2000), bloodSplatter);
			fadeOut.setFromValue(1.0);
			fadeOut.setToValue(0.0);
			fadeOut.setDelay(Duration.millis(300)); // Start after pulse completes
			fadeOut.setOnFinished(e -> root.getChildren().remove(bloodSplatter));

			// Chain animations: pulse then fade
			pulse.setOnFinished(e -> fadeOut.play());

			// Log confirmation for debugging
			//System.out.println("Kill animation created at screen position: " + screenX + ", " + screenY);
			//System.out.println("Target world position was: " + worldX + ", " + worldY);

		} catch (Exception e) {
			System.err.println("Error creating kill animation: " + e.getMessage());
			e.printStackTrace();
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
		double playerBottom = y + 34; // Bottom of the player's bounding box (64 height)
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

		// Render layers that should appear below players
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

		// Render corpses before players so they appear underneath
		renderCorpses();

		// Render alive players
		renderPlayers(false);

		// Render foreground layers
		for (MapLayer layer : map.getLayers()) {
			if (layer.getName().equalsIgnoreCase("Foreground") || layer.getName().equalsIgnoreCase("SuperForeground")) {
				if (layer instanceof TileLayer) {
					drawTileLayer(gc, (TileLayer) layer, viewportX, viewportY, screenWidth, screenHeight);
				} else if (layer instanceof ObjectGroup) {
					drawObjectGroup(gc, (ObjectGroup) layer, viewportX, viewportY);
				}
			}
		}

		// If the local player is dead, also render ghost players
		if (PlayerLogic.getStatus().equals("dead")) {
			renderPlayers(true);
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

	private void showGameStartTransition() {
		// Create a gradient overlay instead of pure black
		Rectangle blackOverlay = new Rectangle(0, 0, screenWidth, screenHeight);
		RadialGradient gradient = new RadialGradient(0, 0, screenWidth / 2, screenHeight / 2,
				Math.max(screenWidth, screenHeight) / 2, false, CycleMethod.NO_CYCLE,
				new Stop(0, Color.rgb(20, 20, 40, 0.9)), // Dark blue-black center
				new Stop(1, Color.rgb(10, 10, 20, 0.95)) // Even darker edges
		);
		blackOverlay.setFill(gradient);
		blackOverlay.setOpacity(0);

		// Create a stylized frame for the content
		StackPane frameContainer = new StackPane();
		frameContainer.setPrefSize(screenWidth * 0.8, screenHeight * 0.7);
		frameContainer.setMaxSize(screenWidth * 0.8, screenHeight * 0.7);
		frameContainer.setAlignment(Pos.CENTER);

		Rectangle frame = new Rectangle(screenWidth * 0.8, screenHeight * 0.7);
		frame.setArcWidth(30);
		frame.setArcHeight(30);
		frame.setFill(Color.rgb(30, 30, 50, 0.9)); // Dark blue-gray
		frame.setStroke(Color.rgb(70, 130, 180, 0.7)); // Steel blue border
		frame.setStrokeWidth(3);
		frame.setOpacity(0);

		frameContainer.getChildren().add(frame);

		// Create the role text
		Text roleText = new Text();

		// Set text and color based on role
		boolean isImposter = "imposter".equals(PlayerLogic.getStatus());
		if (isImposter) {
			roleText.setText("IMPOSTOR");
			roleText.setFill(Color.rgb(255, 80, 80)); // Slightly softer red
		} else {
			roleText.setText("CREWMATE");
			roleText.setFill(Color.rgb(80, 150, 255)); // Softer blue
		}

		// Stylized pixel-like font
		roleText.setFont(Font.font("Monospace", FontWeight.BOLD, 64));
		roleText.setEffect(new DropShadow(10, Color.BLACK)); // Add shadow for depth
		roleText.setTextAlignment(TextAlignment.CENTER);

		// Subtitle describing the role
		Text subtitleText = new Text();
		if (isImposter) {
			subtitleText.setText("Sabotage and eliminate the crew!");
		} else {
			if (GameLogic.getImposterCount() == 1)
				subtitleText.setText("There is 1 imposter among us");
			else {
				subtitleText.setText("There are " + GameLogic.getImposterCount() + " imposters among us");
			}
		}
		subtitleText.setFont(Font.font("Monospace", FontWeight.BOLD, 24));
		subtitleText.setFill(Color.LIGHTGRAY);
		subtitleText.setTextAlignment(TextAlignment.CENTER);

		// Player display setup (similar to previous implementation)
		HBox playerRow = createPlayerRowDisplay(isImposter);

		// Combine elements into a VBox
		VBox contentBox = new VBox(20);
		contentBox.setAlignment(Pos.CENTER);
		contentBox.getChildren().addAll(roleText, subtitleText, playerRow);
		contentBox.setOpacity(0);

		// Create a StackPane to center everything perfectly
		StackPane mainContainer = new StackPane(frameContainer, contentBox);
		mainContainer.setAlignment(Pos.CENTER);
		mainContainer.setPrefSize(screenWidth, screenHeight);
		mainContainer.setLayoutX(0);
		mainContainer.setLayoutY(0);

		// Add everything to root
		root.getChildren().addAll(blackOverlay, mainContainer);

		// Animations (similar to previous implementation)
		FadeTransition fadeInOverlay = new FadeTransition(Duration.millis(400), blackOverlay);
		fadeInOverlay.setFromValue(0);
		fadeInOverlay.setToValue(1);

		FadeTransition fadeInFrame = new FadeTransition(Duration.millis(500), frame);
		fadeInFrame.setFromValue(0);
		fadeInFrame.setToValue(1);
		fadeInFrame.setDelay(Duration.millis(300));

		FadeTransition fadeInContent = new FadeTransition(Duration.millis(600), contentBox);
		fadeInContent.setFromValue(0);
		fadeInContent.setToValue(1);
		fadeInContent.setDelay(Duration.millis(600));

		// Fade out animations
		FadeTransition fadeOutContent = new FadeTransition(Duration.millis(400), contentBox);
		fadeOutContent.setFromValue(1);
		fadeOutContent.setToValue(0);
		fadeOutContent.setDelay(Duration.seconds(4.0));

		FadeTransition fadeOutFrame = new FadeTransition(Duration.millis(400), frame);
		fadeOutFrame.setFromValue(1);
		fadeOutFrame.setToValue(0);
		fadeOutFrame.setDelay(Duration.seconds(4.2));

		FadeTransition fadeOutOverlay = new FadeTransition(Duration.millis(400), blackOverlay);
		fadeOutOverlay.setFromValue(1);
		fadeOutOverlay.setToValue(0);
		fadeOutOverlay.setDelay(Duration.seconds(4.6));

		// Remove everything when done
		fadeOutOverlay.setOnFinished(e -> {
			root.getChildren().removeAll(blackOverlay, frame, contentBox);
		});

		// Play the animations
		fadeInOverlay.play();
		fadeInFrame.play();
		fadeInContent.play();
		fadeOutContent.play();
		fadeOutFrame.play();
		fadeOutOverlay.play();

		// Play appropriate sound effect
		String soundFile = "assets/sounds/Roundstart_MAIN.wav";
		SoundLogic.playSound(soundFile, 0); // Using 0 for full volume

		// Background operations: teleport the player during the black screen
		TeleportToStart();
	}

	// Helper method to create player row display
	private HBox createPlayerRowDisplay(boolean isImposter) {
		HBox playerRow = new HBox(30); // 30 pixels spacing
		playerRow.setAlignment(Pos.CENTER);

		// Collection to hold players to display
		List<PlayerInfo> playersToShow = new ArrayList<>();

		// Add player information based on role
		if (isImposter) {
			// For imposters, only show other imposters
			for (String key : GameLogic.playerList.keySet()) {
				PlayerInfo playerInfo = GameLogic.playerList.get(key);
				if ("imposter".equals(playerInfo.getStatus())) {
					playersToShow.add(playerInfo);
				}
			}
		} else {
			// For crewmates, show all players
			for (String key : GameLogic.playerList.keySet()) {
				PlayerInfo playerInfo = GameLogic.playerList.get(key);
				playersToShow.add(playerInfo);
			}
		}

		// Add main player (you)
		playerRow.getChildren().add(createPlayerBox(null, isImposter));

		// Add other players
		for (PlayerInfo playerInfo : playersToShow) {
			playerRow.getChildren().add(createPlayerBox(playerInfo, isImposter));
		}

		return playerRow;
	}

	// Helper method to create individual player boxes
	private VBox createPlayerBox(PlayerInfo playerInfo, boolean isImposter) {
		VBox playerBox = new VBox(10);
		playerBox.setAlignment(Pos.CENTER);
		playerBox.setStyle(
				"-fx-background-color: rgba(50, 50, 70, 0.6);" + "-fx-background-radius: 15;" + "-fx-padding: 10px;");

		try {
			// Determine character image
			int charImageIndex = playerInfo != null ? playerInfo.getCharacterID() + 1 : PlayerLogic.getCharID() + 1;
			String profilePath = charImageIndex == 10 ? "/player/profile/10.png"
					: "/player/profile/0" + charImageIndex + ".png";

			ImageView playerImage = new ImageView(new Image(getClass().getResourceAsStream(profilePath)));
			playerImage.setFitWidth(96); // Slightly larger
			playerImage.setFitHeight(150);
			playerImage.setPreserveRatio(true);

			// Add player image
			playerBox.getChildren().add(playerImage);

			// Add player name
			Text nameText = new Text(playerInfo != null ? playerInfo.getName() : PlayerLogic.getName());
			nameText.setFont(Font.font("Monospace", FontWeight.BOLD, 18));

			// Color coding for roles
			if (isImposter) {
				nameText.setFill(Color.rgb(255, 100, 100)); // Bright red for imposters
			} else {
				nameText.setFill(Color.rgb(100, 200, 255)); // Bright blue for crewmates
			}

			playerBox.getChildren().add(nameText);

			// Highlight for imposters if the player is an imposter
			if (isImposter && (playerInfo == null || "imposter".equals(playerInfo.getStatus()))) {
				Rectangle highlight = new Rectangle(playerBox.getPrefWidth(), 5);
				highlight.setFill(Color.rgb(255, 50, 50)); // Red highlight
				playerBox.getChildren().add(highlight);
			}

		} catch (Exception e) {
			System.err.println("Error creating player image: " + e.getMessage());

			// Fallback text
			Text fallbackText = new Text(playerInfo != null ? playerInfo.getName() : PlayerLogic.getName());
			fallbackText.setFont(Font.font("Monospace", FontWeight.BOLD, 18));
			fallbackText.setFill(Color.WHITE);

			playerBox.getChildren().add(fallbackText);
		}

		return playerBox;
	}

	/**
	 * Shows a temporary notification message to the player.
	 * 
	 * @param message The message to display
	 */
	private void showNotification(String message) {
		// Create a stylized label for the notification
		Text notification = new Text(message);
		notification.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
		notification.setFill(Color.WHITE);
		notification.setStroke(Color.BLACK);
		notification.setStrokeWidth(1);

		StackPane notificationPane = new StackPane(notification);
		notificationPane.setStyle(
				"-fx-background-color: rgba(50, 50, 70, 0.7);" + "-fx-padding: 10px;" + "-fx-background-radius: 5px;");

		notificationPane.setLayoutX((screenWidth - 200) / 2);
		notificationPane.setLayoutY(screenHeight * 0.7);
		notificationPane.setOpacity(0);

		root.getChildren().add(notificationPane);

		// Fade in
		FadeTransition fadeIn = new FadeTransition(Duration.millis(200), notificationPane);
		fadeIn.setFromValue(0);
		fadeIn.setToValue(1);
		fadeIn.play();

		// Hold then fade out
		FadeTransition fadeOut = new FadeTransition(Duration.millis(200), notificationPane);
		fadeOut.setFromValue(1);
		fadeOut.setToValue(0);
		fadeOut.setDelay(Duration.millis(1500));
		fadeOut.setOnFinished(e -> root.getChildren().remove(notificationPane));

		fadeIn.setOnFinished(e -> fadeOut.play());
	}

	/**
	 * Shows a death panel when the player dies, styled to match the
	 * CharacterSelectGui.
	 * 
	 * @param killerCharId The character ID of the killer
	 */
	private void showDeathPanel(int killerCharId) {
		// Create panel container
		StackPane deathPanel = new StackPane();
		deathPanel.setPrefSize(screenWidth, screenHeight);

		// Add semi-transparent dark background
		Rectangle darkOverlay = new Rectangle(screenWidth, screenHeight);
		darkOverlay.setFill(new Color(0, 0, 0, 0.8)); // Darker overlay to make panel stand out

		// Create content container styled like CharacterSelectGui
		VBox contentBox = new VBox(20);
		contentBox.setAlignment(Pos.CENTER);
		contentBox.setPrefWidth(500);
		contentBox.setPrefHeight(550);
		contentBox.setMaxWidth(500);
		contentBox.setMaxHeight(550);
		contentBox.setPadding(new Insets(25));

		// Use the same dark blue-gray background as CharacterSelectGui
		contentBox.setStyle("-fx-background-color: rgba(30, 30, 50, 0.95);");

		// Create border with same blue accent color
		contentBox.setBorder(new Border(new BorderStroke(Paint.valueOf("#1e90ff"), // Blue border color
				BorderStrokeStyle.SOLID, new CornerRadii(0), // Sharp corners
				new BorderWidths(3))));

		// Death title text
		Text title = new Text("YOU WERE KILLED");
		title.setFill(Paint.valueOf("#ff6347")); // Tomato red
		title.setFont(Font.font("Monospace", FontWeight.BOLD, 28));

		// Shadow effect for title text to make it pop (just like in CharacterSelectGui)
		title.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 3, 0, 0, 0);");

		// Character display container
		HBox characterBox = new HBox(50);
		characterBox.setAlignment(Pos.CENTER);
		characterBox.setPadding(new Insets(15, 0, 15, 0));

		// Create character displays with same style as CharacterSelectGui
		VBox playerBox = createCharSelectStyleCharacterDisplay(PlayerLogic.getCharID(), PlayerLogic.getName(), "Dead");
		VBox killerBox = createCharSelectStyleCharacterDisplay(killerCharId, "Your Killer", "Impostor");

		// Add bob animation to the killer
		TranslateTransition bobAnimation = new TranslateTransition(Duration.millis(700), killerBox);
		bobAnimation.setByY(15);
		bobAnimation.setAutoReverse(true);
		bobAnimation.setCycleCount(Animation.INDEFINITE);
		bobAnimation.play();

		// Add the character boxes
		characterBox.getChildren().addAll(playerBox, killerBox);

		// Ghost task info
		Text ghostInfoText = new Text("You can continue to play as a ghost.\nComplete tasks to help your team win!");
		ghostInfoText.setFill(Paint.valueOf("#87cefa")); // Light blue text
		ghostInfoText.setFont(Font.font("Monospace", 16));
		ghostInfoText.setTextAlignment(TextAlignment.CENTER);

		// Continue button using same style as CharacterSelectGui buttons
		Button continueButton = new Button("CONTINUE AS GHOST");

		// Match the button style from CharacterSelectGui
		String baseButtonStyle = "-fx-background-color: #1e90ff;" + "-fx-text-fill: white;"
				+ "-fx-font-family: 'Monospace';" + "-fx-font-size: 16px;" + "-fx-font-weight: bold;"
				+ "-fx-padding: 10 20 10 20;" + "-fx-border-color: #87cefa;" + "-fx-border-width: 2px;"
				+ "-fx-background-radius: 0;" + "-fx-border-radius: 0;"
				+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);";

		String hoverButtonStyle = "-fx-background-color: #00bfff;" + "-fx-text-fill: white;"
				+ "-fx-font-family: 'Monospace';" + "-fx-font-size: 16px;" + "-fx-font-weight: bold;"
				+ "-fx-padding: 10 20 10 20;" + "-fx-border-color: #b0e2ff;" + "-fx-border-width: 2px;"
				+ "-fx-background-radius: 0;" + "-fx-border-radius: 0;"
				+ "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 8, 0, 0, 1);";

		continueButton.setStyle(baseButtonStyle);
		continueButton.setPrefWidth(250);

		// Add hover effects
		continueButton.setOnMouseEntered(e -> continueButton.setStyle(hoverButtonStyle));
		continueButton.setOnMouseExited(e -> continueButton.setStyle(baseButtonStyle));

		// Button action to close the panel
		continueButton.setOnAction(e -> {
			FadeTransition fadeOut = new FadeTransition(Duration.millis(500), deathPanel);
			fadeOut.setFromValue(1);
			fadeOut.setToValue(0);
			fadeOut.setOnFinished(event -> root.getChildren().remove(deathPanel));
			fadeOut.play();
		});

		// Add all elements to the content box
		contentBox.getChildren().addAll(title, characterBox, ghostInfoText, continueButton);

		// Add elements to the panel
		deathPanel.getChildren().addAll(darkOverlay, contentBox);

		// Add panel to the root with animation
		deathPanel.setOpacity(0);
		root.getChildren().add(deathPanel);

		// Create fade-in animation
		FadeTransition fadeIn = new FadeTransition(Duration.millis(800), deathPanel);
		fadeIn.setFromValue(0);
		fadeIn.setToValue(1);
		fadeIn.play();

		// Play death sound
		SoundLogic.playSound("assets/sounds/impostor_kill.wav", 0);
	}

	/**
	 * Creates a character display matching the CharacterSelectGui style
	 */
	private VBox createCharSelectStyleCharacterDisplay(int charId, String name, String status) {
		VBox charBox = new VBox(10);
		charBox.setAlignment(Pos.CENTER);
		charBox.setStyle("-fx-background-color: rgba(42, 42, 58, 0.6);" + "-fx-padding: 15px;"
				+ "-fx-border-color: #1e90ff;" + "-fx-border-width: 2px;");

		// Load character profile image
		String profilePath = String.format("/player/profile/%02d.png", (charId + 1));
		try {
			Image profileImage = new Image(getClass().getResourceAsStream(profilePath));
			ImageView profileView = new ImageView(profileImage);

			// Set size matching CharacterSelectGui
			profileView.setFitWidth(150);
			profileView.setFitHeight(200);
			profileView.setPreserveRatio(true);

			// Add styling to match CharacterSelectGui
			profileView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);"
					+ "-fx-border-color: #1e90ff;" + "-fx-border-width: 2px;" + "-fx-background-color: #2a2a3a;");

			// Name label
			Text nameText = new Text(name);
			nameText.setFont(Font.font("Monospace", FontWeight.BOLD, 18));
			nameText.setFill(Color.WHITE);

			// Status label
			Text statusText = new Text(status);
			statusText.setFont(Font.font("Monospace", FontWeight.BOLD, 16));

			// Set color based on status
			if (status.equalsIgnoreCase("Impostor")) {
				statusText.setFill(Color.RED);
			} else {
				statusText.setFill(Color.LIGHTBLUE);
			}

			// Add all elements to the box
			charBox.getChildren().addAll(profileView, nameText, statusText);

		} catch (Exception e) {
			System.err.println("Error loading profile image: " + e.getMessage());

			// Fallback for missing image
			Rectangle fallbackImage = new Rectangle(150, 150);
			fallbackImage.setFill(Color.DARKGRAY);

			Text nameText = new Text(name);
			nameText.setFont(Font.font("Monospace", FontWeight.BOLD, 18));
			nameText.setFill(Color.WHITE);

			Text statusText = new Text(status);
			statusText.setFont(Font.font("Monospace", FontWeight.BOLD, 16));
			statusText.setFill(status.equalsIgnoreCase("Impostor") ? Color.RED : Color.LIGHTBLUE);

			charBox.getChildren().addAll(fallbackImage, nameText, statusText);
		}

		return charBox;
	}

	/**
	 * Check for player death and show the death panel
	 */
	private boolean wasAlive = true; // Add as a class field

	private void checkPlayerStateChange() {
		boolean isAlive = !"dead".equalsIgnoreCase(PlayerLogic.getStatus());

		// Check if player just died
		if (wasAlive && !isAlive) {
			// Get the killer character ID - replace with your implementation
			int killerCharId = getKillerCharacterId();

			// Show death panel
			Platform.runLater(() -> showDeathPanel(killerCharId));
		}

		// Update state tracker
		wasAlive = isAlive;
	}

	/**
	 * Gets the killer's character ID Replace this with your actual implementation
	 */
	private int getKillerCharacterId() {
		// This is a placeholder - you should implement this to return the actual killer
		return new Random().nextInt(10);
	}

	/**
	 * Creates an enhanced emergency meeting transition UI that matches the game's
	 * theme. This version takes the reporter ID directly for more reliable player
	 * identification.
	 * 
	 * @param reportedPlayerName  The name of the player whose body was reported
	 * @param reportedCharacterId The character ID of the reported player
	 * @param reporterKey         The unique key/ID of the player who called the
	 *                            meeting
	 */
	public void startEmergencyMeeting(String reporterKey, String reportedPlayerName, int reportedCharacterId) {
		try {
			// Create a full-screen overlay for the meeting
			Rectangle overlay = new Rectangle(0, 0, screenWidth, screenHeight);
			// Use RadialGradient for more atmospheric look, matching game theme
			RadialGradient gradient = new RadialGradient(0, 0, screenWidth / 2, screenHeight / 2,
					Math.max(screenWidth, screenHeight) / 2, false, CycleMethod.NO_CYCLE,
					new Stop(0, Color.rgb(20, 20, 40, 0.9)), // Dark blue-black center
					new Stop(1, Color.rgb(10, 10, 20, 0.95)) // Even darker edges
			);
			overlay.setFill(gradient);

			// Create styled container matching CharacterSelectgui style
			VBox reportPanel = new VBox(20);
			reportPanel.setPrefWidth(600);
			reportPanel.setPrefHeight(500);
			reportPanel.setMaxWidth(600);
			reportPanel.setMaxHeight(500);
			reportPanel.setPadding(new Insets(25));
			reportPanel.setAlignment(Pos.CENTER);

			// Apply styling consistent with CharacterSelectgui
			reportPanel.setStyle("-fx-background-color: rgba(30, 30, 50, 0.95);"); // Dark blue-gray background
			reportPanel.setBorder(new Border(new BorderStroke(Paint.valueOf("#1e90ff"), // Blue border color
					BorderStrokeStyle.SOLID, new CornerRadii(0), // Sharp corners for pixel art style
					new BorderWidths(3))));

			// Create alert heading
			Text headingText = new Text("EMERGENCY MEETING");
			headingText.setFont(Font.font("Monospace", FontWeight.BOLD, 36));
			headingText.setFill(Color.rgb(255, 80, 80)); // Red text
			headingText.setTextAlignment(TextAlignment.CENTER);
			headingText.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 3, 0, 0, 0);"); // Shadow
																											// effect

			// Create report text and reporter info
			String reporterName;
			if (reporterKey.equals(PlayerLogic.getLocalAddressPort())) {
				reporterName = PlayerLogic.getName();
			} else {
				PlayerInfo reporter = GameLogic.playerList.get(reporterKey);
				reporterName = (reporter != null) ? reporter.getName() : "Unknown";
			}

			Text bodyReportedText;
			if (reportedPlayerName != null) {
				bodyReportedText = new Text(reporterName + " REPORTED " + reportedPlayerName + "'S BODY");
			} else {
				bodyReportedText = new Text(reporterName + " CALLED AN EMERGENCY MEETING");
			}
			bodyReportedText.setFont(Font.font("Monospace", FontWeight.BOLD, 24));
			bodyReportedText.setFill(Color.WHITE);
			bodyReportedText.setTextAlignment(TextAlignment.CENTER);

			// Create container for the victim display
			HBox victimContainer = new HBox(15);
			victimContainer.setAlignment(Pos.CENTER);
			victimContainer.setPadding(new Insets(20, 0, 20, 0));

			// Only create victim display if a body was reported
			if (reportedPlayerName != null) {
				// Add blood image beneath the head
				Image bloodImage = new Image(getClass().getResourceAsStream("/player/blood.png"));
				ImageView bloodView = new ImageView(bloodImage);
				bloodView.setFitWidth(120);
				bloodView.setFitHeight(120);
				bloodView.setPreserveRatio(true);

				// Create victim head display - matching corpse rendering
				int charID = reportedCharacterId;

				// Use the same constants as in renderCorpses method
				final int HEAD_WIDTH = 48;
				final int HEAD_HEIGHT = 48;

				// Build character path string (format matches your existing code)
				String playerPath = "/player/profile/" + (charID < 9 ? "0" + (charID + 1) : "10") + ".png";

				// Load character sprite sheet
				Image spriteSheet = new Image(getClass().getResourceAsStream(playerPath));

				// Extract just the head portion of the sprite - using the same region as
				// renderCorpses
				Rectangle2D headRegion = new Rectangle2D(0, 0, HEAD_WIDTH, HEAD_HEIGHT);

				// Create an ImageView for the head
				ImageView headView = new ImageView(spriteSheet);
				headView.setViewport(headRegion);
				headView.setFitWidth(90);
				headView.setFitHeight(90);
				headView.setPreserveRatio(true);
				headView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);");

				// Add styling to the head/profile image match CharacterSelectGui style
				StackPane headContainer = new StackPane();
				headContainer.getChildren().addAll(bloodView, headView);

				// Add victim name
				Text victimName = new Text(reportedPlayerName);
				victimName.setFont(Font.font("Monospace", FontWeight.BOLD, 20));
				victimName.setFill(Color.LIGHTBLUE);
				victimName.setTextAlignment(TextAlignment.CENTER);

				// Create styled victim box
				VBox victimBox = new VBox(15);
				victimBox.setAlignment(Pos.CENTER);
				victimBox.setStyle("-fx-background-color: rgba(42, 42, 58, 0.8);" + "-fx-padding: 20px;"
						+ "-fx-border-color: #1e90ff;" + "-fx-border-width: 2px;");
				victimBox.getChildren().addAll(headContainer, victimName);

				// Add to victim container
				victimContainer.getChildren().add(victimBox);
			}

			// Discussion text
			Text discussionText = new Text("DISCUSS WHO IS THE IMPOSTOR");
			discussionText.setFont(Font.font("Monospace", FontWeight.BOLD, 18));
			discussionText.setFill(Color.LIGHTGRAY);
			discussionText.setTextAlignment(TextAlignment.CENTER);

			// Timer text - would be dynamic in a real implementation
			Text timerText = new Text("DISCUSSION TIME: 5s");
			timerText.setFont(Font.font("Monospace", FontWeight.BOLD, 24));
			timerText.setFill(Color.rgb(255, 200, 100)); // Orange-ish color
			timerText.setTextAlignment(TextAlignment.CENTER);

			// Add all elements to the report panel
			reportPanel.getChildren().addAll(headingText, bodyReportedText);

			// Only add victim display if a body was reported
			if (reportedPlayerName != null) {
				reportPanel.getChildren().add(victimContainer);
			}

			reportPanel.getChildren().addAll(discussionText, timerText);

			// Create pulsing animation for the heading
			ScaleTransition pulse = new ScaleTransition(Duration.millis(600), headingText);
			pulse.setFromX(1.0);
			pulse.setFromY(1.0);
			pulse.setToX(1.1);
			pulse.setToY(1.1);
			pulse.setCycleCount(Animation.INDEFINITE);
			pulse.setAutoReverse(true);
			pulse.play();

			// Center everything on screen
			StackPane meetingPane = new StackPane(overlay, reportPanel);
			meetingPane.setPrefSize(screenWidth, screenHeight);

			// Add entrance animation
			reportPanel.setScaleX(0.1);
			reportPanel.setScaleY(0.1);

			ScaleTransition entranceAnim = new ScaleTransition(Duration.millis(500), reportPanel);
			entranceAnim.setFromX(0.1);
			entranceAnim.setFromY(0.1);
			entranceAnim.setToX(1.0);
			entranceAnim.setToY(1.0);
			entranceAnim.setInterpolator(Interpolator.EASE_OUT);

			// Add to root for visibility with fade-in
			root.getChildren().add(meetingPane);
			meetingPane.setOpacity(0);

			FadeTransition fadeIn = new FadeTransition(Duration.millis(500), meetingPane);
			fadeIn.setFromValue(0);
			fadeIn.setToValue(1);

			// Play entrance animations
			fadeIn.play();
			entranceAnim.play();

			// Timer for transition to voting UI
			Timeline timer = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
				// Start fade out transition
				FadeTransition fadeOut = new FadeTransition(Duration.millis(500), meetingPane);
				fadeOut.setFromValue(1);
				fadeOut.setToValue(0);
				fadeOut.setOnFinished(event -> {
					root.getChildren().remove(meetingPane);
					pulse.stop(); // Make sure to stop the animation

					// Show the voting UI after the transition
					showVotingUI(reportedPlayerName, reportedCharacterId, reporterKey);
				});
				fadeOut.play();
			}));
			timer.play();

			// Update the timer text as countdown progresses
			final int[] secondsLeft = { 5 };
			Timeline countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
				secondsLeft[0]--;
				timerText.setText("DISCUSSION TIME: " + secondsLeft[0] + "s");
			}));
			countdownTimer.setCycleCount(5);
			countdownTimer.play();

			// Play emergency meeting sound
			SoundLogic.playSound("assets/sounds/alarm_emergencymeeting.wav", 0);

		} catch (Exception e) {
			System.err.println("Error creating emergency meeting UI: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Modified reportNearbyCorpse method to pass the character ID to the emergency
	 * meeting UI and trigger it for all players while hiding the reported corpse.
	 */
	public void reportNearbyCorpse() {
		// Skip if the player is dead - dead players can't report bodies
		if ("dead".equalsIgnoreCase(PlayerLogic.getStatus())) {
			System.out.println("Dead players cannot report bodies.");
			showNotification("Cannot report bodies while dead");
			return;
		}

		// Skip if the game has ended
		if (GameLogic.isGameEnded()) {
			return;
		}

		// Define report range
		final double REPORT_RANGE = 150.0; // Adjust as needed

		// Track the closest corpse
		Corpse closestCorpse = null;
		String closestCorpseKey = null;
		double closestDistance = Double.MAX_VALUE;

		// Check all corpses in the game
		for (String corpseKey : GameLogic.corpseList.keySet()) {
			Corpse corpse = GameLogic.corpseList.get(corpseKey);

			// Skip corpses that have already been reported
			if (corpse.isFound()) {
				continue;
			}

			// Calculate distance to corpse
			double dx = corpse.getX() - PlayerLogic.getMyPosX();
			double dy = corpse.getY() - PlayerLogic.getMyPosY();
			double distance = Math.sqrt(dx * dx + dy * dy);

			// Check if this corpse is within range and closer than the current closest
			if (distance <= REPORT_RANGE && distance < closestDistance) {
				closestCorpse = corpse;
				closestCorpseKey = corpseKey;
				closestDistance = distance;
			}
		}

		// If a corpse was found within range, report it
		if (closestCorpse != null) {
			System.out.println(
					"Reporting corpse of " + closestCorpse.getPlayerName() + " at distance " + closestDistance);

			// Mark the corpse as found (will hide it from rendering)
			closestCorpse.setFound(true);

			// Play report sound
			SoundLogic.playSound("assets/sounds/alarm_emergencymeeting.wav", 0);

			// Send the report to the server
			String reporterKey = PlayerLogic.getLocalAddressPort();
			String reportedPlayerName = closestCorpse.getPlayerName();
			int reportedCharId = closestCorpse.getCharacterID();

			// Send report based on game mode (server or client)
			if (MainMenuPane.getState().equals(State.SERVER)) {
				// For server: handle the report locally and broadcast to all clients
				ServerLogic.handleBodyReport(reporterKey, closestCorpseKey, ServerSelectGui.getLogArea());

				// Additionally, broadcast a special emergency meeting message to all clients
				try {
					JSONObject meetingData = new JSONObject();
					meetingData.put("reporter", reporterKey);
					meetingData.put("reportedPlayer", reportedPlayerName);
					meetingData.put("reportedCharId", reportedCharId);
					meetingData.put("time", System.currentTimeMillis());

					String meetingMessage = "/report/" + meetingData.toString();

					// Broadcast to all clients
					for (ClientInfo clientInfo : ServerLogic.getConnectedClients()) {
						DatagramSocket socket = ServerLogic.getServerSocket();
						if (socket != null) {
							byte[] buf = meetingMessage.getBytes(StandardCharsets.UTF_8);
							DatagramPacket packet = new DatagramPacket(buf, buf.length, clientInfo.getAddress(),
									clientInfo.getPort());
							socket.send(packet);
						}
					}

					System.out.println("Emergency meeting broadcast to all clients");
				} catch (Exception e) {
					System.err.println("Error broadcasting emergency meeting: " + e.getMessage());
				}
			} else if (MainMenuPane.getState().equals(State.CLIENT)) {
				// For client: send the report to the server
				JSONObject reportData = new JSONObject();
				reportData.put("reporter", reporterKey);
				reportData.put("corpse", closestCorpseKey);
				reportData.put("reportedPlayer", reportedPlayerName);
				reportData.put("reportedCharId", reportedCharId);
				System.out.println("Client send report to server");
				String reportMessage = "/report/" + reportData.toString();
				ClientLogic.sendMessage(reportMessage, ServerSelectGui.getLogArea());
			}

			// Start emergency meeting UI with character ID for the local player
			startEmergencyMeeting(reporterKey, closestCorpse.getPlayerName(), closestCorpse.getCharacterID());
		} else {
			// No corpses nearby
			System.out.println("No unreported corpses nearby");
			showNotification("No bodies nearby to report");
		}
	}

	// Add this method to GameWindow class to make it callable from other classes
	public static void triggerGameStartTransition() {
		if (gameWindowInstance != null) {
			Platform.runLater(() -> gameWindowInstance.showGameStartTransition());
		}
	}

	public boolean hasNearbyInteractiveTask() {
		// Check for event collisions
		String eventId = TaskLogic.isPlayerCollidingWithEvent(eventObjects);

		// If we're colliding with an event object and it's not completed yet
		if (!eventId.isEmpty() && !TaskLogic.isTaskCompleted(eventId)) {
			return true;
		}

		return false;
	}

	/**
	 * Checks if there's a killable player nearby
	 * 
	 * @return true if there's a killable player in range
	 */
	public boolean hasNearbyKillablePlayer() {
		// Only imposters can kill and only if they're not dead
		if (!"imposter".equals(PlayerLogic.getStatus()) || "dead".equals(PlayerLogic.getStatus())) {
			return false;
		}

		// Kill range
		final double KILL_RANGE = 100.0;

		for (String key : GameLogic.playerList.keySet()) {
			PlayerInfo playerInfo = GameLogic.playerList.get(key);

			// Skip dead players, imposters, and the local player
			if ("dead".equals(playerInfo.getStatus()) || "imposter".equals(playerInfo.getStatus())
					|| key.equals(PlayerLogic.getLocalAddressPort())) {
				continue;
			}

			// Calculate distance
			double dx = playerInfo.getX() - PlayerLogic.getMyPosX();
			double dy = playerInfo.getY() - PlayerLogic.getMyPosY();
			double distance = Math.sqrt(dx * dx + dy * dy);

			// Check if player is within kill range
			if (distance <= KILL_RANGE) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks if there's a reportable corpse nearby
	 * 
	 * @return true if there's a reportable corpse in range
	 */
	public boolean hasNearbyCorpse() {
		// Dead players can't report
		if ("dead".equals(PlayerLogic.getStatus())) {
			return false;
		}

		// Define report range
		final double REPORT_RANGE = 150.0;

		// Check all corpses in the game
		for (String corpseKey : GameLogic.corpseList.keySet()) {
			Corpse corpse = GameLogic.corpseList.get(corpseKey);

			// Skip corpses that have already been reported
			if (corpse.isFound()) {
				continue;
			}

			// Calculate distance to corpse
			double dx = corpse.getX() - PlayerLogic.getMyPosX();
			double dy = corpse.getY() - PlayerLogic.getMyPosY();
			double distance = Math.sqrt(dx * dx + dy * dy);

			// Check if corpse is within report range
			if (distance <= REPORT_RANGE) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Interact with the nearest task if any
	 */
	public void interactWithNearbyTask() {
		if (System.currentTimeMillis() - lastFpressed > 250) {
			lastFpressed = System.currentTimeMillis();

			if (PlayerLogic.getStatus().equals("crewmate") || PlayerLogic.getStatus().equals("dead")) {
				System.out.println("Interact - checking for interactive objects");

				// Check for event collisions
				String eventId = TaskLogic.isPlayerCollidingWithEvent(eventObjects);

				// If we're colliding with an event object
				if (!eventId.isEmpty()) {
					System.out.println("Interacting with event: " + eventId);

					// Attempt to open the task
					boolean taskOpened = TaskLogic.openTask(eventId, taskContainer);

					if (taskOpened) {
						System.out.println("Task opened: " + eventId);
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

	// Modify the updateUIForPrepPhase method to show/hide the overlay
	private void updateUIForPrepPhase() {
		Platform.runLater(() -> {
			if (!GameLogic.isPrepEnded()) { // In prep phase
				if (prepPhaseGui != null) {
					prepPhaseGui.show();
				}
				if (characterSelectButton != null) {
					characterSelectButton.setVisible(true);
				}
				if (overlayManager != null) {
					overlayManager.hide();
				}
			} else { // hide prep stuff
				if (prepPhaseGui != null) {
					prepPhaseGui.hide();
				}
				if (characterSelectButton != null) {
					characterSelectButton.setVisible(false);
				}
				if (characterSelectGui != null && characterSelectVisible) {
					hideCharacterSelectGui();
				}
				if (overlayManager != null) {
					overlayManager.show();
				}
			}
		});
	}

	public void showVotingUI(String reportedPlayerName, int reportedCharacterId, String reporterKey) {
		// Create the meeting UI
		MeetingUI meetingUI = new MeetingUI(this, reportedPlayerName, reporterKey);

		// Store reference
		this.activeMeetingUI = meetingUI;

		// Add it to the root node
		root.getChildren().add(meetingUI);

		// Make sure it's on top
		meetingUI.toFront();
	}

	public MeetingUI getActiveMeetingUI() {
		return activeMeetingUI;
	}

	public void clearActiveMeetingUI() {
		this.activeMeetingUI = null;
	}

	public double getWidth() {
		return screenWidth;
	}

	public double getHeight() {
		return screenHeight;
	}

	public static GameWindow getGameWindowInstance() {
		return gameWindowInstance;
	}

}
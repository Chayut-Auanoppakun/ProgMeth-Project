import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//THIS IS FOR TESTING PURPOSES ONLY 
public class Pseudo3DGame extends Application {
	private Image player;
	private Image background;
	private double playerX = 400;
	private double playerY = 300;
	private double speed = 5;
	private double backgroundX = 0;
	private double backgroundY = 0;
	private boolean up, down, left, right;

	private final double screenWidth = 1920;
	private final double screenHeight = 1080;

	// Add these new fields to track the actual world position
	private double worldX = 400;
	private double worldY = 300;

	private ScheduledExecutorService executorService;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("Pseudo-3D Game Demo");

		Group root = new Group();
		Canvas canvas = new Canvas(screenWidth, screenHeight);
		root.getChildren().add(canvas);

		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.show();

		GraphicsContext gc = canvas.getGraphicsContext2D();
		player = new Image(getClass().getResourceAsStream("/player.png"));
		background = new Image(getClass().getResourceAsStream("/background.png"));

		scene.setOnKeyPressed(this::handleKeyPress);
		scene.setOnKeyReleased(this::handleKeyRelease);

		executorService = Executors.newScheduledThreadPool(1);
		executorService.scheduleAtFixedRate(() -> {
			boolean canMoveBackground = update();
			draw(gc, canMoveBackground);
			logState();
		}, 0, 16, TimeUnit.MILLISECONDS); // Approximately 60 FPS

		// Stop the executor service when the application stops
		primaryStage.setOnCloseRequest(event -> executorService.shutdown());
	}

	private void handleKeyPress(KeyEvent event) {
		switch (event.getCode()) {
		case W:
			up = true;
			break;
		case S:
			down = true;
			break;
		case A:
			left = true;
			break;
		case D:
			right = true;
			break;
		}
	}

	private void handleKeyRelease(KeyEvent event) {
		switch (event.getCode()) {
		case W:
			up = false;
			break;
		case S:
			down = false;
			break;
		case A:
			left = false;
			break;
		case D:
			right = false;
			break;
		}
	}

	private boolean update() {
		// Calculate movement delta
		double dx = 0, dy = 0;
		if (up)
			dy -= speed;
		if (down)
			dy += speed;
		if (left)
			dx -= speed;
		if (right)
			dx += speed;

		// Update world position
		worldX += dx;
		worldY += dy;

		// Clamp world position to map bounds
		worldX = Math.max(0, Math.min(worldX, background.getWidth()));
		worldY = Math.max(0, Math.min(worldY, background.getHeight()));

		// Calculate ideal background position (center player in screen)
		double idealBackgroundX = worldX - screenWidth / 2;
		double idealBackgroundY = worldY - screenHeight / 2;

		// Clamp background position to valid bounds
		backgroundX = Math.max(0, Math.min(idealBackgroundX, background.getWidth() - screenWidth));
		backgroundY = Math.max(0, Math.min(idealBackgroundY, background.getHeight() - screenHeight));

		// Calculate player screen position
		if (idealBackgroundX < 0) {
			// Left edge of map
			playerX = worldX;
		} else if (idealBackgroundX > background.getWidth() - screenWidth) {
			// Right edge of map
			playerX = worldX - (background.getWidth() - screenWidth);
		} else {
			// Middle of map
			playerX = screenWidth / 2;
		}

		if (idealBackgroundY < 0) {
			// Top edge of map
			playerY = worldY;
		} else if (idealBackgroundY > background.getHeight() - screenHeight) {
			// Bottom edge of map
			playerY = worldY - (background.getHeight() - screenHeight);
		} else {
			// Middle of map
			playerY = screenHeight / 2;
		}

		// Return whether we're using centered or edge movement
		return (idealBackgroundX >= 0 && idealBackgroundX <= background.getWidth() - screenWidth
				&& idealBackgroundY >= 0 && idealBackgroundY <= background.getHeight() - screenHeight);
	}

	private void draw(GraphicsContext gc, boolean isCentered) {
		gc.clearRect(0, 0, screenWidth, screenHeight);

		// Draw background
		gc.drawImage(background, -backgroundX, -backgroundY, background.getWidth(), background.getHeight());

		// Draw player
		gc.drawImage(player, playerX - player.getWidth() / 2, playerY - player.getHeight() / 2);
	}

	private void logState() {
		System.out.println("World Position: (" + worldX + ", " + worldY + ")");
		System.out.println("Player Screen Position: (" + playerX + ", " + playerY + ")");
		System.out.println("Background Position: (" + backgroundX + ", " + backgroundY + ")");
		System.out.println("Key States - Up: " + up + ", Down: " + down + ", Left: " + left + ", Right: " + right);
	}
}

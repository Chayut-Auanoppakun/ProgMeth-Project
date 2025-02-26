package Test_Code;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.HashMap;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
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

public class NewGameWindow extends Application {
	private static final String FILE_TO_OPEN = "assets/map.tmx";

	private static final int MAX_CACHE_SIZE = 2000; // Increased for complex maps
	private HashMap<String, WritableImage> tileImageCache = new HashMap<>();
	private java.util.LinkedList<String> tileCacheOrder = new java.util.LinkedList<>();
	private Map map;
	private MapRenderer renderer;
	// Add these fields to your class
	public static void main(String[] args) {
		launch(args);
	}
	private double playerX = 0;
	private double playerY = 0;

	@Override
	public void start(Stage primaryStage) {
	    try {
	        System.out.println("Attempting to read the map...");
	        TMXMapReader mapReader = new TMXMapReader();
	        File file = new File(FILE_TO_OPEN);
	        URL url = file.toURI().toURL();
	        map = mapReader.readMap(url);
	        renderer = createRenderer(map);
	        System.out.println("Map loaded successfully.");
	    } catch (Exception e) {
	        System.out.println("Error while reading the map:\n" + e.getMessage());
	        e.printStackTrace(); // Print stack trace to get more details about the exception
	        return;
	    }

	    System.out.println(map.toString() + " loaded");

	    try {
	        Canvas canvas = new Canvas(800, 600); // Set canvas size to match viewport size
	        GraphicsContext gc = canvas.getGraphicsContext2D();
	        
	        double viewportWidth = 800;
	        double viewportHeight = 600;

	        drawMap(gc, playerX, playerY, viewportWidth, viewportHeight);

	        Pane root = new Pane(canvas);

	        Scene scene = new Scene(root);

	        // Add key event handlers for player movement
	        scene.setOnKeyPressed(event -> {
	            switch (event.getCode()) {
	                case W:
	                    playerY -= map.getTileHeight();
	                    break;
	                case S:
	                    playerY += map.getTileHeight();
	                    break;
	                case A:
	                    playerX -= map.getTileWidth();
	                    break;
	                case D:
	                    playerX += map.getTileWidth();
	                    break;
	                default:
	                    break;
	            }
	            drawMap(gc, playerX, playerY, viewportWidth, viewportHeight);
	        });

	        primaryStage.setTitle("TMX Viewer - JavaFX");
	        primaryStage.setScene(scene);
	        primaryStage.show();
	    } catch (Exception e) {
	        System.out.println("Error during canvas setup or rendering:\n" + e.getMessage());
	        e.printStackTrace(); // Print stack trace to get more details about the exception
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


	private void drawMap(GraphicsContext gc, double viewportX, double viewportY, double viewportWidth, double viewportHeight) {
	    gc.setFill(javafx.scene.paint.Color.GRAY);
	    gc.fillRect(0, 0, viewportWidth, viewportHeight);

	    for (MapLayer layer : map.getLayers()) {
	        if (layer instanceof TileLayer) {
	            System.out.println("Processing TileLayer: " + layer.getName());
	            drawTileLayer(gc, (TileLayer) layer, viewportX, viewportY, viewportWidth, viewportHeight);
	        } else if (layer instanceof ObjectGroup) {
	            System.out.println("Processing ObjectGroup: " + layer.getName());
	            drawObjectGroup(gc, (ObjectGroup) layer);
	        } else {
	            System.out.println("Unknown layer type: " + layer.getName());
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
	
	private void drawObjectGroup(GraphicsContext gc, ObjectGroup objectGroup) {
		gc.setStroke(javafx.scene.paint.Color.RED);
		for (org.mapeditor.core.MapObject object : objectGroup.getObjects()) {
			gc.strokeRect(object.getX(), object.getY(), object.getWidth(), object.getHeight());
		}
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
}

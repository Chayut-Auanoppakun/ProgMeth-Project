ackage Test_Code;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.w3c.dom.*;

import gameObjects.CollisionObject;

import javax.xml.parsers.*;
import java.io.File;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.Level;


class Tileset {
	int firstGid;
	Image image;
	int tileWidth;
	int tileHeight;
	int tileCount;
	int columns;
	String name;
	boolean isValid = false;
	private final List<CollisionObject> obstacles = new ArrayList<>();

    
	Tileset(int firstGid, Image image, int tileWidth, int tileHeight, int tileCount, int columns, String name) {
		this.firstGid = firstGid;
		this.image = image;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.tileCount = tileCount;
		this.columns = columns;
		this.name = name;

		// Validate the image was loaded correctly
		this.isValid = image != null && !image.isError();
		if (!isValid) {
			System.err.println("Warning: Failed to load tileset image for " + name);
		}
	}
}

class Layer {
	String name;
	int[][] tiles;
	boolean visible;
	float opacity;

	Layer(String name, int[][] tiles, boolean visible, float opacity) {
		this.name = name;
		this.tiles = tiles;
		this.visible = visible;
		this.opacity = opacity;
	}
}

public class Multiple_TSX extends Application {
	private static final int TILE_SIZE = 32;
	private static final int PLAYER_WIDTH = 32, PLAYER_HEIGHT = 48;
	private static final int VIEW_WIDTH = 640, VIEW_HEIGHT = 480;

	private double playerX = 300; // Starting position
	private double playerY = 350;
	private double cameraX = 0, cameraY = 0;
	private final double speed = 10;

	private final Set<KeyCode> keysPressed = new HashSet<>();
	private final List<CollisionObject> obstacles = new ArrayList<>();
	private List<Tileset> tilesets = new ArrayList<>();
	private List<Layer> layers = new ArrayList<>();
	private int mapWidth, mapHeight;
	private boolean debugCollisions = true; // Toggle for collision box visibility
    private static final Logger logger = Logger.getLogger(Multiple_TSX.class.getName());
    
	@Override
	public void start(Stage stage) {
		try {
            FileHandler fileHandler = new FileHandler("debug_log.txt", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
        } catch (Exception e) {
            e.printStackTrace();
        }
		Pane root = new Pane();
		Canvas canvas = new Canvas(VIEW_WIDTH, VIEW_HEIGHT);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		root.getChildren().add(canvas);

		// Ensure player image is loaded correctly
		Image playerImg = null;
		try {
			playerImg = new Image("file:assets/player.png");
			if (playerImg.isError()) {
				throw new Exception("Failed to load player image");
			}
		} catch (Exception e) {
			System.err.println("Error loading player image: " + e.getMessage());
			// Fallback to a colored rectangle if player image is missing
			playerImg = null;
		}

		final Image finalPlayerImg = playerImg; // Final reference for use in animation timer
		obstacles.clear();

		// Load map and tilesets
		loadMapData("assets/map.tmx");

		Scene scene = new Scene(root, VIEW_WIDTH, VIEW_HEIGHT);
		scene.setOnKeyPressed(event -> {
			keysPressed.add(event.getCode());

			// Toggle collision box visibility with F1
			if (event.getCode() == KeyCode.F1) {
				debugCollisions = !debugCollisions;
			}
		});
		scene.setOnKeyReleased(event -> keysPressed.remove(event.getCode()));

		AnimationTimer gameLoop = new AnimationTimer() {
			private long lastUpdate = 0;

			@Override
			public void handle(long now) {
				// Calculate delta time to ensure smooth movement regardless of frame rate
				if (lastUpdate == 0) {
					lastUpdate = now;
					return;
				}

				double deltaTime = (now - lastUpdate) / 1_000_000_000.0; // Convert to seconds
				lastUpdate = now;

				// Cap delta time to avoid huge jumps after lag
				if (deltaTime > 0.1)
					deltaTime = 0.1;

				// Calculate movement
				double dx = 0, dy = 0;
				if (keysPressed.contains(KeyCode.W))
					dy -= speed * deltaTime * 60;
				if (keysPressed.contains(KeyCode.S))
					dy += speed * deltaTime * 60;
				if (keysPressed.contains(KeyCode.A))
					dx -= speed * deltaTime * 60;
				if (keysPressed.contains(KeyCode.D))
					dx += speed * deltaTime * 60;

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

				// Update camera position
				cameraX = playerX + PLAYER_WIDTH / 2 - VIEW_WIDTH / 2;
				cameraY = playerY + PLAYER_HEIGHT / 2 - VIEW_HEIGHT / 2;

				// Prevent camera from showing outside the map
				cameraX = Math.max(0, Math.min(cameraX, mapWidth * TILE_SIZE - VIEW_WIDTH));
				cameraY = Math.max(0, Math.min(cameraY, mapHeight * TILE_SIZE - VIEW_HEIGHT));

				draw(gc, finalPlayerImg);
			}
		};
		for (Layer layer : layers) {
			System.out.println(layer.name);
		}

		gameLoop.start();
		stage.setScene(scene);
		stage.setTitle("JavaFX Tiled Map with Multiple Tilesets and Layers");
		stage.show();
	}

	private void draw(GraphicsContext gc, Image player) {
	    if (gc == null) {
	        logger.severe("GraphicsContext is null.");
	        return;
	    }
	    
	    gc.clearRect(0, 0, VIEW_WIDTH, VIEW_HEIGHT);
	    
	    gc.setFill(Color.BLACK);
	    gc.fillRect(0, 0, VIEW_WIDTH, VIEW_HEIGHT);
	    
	    int startX = Math.max(0, (int)(cameraX / TILE_SIZE));
	    int startY = Math.max(0, (int)(cameraY / TILE_SIZE));
	    int endX = Math.min(mapWidth, startX + (VIEW_WIDTH / TILE_SIZE) + 2);
	    int endY = Math.min(mapHeight, startY + (VIEW_HEIGHT / TILE_SIZE) + 2);
	    
	    for (Layer layer : layers) {
	        if (layer.visible) {
	            double oldGlobalAlpha = gc.getGlobalAlpha();
	            gc.setGlobalAlpha(layer.opacity);
	            
	            for (int y = startY; y < endY; y++) {
	                for (int x = startX; x < endX; x++) {
	                    if (y >= 0 && y < layer.tiles.length && x >= 0 && x < layer.tiles[y].length) {
	                        int tileId = layer.tiles[y][x];
	                        if (tileId > 0) {
	                            try {
	                                Tileset tileset = getTilesetForGid(tileId);
	                                if (tileset != null && tileset.isValid) {
	                                    if (tileset.image == null) {
	                                        logger.severe("Tileset image is null for tileset: " + tileset.name);
	                                        continue;
	                                    }
	                                    
	                                    int localId = tileId - tileset.firstGid;
	                                    if (localId >= 0 && localId < tileset.tileCount) {
	                                        int sx = (localId % tileset.columns) * tileset.tileWidth;
	                                        int sy = (localId / tileset.columns) * tileset.tileHeight;
	                                        if (sx >= 0 && sy >= 0 && sx + TILE_SIZE <= tileset.image.getWidth() && sy + TILE_SIZE <= tileset.image.getHeight()) {
	                                            double drawX = x * TILE_SIZE - cameraX;
	                                            double drawY = y * TILE_SIZE - cameraY;
	                                            //logger.info("Drawing tile: " + localId + " from tileset: " + tileset.name + " at " + x + "," + y);
	                                            gc.drawImage(tileset.image, sx, sy, TILE_SIZE, TILE_SIZE, drawX, drawY, TILE_SIZE, TILE_SIZE);
	                                        } else {
	                                            logger.severe("Invalid source coordinates for tile: " + localId + " in tileset: " + tileset.name);
	                                        }
	                                    } else {
	                                        logger.severe("Invalid localId for tileId: " + tileId + " in tileset: " + tileset.name);
	                                    }
	                                } else {
	                                    logger.severe("Invalid tileset for tileId: " + tileId);
	                                }
	                            } catch (Exception e) {
	                                logger.severe("Error rendering tile at " + x + "," + y + ": " + e.getMessage());
	                            }
	                        }
	                    }
	                }
	            }
	            gc.setGlobalAlpha(oldGlobalAlpha);
	        }
	    }
	    
	    if (player != null) {
	        gc.drawImage(player, playerX - cameraX, playerY - cameraY, PLAYER_WIDTH, PLAYER_HEIGHT);
	    } else {
	        gc.setFill(Color.RED);
	        gc.fillRect(playerX - cameraX, playerY - cameraY, PLAYER_WIDTH, PLAYER_HEIGHT);
	    }
	    
	    if (debugCollisions) {
	        gc.setStroke(Color.RED);
	        for (CollisionObject obj : obstacles) {
	            double drawX = obj.x - cameraX;
	            double drawY = obj.y - cameraY;
	            if (drawX + obj.width > 0 && drawX < VIEW_WIDTH && drawY + obj.height > 0 && drawY < VIEW_HEIGHT) {
	                gc.strokeRect(drawX, drawY, obj.width, obj.height);
	            }
	        }
	        
	        gc.setStroke(Color.GREEN);
	        double playerBoxX = playerX + 6 - cameraX;
	        double playerBoxY = playerY - cameraY;
	        double playerBoxWidth = PLAYER_WIDTH - 12;
	        double playerBoxHeight = PLAYER_HEIGHT - 6;
	        gc.strokeRect(playerBoxX, playerBoxY, playerBoxWidth, playerBoxHeight);
	    }
	}


	private Tileset getTilesetForGid(int gid) {
	    Tileset result = null;
	    
	    
	    for (Tileset tileset : tilesets) {
	        //System.out.println("Checking tileset: " + tileset.name + " with firstGid: " + tileset.firstGid);
	        //logger.info("Checking tileset: " + tileset.name + " with firstGid: " + tileset.firstGid);
	        if (gid >= tileset.firstGid && (result == null || tileset.firstGid > result.firstGid)) {
	            result = tileset;
	            //System.out.println("Found matching tileset: " + tileset.name);
	        }
	    }
	    
	    if (result == null) {
	        System.err.println("No matching tileset found for gid: " + gid);
	        logger.severe("No matching tileset found for gid: " + gid);
	    }
	    
	    if (result == null) {
	        // Handle the case where no matching tileset is found
	        // You can return a default Tileset or handle this case as needed
		    System.out.println("Searching for tileset with gid: " + gid);

	        System.err.println("Error: No matching tileset found for gid: " + gid);
            logger.severe("Error: No matching tileset found for gid: " + gid);
	        // For demonstration, we'll return a default empty Tileset
	        // You can customize this behavior as needed
	        result = new Tileset(-1, null, 0, 0, 0, 0, "Default");
	    }
	    
	    return result;
	}



	private void loadMapData(String tmxFilePath) {
	    try {
	        File file = new File(tmxFilePath);
	        if (!file.exists()) {
	            logger.severe("TMX file not found: " + tmxFilePath);
	            return;
	        }

	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        Document doc = builder.parse(file);
	        doc.getDocumentElement().normalize();

	        Element mapElement = doc.getDocumentElement();
	        mapWidth = Integer.parseInt(mapElement.getAttribute("width"));
	        mapHeight = Integer.parseInt(mapElement.getAttribute("height"));

	        logger.info("Map dimensions: " + mapWidth + "x" + mapHeight);

	        NodeList tilesetNodes = doc.getElementsByTagName("tileset");
	        logger.info("Found " + tilesetNodes.getLength() + " tilesets");

	        for (int i = 0; i < tilesetNodes.getLength(); i++) {
	            Element tilesetElement = (Element) tilesetNodes.item(i);
	            int firstGid = Integer.parseInt(tilesetElement.getAttribute("firstgid"));
	            logger.info("Tileset firstGid: " + firstGid);

	            if (tilesetElement.hasAttribute("source")) {
	                String tsxSource = tilesetElement.getAttribute("source");
	                logger.info("Loading external tileset: " + tsxSource + " with firstGid: " + firstGid);
	                loadExternalTileset(tsxSource, firstGid);
	            } else {
	                logger.info("Loading embedded tileset with firstGid: " + firstGid);
	                loadEmbeddedTileset(tilesetElement, firstGid);
	            }
	        }

	        NodeList layerList = doc.getElementsByTagName("layer");
	        logger.info("Found " + layerList.getLength() + " layers");

	        for (int i = 0; i < layerList.getLength(); i++) {
	            Element layerElement = (Element) layerList.item(i);
	            String layerName = layerElement.getAttribute("name");
	            logger.info("Loading layer: " + layerName);

	            boolean visible = !layerElement.hasAttribute("visible")
	                    || layerElement.getAttribute("visible").equals("1");

	            float opacity = layerElement.hasAttribute("opacity")
	                    ? Float.parseFloat(layerElement.getAttribute("opacity"))
	                    : 1.0f;

	            int[][] layerTiles = new int[mapHeight][mapWidth];

	            NodeList dataList = layerElement.getElementsByTagName("data");
	            if (dataList.getLength() > 0) {
	                Element dataElement = (Element) dataList.item(0);
	                String encoding = dataElement.hasAttribute("encoding") ? dataElement.getAttribute("encoding") : "";

	                if (encoding.equals("csv")) {
	                    String csvData = dataElement.getTextContent().trim();
	                    csvData = csvData.replace("\r\n", "\n").replace("\r", "\n");

	                    if (csvData.contains("\n")) {
	                        String[] rows = csvData.split("\n");
	                        int rowIndex = 0;

	                        for (String row : rows) {
	                            if (row.trim().isEmpty())
	                                continue;

	                            String[] tileValues = row.trim().split(",");
	                            for (int x = 0; x < Math.min(tileValues.length, mapWidth); x++) {
	                                try {
	                                    String trimmedValue = tileValues[x].trim();
	                                    if (!trimmedValue.isEmpty()) {
	                                        int tileId = Integer.parseInt(trimmedValue);
	                                        //logger.info("Parsed tileId: " + tileId + " at " + rowIndex + "," + x);
	                                        layerTiles[rowIndex][x] = tileId;
	                                    }
	                                } catch (NumberFormatException e) {
	                                    layerTiles[rowIndex][x] = 0;
	                                }
	                            }

	                            rowIndex++;
	                            if (rowIndex >= mapHeight)
	                                break;
	                        }
	                    } else {
	                        String[] tileValues = csvData.split(",");
	                        int index = 0;

	                        for (int y = 0; y < mapHeight; y++) {
	                            for (int x = 0; x < mapWidth; x++) {
	                                if (index < tileValues.length) {
	                                    try {
	                                        String trimmedValue = tileValues[index].trim();
	                                        if (!trimmedValue.isEmpty()) {
	                                            int tileId = Integer.parseInt(trimmedValue);
	                                            //logger.info("Parsed tileId: " + tileId + " at " + y + "," + x);
	                                            layerTiles[y][x] = tileId;
	                                        }
	                                    } catch (NumberFormatException e) {
	                                        layerTiles[y][x] = 0;
	                                    }
	                                }
	                                index++;
	                            }
	                        }
	                    }
	                } else {
	                    logger.warning("Warning: Unsupported encoding: " + encoding);
	                    String csvData = dataElement.getTextContent().trim();
	                    String[] tileValues = csvData.split(",");
	                    for (int y = 0; y < mapHeight; y++) {
	                        for (int x = 0; x < mapWidth; x++) {
	                            int index = y * mapWidth + x;
	                            if (index < tileValues.length) {
	                                try {
	                                    int tileId = Integer.parseInt(tileValues[index].trim());
	                                    //logger.info("Parsed tileId: " + tileId + " at " + y + "," + x);
	                                    layerTiles[y][x] = tileId;
	                                } catch (NumberFormatException e) {
	                                    layerTiles[y][x] = 0;
	                                }
	                            }
	                        }
	                    }
	                }
	            }

	            Layer layer = new Layer(layerName, layerTiles, visible, opacity);
	            layers.add(layer);
	        }

	        obstacles.addAll(loadCollisions("assets/map.tmx"));

	        logger.info("Map loaded successfully");
	        logger.info("Tilesets: " + tilesets.size());
	        logger.info("Layers: " + layers.size());
	        logger.info("Collision objects: " + obstacles.size());

	    } catch (Exception e) {
	        logger.severe("Error loading map data: " + e.getMessage());
	        e.printStackTrace();
	    }
	}


	private void loadExternalTileset(String tsxPath, int firstGid) {
	    try {
	        String basePath = "assets/";
	        File file = new File(basePath + tsxPath);
	        
	        if (!file.exists()) {
	            System.err.println("TSX file not found: " + basePath + tsxPath);
	            return;
	        }
	        
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        Document doc = builder.parse(file);
	        doc.getDocumentElement().normalize();
	        
	        Element tilesetElement = doc.getDocumentElement();
	        loadEmbeddedTileset(tilesetElement, firstGid);
	        
	    } catch (Exception e) {
	        System.err.println("Error loading external tileset: " + tsxPath);
	        e.printStackTrace();
	    }
	}


	private void loadEmbeddedTileset(Element tilesetElement, int firstGid) {
	    try {
	        String name = tilesetElement.getAttribute("name");
	        int tileWidth = Integer.parseInt(tilesetElement.getAttribute("tilewidth"));
	        int tileHeight = Integer.parseInt(tilesetElement.getAttribute("tileheight"));
	        
	        int tileCount = tilesetElement.hasAttribute("tilecount") ? 
	                      Integer.parseInt(tilesetElement.getAttribute("tilecount")) : 0;
	        int columns = tilesetElement.hasAttribute("columns") ? 
	                    Integer.parseInt(tilesetElement.getAttribute("columns")) : 0;
	        
	        NodeList imageNodes = tilesetElement.getElementsByTagName("image");
	        if (imageNodes.getLength() > 0) {
	            Element imageElement = (Element) imageNodes.item(0);
	            String imagePath = imageElement.getAttribute("source");
	            
	            if (tileCount == 0 || columns == 0) {
	                int imageWidth = Integer.parseInt(imageElement.getAttribute("width"));
	                int imageHeight = Integer.parseInt(imageElement.getAttribute("height"));
	                
	                if (columns == 0) {
	                    columns = imageWidth / tileWidth;
	                }
	                
	                if (tileCount == 0) {
	                    tileCount = (imageWidth / tileWidth) * (imageHeight / tileHeight);
	                }
	            }
	            
	            String fullPath = resolveImagePath(imagePath);
	            logger.info("Loading tileset image: " + fullPath);
	            
	            try {
	                Image tilesetImage = new Image("file:" + fullPath);
	                
	                if (tilesetImage.isError()) {
	                    throw new Exception("Failed to load image: " + fullPath);
	                }
	                
	                Tileset tileset = new Tileset(firstGid, tilesetImage, tileWidth, tileHeight, tileCount, columns, name);
	                logger.info("Loaded tileset: " + tileset.name + ", firstGid: " + tileset.firstGid + ", tileCount: " + tileset.tileCount);
	                tilesets.add(tileset);
	            } catch (Exception e) {
	                logger.severe("Error loading tileset image: " + e.getMessage());
	            }
	        } else {
	            logger.severe("No image element found in tileset: " + name);
	        }
	    } catch (Exception e) {
	        logger.severe("Error loading embedded tileset with firstGid: " + firstGid);
	        e.printStackTrace();
	    }
	}


	private String resolveImagePath(String imagePath) {
	    String basePath = "assets/";
	    
	    String[] possiblePaths = {
	        basePath + imagePath,
	        basePath + imagePath.replace("../", ""),
	        imagePath
	    };
	    
	    for (String path : possiblePaths) {
	        File file = new File(path);
	        if (file.exists()) {
	            return path;
	        }
	    }
	    
	    return basePath + imagePath;
	}


	private boolean checkCollision(double x, double y) {
		double nextLeft = x + 6;
		double nextRight = x + PLAYER_WIDTH - 12;
		double nextBottom = y + PLAYER_HEIGHT - 6;
		double nextFeetTop = nextBottom - 5;

		for (CollisionObject obj : obstacles) {
			double objLeft = obj.x;
			double objRight = obj.x + obj.width;
			double objTop = obj.y;
			double objBottom = obj.y + obj.height;

			if (nextRight > objLeft && nextLeft < objRight && nextBottom > objTop && nextFeetTop < objBottom) {
				return true;
			}
		}
		return false;
	}

	private List<CollisionObject> loadCollisions(String filePath) {
		List<CollisionObject> collisionObjects = new ArrayList<>();
		try {
			File file = new File(filePath);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(file);
			doc.getDocumentElement().normalize();

			// Look for objectgroup with name="object" instead
			NodeList groupList = doc.getElementsByTagName("objectgroup");
			for (int i = 0; i < groupList.getLength(); i++) {
				Element group = (Element) groupList.item(i);

				// Check if this is the collision layer (by name or id)
				if (group.getAttribute("name").equals("collision")) {
					// Now get all object elements within this group
					NodeList objectList = group.getElementsByTagName("object");
					for (int j = 0; j < objectList.getLength(); j++) {
						Element obj = (Element) objectList.item(j);
						double x = Double.parseDouble(obj.getAttribute("x"));
						double y = Double.parseDouble(obj.getAttribute("y"));
						double width = obj.hasAttribute("width") ? Double.parseDouble(obj.getAttribute("width")) : 0;
						double height = obj.hasAttribute("height") ? Double.parseDouble(obj.getAttribute("height")) : 0;

						collisionObjects.add(new CollisionObject(x, y, width, height));
						System.out.println("Added collision object: x=" + x + ", y=" + y + ", width=" + width
								+ ", height=" + height);
					}
					break; // Found the collision layer, no need to continue
				}
			}

			System.out.println("Loaded " + collisionObjects.size() + " collision objects");
		} catch (Exception e) {
			System.err.println("Error loading collision objects: " + e.getMessage());
			e.printStackTrace();
		}
		return collisionObjects;
	}

	public static void main(String[] args) {
		launch();
	}
}
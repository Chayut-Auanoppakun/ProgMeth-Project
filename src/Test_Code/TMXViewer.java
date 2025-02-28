package Test_Code;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.net.URL;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.mapeditor.core.Map;
import org.mapeditor.core.MapLayer;
import org.mapeditor.core.ObjectGroup;
import org.mapeditor.core.Tile;
import org.mapeditor.core.TileLayer;
import org.mapeditor.io.TMXMapReader;
import org.mapeditor.view.HexagonalRenderer;
import org.mapeditor.view.MapRenderer;
import org.mapeditor.view.OrthogonalRenderer;
import org.mapeditor.view.IsometricRenderer;

public class TMXViewer {

	public static void main(String[] args) {
		String fileToOpen = "assets/map.tmx";

		Map map;
		try {
			TMXMapReader mapReader = new TMXMapReader();
			File file = new File(fileToOpen);
			URL url = file.toURI().toURL();
			map = mapReader.readMap(url);
		} catch (Exception e) {
			System.out.println("Error while reading the map:\n" + e.getMessage());
			return;
		}

		System.out.println(map.toString() + " loaded");

		JScrollPane scrollPane = new JScrollPane(new MapView(map));
		scrollPane.setBorder(null);
		scrollPane.setPreferredSize(new Dimension(800, 600));

		JFrame appFrame = new JFrame("TMX Viewer");
		appFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		appFrame.setContentPane(scrollPane);
		appFrame.pack();
		appFrame.setVisible(true);
	}

	private static void printHelpMessage() {
		System.out.println("Java TMX Viewer\n" + "\n" + "Usage: java TMXViewer <tmx-file>\n");
	}
}

class MapView extends JPanel implements Scrollable {
	private final Map map;
	private final MapRenderer renderer;

	public MapView(Map map) {
		this.map = map;
		renderer = createRenderer(map);

		setPreferredSize(renderer.getMapSize());
		setOpaque(true);
	}

	@Override
	public void paintComponent(Graphics g) {
		final Graphics2D g2d = (Graphics2D) g.create();
		final Rectangle clip = g2d.getClipBounds();

		// Draw a gray background
		g2d.setPaint(new Color(100, 100, 100));
		g2d.fill(clip);

		// Draw each map layer
		for (MapLayer layer : map.getLayers()) {
			if (layer instanceof TileLayer) {
				TileLayer tileLayer = (TileLayer) layer;
				drawTileLayer(g2d, tileLayer);
			} else if (layer instanceof ObjectGroup) {
				renderer.paintObjectGroup(g2d, (ObjectGroup) layer);
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void drawTileLayer(Graphics2D g2d, TileLayer layer) {
		for (int x = 0; x < layer.getWidth(); x++) { // Updated here
			for (int y = 0; y < layer.getHeight(); y++) { // Updated here
				final Tile tile = layer.getTileAt(x, y);
				if (tile == null) {
					continue;
				}

				AffineTransform transform = new AffineTransform();
				int tileX = x * tile.getWidth();
				int tileY = y * tile.getHeight();

				// Translate to the tile position
				transform.translate(tileX, tileY);

				// Apply flipping and rotation
				boolean flipHorizontally = layer.isFlippedHorizontally(x, y);
				boolean flipVertically = layer.isFlippedVertically(x, y);
				boolean flipDiagonally = layer.isFlippedDiagonally(x, y);

				if (flipHorizontally && flipVertically) {
					transform.translate(tile.getWidth(), tile.getHeight());
					transform.rotate(Math.PI);
				} else if (flipHorizontally) {
					transform.translate(tile.getWidth(), 0);
					transform.scale(-1, 1);
				} else if (flipVertically) {
					transform.translate(0, tile.getHeight());
					transform.scale(1, -1);
				}

				if (flipDiagonally) {
					transform.translate(tile.getWidth(), 0);
					transform.rotate(Math.PI / 2);
				}

				g2d.drawImage(tile.getImage(), transform, null);
			}
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

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		if (orientation == SwingConstants.HORIZONTAL)
			return map.getTileWidth();
		else
			return map.getTileHeight();
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		if (orientation == SwingConstants.HORIZONTAL) {
			final int tileWidth = map.getTileWidth();
			return (visibleRect.width / tileWidth - 1) * tileWidth;
		} else {
			final int tileHeight = map.getTileHeight();
			return (visibleRect.height / tileHeight - 1) * tileHeight;
		}
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return false;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}
}

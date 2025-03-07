package gameObjects;

/**
 * Abstract base class for all game objects that provides position and dimension properties.
 */
public abstract class BaseObject {
    private double x, y, width, height;

    /**
     * Constructor to initialize a base object with position and dimensions.
     * 
     * @param x The x-coordinate of the object
     * @param y The y-coordinate of the object
     * @param width The width of the object
     * @param height The height of the object
     */
    public BaseObject(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }
    
    /**
     * Returns a string representation of this object.
     * 
     * @return A string representation of the object
     */
    @Override
    public abstract String toString();
}
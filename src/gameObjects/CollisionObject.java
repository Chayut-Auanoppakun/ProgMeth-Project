package gameObjects;


public class CollisionObject extends BaseObject {

   
    public CollisionObject(double x, double y, double width, double height) {
        super(x, y, width, height);
    }
    
    @Override
    public String toString() {
        return "CollisionObject at (" + getX() + "," + getY() + ") with size " + 
               getWidth() + "x" + getHeight();
    }
}
package gameObjects;


public class eventObject extends BaseObject {
    private String EventID;

    public eventObject(double x, double y, double width, double height, String EventID) {
        super(x, y, width, height);
        this.EventID = EventID;
    }

    public String getEventID() {
        return EventID;
    }
    
    @Override
    public String toString() {
        return "eventObject(ID: " + EventID + ") at (" + getX() + "," + getY() + 
               ") with size " + getWidth() + "x" + getHeight();
    }
}
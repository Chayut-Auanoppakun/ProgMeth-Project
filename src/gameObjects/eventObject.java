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

//	public void setEventID(String eventID) {
//		EventID = eventID;
//	}
    
    @Override
    public String toString() {
    	return super.toString();
    }
}

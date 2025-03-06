package server;

import java.net.InetAddress;

public class PlayerInfo extends ClientInfo {
	private double x;
	private double y;
	public String name;
	private String status;
	private int Direction;
	private boolean isMoving;
	private int CharacterID;
	private boolean isReady = false;
	private double taskPercent = 0;
	// New attributes for body tracking
	private boolean isFound = false;
	
	public PlayerInfo(InetAddress address, int port, String name, double x, double y, boolean isMoving, int Direction,
			String status, int CharID) {
		super(address, port, name);
		this.x = x;
		this.y = y;
		this.name = name;
		this.isMoving = isMoving;
		this.Direction = Direction;
		this.status = status;
		this.CharacterID = CharID;
	}

	public double getTaskPercent() {
		return taskPercent;
	}

	public void setTaskPercent(double taskPercent) {
		this.taskPercent = taskPercent;
	}

	public boolean isFound() {
		return isFound;
	}

	public void setFound(boolean found) {
		isFound = found;
	}

	// Existing getters and setters remain the same
	public boolean isReady() {
		return isReady;
	}

	public void setReady(boolean isReady) {
		this.isReady = isReady;
	}

	public int getCharacterID() {
		return CharacterID;
	}

	public void setCharacterID(int characterID) {
		CharacterID = characterID;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getDirection() {
		return Direction;
	}

	public void setDirection(int direction) {
		Direction = direction;
	}

	public boolean isMoving() {
		return isMoving;
	}

	public void setMoving(boolean isMoving) {
		this.isMoving = isMoving;
	}

	public String getName() {
		return name;
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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String toString() {
		return this.getAddress() + ":" + this.getPort();
	}
}
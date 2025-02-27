package server;

import java.net.InetAddress;

public class PlayerInfo extends ClientInfo {
    @Override
	public String toString() {
		return this.getAddress() + ":" + this.getPort();
	}

	private double x;
    private double y;
    public String name;
    private String status;
    private int Direction;
    private boolean isMoving;

    public PlayerInfo(InetAddress address, int port, String name, double x, double y,boolean isMoving, int Direction, String status) {
        super(address, port, name);
        this.x = x;
        this.y = y;
        this.name = name;
        this.isMoving = isMoving;
        this.Direction = Direction;
        this.status = status;
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
}

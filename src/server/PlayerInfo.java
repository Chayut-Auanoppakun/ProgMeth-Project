package server;

import java.net.InetAddress;

public class PlayerInfo extends ClientInfo {
    private double x;
    private double y;
    public String name;
    private int score;
    private String status;

    public PlayerInfo(InetAddress address, int port, String name, double x, double y, int score, String status) {
        super(address, port, name);
        this.x = x;
        this.y = y;
        this.name = name;
        this.score = score;
        this.status = status;
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

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

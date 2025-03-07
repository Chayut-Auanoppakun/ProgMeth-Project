package gameObjects;

import server.PlayerInfo;

public class Corpse extends BaseObject {
    private final String playerName;
    private final int characterID;
    private boolean isFound;
    private final String playerKey; // Unique identifier for the player

    public Corpse(PlayerInfo player) {
        super(player.getX(), player.getY(), 48, 64); // Standard player dimensions
        this.playerName = player.getName();
        this.characterID = player.getCharacterID();
        this.playerKey = player.getAddress().getHostAddress() + ":" + player.getPort();
        this.isFound = false;
    }

    // Getters
    public String getPlayerName() {
        return playerName;
    }

    public int getCharacterID() {
        return characterID;
    }

    public boolean isFound() {
        return isFound;
    }

    public void setFound(boolean found) {
        this.isFound = found;
    }

    public String getPlayerKey() {
        return playerKey;
    }

    @Override
    public String toString() {
        return "Corpse{" +
               "playerName='" + playerName + '\'' +
               ", characterID=" + characterID +
               ", isFound=" + isFound +
               ", x=" + getX() +
               ", y=" + getY() +
               '}';
    }

}
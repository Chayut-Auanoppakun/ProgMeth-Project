package logic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

import application.Main;
import server.PlayerInfo;

public class GameLogic {
	static int  ImposterCount;
	public static ConcurrentHashMap<String, PlayerInfo> playerList = new ConcurrentHashMap<>();
	
    public static boolean gameStarted() {
    	return true;
    }
	public GameLogic() {
        System.out.println("GameLogic initialized.");
    }

    public static void reportDeadBody(String playerName) {
    	//check first if there is a body near
        System.out.println("Dead body reported by: " + playerName);
        //meeting start
    }
    public static void imposterVentEnter(String playerName) {
        System.out.println(playerName+" Entered vent");
        //imposter enter vent
    }
    

}
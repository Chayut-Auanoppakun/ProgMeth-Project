package logic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;

import application.Main;

public class GameLogic {
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
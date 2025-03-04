package logic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

import application.Main;
import gui.MainMenuPane;
import server.PlayerInfo;

public class GameLogic {
	private static int ImposterCount;

	public static ConcurrentHashMap<String, PlayerInfo> playerList = new ConcurrentHashMap<>();
	private static boolean prepEnded = false;

	public GameLogic() {
		System.out.println("GameLogic initialized.");
	}

	public static boolean isPrepEnded() {
		return prepEnded;
	}

	public static void setPrepEnded(boolean prepEnded) {
		GameLogic.prepEnded = prepEnded;
	}

	public static void reportDeadBody(String playerName) {
		// check first if there is a body near
		System.out.println("Dead body reported by: " + playerName);
		// meeting start
	}

	public static void imposterVentEnter(String playerName) {
		System.out.println(playerName + " Entered vent");
		// imposter enter vent
	}

	public static void setImposterCount(int count) {
		if (count < Math.floor(playerList.size() / 2)) { // imposter must not be more than 1/2 of player
			ImposterCount = count;
		}
	}

	public static int autoImposterCount() {
		int count = playerList.size() / 4;
		if (count < 1) {
			count = 1;
		}
		setImposterCount(count);
		return ImposterCount;
	}

	public static int getImposterCount() {
		return ImposterCount;
	}

}
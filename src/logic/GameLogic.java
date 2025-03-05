package logic;

import java.util.concurrent.ConcurrentHashMap;
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
		ImposterCount = count;
	}

	public static int autoImposterCount() {
		int count = playerList.size() / 4;
		if (count < 1) {
			count = 1;
		}
		setImposterCount(count);
		System.out.println(playerList.size());
		System.out.println(ImposterCount);
		return ImposterCount;
	}

	public static int getImposterCount() {
		return ImposterCount;
	}

}
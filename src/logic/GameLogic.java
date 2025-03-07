package logic;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import gameObjects.Corpse;
import gui.GameWindow;
import gui.MainMenuPane;
import javafx.application.Platform;
import server.PlayerInfo;

public class GameLogic {
	private static int ImposterCount;
	private static int AlivePlayers;
	private static int AliveCrewMates;
	private static int AliveImposters;
	private static boolean gameEnded = false;
	private static GameResult gameResult = GameResult.ONGOING;
	public static ConcurrentHashMap<String, Corpse> corpseList = new ConcurrentHashMap<>();
	public static ConcurrentHashMap<String, PlayerInfo> playerList = new ConcurrentHashMap<>();
	private static boolean prepEnded = false;
	private static ScheduledExecutorService gameLoopExecutor;
	private static boolean isImposterRolesSet = false;

	// Cooldown related variables
	private static int KillCooldown = 25; // Kill cooldown in seconds
	private static int EmergencyMeetingCooldown = 10; // Emergency meeting cooldown in seconds
	private static long lastKillTime = 0;
	private static long lastEmergencyMeetingTime = 0;

	private static int taskAmount = 5;
	private static float SFXVolume = 0; // 0 is max -- to make quiet
	private static float MUSICVolume = 0;

	public enum GameResult {
		ONGOING, CREWMATE_WIN, IMPOSTER_WIN
	}

	public static boolean isImposterRolesSet() {
		return isImposterRolesSet;
	}

	public static void setImposterRolesSet(boolean isImposterRolesSet) {
		GameLogic.isImposterRolesSet = isImposterRolesSet;
	}

	public static float getSFXVolume() {
		return SFXVolume;
	}

	public static void setSFXVolume(float sFXVolume) {
		SFXVolume = sFXVolume;
	}

	public static float getMUSICVolume() {
		return MUSICVolume;
	}

	public static void setMUSICVolume(float mUSICVolume) {
		MUSICVolume = mUSICVolume;
	}

	public static int getTaskAmount() {
		return taskAmount;
	}

	public static void setTaskAmount(int ntaskAmount) {
		taskAmount = ntaskAmount;
	}

	public GameLogic() {
		System.out.println("GameLogic initialized.");
	}

	public static void checkGameConditions() {
		// Count alive players and imposters
		updatePlayerCounts();

		if (!GameLogic.prepEnded||!isImposterRolesSet)
			return;
		// Check win conditions
		if (checkImpostorWinCondition()) {
			System.out.println("IMPOSTER WINS");
			endGame(GameResult.IMPOSTER_WIN);
		} else if (checkCrewmateWinCondition()) {
			System.out.println("CREWMATE WINS");
			endGame(GameResult.CREWMATE_WIN);
		}
	}

	private static void updatePlayerCounts() {
		int totalPlayers = 0;
		int aliveImposters = 0;
		int aliveCrewmates = 0;

		if (PlayerLogic.getStatus().equals("dead")) {
			totalPlayers++;
		} else if (PlayerLogic.getStatus().equals("imposter")) {
			aliveImposters++;
		} else {
			aliveCrewmates++;
		}

		for (PlayerInfo player : playerList.values()) {
			// Assuming each player has a "status" field that tracks their alive/dead state
			if (!player.getStatus().equals("dead")) {
				totalPlayers++;

				if (player.getStatus().equals("imposter")) {
					aliveImposters++;
				} else {
					aliveCrewmates++;
				}
			}
		}
		AliveCrewMates = aliveCrewmates;
		AlivePlayers = totalPlayers;
		AliveImposters = aliveImposters;
//		System.out.println("ALIVE PLAYERS = " + totalPlayers);
//		System.out.println("ALIVE IMPOSTERS = " + aliveImposters);
	}

	private static boolean checkImpostorWinCondition() {
		// Imposters win if they equal or outnumber crewmates
		return AliveImposters >= AliveCrewMates;
	}

	private static boolean checkCrewmateWinCondition() {
		// Crewmates win if all tasks are complete and at least one crewmate is alive
		return (GameWindow.getTotalPercentage() >= 100) || (AliveImposters == 0);
	}

	private static void endGame(GameResult result) {
		if (gameEnded || !GameLogic.isPrepEnded())
			return;

		gameEnded = true;
		gameResult = result;

		// Shutdown the game loop executor
		if (gameLoopExecutor != null) {
			gameLoopExecutor.shutdown();
		}

		// Run game end logic on JavaFX thread
		Platform.runLater(() -> {
			// Get the GameWindow instance and show the result screen
			GameWindow gameWindowInstance = GameWindow.getGameWindowInstance();
			if (gameWindowInstance != null) {
				gameWindowInstance.showGameResultScreen();
			}
		});
	}

//		// Run game end logic on JavaFX thread
//		Platform.runLater(() -> {
//			switch (result) {
//			case IMPOSTER_WIN:
//				System.out.println("GAME OVER: Imposters Win!");
//				// Add game end UI or logic for imposter victory
//				break;
//			case CREWMATE_WIN:
//				System.out.println("GAME OVER: Crewmates Win!");
//				// Add game end UI or logic for crewmate victory
//				break;
//			}
//		});
//	}

	// Cooldown management methods
	public static int getKillCooldown() {
		return KillCooldown;
	}

	public static void setKillCooldown(int killCooldown) {
		KillCooldown = killCooldown;
	}

	public static boolean isKillAvailable() {
		long currentTime = System.currentTimeMillis() / 1000; // Convert to seconds
		return currentTime - lastKillTime >= KillCooldown;
	}

	public static void recordKillTime() {
		lastKillTime = System.currentTimeMillis() / 1000; // Convert to seconds
	}

	public static int getRemainingKillCooldown() {
		long currentTime = System.currentTimeMillis() / 1000;
		int remainingCooldown = KillCooldown - (int) (currentTime - lastKillTime);
		return Math.max(0, remainingCooldown);
	}

	public static int getEmergencyMeetingCooldown() {
		return EmergencyMeetingCooldown;
	}

	public static void setEmergencyMeetingCooldown(int cooldown) {
		EmergencyMeetingCooldown = cooldown;
	}

	// Reset emergency meeting cooldown when game prep ends
	public static void resetEmergencyMeetingCooldown() {
		lastEmergencyMeetingTime = 0; // This ensures the first meeting is always available
	}

	public static boolean isEmergencyMeetingAvailable() {
		long currentTime = System.currentTimeMillis() / 1000; // Convert to seconds
		// If lastEmergencyMeetingTime is 0, it means the first meeting is always
		// available
		return lastEmergencyMeetingTime == 0 || currentTime - lastEmergencyMeetingTime >= EmergencyMeetingCooldown;
	}

	public static void recordEmergencyMeetingTime() {
		lastEmergencyMeetingTime = System.currentTimeMillis() / 1000; // Convert to seconds
	}

	public static int getRemainingEmergencyMeetingCooldown() {
		long currentTime = System.currentTimeMillis() / 1000;
		// If lastEmergencyMeetingTime is 0, there's no cooldown
		if (lastEmergencyMeetingTime == 0) {
			return 0;
		}
		int remainingCooldown = EmergencyMeetingCooldown - (int) (currentTime - lastEmergencyMeetingTime);
		return Math.max(0, remainingCooldown);
	}

	// Existing methods remain the same
	public static boolean isPrepEnded() {
		return prepEnded;
	}

	public static void setPrepEnded(boolean prepEnded) {
		GameLogic.prepEnded = prepEnded;
	}

	public static void reportDeadBody(String playerName) {
		System.out.println("Dead body reported by: " + playerName);
		// Add meeting start logic
	}

	public static void imposterVentEnter(String playerName) {
		System.out.println(playerName + " Entered vent");
		// Add vent enter logic
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

	// Getter for game result
	public static GameResult getGameResult() {
		return gameResult;
	}

	// Getter for game ended status
	public static boolean isGameEnded() {
		return gameEnded;
	}

	public static Corpse createCorpse(PlayerInfo player) {
		String playerKey = player.getAddress().getHostAddress() + ":" + player.getPort();

		// Check if a corpse already exists for this player
		if (corpseList.containsKey(playerKey)) {
			System.out.println("GAMELOGIC: Corpse already exists for player " + player.getName());
			return corpseList.get(playerKey);
		}

		System.out.println(
				"GAMELOGIC: Creating corpse for " + player.getName() + " at " + player.getX() + "," + player.getY());
		Corpse corpse = new Corpse(player);
		corpseList.put(playerKey, corpse);
		System.out.println("GAMELOGIC: Corpse created, total corpses: " + corpseList.size());
		return corpse;
	}

	public static Corpse getCorpse(String playerKey) {
		return corpseList.get(playerKey);
	}

	public static void removeCorpse(String playerKey) {
		corpseList.remove(playerKey);
	}

	public static void clearCorpses() {
		corpseList.clear();
	}

	public static int getCorpseCount() {
		return corpseList.size();
	}

	public static int getFoundCorpseCount() {
		return (int) corpseList.values().stream().filter(Corpse::isFound).count();
	}
}
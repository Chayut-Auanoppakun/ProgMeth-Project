package logic;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import gameObjects.Corpse;
import javafx.application.Platform;
import server.PlayerInfo;

public class GameLogic {
	private static int ImposterCount;
	private static int TaskPercent;
	private static int AlivePlayers;
	private static int AliveImposters;
	private static boolean gameEnded = false;
	private static GameResult gameResult = GameResult.ONGOING;
	public static ConcurrentHashMap<String, Corpse> corpseList = new ConcurrentHashMap<>();
	public static ConcurrentHashMap<String, PlayerInfo> playerList = new ConcurrentHashMap<>();
	private static boolean prepEnded = false;
	private static ScheduledExecutorService gameLoopExecutor;

	public enum GameResult {
		ONGOING, CREWMATE_WIN, IMPOSTER_WIN
	}

	public GameLogic() {
		System.out.println("GameLogic initialized.");
	}

	public static void startGameLoop() {
		if (gameLoopExecutor != null) {
			gameLoopExecutor.shutdown();
		}

		// Reset game state
		gameEnded = false;
		gameResult = GameResult.ONGOING;

		gameLoopExecutor = Executors.newSingleThreadScheduledExecutor();

		// Run game loop every second
		gameLoopExecutor.scheduleAtFixedRate(() -> {
			if (!gameEnded) {
				checkGameConditions();
			}
		}, 0, 1, TimeUnit.SECONDS);
	}

	private static void checkGameConditions() {
		// Count alive players and imposters
		updatePlayerCounts();

		// Check win conditions
		if (checkImpostorWinCondition()) {
			endGame(GameResult.IMPOSTER_WIN);
		} else if (checkCrewmateWinCondition()) {
			endGame(GameResult.CREWMATE_WIN);
		}
	}

	private static void updatePlayerCounts() {
		int totalPlayers = 0;
		int aliveImposters = 0;
		int aliveCrewmates = 0;

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

		AlivePlayers = totalPlayers;
		AliveImposters = aliveImposters;
	}

	private static boolean checkImpostorWinCondition() {
		// Imposters win if they equal or outnumber crewmates
		return AliveImposters >= (AlivePlayers / 2);
	}

	private static boolean checkCrewmateWinCondition() {
		// Crewmates win if all tasks are complete and at least one crewmate is alive
		return (TaskPercent >= 100) || (AliveImposters == 0);
	}

	private static void endGame(GameResult result) {
		if (gameEnded)
			return;

		gameEnded = true;
		gameResult = result;

		// Shutdown the game loop executor
		if (gameLoopExecutor != null) {
			gameLoopExecutor.shutdown();
		}

		// Run game end logic on JavaFX thread
		Platform.runLater(() -> {
			switch (result) {
			case IMPOSTER_WIN:
				System.out.println("GAME OVER: Imposters Win!");
				// Add game end UI or logic for imposter victory
				break;
			case CREWMATE_WIN:
				System.out.println("GAME OVER: Crewmates Win!");
				// Add game end UI or logic for crewmate victory
				break;
			}
		});
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

	// New method to update task percentage
	public static void updateTaskPercentage(double percentage) {
		TaskPercent = (int) percentage;
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

	public static String findReportableCorpseNearby(double x, double y, double maxDistance) {
		for (Corpse corpse : corpseList.values()) {
			// Check for corpses that haven't been found yet
			if (!corpse.isFound()) {
				double dx = corpse.getX() - x;
				double dy = corpse.getY() - y;
				double distance = Math.sqrt(dx * dx + dy * dy);

				if (distance <= maxDistance) {
					return corpse.getPlayerKey();
				}
			}
		}
		return null;
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
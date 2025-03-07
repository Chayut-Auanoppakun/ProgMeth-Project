package logic;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import gui.MainMenuPane;

//Logic for all player both client and server
//For our own Player
//In PlayerLogic.java, modify the status handling to support different death types

public class PlayerLogic  {
	static boolean isMoving;
	static int Direction; // 1 left, 2 Right
	private static String name;
	private static double myPosX = 0;
	private static double myPosY = 0;
	private static int charID = 99; // 99 is not initialized
	private static boolean isPlayerReady = false;
	private static boolean temporaryKilled = false;
	private static boolean temporaryEjected = false;
	private static boolean wasImposter = false;
	// Modified status to be more specific
	private static String status = "crewmate";

	private static boolean playdeadsound = false;
	private static Set<Integer> tasks = new HashSet<>();
	private static double taskPercent = 0;

	public static void finalizeDeathState() {
		if (temporaryKilled || temporaryEjected) {
			System.out.println("PLAYERLOGIC: Finalizing death state to 'dead'");
			temporaryKilled = false;
			temporaryEjected = false;
			if (status.equals("imposter")) {
				wasImposter = true;
			}
			status = "dead"; // Set final status to dead for compatibility
		}

	}

	public static boolean isWasImposter() {
		return wasImposter;
	}

	public static void setWasImposter(boolean wasImposter) {
		PlayerLogic.wasImposter = wasImposter;
	}

	public static boolean isBeingKilled() {
		return temporaryKilled;
	}

	public static boolean isBeingEjected() {
		return temporaryEjected;
	}

	public static void flagKilled(boolean killed) {
		temporaryKilled = killed;
		if (killed) {
			System.out.println("PLAYERLOGIC: Player flagged as being killed");
		}
	}

	public static void flagEjected(boolean ejected) {
		temporaryEjected = ejected;
		if (ejected) {
			System.out.println("PLAYERLOGIC: Player flagged as being ejected");
		}
	}

	public static void setStatus(String newstatus) {
		System.out.println("PLAYERLOGIC: Player status changing from " + PlayerLogic.status + " to " + newstatus);

		// Special handling for death states
		if ("dead".equals(newstatus)) {
			if (temporaryEjected) {
				System.out.println("PLAYERLOGIC: Player is in ejection animation, deferring 'dead' status");
				// Don't set status yet, let finalizeDeathState do it after animation
				return;
			} else if (!temporaryKilled) {
				// If no temporary death state is set, assume killed
				temporaryKilled = true;
				System.out.println("PLAYERLOGIC: Assuming player was killed (no ejection flag)");

				// Play death sound for kills
				if (!playdeadsound) {
					playdeadsound = true;
					SoundLogic.playSound("assets/sounds/impostor_kill.wav", 0);
				}
			}
		} else {
			// For other statuses (crewmate, imposter), set them directly
			status = newstatus;
		}
	}

	public static Set<Integer> getTasks() {
		return tasks;
	}

	public static void setTasks(Set<Integer> tasks) {
		PlayerLogic.tasks = tasks;
	}

	public static double getTaskPercent() {
		return taskPercent;
	}

	public static void setTaskPercent(double taskPercent) {
		PlayerLogic.taskPercent = taskPercent;
	}

	public static String getStatus() {
		return status;
	}

	// For compatibility with existing code that checks for "dead" status
	public static boolean isDeadStatus(String statusToCheck) {
		return "dead".equals(statusToCheck) || "killed".equals(statusToCheck) || "ejected".equals(statusToCheck);
	}

	private static boolean wasEjected = false;

	public static boolean wasEjected() {
		return wasEjected;
	}

	// For compatibility, check if player is dead or in a death animation
	public static boolean isEffectivelyDead() {
		return "dead".equals(status) || temporaryKilled || temporaryEjected;
	}

	public static void updateTaskPercent() {
		int taskfinished = GameLogic.getTaskAmount() - tasks.size();
		taskPercent = ((double) taskfinished / GameLogic.getTaskAmount()) * 100;
	}

	public static boolean isPlayerReady() {
		return isPlayerReady;
	}

	public static void setPlayerReady(boolean isPlayerReady) {
		PlayerLogic.isPlayerReady = isPlayerReady;
	}

	public static String getName() {
		return name;
	}

	public static void setName(String name) {
		PlayerLogic.name = name;
	}

	public static int getCharID() {
		return charID;
	}

	public static void setCharID(int charID) {
		PlayerLogic.charID = charID;
	}

	public static void isMoving(boolean newMoving, int newDirection) {
		isMoving = newMoving;
		Direction = newDirection;

		if (isMoving) {
			SoundLogic.playWalkingSound();
		}
	}

	public static double getMyPosX() {
		return myPosX;
	}

	public static double getMyPosY() {
		return myPosY;
	}

	public static void setPosition(double x, double y) {
		myPosX = x;
		myPosY = y;
	}

	public static int getDirection() {
		return Direction;
	}

	public static boolean getMoving() {
		return isMoving;
	}

	public static String getLocalAddressPort() {
		DatagramSocket socket;
		if (MainMenuPane.getState().equals(logic.State.SERVER)) {
			socket = ServerLogic.getServerSocket();
		} else {
			socket = ClientLogic.getClientSocket();
		}
		if (socket == null) {
			return "unknown:0";
		}
		try {
			InetAddress localAddress = InetAddress.getLocalHost();
			int localPort = socket.getLocalPort();
			return localAddress.getHostAddress() + ":" + localPort;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return "unknown:0";
		}
	}

	public static void randomizeTasks(int Amount) {
		Random random = new Random();
		while (tasks.size() < Amount) {
			tasks.add(random.nextInt(1, 22)); // 1-23
		}
		System.out.println("Random Tasks : " + tasks);
	}



	public static void resetPlayerState() {
		// Movement and position
		isMoving = false;
		Direction = 2; // Default direction
		myPosX = 0;
		myPosY = 0;

		// Player identification and name
		name = "Player";
		charID = 99; // Not initialized state

		// Player status
		status = "crewmate";
		isPlayerReady = false;

		// Death-related flags
		temporaryKilled = false;
		temporaryEjected = false;
		wasImposter = false;
		wasEjected = false;
		playdeadsound = false;

		// Task-related variables
		tasks.clear();
		taskPercent = 0;
	}
}
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
public class PlayerLogic {
	static boolean isMoving;
	static int Direction; // 1 left, 2 Right
	private static String name;
	private static double myPosX = 0;
	private static double myPosY = 0;
	private static int charID = 99; // 99 is not initialized
	private static boolean isPlayerReady = false;
	private static String status = "crewmate";
	private static boolean playdeadsound = false;
	private static Set<Integer> tasks = new HashSet<>();
	
	
	public static Set<Integer> getTasks() {
		return tasks;
	}

	public static void setTasks(Set<Integer> tasks) {
		PlayerLogic.tasks = tasks;
	}

	private static double taskPercent = 0;

	public static double getTaskPercent() {
		return taskPercent;
	}

	public static void setTaskPercent(double taskPercent) {
		PlayerLogic.taskPercent = taskPercent;
	}

	public static String getStatus() {
		// System.out.println("Sending out Status : " + status);
		return status;
	}

	public static void setStatus(String newstatus) {
		System.out.println("PLAYERLOGIC: Player status changed from " + PlayerLogic.status + " to " + newstatus);
		status = newstatus;

		// When player is killed, play a death sound
		if ("dead".equals(status) && !playdeadsound) {
			playdeadsound = true;
			// Play death sound
			SoundLogic.playSound("assets/sounds/impostor_kill.wav", 0);
		}
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
			tasks.add(random.nextInt(1, 13)); // 1-13
		}
		System.out.println("Random Tasks : " + tasks);
	}

}
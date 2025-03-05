package logic;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

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
	private static String status;

	public static String getStatus() {
		return status;
	}

	public static void setStatus(String status) {
		PlayerLogic.status = status;
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

	// For Imposters
	public void KillPlayer() {
		if (MainMenuPane.getState().equals(logic.State.SERVER)) {

		} else {

		}
	}
}
package logic;

//Logic for all player both client and server

public class PlayerLogic {
	static boolean isMoving;
	static int Direction; //1 left, 2 Right
	private static double myPosX = 0;
	private static double myPosY = 0;
	
	public static void isMoving(boolean newMoving, int newDirection) {
		isMoving = newMoving;
		Direction = newDirection;
	
		if(isMoving) {
			SoundLogic.playWalkingSound();
		}
	}

	public static double getMyPosX() {
		return myPosX;
	}

	public static double getMyPosY() {
		return myPosY;
	}

	public static void setPosition(double x,double y) {
		myPosX = x;
		myPosY = y;
	}
	
	
	public static int getDirection() {
		return Direction;
	}
	
	public static boolean getMoving() {
		return isMoving;
	}
}
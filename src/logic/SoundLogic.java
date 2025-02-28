package logic;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import gui.MainMenuPane;

import javax.sound.sampled.AudioInputStream;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import server.PlayerInfo;

public class SoundLogic {
	private static final double SOUND_RANGE = 100.0;
	private static final String WALK_SOUND_PATH = "assets/sounds/FootstepCarpet01.wav";
	private static final String OTHER_WALK_SOUND_PATH = "assets/sounds/FootstepTile01.wav";
	private static long myLastPlayed = 0;
	private static long otherLastPlayed = 0;

	public static void checkAndPlayWalkingSounds(PlayerInfo player) throws UnknownHostException {
        String myKey = "";
        if (MainMenuPane.getState().equals(logic.State.SERVER)) {
            myKey = ServerLogic.getLocalAddressPort();
        }
        else if(MainMenuPane.getState().equals(logic.State.CLIENT))
        {
            myKey = ClientLogic.getLocalAddressPort();
        }
         if (myKey != player.toString()) { //this player OBJ is us
             if(player.isMoving() &&    isInRange(player, PlayerLogic.getMyPosX(), PlayerLogic.getMyPosY())) {
                    //System.out.println(player.name);
                    if (System.currentTimeMillis() - otherLastPlayed > 500) {
                        playSound(OTHER_WALK_SOUND_PATH);
                        otherLastPlayed = System.currentTimeMillis();
                    }
             }
         }
    }

	public static void playWalkingSound() {
		if (System.currentTimeMillis() - myLastPlayed > 500) {
			playSound(WALK_SOUND_PATH);
			myLastPlayed = System.currentTimeMillis();
		}
	}

	private static boolean isInRange(PlayerInfo p1, double x, double y) {
		double dx = p1.getX() - x;
		double dy = p1.getY() - y;
		return Math.sqrt(dx * dx + dy * dy) <= SOUND_RANGE;
	}

	private static void playSound(String soundFile) {
		try {
			File file = new File(soundFile);
			AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
			Clip clip = AudioSystem.getClip();
			clip.open(audioStream);
			clip.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

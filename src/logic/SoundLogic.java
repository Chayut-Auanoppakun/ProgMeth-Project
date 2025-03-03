package logic;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

import gui.MainMenuPane;

import javax.sound.sampled.AudioInputStream;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import server.PlayerInfo;

public class SoundLogic {
	private static final double SOUND_RANGE = 800.0;
	private static final String WALK_SOUND_PATH = "assets/sounds/FootstepCarpet01.wav";
	private static final String OTHER_WALK_SOUND_PATH = "assets/sounds/FootstepTile01.wav";
	private static long myLastPlayed = 0;
	private static long otherLastPlayed = 0;

	public static void checkAndPlayWalkingSounds(PlayerInfo player) throws UnknownHostException {
		String myKey = PlayerLogic.getLocalAddressPort();
		if (myKey != player.toString()) { // this player OBJ is us
			if (player.isMoving()) {
				float volume = isInRange(player, PlayerLogic.getMyPosX(), PlayerLogic.getMyPosY());
				if (volume != -99) {
					// System.out.println(player.name);
					if (System.currentTimeMillis() - otherLastPlayed > 500) {
						playSound(OTHER_WALK_SOUND_PATH, volume);
						otherLastPlayed = System.currentTimeMillis();
					}
				}
			}
		}
	}

	public static void playWalkingSound() {
		if (System.currentTimeMillis() - myLastPlayed > 500) {
			playSound(WALK_SOUND_PATH, 0);
			myLastPlayed = System.currentTimeMillis();
		}
	}

	private static float isInRange(PlayerInfo p1, double x, double y) {
        double dx = p1.getX() - x;
        double dy = p1.getY() - y;
        double range = Math.sqrt(dx * dx + dy * dy);
        if (range <= SOUND_RANGE) {
            return calculateVolume(range);
        } else {
            return -99.0f;
        }
    }

    private static float calculateVolume(double range) {
        // Map the range to a volume level (in decibels)
        // Here, we assume that SOUND_RANGE corresponds to -30 dB (minimum volume)
        // and 0 range corresponds to 0 dB (maximum volume)
        float minVolume = -30.0f;
        float maxVolume = 0.0f;

        if (range <= 0) {
            return maxVolume;
        } else if (range >= SOUND_RANGE) {
            return minVolume;
        } else {
            return (float) (maxVolume - ((range / SOUND_RANGE) * (maxVolume - minVolume)));
        }
    }

	private static void playSound(String soundFile, float loudness) {
		try {
			File file = new File(soundFile);
			AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
			Clip clip = AudioSystem.getClip();
			clip.open(audioStream);

			// Set the loudness
			FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			gainControl.setValue(loudness);

			clip.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

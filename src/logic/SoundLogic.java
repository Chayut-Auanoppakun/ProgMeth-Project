package logic;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.AudioInputStream;
import java.io.File;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import server.PlayerInfo;

public class SoundLogic {
    private static final double SOUND_RANGE = 800.0;
    private static final String WALK_SOUND_PATH = "assets/sounds/FootstepCarpet01.wav";
    private static final String OTHER_WALK_SOUND_PATH = "assets/sounds/FootstepTile01.wav";
    private static long myLastPlayed = 0;
    private static ConcurrentHashMap<String, Long> otherLastPlayed = new ConcurrentHashMap<>();
    
    // Sound caching system
    private static ConcurrentHashMap<String, Clip> soundCache = new ConcurrentHashMap<>();
    private static ExecutorService soundThreadPool = Executors.newSingleThreadExecutor();
    
    // Initialize the sound system on first use
    static {
        initSoundCache();
    }
    
    // Pre-load sounds into memory
    private static void initSoundCache() {
        soundThreadPool.submit(() -> {
            try {
                // Pre-load common sounds
                loadSoundToCache(WALK_SOUND_PATH);
                loadSoundToCache(OTHER_WALK_SOUND_PATH);
                System.out.println("Sound cache initialized.");
            } catch (Exception e) {
                System.err.println("Error initializing sound cache: " + e.getMessage());
            }
        });
    }
    
    private static void loadSoundToCache(String soundPath) {
        try {
            File file = new File(soundPath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            soundCache.put(soundPath, clip);
        } catch (Exception e) {
            System.err.println("Error loading sound: " + soundPath + " - " + e.getMessage());
        }
    }

    // Optimized sound check - only run if enough time has passed since last check
    private static HashMap<String, Long> lastSoundCheckTimes = new HashMap<>();
    private static final long SOUND_CHECK_INTERVAL = 250; // Check every 250ms
    
    public static void checkAndPlayWalkingSounds(PlayerInfo player) throws UnknownHostException {
        String playerKey = player.toString();
        long currentTime = System.currentTimeMillis();
        
        // Only check each player at most once per interval
        Long lastChecked = lastSoundCheckTimes.get(playerKey);
        if (lastChecked != null && currentTime - lastChecked < SOUND_CHECK_INTERVAL) {
            return;
        }
        lastSoundCheckTimes.put(playerKey, currentTime);
        
        String myKey = PlayerLogic.getLocalAddressPort();
        if (!myKey.equals(playerKey)) { // This player OBJ is not us
            if (player.isMoving()) {
                float volume = isInRange(player, PlayerLogic.getMyPosX(), PlayerLogic.getMyPosY());
                if (volume > -99) {
                    // Check if we should play sound for this player
                    Long lastPlayed = otherLastPlayed.getOrDefault(playerKey, 0L);
                    if (currentTime - lastPlayed > 500) { // Minimum interval between footsteps
                        playSound(OTHER_WALK_SOUND_PATH, volume);
                        otherLastPlayed.put(playerKey, currentTime);
                    }
                }
            }
        }
    }

    public static void playWalkingSound() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - myLastPlayed > 500) { // Don't play too frequently
            playSound(WALK_SOUND_PATH, 0);
            myLastPlayed = currentTime;
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
        // Use a logarithmic falloff for more natural sound attenuation
        float minVolume = -30.0f;
        float maxVolume = 0.0f;

        if (range <= 0) {
            return maxVolume;
        } else if (range >= SOUND_RANGE) {
            return minVolume;
        } else {
            // Logarithmic falloff for more realistic sound
            double volumeRatio = 1.0 - Math.log10(1 + 9 * range / SOUND_RANGE);
            return (float)(maxVolume - ((1 - volumeRatio) * (maxVolume - minVolume)));
        }
    }

    private static void playSound(String soundFile, float loudness) {
        soundThreadPool.submit(() -> {
            try {
                // Try to reuse an existing clip for better performance
                Clip clip = soundCache.get(soundFile);
                
                // If no cached clip or it's currently playing, create a new one
                if (clip == null || clip.isRunning()) {
                    File file = new File(soundFile);
                    AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
                    clip = AudioSystem.getClip();
                    clip.open(audioStream);
                    
                    // If this is a new sound, cache it for future use
                    if (!soundCache.containsKey(soundFile)) {
                        soundCache.put(soundFile, clip);
                    }
                }
                
                // Reset clip position if needed
                if (clip.getFramePosition() > 0 || clip.isRunning()) {
                    clip.stop();
                    clip.setFramePosition(0);
                }
                
                // Set the loudness
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(loudness);
                
                // Play the sound
                clip.start();
            } catch (Exception e) {
                System.err.println("Error playing sound: " + e.getMessage());
            }
        });
    }
    
    // Call this on game shutdown
    public static void cleanup() {
        // Close all sound clips
        for (Clip clip : soundCache.values()) {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.close();
        }
        
        // Shutdown thread pool
        soundThreadPool.shutdown();
    }
}
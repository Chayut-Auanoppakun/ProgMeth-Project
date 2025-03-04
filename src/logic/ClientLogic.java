package logic;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import server.PlayerInfo;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONObject;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import gui.MainMenuPane;
import gui.ServerSelectGui;

public class ClientLogic {
	private static final Set<String> serverSet = new HashSet<>();
	private static final List<String> serverList = new ArrayList<>();
	private static String lastLoggedData = "";
	private static DatagramSocket clientSocket;
	private static InetAddress connectedServerAddress;
	private static int connectedServerPort = -1;
	private static Thread clientThread;
	private static Timer timer;
	private static int missedPings = 0;
	private static boolean wasDiscon = false;
	private static int sendPingCount = 0;
	private static long lastPositionUpdate = 0;
	private static final long POSITION_UPDATE_INTERVAL = 100; // Only send position updates every 100ms
	private static double lastSentX = 0;
	private static double lastSentY = 0;
	private static boolean lastSentMoving = false;
	private static int lastSentDirection = 0;

	public static void startClient(State state, TextArea logArea) {
		try {
			clientSocket = new DatagramSocket();
			clientSocket.setBroadcast(true);
			clientSocket.setSoTimeout(10000); // Set timeout for the socket to a higher value
		} catch (SocketException e) {
			log(logArea, "Error: " + e.getMessage());
		}

		clientThread = new Thread(() -> {
			try (DatagramSocket socket = new DatagramSocket(4446)) {
				socket.setBroadcast(true);
				byte[] buf = new byte[1024];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);

				log(logArea, "Client started. Waiting for server information...");

				while (state.equals(logic.State.CLIENT)) {
					socket.receive(packet);
					String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
					InetAddress serverAddress = packet.getAddress();
					int serverPort = packet.getPort();
					String serverID = serverAddress.getHostAddress() + ":" + serverPort + " - " + received;
					if (serverSet.add(serverID)) {
						serverList.add(serverID);
						// log(logArea, "Received new server info: " + serverID);
					}
				}
			} catch (IOException e) {
				log(logArea, "Error: " + e.getMessage());
			}
		});
		clientThread.start();

		timer = new Timer(true);
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				Platform.runLater(() -> logUniqueServerIDs(logArea));
			}
		}, 0, 1000); // 0 delay, 1000 milliseconds (1 seconds) period
	}

	public static boolean connectToServer(int serverIndex, TextArea logArea, String playerName) {
		if (serverIndex >= 0 && serverIndex < serverList.size()) {
			String selectedServer = serverList.get(serverIndex);
			try {
				String[] parts = selectedServer.split(" - ");
				String[] addressParts = parts[1].split(":");
				connectedServerAddress = InetAddress.getByName(parts[0].split(":")[0]);
				connectedServerPort = Integer.parseInt(addressParts[1]);
				log(logArea, "Connected to " + connectedServerAddress.getHostAddress() + ":" + connectedServerPort
						+ " - " + addressParts[0]);

				// Send player's name to the server
				sendMessage("/name/" + playerName, logArea);

				// Perform handshake
				sendMessage("/sys/Test_Handshake", logArea);
				byte[] buf = new byte[1024];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				try {
					System.out.println("Waiting for response...");
					clientSocket.receive(packet);
					String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
					System.out.println("Server response: " + received);
					System.out.println("Handshake Complete");
					if ("/sys/ACK".equals(received)) {
						log(logArea, "Connection established.");
						// Start receiving messages from server
						receiveMessagesFromServer(logArea);

						// Start the ping thread
						startPingThread(logArea);
						return true;
					} else {
						log(logArea, "Received unexpected response: " + received);
						return false;
					}
				} catch (SocketTimeoutException e) {
					log(logArea, "Handshake failed : Connection not established.");
					return false;
				}
			} catch (Exception e) {
				log(logArea, "Error connecting to server: " + e.getMessage());
				return false;
			}
		} else {
			log(logArea, "Invalid server selection.");
			return false;
		}
	}

	public static void sendMessage(String message, TextArea logArea) {
		if (connectedServerAddress != null && connectedServerPort != -1) {
			try {
				if (message.startsWith("/sys/") || message.startsWith("/name/")) {
					byte[] buf = message.getBytes(StandardCharsets.UTF_8);
					DatagramPacket packet = new DatagramPacket(buf, buf.length, connectedServerAddress,
							connectedServerPort);
					clientSocket.send(packet);
					System.out.println("Sent system message: " + message); // Print to terminal for debugging
				} else {
					byte[] buf = message.getBytes(StandardCharsets.UTF_8);
					DatagramPacket packet = new DatagramPacket(buf, buf.length, connectedServerAddress,
							connectedServerPort);
					clientSocket.send(packet);
					log(logArea, "You : " + message);
				}
			} catch (IOException e) {
				log(logArea, "Error: " + e.getMessage());
			}
		} else {
			log(logArea, "Not connected to any server.");
		}
	}

	public static void stopClient(TextArea logArea) {
		if (clientSocket != null && !clientSocket.isClosed()) {
			clientSocket.close();
			log(logArea, "Disconnected from server and closed socket.");
		}
		connectedServerAddress = null;
		connectedServerPort = -1;
		if (clientThread != null && clientThread.isAlive()) {
			clientThread.interrupt();
		}
		if (timer != null) {
			timer.cancel();
		}
		serverSet.clear();
		serverList.clear();
		lastLoggedData = "";
	}

	private static void log(TextArea logArea, String message) {
		Platform.runLater(() -> logArea.appendText(message + "\n"));
	}

	private static void logUniqueServerIDs(TextArea logArea) {
		StringBuilder builder = new StringBuilder();
		builder.append("Unique Server IDs:\n");
		Set<String> uniqueServers = new HashSet<>();

		for (int i = 0; i < serverList.size(); i++) {
			String serverEntry = serverList.get(i);
			String[] parts = serverEntry.split(" - ");
			String[] serverInfo = parts[1].split(":");
			String uniqueServerEntry = parts[0].split(":")[0] + ":" + serverInfo[1] + " - " + serverInfo[0];

			if (uniqueServers.add(uniqueServerEntry)) {
				builder.append(uniqueServers.size()).append(". ").append(uniqueServerEntry).append("\n");
			}
		}

		String currentData = builder.toString();
		if (!currentData.equals(lastLoggedData)) {
			log(logArea, currentData);
			lastLoggedData = currentData;
			serverSet.clear();
			serverList.clear();
		}
	}

	public static void receiveMessagesFromServer(TextArea logArea) {
	    Thread receiveThread = new Thread(() -> {
	        try {
	            byte[] buf = new byte[1024];
	            DatagramPacket packet = new DatagramPacket(buf, buf.length);

	            while (true) {
	                try {
	                    clientSocket.receive(packet);
	                    String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
	                    
	                    if (received.startsWith("/sys/")) {
	                        if ("/sys/PONG".equals(received)) {
	                            missedPings = 0; // Reset missed ping count
	                        } else if ("/sys/ACK".equals(received)) {
	                            System.out.println("Handshake Test Complete");
	                        }
	                    } else if (received.startsWith("/data/")) {
	                        // Process data updates more efficiently
	                        processDataUpdate(received);
	                    } else if (received.startsWith("/r/")) {
	                        String msg = received.substring(3);
	                        log(logArea, msg);
	                    } else if (received.startsWith("/sname/")) {
	                        String output = received.substring(7);
	                        log(logArea, output);
	                    } else if (received.startsWith("/ls/")) {
	                        String list = received.substring(4);
	                        log(logArea, list);
	                    } else {
	                        log(logArea, received);
	                    }
	                } catch (SocketTimeoutException e) {
	                    // Just continue trying
	                }
	            }
	        } catch (IOException e) {
	            if (!e.getMessage().contains("Socket closed")) {
	                log(logArea, "Error receiving message from server: " + e.getMessage());
	            }
	        }
	    });
	    receiveThread.setDaemon(true); // Make this a daemon thread
	    receiveThread.start();
	}
	
	private static void processDataUpdate(String dataMessage) {
	    try {
	        String jsonStr = dataMessage.substring(6);
	        JSONObject json = new JSONObject(jsonStr);
	        
	        // Process only keys that exist in the JSON
	        for (String key : json.keySet()) {
	            JSONObject playerData = json.getJSONObject(key);
	            
	            // Only access fields that exist in this update
	            double[] pos = null;
	            if (playerData.has("position")) {
	                pos = playerData.getJSONArray("position").toList().stream()
	                      .mapToDouble(o -> ((Number) o).doubleValue()).toArray();
	            }
	            
	            String name = playerData.optString("name", null);
	            Integer direction = playerData.has("Direction") ? playerData.getInt("Direction") : null;
	            String status = playerData.optString("status", null);
	            Boolean isMoving = playerData.has("isMoving") ? playerData.getBoolean("isMoving") : null;
	            Integer charID = playerData.has("charID") ? playerData.getInt("charID") : null;
	            
	            // Check for prep phase end flag
	            if (!GameLogic.isPrepEnded() && playerData.has("prepEnded")) {
	                boolean prepEnded = playerData.getBoolean("prepEnded");
	                GameLogic.setPrepEnded(prepEnded);
	            }
	            
	            // Update player data efficiently
	            if (PlayerLogic.getLocalAddressPort().equals(key)) {
	                // This is data about our local player - typically don't need to update
	            } else if (GameLogic.playerList.containsKey(key)) {
	                // Update existing player
	                PlayerInfo existing = GameLogic.playerList.get(key);
	                
	                // Only update fields that were included in the message
	                if (pos != null) {
	                    existing.setX(pos[0]);
	                    existing.setY(pos[1]);
	                }
	                if (isMoving != null) existing.setMoving(isMoving);
	                if (direction != null) existing.setDirection(direction);
	                if (status != null) existing.setStatus(status);
	                if (charID != null) existing.setCharacterID(charID);
	            } else if (pos != null && name != null) {
	                // Add new player
	                try {
	                    GameLogic.playerList.put(key,
	                        new PlayerInfo(InetAddress.getByName(key.split(":")[0]),
	                                       Integer.parseInt(key.split(":")[1]), 
	                                       name, 
	                                       pos[0], pos[1], 
	                                       isMoving != null ? isMoving : false,
	                                       direction != null ? direction : 0,
	                                       status != null ? status : "active", 
	                                       charID != null ? charID : 0));
	                } catch (UnknownHostException e) {
	                    System.err.println("Error creating PlayerInfo: " + e.getMessage());
	                }
	            }
	        }
	    } catch (Exception e) {
	        System.err.println("Error processing data update: " + e.getMessage());
	    }
	}
	
	private static void startPingThread(TextArea logArea) {
		Thread pingThread = new Thread(() -> {
			while (connectedServerAddress != null && connectedServerPort != -1) {
				try {
					sendPing(logArea);
					Thread.sleep(40); // Ping every 40 ms

				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					log(logArea, "Ping thread interrupted.");
				}
			}
		});
		pingThread.start();
	}

	private static void sendPing(TextArea logArea) {
	    long currentTime = System.currentTimeMillis();
	    
	    if (connectedServerAddress != null && connectedServerPort != -1) {
	        try {
	            // Send server availability ping less frequently (every ~400ms)
	            if (sendPingCount > 8) {
	                sendPingCount = 0;

	                if (missedPings > 5) {
	                    wasDiscon = true;
	                    if (missedPings == 11) { // run once on disconnect
	                        log(logArea, "Server disconnected.");
	                        System.out.println("Server disconnected.");
	                        log(logArea, "Attempting Reconnection ...");
	                    }
	                } else {
	                    if (wasDiscon) {
	                        sendMessage("/name/" + MainMenuPane.getPlayerName(), logArea);
	                        log(logArea, "Server Reconnect Successful");
	                        wasDiscon = false;
	                    }
	                }

	                String pingMessage = "/sys/PING";
	                byte[] buf = pingMessage.getBytes(StandardCharsets.UTF_8);
	                DatagramPacket packet = new DatagramPacket(buf, buf.length, connectedServerAddress,
	                        connectedServerPort);
	                clientSocket.send(packet);
	                missedPings += 1;
	            } else {
	                sendPingCount++;
	            }

	            // Send Player Position ONLY when needed (position changed or on interval)
	            if (ServerSelectGui.isGameWindow()) {
	                // Check if it's time to send an update
	                boolean shouldUpdate = false;
	                
	                // Check if position actually changed enough to warrant an update
	                double currentX = PlayerLogic.getMyPosX();
	                double currentY = PlayerLogic.getMyPosY();
	                boolean currentMoving = PlayerLogic.getMoving();
	                int currentDirection = PlayerLogic.getDirection();
	                
	                // Send update if significant position change (more than 1 pixel)
	                double positionDelta = Math.sqrt(Math.pow(currentX - lastSentX, 2) + 
	                                               Math.pow(currentY - lastSentY, 2));
	                
	                if (positionDelta > 1.0 || 
	                    currentMoving != lastSentMoving || 
	                    currentDirection != lastSentDirection) {
	                    
	                    // Only send if we haven't sent too recently
	                    if (currentTime - lastPositionUpdate >= POSITION_UPDATE_INTERVAL) {
	                        shouldUpdate = true;
	                    }
	                }
	                
	                // Always send periodic updates even without movement (every ~300ms)
	                if (currentTime - lastPositionUpdate >= 300) {
	                    shouldUpdate = true;
	                }
	                
	                if (shouldUpdate) {
	                    try {
	                        // Send more compact position update with only changed values
	                        JSONObject json = new JSONObject();
	                        json.put("name", PlayerLogic.getName());
	                        json.put("PosX", currentX);
	                        json.put("PosY", currentY);
	                        json.put("Direction", currentDirection);
	                        json.put("isMoving", currentMoving);
	                        json.put("charID", PlayerLogic.getCharID());
	                        json.put("playerReady", PlayerLogic.isPlayerReady());

	                        String message = "/data/" + json.toString();
	                        byte[] buf = message.getBytes(StandardCharsets.UTF_8);
	                        DatagramPacket packet = new DatagramPacket(buf, buf.length, connectedServerAddress,
	                                connectedServerPort);
	                        clientSocket.send(packet);
	                        
	                        // Update last sent values
	                        lastSentX = currentX;
	                        lastSentY = currentY;
	                        lastSentMoving = currentMoving;
	                        lastSentDirection = currentDirection;
	                        lastPositionUpdate = currentTime;
	                    } catch (IOException ex) {
	                        System.err.println("Error sending position update: " + ex.getMessage());
	                    }
	                }
	            }
	        } catch (IOException e) {
	            log(logArea, "Error communicating with server: " + e.getMessage());
	        }
	    } else {
	        log(logArea, "Server connection not established. Cannot send PING.");
	    }
	}

	public static DatagramSocket getClientSocket() {
		return clientSocket;
	}

	public static String getConnectedServerInfo() {
		if (connectedServerAddress != null && connectedServerPort != -1) {
			return connectedServerAddress.getHostAddress() + ":" + connectedServerPort;
		}
		return null;
	}

}
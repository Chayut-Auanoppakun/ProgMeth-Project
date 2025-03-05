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
import org.json.JSONObject;

import gameObjects.Corpse;

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
						// System.out.println(received);
						if (received.startsWith("/sys/")) {
							if ("/sys/PONG".equals(received)) {
								missedPings = 0; // Reset missed ping count
								// System.out.println("Received PONG from server"); // Print to terminal for
								// debugging
							} else if ("/sys/ACK".equals(received)) {
								System.out.println("Handshake Test Complete"); // Print to terminal for debugging
							}
						} else if (received.startsWith("/data/") || received.startsWith("/body/")
								|| received.startsWith("/report/")) {
							// put here to prevent cout to log area
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

						if (received.startsWith("/data/")) {
							String jsonStr = received.substring(6);
							JSONObject json = new JSONObject(jsonStr);
							// System.out.println(received);

							// Update positions and additional fields
							for (String key : json.keySet()) {
								JSONObject playerData = json.getJSONObject(key);
								double[] pos = playerData.getJSONArray("position").toList().stream()
										.mapToDouble(o -> ((Number) o).doubleValue()).toArray();

								String name = playerData.getString("name");
								int direction = playerData.getInt("Direction");
								boolean isMoving = playerData.getBoolean("isMoving");
								int charID = playerData.getInt("charID");
								String Status = playerData.optString("Status", "Unknown");

								// === For ending Prep Phase ===
								if (!GameLogic.isPrepEnded()) {
									boolean prepEnded = playerData.getBoolean("prepEnded");
									GameLogic.setPrepEnded(prepEnded);
								}
								if (getConnectedServerInfo().equals(key)) { // this is the servers data

								}
								// ================================
								if (PlayerLogic.getLocalAddressPort().equals(key)) { // our own
									PlayerLogic.setStatus(Status);
								} else if (GameLogic.playerList.containsKey(key)) {
									PlayerInfo existing = GameLogic.playerList.get(key);
									existing.setX(pos[0]);
									existing.setY(pos[1]);
									existing.setMoving(isMoving);
									existing.setDirection(direction);
									existing.setStatus(Status);
									existing.setCharacterID(charID);
									existing.setStatus(Status);
								} else {
									GameLogic.playerList.put(key,
											new PlayerInfo(InetAddress.getByName(key.split(":")[0]),
													Integer.parseInt(key.split(":")[1]), name, pos[0], pos[1], isMoving,
													direction, Status, charID));
								}
							}

						}

						if (received.startsWith("/kill/")) {
							try {
								String jsonStr = received.substring(6); // Remove "/kill/" prefix
								JSONObject killReport = new JSONObject(jsonStr);

								String killedPlayerKey = killReport.getString("killedPlayer");
								String playerName = killReport.getString("playerName");
								double x = killReport.getDouble("x");
								double y = killReport.getDouble("y");
								int characterID = killReport.getInt("characterID");
								long timeOfDeath = killReport.getLong("timeOfDeath");

								// Find the killed player
								PlayerInfo killedPlayer = GameLogic.playerList.get(killedPlayerKey);
								if (killedPlayer != null) {
									if (killedPlayerKey.equals(PlayerLogic.getLocalAddressPort())) {
										System.out.println("WE GOT KILLED");
									}
									// Mark player as dead
									killedPlayer.setStatus("dead");

									// Create a corpse
									GameLogic.createCorpse(killedPlayer); //create corpse and put into list
								}
							} catch (Exception e) {
								System.err.println("Error processing dead body report: " + e.getMessage());
							}
						} else if (received.startsWith("/report/")) {
							try {
								String jsonStr = received.substring(8); // Remove "/report/" prefix
								JSONObject reportJSON = new JSONObject(jsonStr);

								String reporterKey = reportJSON.getString("reporter");
								String deadBodyKey = reportJSON.getString("deadBody");

								// Find the reporter and dead body
								PlayerInfo reporter = GameLogic.playerList.get(reporterKey);
								Corpse corpse = GameLogic.getCorpse(deadBodyKey);

								if (reporter != null && corpse != null) {
									// Mark body as found
									corpse.setFound(true);
									System.out.println("REPORT PLAYER : " + corpse.getPlayerName());
									// TODO
									// Trigger emergency meeting
									// GameLogic.startEmergencyMeeting(corpse.getPlayerName(), reporter.getName());
								}
							} catch (Exception e) {
								System.err.println("Error processing body report: " + e.getMessage());
							}
						}
					} catch (SocketTimeoutException e) {
						// log(logArea, "No message received. Waiting...");
					}
				}
			} catch (IOException e) {
				if (!e.getMessage().contains("Socket closed")) {
					log(logArea, "Error receiving message from server: " + e.getMessage());
				} else {
					// log(logArea, "Socket closed while receiving messages.");
				}
			}
		});
		receiveThread.start();
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

	private static int sendPingCount = 0;

	private static void sendPing(TextArea logArea) { /// send ping and also information to server run every 100ms
		if (connectedServerAddress != null && connectedServerPort != -1) {
			try {
				if (sendPingCount > 8) { // run to check if server avail
					sendPingCount = 0;

					if (missedPings > 5) {
						wasDiscon = true;
						if (missedPings == 11) { // run once on disconnect
							log(logArea, "Server disconnected.");
							System.out.println("Server disconnected."); // Print to terminal for debugging
							log(logArea, "Attempting Reconnection ...");
						}
					} else {
						if (wasDiscon) {
							sendMessage("/name/" + MainMenuPane.getPlayerName(), logArea);
							log(logArea, "Server Reconnect Succesful");
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

				// Send Player Information (Not Ping)
				if (ServerSelectGui.isGameWindow()) {
					try {
						JSONObject json = new JSONObject();
						json.put("name", PlayerLogic.getName());
						json.put("PosX", PlayerLogic.getMyPosX());
						json.put("PosY", PlayerLogic.getMyPosY());
						json.put("Direction", PlayerLogic.getDirection());
						json.put("isMoving", PlayerLogic.getMoving());
						json.put("charID", PlayerLogic.getCharID());
						json.put("playerReady", PlayerLogic.isPlayerReady());

						// TODO Add more data
						String message = "/data/" + json.toString();
						byte[] buf = message.getBytes(StandardCharsets.UTF_8);
						DatagramPacket packet = new DatagramPacket(buf, buf.length, connectedServerAddress,
								connectedServerPort);
						clientSocket.send(packet);
						// System.out.println("Sent information to server: " + message);
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			} catch (IOException e) {
				log(logArea, "Error sending PING to server: " + e.getMessage());
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
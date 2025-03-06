package logic;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import server.PlayerInfo;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONObject;

import gameObjects.Corpse;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import gui.GameWindow;
import gui.MainMenuPane;
import gui.MeetingUI;
import gui.ServerSelectGui;

public class ClientLogic {
	private static final Set<String> serverSet = new HashSet<>();
	private static final List<String> serverList = new ArrayList<>();
	private static Set<String> processedVotes = new HashSet<>();

	private static String lastLoggedData = "";
	private static DatagramSocket clientSocket;
	private static InetAddress connectedServerAddress;
	private static int connectedServerPort = -1;
	private static Thread clientThread;
	private static Timer timer;
	private static int missedPings = 0;
	private static boolean wasDiscon = false;
	private static long lastPRint = 0;

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
				// System.out.println("Corpse size =" + GameLogic.corpseList.size());
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
				if (message.startsWith("/sys/") || message.startsWith("/name/") || message.startsWith("/kill/")) {
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
		if (logArea != null)
			Platform.runLater(() -> logArea.appendText(message + "\n"));
		else
			System.out.println("logArea missing " + message);
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
						// Handle system messages
						if (received.startsWith("/sys/")) {
							if ("/sys/PONG".equals(received)) {
								missedPings = 0; // Reset missed ping count
								// System.out.println("Received PONG from server"); // Print to terminal for
								// debugging
							} else if ("/sys/ACK".equals(received)) {
								System.out.println("Handshake Test Complete"); // Print to terminal for debugging
							}
						}
						// Handle game data messages
						else if (received.startsWith("/data/")) {
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
								double taskCompleted = playerData.getDouble("task");
								String Status = "crewmate";
								Status = playerData.optString("Status", "Unknown");

								// === For ending Prep Phase ===
								if (!GameLogic.isPrepEnded()) {
									boolean prepEnded = playerData.getBoolean("prepEnded");
									GameLogic.setPrepEnded(prepEnded);
								}
								if (getConnectedServerInfo().equals(key)) { // this is the servers data

								}
								// ================================
								if (PlayerLogic.getLocalAddressPort().equals(key)) { // our own
									if (!PlayerLogic.getStatus().equals(Status)) {
										System.out.println("Set Status to " + Status);
										PlayerLogic.setStatus(Status);
									}
								} else if (GameLogic.playerList.containsKey(key)) {
									PlayerInfo existing = GameLogic.playerList.get(key);
									existing.setX(pos[0]);
									existing.setY(pos[1]);
									existing.setMoving(isMoving);
									existing.setDirection(direction);
									existing.setStatus(Status);
									existing.setCharacterID(charID);
									existing.setStatus(Status);
									existing.setTaskPercent(taskCompleted);
								} else {
									PlayerInfo newPlayer = new PlayerInfo(InetAddress.getByName(key.split(":")[0]),
											Integer.parseInt(key.split(":")[1]), name, pos[0], pos[1], isMoving,
											direction, Status, charID);
									newPlayer.setTaskPercent(taskCompleted);
									GameLogic.playerList.put(key, newPlayer);
								}
							}
						} else if (received.startsWith("/kill/")) {
							try {
								System.out.println("CLIENT: Received kill message: " + received);

								// Extract and parse the JSON data
								String jsonStr = received.substring(6); // Remove "/kill/" prefix
								JSONObject killData = new JSONObject(jsonStr);

								// Extract all needed fields
								String killedPlayerKey = killData.getString("killedPlayer");
								String playerName = killData.getString("playerName");
								double x = killData.getDouble("x");
								double y = killData.getDouble("y");
								int characterID = killData.getInt("characterID");

								System.out.println("CLIENT: Processing kill report for " + playerName + " at " + x + ","
										+ y + " (Character ID: " + characterID + ")");

								// Special handling if we are the killed player
								if (killedPlayerKey.equals(PlayerLogic.getLocalAddressPort())) {
									System.out.println("CLIENT: Local player was killed!");

									// Update our own status
									PlayerLogic.setStatus("dead");

									// Play death sound
									SoundLogic.playSound("assets/sounds/impostor_kill.wav", 0);

									System.out.println("CLIENT: Updated local player status to dead");
								}

								// Find the killed player in our list
								PlayerInfo killedPlayer = GameLogic.playerList.get(killedPlayerKey);

								if (killedPlayer != null) {
									System.out
											.println("CLIENT: Found player in player list: " + killedPlayer.getName());

									// Update player status to dead
									killedPlayer.setStatus("dead");

									// Create a corpse if not already exists
									if (!GameLogic.corpseList.containsKey(killedPlayerKey)) {
										Corpse corpse = new Corpse(killedPlayer);
										GameLogic.corpseList.put(killedPlayerKey, corpse);

										System.out.println("CLIENT: Created corpse for " + killedPlayer.getName()
												+ " at " + corpse.getX() + "," + corpse.getY());

										// Print info about the corpse list
										System.out.println(
												"CLIENT: Current corpse list size: " + GameLogic.corpseList.size());
									} else {
										Corpse existingCorpse = GameLogic.corpseList.get(killedPlayerKey);
										System.out.println("CLIENT: Corpse already exists at " + existingCorpse.getX()
												+ "," + existingCorpse.getY());
									}
								} else {
									System.out.println("CLIENT: Player " + killedPlayerKey
											+ " not found in player list, creating placeholder");

									// Create a placeholder player
									try {
										String[] addressParts = killedPlayerKey.split(":");
										InetAddress address = InetAddress.getByName(addressParts[0]);
										int port = Integer.parseInt(addressParts[1]);

										// Create new player info
										PlayerInfo placeholderPlayer = new PlayerInfo(address, port, playerName, x, y,
												false, 0, "dead", characterID);

										// Add to player list
										GameLogic.playerList.put(killedPlayerKey, placeholderPlayer);

										// Create corpse
										Corpse corpse = new Corpse(placeholderPlayer);
										GameLogic.corpseList.put(killedPlayerKey, corpse);

										System.out.println(
												"CLIENT: Created placeholder player and corpse at " + x + "," + y);
										System.out.println(
												"CLIENT: Current corpse list size: " + GameLogic.corpseList.size());
									} catch (Exception e) {
										System.err
												.println("CLIENT ERROR creating placeholder player: " + e.getMessage());
										e.printStackTrace();
									}
								}

								// Play sound if not our own death (which already plays a sound)
								if (!killedPlayerKey.equals(PlayerLogic.getLocalAddressPort())) {
									SoundLogic.playSound("assets/sounds/dead_body.wav", 0);
								}

								// Debug info about all corpses
								for (String key : GameLogic.corpseList.keySet()) {
									Corpse c = GameLogic.corpseList.get(key);
									System.out.println("CLIENT DEBUG - Corpse: " + c.getPlayerName() + " at " + c.getX()
											+ "," + c.getY() + " (found: " + c.isFound() + ")");
								}

							} catch (Exception e) {
								System.err.println("CLIENT ERROR processing kill message: " + e.getMessage());
								e.printStackTrace();
							}
						}

						else if (received.startsWith("/meeting/")) {
							try {
								String jsonStr = received.substring(9); // Remove "/meeting/" prefix
								JSONObject meetingData = new JSONObject(jsonStr);

								System.out.println("Client received meeting message: " + jsonStr);
								String meetingType = meetingData.optString("type", "null");

								if (meetingType.equals("chat")) {
									String meetingId = meetingData.getString("meetingId");
									String senderName = meetingData.getString("name");
									String message = meetingData.getString("message");
									String status = meetingData.optString("status", "crewmate");
									boolean isGhostMessage = meetingData.optBoolean("isGhostMessage", false);

									// Check if this is a ghost message and if we should display it
									boolean shouldDisplay = true;
									if (isGhostMessage) {
										// If ghost message, only show to other ghosts
										if (!"dead".equals(PlayerLogic.getStatus())) {
											shouldDisplay = false;
										}
									}

									if (shouldDisplay) {
										System.out.println("Client processing chat message for meeting: " + meetingId);

										GameWindow gameWindowInstance = GameWindow.getGameWindowInstance();
										if (gameWindowInstance != null) {
											MeetingUI activeMeeting = gameWindowInstance.getActiveMeetingUI();

											if (activeMeeting != null) {
												activeMeeting.receiveChatMessage(senderName, message, status);
											}
										}
									} else {
										System.out
												.println("Client ignoring ghost message (player is alive): " + message);
									}
								} else {
									// Extract meeting details
									String reporterKey = meetingData.getString("reporter");
									String reportedPlayerName = meetingData.optString("reportedPlayer", null);
									int reportedCharId = meetingData.getInt("reportedCharId");

									Platform.runLater(() -> {
										GameWindow.getGameWindowInstance().startEmergencyMeeting(reporterKey,
												reportedPlayerName, reportedCharId);
									});
								}
							} catch (Exception e) {
								System.err.println("Error processing meeting message: " + e.getMessage());
								e.printStackTrace();
							}
						} else if (received.startsWith("/vote/")) {
							try {
								// Extract vote data
								String jsonStr = received.substring(6); // Remove "/vote/" prefix
								JSONObject voteData = new JSONObject(jsonStr);

								String voterKey = voteData.getString("voter");
								String targetKey = voteData.getString("target");

								// Extract voteId to identify duplicates - if not present, create one
								String voteId = voteData.has("voteId") ? voteData.getString("voteId")
										: voterKey + "_" + (voteData.has("time") ? voteData.getLong("time")
												: System.currentTimeMillis());

								// Check if this vote has already been processed
								if (processedVotes.contains(voteId)) {
									System.out.println("CLIENT: Ignoring duplicate vote from " + voterKey);
									// return;
								} else {

									// Mark this vote as processed
									processedVotes.add(voteId);

									// Limit the size of processedVotes to prevent memory issues
									if (processedVotes.size() > 1000) {
										// Remove oldest votes (just keep the latest 500)
										processedVotes = processedVotes.stream().skip(processedVotes.size() - 500)
												.collect(java.util.stream.Collectors.toSet());
									}

									System.out.println("CLIENT: Received vote data - Voter: " + voterKey + ", Target: "
											+ targetKey);

									// If there's an active meeting UI, update it with this vote
									if (GameWindow.getGameWindowInstance() != null) {
										MeetingUI activeMeeting = GameWindow.getGameWindowInstance()
												.getActiveMeetingUI();
										if (activeMeeting != null) {
											// Only process the vote if it's for the active meeting
											System.out.println("CLIENT: Updating meeting UI with vote");
											activeMeeting.receiveVote(voterKey, targetKey);
										} else {
											System.err.println("CLIENT: Received vote but no active meeting UI found");
										}
									}
								}
							} catch (Exception e) {
								System.err.println("CLIENT ERROR: Failed to process vote message: " + e.getMessage());
								e.printStackTrace();
							}
						} else if (received.startsWith("/results/")) {
							try {
								// Extract results data
								String jsonStr = received.substring(9); // Remove "/results/" prefix
								JSONObject resultsData = new JSONObject(jsonStr);

								// Create a unique ID for this results message
								String resultsId = resultsData.has("meetingId")
										? resultsData.getString("meetingId") + "_"
												+ (resultsData.has("time") ? resultsData.getLong("time") : 0)
										: "results_" + System.currentTimeMillis();

								// Check if this results message has already been processed
								if (processedVotes.contains(resultsId)) {
									System.out.println("CLIENT: Ignoring duplicate results message");
								} else {
									// Mark this results message as processed
									processedVotes.add(resultsId);

									System.out.println("CLIENT: Received voting results: " + jsonStr);

									// Check if ejected is null
									String ejectedPlayerKey = null;
									if (!resultsData.isNull("ejected")) {
										ejectedPlayerKey = resultsData.getString("ejected");
										System.out.println("CLIENT: Player ejected: " + ejectedPlayerKey);
									} else {
										System.out.println("CLIENT: No player ejected");
									}

									String meetingId = resultsData.getString("meetingId");

									// Get vote counts
									JSONObject votesJson = resultsData.getJSONObject("votes");
									Map<String, Integer> voteResults = new HashMap<>();
									for (String key : votesJson.keySet()) {
										voteResults.put(key, votesJson.getInt(key));
									}

									System.out.println("CLIENT: Vote counts: " + voteResults);

									// Handle player ejection
									if (ejectedPlayerKey != null) {
										// If it's the local player
										if (ejectedPlayerKey.equals(PlayerLogic.getLocalAddressPort())) {
											System.out.println("CLIENT: You were ejected!");
											PlayerLogic.setStatus("dead");
										}
										// If it's another player
										else if (GameLogic.playerList.containsKey(ejectedPlayerKey)) {
											PlayerInfo player = GameLogic.playerList.get(ejectedPlayerKey);
											if (player != null) {
												System.out.println("CLIENT: " + player.getName() + " was ejected");
												player.setStatus("dead");
											}
										}
									}

									// If there's an active meeting UI, update it with the results
									if (GameWindow.getGameWindowInstance() != null) {
										MeetingUI activeMeeting = GameWindow.getGameWindowInstance()
												.getActiveMeetingUI();
										if (activeMeeting != null && activeMeeting.getMeetingId().equals(meetingId)) {
											System.out.println("CLIENT: Updating meeting UI with voting results");
											activeMeeting.showVotingResults(ejectedPlayerKey, voteResults);
										}
									}
								}
							} catch (Exception e) {
								System.err.println("CLIENT ERROR: Failed to process voting results: " + e.getMessage());
								e.printStackTrace();
							}
						}
						// Handle relay messages
						else if (received.startsWith("/r/")) {
							String msg = received.substring(3);
							log(logArea, msg);
						}
						// Handle server name messages
						else if (received.startsWith("/sname/")) {
							String output = received.substring(7);
							log(logArea, output);
						}
						// Handle list messages
						else if (received.startsWith("/ls/")) {
							String list = received.substring(4);
							log(logArea, list);
						}
						// Handle other messages (regular chat, etc.)
						else {
							log(logArea, received);
						}
					} catch (SocketTimeoutException e) {
						System.out.println("Waiting for msg : shouldn't timeout");
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
		receiveThread.setDaemon(true);
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
						json.put("task", PlayerLogic.getTaskPercent());

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
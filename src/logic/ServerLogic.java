package logic;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import server.ClientInfo;
import server.PlayerInfo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;

import gameObjects.Corpse;
import gui.GameWindow;
import gui.MainMenuPane;
import gui.MeetingUI;
import gui.PrepGui;
import gui.ServerSelectGui;

public class ServerLogic {
	// Constants
	private static final int MAX_MISSED_PINGS = 5;
	private static final int BROADCAST_INTERVAL_MS = 1000;
	private static final int PING_CHECK_INTERVAL_MS = 1000;
	private static final byte[] BUFFER = new byte[1024];

	// Server state
	private static DatagramSocket serverSocket;
	private static Thread serverThread;
	private static Set<ClientInfo> clientAddresses = new HashSet<>();
	private static ConcurrentHashMap<ClientInfo, AtomicInteger> clientPingCount = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<String, Map<String, String>> meetingVotes = new ConcurrentHashMap<>();
	private static Set<String> recentChatMessages = new HashSet<>();
	private static Timer pingCheckTimer;
	private static int readyPlayers = 0;
	private static boolean isRunning = false;

	/**
	 * Starts broadcasting server information to potential clients
	 * 
	 * @param state      Current server state
	 * @param logArea    TextArea for logging
	 * @param serverName Name of the server
	 * @param serverPort Port on which the server is running
	 */
	public static void startBroadcasting(State state, TextArea logArea, String serverName, int serverPort) {
		Thread thread = new Thread(() -> {
			try (DatagramSocket socket = new DatagramSocket()) {
				socket.setBroadcast(true);
				String broadcastMessage = serverName + ":" + serverPort;
				byte[] buf = broadcastMessage.getBytes(StandardCharsets.UTF_8);
				InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
				DatagramPacket packet = new DatagramPacket(buf, buf.length, broadcastAddress, 4446);

				while (state.equals(State.SERVER)) {
					socket.send(packet);
					Thread.sleep(BROADCAST_INTERVAL_MS);
				}
			} catch (IOException | InterruptedException e) {
				log(logArea, "Error in broadcasting: " + e.getMessage());
				System.err.println("Broadcasting error: " + e.getMessage());
			}
		});
		thread.setName("Server-Broadcast-Thread");
		thread.setDaemon(true);
		thread.start();

		log(logArea, "Broadcasting as " + serverName + " on port " + serverPort + "...");
	}

	/**
	 * Starts the server to listen for client connections and messages
	 * 
	 * @param state      Current server state
	 * @param logArea    TextArea for logging
	 * @param serverPort Port on which to run the server
	 */
	public static void startServer(State state, TextArea logArea, int serverPort) {
		if (isRunning) {
			log(logArea, "Server is already running.");
			return;
		}

		isRunning = true;

		serverThread = new Thread(() -> {
			try {
				serverSocket = new DatagramSocket(serverPort);
				log(logArea, "Server started on port " + serverPort + ", waiting for messages...");

				// Start the ping check timer
				pingCheckTimer = new Timer(true);
				pingCheckTimer.scheduleAtFixedRate(new TimerTask() {
					@Override
					public void run() {
						checkClientPings(logArea);
					}
				}, 0, PING_CHECK_INTERVAL_MS);

				// Schedule regular ready player check
				ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
					Thread t = new Thread(r, "Ready-Players-Check-Thread");
					t.setDaemon(true);
					return t;
				});
				executor.scheduleAtFixedRate(ServerLogic::checkReadyPlayers, 0, 1, TimeUnit.SECONDS);

				// Main server loop
				while (state.equals(State.SERVER)) {
					DatagramPacket packet = new DatagramPacket(BUFFER, BUFFER.length);
					serverSocket.receive(packet);

					String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
					InetAddress clientAddress = packet.getAddress();
					int clientPort = packet.getPort();

					handleIncomingMessage(received, clientAddress, clientPort, packet, logArea);
				}

				executor.shutdown();
				isRunning = false;
			} catch (IOException e) {
				if (!e.getMessage().contains("Socket closed")) {
					log(logArea, "Server error: " + e.getMessage());
					System.err.println("Server error: " + e.getMessage());
				}
				isRunning = false;
			}
		});

		serverThread.setName("Server-Main-Thread");
		serverThread.start();
	}

	/**
	 * Handles incoming messages from clients
	 */
	private static void handleIncomingMessage(String received, InetAddress clientAddress, int clientPort,
			DatagramPacket packet, TextArea logArea) throws IOException {
		if (received.startsWith("/name/")) {
			handleNameRegistration(received, clientAddress, clientPort, logArea);
		} else if (received.startsWith("/sys/")) {
			handleSystemMessage(received, clientAddress, clientPort, packet, logArea);
		} else if (received.startsWith("/data/")) {
			handlePlayerData(received, packet);
		} else if (received.startsWith("/kill/")) {
			handleKillMessage(received, clientAddress, clientPort, logArea);
		} else if (received.startsWith("/report/")) {
			handleReportMessage(received, clientAddress, clientPort, logArea);
		} else if (received.startsWith("/meeting/")) {
			// New handler for meeting-specific messages
			System.out.println("RECIEVE MEETING");
			handleMeetingMessage(received, clientAddress, clientPort, logArea);
		} else if (received.startsWith("/vote/")) {
			handleVoteMessage(received, clientAddress, clientPort, logArea);
		} else {
			handleChatMessage(received, clientAddress, clientPort, logArea);
		}
	}

	private static void handleVoteMessage(String received, InetAddress clientAddress, int clientPort,
			TextArea logArea) {
		try {
			// Extract the JSON data from the message
			String jsonStr = received.substring(6); // Remove "/vote/" prefix
			JSONObject voteData = new JSONObject(jsonStr);

			// Extract vote details
			String voterKey = voteData.getString("voter");
			String targetKey = voteData.getString("target");
			String meetingId = "default";

			System.out.println("SERVER: Received vote from " + clientAddress + ":" + clientPort);
			System.out.println("SERVER: Vote details - Voter: " + voterKey + ", Target: " + targetKey);

			// Process the vote
			handleVote(voterKey, targetKey, meetingId, logArea);
		} catch (Exception e) {
			System.err.println("SERVER ERROR: Failed to process vote message: " + e.getMessage());
			e.printStackTrace();
			log(logArea, "Error processing vote: " + e.getMessage());
		}
	}

	private static void handleKillMessage(String received, InetAddress clientAddress, int clientPort,
			TextArea logArea) {
		try {
			String jsonStr = received.substring(6); // Remove "/kill/" prefix
			JSONObject killReport = new JSONObject(jsonStr);

			String killedPlayerKey = killReport.getString("killedPlayer");
			String reporterKey = killReport.getString("reporter");

			System.out.println("SERVER: Received kill report from " + clientAddress + ":" + clientPort);
			System.out.println("SERVER: Kill details - Victim: " + killedPlayerKey + ", Reporter: " + reporterKey);

			// Process the kill report
			handleKillReport(killedPlayerKey, reporterKey, logArea);
		} catch (Exception e) {
			System.err.println("SERVER ERROR: Failed to process kill message: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static void handleMeetingMessage(String received, InetAddress clientAddress, int clientPort,
			TextArea logArea) {
		try {
			// Extract the JSON data from the message
			String jsonStr = received.substring(9); // Remove "/meeting/" prefix
			JSONObject meetingData = new JSONObject(jsonStr);

			String messageType = meetingData.getString("type");
			String senderKey = clientAddress.getHostAddress() + ":" + clientPort;

			// Handle different types of meeting messages
			switch (messageType) {
			case "chat":
				// Meeting chat message
				String chatMessage = meetingData.getString("message");
				String senderName = meetingData.getString("name");
				String senderStatus = meetingData.optString("status", "crewmate");
				boolean isGhostMessage = meetingData.optBoolean("isGhostMessage", false);

				// Get timestamp for deduplication if available
				long timestamp = meetingData.has("timestamp") ? meetingData.getLong("timestamp")
						: System.currentTimeMillis();

				// Create a unique message ID for server-side deduplication
				String messageId = senderName + ":" + chatMessage + ":" + timestamp;

				// Check if this is a duplicate on the server side
				if (recentChatMessages.contains(messageId)) {
					System.out.println("SERVER: Ignoring duplicate chat message: " + messageId);
					return;
				}

				// Add to recent messages
				recentChatMessages.add(messageId);

				// Limit cache size
				if (recentChatMessages.size() > 100) {
					// Keep only the most recent 50 messages
					recentChatMessages = recentChatMessages.stream().skip(recentChatMessages.size() - 50)
							.collect(java.util.stream.Collectors.toSet());
				}

				// Log the message
				if (isGhostMessage) {
					System.out.println("SERVER: Ghost " + senderName + " sent ghost message: " + chatMessage);
				} else {
					System.out.println("SERVER: " + senderName + " sent message: " + chatMessage);
				}

				// If server is a ghost, display ghost messages or if it's a regular message
				boolean serverIsGhost = "dead".equals(PlayerLogic.getStatus());
				if (!isGhostMessage || serverIsGhost) {
					// Display the message in the server's MeetingUI if appropriate
					if (GameWindow.getGameWindowInstance() != null) {
						MeetingUI activeMeeting = GameWindow.getGameWindowInstance().getActiveMeetingUI();
						if (activeMeeting != null) {
							activeMeeting.receiveChatMessage(senderName, chatMessage, senderStatus);
						}
					}
				}

				// Relay the chat message to appropriate clients
				relayMeetingChatToClients(senderKey, senderName, chatMessage, "default", senderStatus, timestamp,
						isGhostMessage, logArea);
				break;
			default:
				log(logArea, "Unknown meeting message type: " + messageType);
				break;
			}
		} catch (Exception e) {
			log(logArea, "Error handling meeting message: " + e.getMessage());
		}
	}

	private static void relayMeetingChatToClients(String senderKey, String senderName, String message, String meetingId,
			String senderStatus, long timestamp, boolean isGhostMessage, TextArea logArea) {
		try {
// Create JSON for the relay message
			JSONObject relayData = new JSONObject();
			relayData.put("type", "chat");
			relayData.put("name", senderName);
			relayData.put("message", message);
			relayData.put("meetingId", "default");
			relayData.put("status", senderStatus);
			relayData.put("timestamp", timestamp);
			relayData.put("isGhostMessage", isGhostMessage);

			String relayMessage = "/meeting/" + relayData.toString();
			byte[] buf = relayMessage.getBytes(StandardCharsets.UTF_8);

// Send to appropriate connected clients
			for (ClientInfo clientInfo : clientAddresses) {
				String clientKey = clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort();

// Skip the sender to avoid echoing back
				if (clientKey.equals(senderKey)) {
					continue;
				}

// For ghost messages, only send to other ghosts
				if (isGhostMessage) {
// Check if this client is a ghost
					PlayerInfo clientPlayer = GameLogic.playerList.get(clientKey);
					if (clientPlayer == null || !"dead".equals(clientPlayer.getStatus())) {
// Skip living players for ghost messages
						continue;
					}
				}

				try {
					DatagramPacket packet = new DatagramPacket(buf, buf.length, clientInfo.getAddress(),
							clientInfo.getPort());
					serverSocket.send(packet);
				} catch (IOException e) {
					log(logArea, "Error relaying meeting message to " + clientInfo.getAddress() + ":"
							+ clientInfo.getPort() + ": " + e.getMessage());
				}
			}
		} catch (Exception e) {
			log(logArea, "Error building relay message: " + e.getMessage());
		}
	}

	/**
	 * Handles player name registration
	 */
	private static void handleNameRegistration(String received, InetAddress clientAddress, int clientPort,
			TextArea logArea) {
		String clientName = received.substring(6);
		ClientInfo clientInfo = new ClientInfo(clientAddress, clientPort, clientName);
		clientAddresses.remove(clientInfo); // Remove old client info if exists
		clientAddresses.add(clientInfo); // Add updated client info
		log(logArea, clientName + " has connected");
	}

	/**
	 * Handles system messages from clients
	 */
	private static void handleSystemMessage(String received, InetAddress clientAddress, int clientPort,
			DatagramPacket packet, TextArea logArea) throws IOException {
		switch (received) {
		case "/sys/PING":
			sendPongResponse(clientAddress, clientPort);
			updateClientPingStatus(clientAddress, clientPort);
			break;

		case "/sys/Test_Handshake":
			sendAckResponse(clientAddress, clientPort);
			System.out.println("Sent ACK to client at " + clientAddress + ":" + clientPort);
			ServerSelectGui.settoGamedisable(false);
			break;

		case "/sys/ls":
			sendClientList(packet);
			log(logArea, "Sent client list to " + clientAddress + ":" + clientPort);
			break;

		default:
			System.out.println("Received unknown system message: " + received);
			break;
		}
	}

	private static void handleReportMessage(String received, InetAddress clientAddress, int clientPort,
			TextArea logArea) {
		try {
			// Extract the JSON data from the message
			String jsonStr = received.substring(8); // Remove "/report/" prefix
			JSONObject reportData = new JSONObject(jsonStr);

			// Extract report details
			String reporterKey = reportData.getString("reporter");
			String corpseKey = reportData.getString("corpse");

			System.out.println("SERVER: Received body report from " + clientAddress + ":" + clientPort);
			System.out.println("SERVER: Report details - Reporter: " + reporterKey + ", Corpse: " + corpseKey);

			// Call the existing handleBodyReport method
			handleBodyReport(reporterKey, corpseKey, logArea);

		} catch (Exception e) {
			System.err.println("ERROR: Failed to process report message: " + e.getMessage());
			e.printStackTrace();
			log(logArea, "Error processing body report: " + e.getMessage());
		}
	}

	/**
	 * Sends a PONG response to a client's PING
	 */
	private static void sendPongResponse(InetAddress clientAddress, int clientPort) throws IOException {
		String response = "/sys/PONG";
		byte[] buf = response.getBytes(StandardCharsets.UTF_8);
		DatagramPacket responsePacket = new DatagramPacket(buf, buf.length, clientAddress, clientPort);
		serverSocket.send(responsePacket);
	}

	/**
	 * Updates the ping status for a client
	 */
	private static void updateClientPingStatus(InetAddress clientAddress, int clientPort) {
		ClientInfo clientInfo = getClientInfo(clientAddress, clientPort);
		if (clientInfo != null) {
			clientPingCount.putIfAbsent(clientInfo, new AtomicInteger(0));
			clientPingCount.get(clientInfo).set(0);
		}
	}

	/**
	 * Sends an ACK response to a client's handshake request
	 */
	private static void sendAckResponse(InetAddress clientAddress, int clientPort) throws IOException {
		String response = "/sys/ACK";
		byte[] buf = response.getBytes(StandardCharsets.UTF_8);
		DatagramPacket responsePacket = new DatagramPacket(buf, buf.length, clientAddress, clientPort);
		serverSocket.send(responsePacket);
	}

	/**
	 * Sends the list of connected clients to a requester
	 */
	private static void sendClientList(DatagramPacket packet) throws IOException {
		StringBuilder response = new StringBuilder("/ls/");
		response.append("======LIST OF PLAYERS======\n");

		try {
			response.append(InetAddress.getLocalHost().getHostAddress()).append(":").append(serverSocket.getLocalPort())
					.append(" - ").append(MainMenuPane.getServerName()).append("\n");

			if (!clientAddresses.isEmpty()) {
				for (ClientInfo clientInfo : clientAddresses) {
					String key = clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort();
					response.append(key).append(" - ").append(clientInfo.getName());
				}
			}

			byte[] responseBuf = response.toString().getBytes(StandardCharsets.UTF_8);
			DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length, packet.getAddress(),
					packet.getPort());
			serverSocket.send(responsePacket);
		} catch (UnknownHostException e) {
			System.err.println("Failed to get local host: " + e.getMessage());
		}
	}

	/**
	 * Handles player data updates from clients
	 */
	private static void handlePlayerData(String received, DatagramPacket packet) throws IOException {
		String jsonStr = received.substring(6);
		JSONObject json = new JSONObject(jsonStr);

		// Extract player data
		double posX = json.getDouble("PosX");
		double posY = json.getDouble("PosY");
		int direction = json.getInt("Direction");
		boolean isMoving = json.getBoolean("isMoving");
		String name = json.getString("name");
		int charID = json.getInt("charID");
		boolean isReady = json.getBoolean("playerReady");
		double taskFinish = json.getDouble("task");
		// Update or create player info
		String clientKey = packet.getAddress().getHostAddress() + ":" + packet.getPort();
		updatePlayerInfo(clientKey, packet, posX, posY, direction, isMoving, name, charID, isReady, taskFinish);

		// Prepare and send response with all player data
		sendPlayerDataResponse(packet);
	}

	/**
	 * Updates player information in the global player list
	 */
	private static void updatePlayerInfo(String clientKey, DatagramPacket packet, double posX, double posY,
			int direction, boolean isMoving, String name, int charID, boolean isReady, double taskfinished) {
		PlayerInfo playerInfo = GameLogic.playerList.get(clientKey);
		if (playerInfo == null) {
			// New player - create new player info
			Random random = new Random();
			int randomChar = random.nextInt(9);
			playerInfo = new PlayerInfo(packet.getAddress(), packet.getPort(), name, 0, 0, false, 0, "crewmate",
					randomChar);
			playerInfo.setTaskPercent(taskfinished);
			GameLogic.playerList.put(clientKey, playerInfo);
		} else {
			// Existing player - update data
			playerInfo.setX(posX);
			playerInfo.setY(posY);
			playerInfo.setDirection(direction);
			playerInfo.setMoving(isMoving);
			playerInfo.setCharacterID(charID);
			playerInfo.setReady(isReady);
			playerInfo.setTaskPercent(taskfinished);
		}
	}

	/**
	 * Prepares and sends a response with all player data to the client
	 */
	private static void sendPlayerDataResponse(DatagramPacket packet) throws IOException {
		try {
			JSONObject responseJson = new JSONObject();

			// Add server's data
			String serverKey = InetAddress.getLocalHost().getHostAddress() + ":" + serverSocket.getLocalPort();
			JSONObject serverData = new JSONObject();
			serverData.put("position", new double[] { PlayerLogic.getMyPosX(), PlayerLogic.getMyPosY() });
			serverData.put("name", MainMenuPane.getServerName());
			serverData.put("Direction", PlayerLogic.getDirection());
			serverData.put("isMoving", PlayerLogic.getMoving());
			serverData.put("charID", PlayerLogic.getCharID());
			serverData.put("prepEnded", GameLogic.isPrepEnded());
			serverData.put("Status", PlayerLogic.getStatus());
			serverData.put("task", PlayerLogic.getTaskPercent());

			responseJson.put(serverKey, serverData);

			// Add all other players' data
			for (PlayerInfo info : GameLogic.playerList.values()) {
				String key = info.getAddress().getHostAddress() + ":" + info.getPort();
				JSONObject playerData = new JSONObject();
				playerData.put("position", new double[] { info.getX(), info.getY() });
				playerData.put("name", info.getName());
				playerData.put("status", info.getStatus());
				playerData.put("Direction", info.getDirection());
				playerData.put("isMoving", info.isMoving());
				playerData.put("charID", info.getCharacterID());
				playerData.put("prepEnded", GameLogic.isPrepEnded());
				playerData.put("Status", info.getStatus());
				playerData.put("task", info.getTaskPercent());

				responseJson.put(key, playerData);
			}

			// Send the response
			String response = "/data/" + responseJson.toString();
			byte[] responseBuf = response.getBytes(StandardCharsets.UTF_8);
			DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length, packet.getAddress(),
					packet.getPort());
			serverSocket.send(responsePacket);
		} catch (UnknownHostException e) {
			System.err.println("Error getting local host: " + e.getMessage());
		}
	}

	/**
	 * Handles chat messages from clients
	 */
	private static void handleChatMessage(String message, InetAddress clientAddress, int clientPort, TextArea logArea) {
		ClientInfo clientInfo = getClientInfo(clientAddress, clientPort);
		if (clientInfo != null) {
			log(logArea, clientInfo.getName() + " : " + message);
			relayMessageToClients(clientInfo, message, logArea);
		} else {
			log(logArea, "Player : " + message + " from " + clientAddress + ":" + clientPort);
		}
	}

	/**
	 * Relays a message from one client to all other clients
	 */
	private static void relayMessageToClients(ClientInfo sender, String message, TextArea logArea) {
		String relayMessage = "/r/" + sender.getName() + " : " + message;
		byte[] buf = relayMessage.getBytes(StandardCharsets.UTF_8);

		for (ClientInfo clientInfo : clientAddresses) {
			if (!clientInfo.equals(sender)) {
				try {
					DatagramPacket packet = new DatagramPacket(buf, buf.length, clientInfo.getAddress(),
							clientInfo.getPort());
					serverSocket.send(packet);
				} catch (IOException e) {
					log(logArea, "Error relaying message to " + clientInfo.getAddress() + ":" + clientInfo.getPort()
							+ ": " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Finds a client's info based on address and port
	 */
	private static ClientInfo getClientInfo(InetAddress address, int port) {
		for (ClientInfo clientInfo : clientAddresses) {
			if (clientInfo.getAddress().equals(address) && clientInfo.getPort() == port) {
				return clientInfo;
			}
		}
		return null;
	}

	/**
	 * Checks for clients that have missed too many pings
	 */
	private static void checkClientPings(TextArea logArea) {
		for (ClientInfo clientInfo : new HashSet<>(clientPingCount.keySet())) {
			int missedPings = clientPingCount.get(clientInfo).incrementAndGet();
			if (missedPings > MAX_MISSED_PINGS) {
				log(logArea, "Client " + clientInfo.getAddress() + ":" + clientInfo.getPort() + " has missed "
						+ MAX_MISSED_PINGS + " PINGs and is considered disconnected.");
				clientPingCount.remove(clientInfo);
				clientAddresses.remove(clientInfo);

				// Remove player from player list
				String clientKey = clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort();
				GameLogic.playerList.remove(clientKey);
			}
		}
	}

	/**
	 * Stops the server and cleans up resources
	 */
	public static void stopServer() {
		isRunning = false;

		clientPingCount.clear();
		clientAddresses.clear();
		GameLogic.playerList.clear();

		if (serverSocket != null && !serverSocket.isClosed()) {
			serverSocket.close();
		}

		if (serverThread != null && serverThread.isAlive()) {
			serverThread.interrupt();
		}

		if (pingCheckTimer != null) {
			pingCheckTimer.cancel();
			pingCheckTimer = null;
		}
	}

	/**
	 * Sends a message from the server to all connected clients
	 */
	public static void sendMessageToClients(String message, TextArea logArea) {
		if (clientAddresses.isEmpty()) {
			log(logArea, "No connected clients to send the message.");
			return;
		}

		try {
			String servermsg = "/sname/" + MainMenuPane.getServerName() + " : " + message;
			byte[] buf = servermsg.getBytes(StandardCharsets.UTF_8);

			for (ClientInfo clientInfo : clientAddresses) {
				DatagramPacket packet = new DatagramPacket(buf, buf.length, clientInfo.getAddress(),
						clientInfo.getPort());
				serverSocket.send(packet);
			}

			log(logArea, "You : " + message);
		} catch (IOException e) {
			log(logArea, "Error sending message to clients : " + e.getMessage());
		}
	}

	/**
	 * Logs a message to the text area on the JavaFX application thread
	 */
	private static void log(TextArea logArea, String message) {
		if (logArea != null)
			Platform.runLater(() -> logArea.appendText(message + "\n"));
		else
			System.out.println("logArea missing " + message);
	}

	/**
	 * Checks how many players are ready and updates the UI accordingly
	 */
	private static void checkReadyPlayers() {
		try {
			long readyPlayerCount = GameLogic.playerList.values().stream().filter(PlayerInfo::isReady).count();

			Platform.runLater(() -> {
				if (readyPlayerCount == GameLogic.playerList.size() && !GameLogic.playerList.isEmpty()) {
					PrepGui.setReadydisable(false);
				} else {
					PrepGui.setReadydisable(true);
				}
			});
		} catch (Exception e) {
			System.err.println("Error in checkReadyPlayers: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Randomly assigns imposter roles to players
	 */
	public static void randomizeImposters() {
		List<String> allPlayers = new ArrayList<>();
		String serverKey = PlayerLogic.getLocalAddressPort();
		allPlayers.add(serverKey);
		allPlayers.addAll(GameLogic.playerList.keySet());

		// Determine how many imposters to assign
		int imposterCount = GameLogic.getImposterCount();

		// Shuffle the player list for random selection
		Collections.shuffle(allPlayers);

		// Set all players as crewmates initially
		PlayerLogic.setStatus("crewmate");
		for (String key : GameLogic.playerList.keySet()) {
			PlayerInfo player = GameLogic.playerList.get(key);
			player.setStatus("crewmate");
		}

		// Select imposters from the shuffled list
		List<String> imposterKeys = allPlayers.subList(0, Math.min(imposterCount, allPlayers.size()));

		// Set selected players as imposters
		for (String key : imposterKeys) {
			if (key.equals(serverKey)) {
				// Local player (server/host) is an imposter
				PlayerLogic.setStatus("imposter");
				System.out.println("You are an imposter!");
			} else {
				// A client is an imposter
				PlayerInfo player = GameLogic.playerList.get(key);
				if (player != null) {
					player.setStatus("imposter");
					System.out.println("Imposter assigned: " + player.getName());
				}
			}
		}

		// Log info about the game configuration
		System.out.println("Game started with " + imposterCount + " imposter(s) and "
				+ (allPlayers.size() - imposterCount) + " crewmate(s)");
	}

	public static void handleKillReport(String killedPlayerKey, String reporterKey, TextArea logArea) {
		try {
			System.out.println(
					"SERVER: Processing kill report - victim: " + killedPlayerKey + ", killer: " + reporterKey);

			// Find the killed player
			PlayerInfo killedPlayer = null;

			// Check if it's the server player
			if (PlayerLogic.getLocalAddressPort().equals(killedPlayerKey)) {
				System.out.println("SERVER: The server player got killed");

				// Flag as killed instead of setting status directly
				PlayerLogic.flagKilled(true);

				// Create a special corpse for the server player
				Corpse serverCorpse = new Corpse(new PlayerInfo(InetAddress.getLocalHost(), serverSocket.getLocalPort(),
						MainMenuPane.getServerName(), PlayerLogic.getMyPosX(), PlayerLogic.getMyPosY(), false,
						PlayerLogic.getDirection(), "dead", PlayerLogic.getCharID()));

				GameLogic.corpseList.put(killedPlayerKey, serverCorpse);
				System.out.println("SERVER: Created corpse for server player at " + PlayerLogic.getMyPosX() + ","
						+ PlayerLogic.getMyPosY());

				// Broadcast the kill to all clients
				broadcastKillReport(killedPlayerKey, serverCorpse);

				return;
			} else {
				// Find the client player
				killedPlayer = GameLogic.playerList.get(killedPlayerKey);
			}

			if (killedPlayer == null) {
				log(logArea, "Error: Player with key " + killedPlayerKey + " not found");
				System.out.println("SERVER ERROR: Victim player not found in player list: " + killedPlayerKey);
				System.out.println("SERVER: Available players: " + GameLogic.playerList.keySet());
				return;
			}

			// Mark the player as dead
			killedPlayer.setStatus("dead");
			log(logArea,
					"Player " + killedPlayer.getName() + " has been killed by "
							+ (GameLogic.playerList.containsKey(reporterKey)
									? GameLogic.playerList.get(reporterKey).getName()
									: "Unknown"));

			// Create a corpse
			Corpse corpse = GameLogic.createCorpse(killedPlayer);
			System.out.println("SERVER: Created corpse at " + corpse.getX() + "," + corpse.getY() + " for player "
					+ killedPlayer.getName());

			// Broadcast the kill to all clients
			broadcastKillReport(killedPlayerKey, corpse);
		} catch (Exception e) {
			System.err.println("SERVER ERROR in handleKillReport: " + e.getMessage());
			e.printStackTrace();
			log(logArea, "Error processing kill report: " + e.getMessage());
		}
	}

	// In ServerLogic.java, update the endMeetingAndBroadcastResults method to use
	// the specific ejection status:

	public static void endMeetingAndBroadcastResults(String meetingId, TextArea logArea) {
		try {
			// Calculate results
			meetingId = "default";
			String ejectedPlayerKey = calculateVotingResult(meetingId);
			System.out.println("VOTING RESULT = " + ejectedPlayerKey);

			// Get vote counts
			Map<String, String> votesMap = meetingVotes.getOrDefault(meetingId, new HashMap<>());
			Map<String, Integer> voteCounts = new HashMap<>();
			for (String targetKey : votesMap.values()) {
				voteCounts.put(targetKey, voteCounts.getOrDefault(targetKey, 0) + 1);
			}

			// Handle ejection locally first
			if (ejectedPlayerKey != null) {
				boolean wasImposter = false;

				// If local player was ejected
				if (ejectedPlayerKey.equals(PlayerLogic.getLocalAddressPort())) {
					wasImposter = "imposter".equals(PlayerLogic.getStatus());

					// Flag as ejected instead of status directly
					PlayerLogic.flagEjected(true);

					System.out.println("SERVER: Local player (you) has been flagged for ejection");
					System.out.println("SERVER: Local player wasImposter: " + wasImposter);
				}
				// If another player was ejected
				else if (GameLogic.playerList.containsKey(ejectedPlayerKey)) {
					PlayerInfo player = GameLogic.playerList.get(ejectedPlayerKey);
					if (player != null) {
						wasImposter = "imposter".equals(player.getStatus());

						// For other players, just set to dead
						player.setStatus("dead");

						System.out.println("SERVER: Player " + player.getName() + " has been ejected");
						System.out.println("SERVER: Ejected player wasImposter: " + wasImposter);
					}
				}

				// Create results data JSON with imposter info
				JSONObject resultsData = new JSONObject();
				resultsData.put("ejected", ejectedPlayerKey != null ? ejectedPlayerKey : JSONObject.NULL);
				resultsData.put("meetingId", "default");
				resultsData.put("time", System.currentTimeMillis());
				resultsData.put("wasImposter", wasImposter); // Include imposter status in results
				voteCounts.put("wasImposter", wasImposter ? 1 : 0);
				// Convert vote counts to JSON
				JSONObject votesJson = new JSONObject();
				for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
					votesJson.put(entry.getKey(), entry.getValue());
				}
				resultsData.put("votes", votesJson);

				String resultsMessage = "/results/" + resultsData.toString();
				byte[] buf = resultsMessage.getBytes(StandardCharsets.UTF_8);

				System.out.println("SERVER: Broadcasting voting results: " + resultsMessage);

				// Send to all clients
				for (ClientInfo clientInfo : clientAddresses) {
					try {
						DatagramPacket packet = new DatagramPacket(buf, buf.length, clientInfo.getAddress(),
								clientInfo.getPort());

						// Send multiple times to ensure delivery
						for (int i = 0; i < 5; i++) {
							serverSocket.send(packet);
							Thread.sleep(50);
						}

						System.out.println("SERVER: Sent voting results to " + clientInfo.getAddress() + ":"
								+ clientInfo.getPort());
					} catch (Exception e) {
						log(logArea, "Error sending results to " + clientInfo.getAddress() + ":" + clientInfo.getPort()
								+ ": " + e.getMessage());
					}
				}

				// Log the results
				if (ejectedPlayerKey == null) {
					log(logArea, "No one was ejected.");
				} else {
					String playerName = ejectedPlayerKey.equals(PlayerLogic.getLocalAddressPort())
							? PlayerLogic.getName()
							: GameLogic.playerList.containsKey(ejectedPlayerKey)
									? GameLogic.playerList.get(ejectedPlayerKey).getName()
									: "Unknown";
					log(logArea, playerName + " was ejected.");
				}

				// Update local UI if present
				if (GameWindow.getGameWindowInstance() != null) {
					MeetingUI activeMeeting = GameWindow.getGameWindowInstance().getActiveMeetingUI();
					if (activeMeeting != null) {
						activeMeeting.showVotingResults(ejectedPlayerKey, voteCounts);
					}
				}
			} else {
				// No player ejected case - original code remains the same
				JSONObject resultsData = new JSONObject();
				resultsData.put("ejected", JSONObject.NULL);
				resultsData.put("meetingId", "default");
				resultsData.put("time", System.currentTimeMillis());

				// Convert vote counts to JSON
				JSONObject votesJson = new JSONObject();
				for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
					votesJson.put(entry.getKey(), entry.getValue());
				}
				resultsData.put("votes", votesJson);

				String resultsMessage = "/results/" + resultsData.toString();
				byte[] buf = resultsMessage.getBytes(StandardCharsets.UTF_8);

				System.out.println("SERVER: Broadcasting voting results: " + resultsMessage);

				// Send to all clients
				for (ClientInfo clientInfo : clientAddresses) {
					try {
						DatagramPacket packet = new DatagramPacket(buf, buf.length, clientInfo.getAddress(),
								clientInfo.getPort());

						// Send multiple times to ensure delivery
						for (int i = 0; i < 5; i++) {
							serverSocket.send(packet);
							Thread.sleep(50);
						}

						System.out.println("SERVER: Sent voting results to " + clientInfo.getAddress() + ":"
								+ clientInfo.getPort());
					} catch (Exception e) {
						log(logArea, "Error sending results to " + clientInfo.getAddress() + ":" + clientInfo.getPort()
								+ ": " + e.getMessage());
					}
				}

				// Log the results
				log(logArea, "No one was ejected.");

				// Update local UI if present
				if (GameWindow.getGameWindowInstance() != null) {
					MeetingUI activeMeeting = GameWindow.getGameWindowInstance().getActiveMeetingUI();
					if (activeMeeting != null) {
						activeMeeting.showVotingResults(null, voteCounts);
					}
				}
			}
			meetingVotes.remove(meetingId);

		} catch (Exception e) {
			log(logArea, "Error broadcasting voting results: " + e.getMessage());
			System.err.println("SERVER ERROR broadcasting voting results: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static void broadcastKillReport(String killedPlayerKey, Corpse corpse) {
		try {
			// Prepare the kill report with all necessary information
			JSONObject killData = new JSONObject();
			killData.put("killedPlayer", killedPlayerKey);
			killData.put("playerName", corpse.getPlayerName());
			killData.put("x", corpse.getX());
			killData.put("y", corpse.getY());
			killData.put("characterID", corpse.getCharacterID());
			killData.put("timeOfDeath", System.currentTimeMillis());

			String killReport = "/kill/" + killData.toString();
			byte[] data = killReport.getBytes(StandardCharsets.UTF_8);

			System.out.println("SERVER: Broadcasting kill report: " + killReport);

			// Send to all clients
			for (ClientInfo clientInfo : clientAddresses) {
				try {
					DatagramPacket packet = new DatagramPacket(data, data.length, clientInfo.getAddress(),
							clientInfo.getPort());

					// Send multiple times to reduce chance of packet loss
					for (int i = 0; i < 3; i++) {
						serverSocket.send(packet);
						Thread.sleep(50); // Short delay between retransmissions
					}

					System.out.println(
							"SERVER: Kill report sent to " + clientInfo.getAddress() + ":" + clientInfo.getPort());
				} catch (Exception e) {
					System.err.println("SERVER ERROR sending kill report to " + clientInfo.getAddress() + ":"
							+ clientInfo.getPort() + ": " + e.getMessage());
				}
			}
		} catch (Exception e) {
			System.err.println("SERVER ERROR in broadcastKillReport: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void handleBodyReport(String reporterKey, String corpsePlayerKey, TextArea logArea) {
		PlayerInfo reporter = GameLogic.playerList.get(reporterKey);
		Corpse corpse = GameLogic.getCorpse(corpsePlayerKey);
		System.out.println("List of Corpses = " + GameLogic.corpseList.size());
		System.out.println(corpse.getPlayerName() + " has been found");
		// Mark body as found if not already found
		if (!corpse.isFound()) {
			corpse.setFound(true);
			// log(logArea, reporter.getName() + " reported " + corpse.getPlayerName() + "'s
			// body");

			broadcastEmergencyMeeting(reporterKey, corpse.getPlayerName(), corpse.getCharacterID());
		}
	}

	private static void broadcastEmergencyMeeting(String reporterKey, String reportedPlayerName, int reportedCharId) {
		try {
			// Create meeting data
			JSONObject meetingData = new JSONObject();
			meetingData.put("reporter", reporterKey);
			meetingData.put("reportedPlayer", reportedPlayerName);
			meetingData.put("reportedCharId", reportedCharId);
			meetingData.put("time", System.currentTimeMillis());

			String meetingMessage = "/meeting/" + meetingData.toString();
			byte[] buf = meetingMessage.getBytes(StandardCharsets.UTF_8);

			// Send to all connected clients
			System.out.println(clientAddresses.size());
			for (ClientInfo clientInfo : clientAddresses) {
				try {
					DatagramPacket packet = new DatagramPacket(buf, buf.length, clientInfo.getAddress(),
							clientInfo.getPort());
					serverSocket.send(packet);

					// Send multiple times to reduce chance of packet loss
					for (int i = 0; i < 5; i++) {
						Thread.sleep(50); // Short delay between retransmissions
						serverSocket.send(packet);
					}

					System.out.println(
							"Emergency meeting broadcast to " + clientInfo.getAddress() + ":" + clientInfo.getPort());
				} catch (Exception e) {
					System.err.println("Error sending meeting message to " + clientInfo.getAddress() + ":"
							+ clientInfo.getPort() + ": " + e.getMessage());
				}
			}

			// Also trigger meeting locally on the server
			if (GameWindow.getGameWindowInstance() != null) {
				Platform.runLater(() -> {
					GameWindow.getGameWindowInstance().startEmergencyMeeting(reporterKey, reportedPlayerName,
							reportedCharId);
				});
			}

		} catch (Exception e) {
			System.err.println("Error in broadcastEmergencyMeeting: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Handles a vote from a client
	 * 
	 * @param voterKey  The key of the voting player
	 * @param targetKey The key of the target player (or "skip")
	 * @param meetingId The unique ID of the meeting
	 * @param logArea   TextArea for logging
	 */
	public static void handleVote(String voterKey, String targetKey, String meetingId, TextArea logArea) {
		try {
			meetingId = "default";
			// Ensure meetingVotes map exists for this meeting
			meetingVotes.putIfAbsent(meetingId, new ConcurrentHashMap<>());
			Map<String, String> votes = meetingVotes.get(meetingId);

			// Check for duplicate vote - if already voted for the same target, ignore
			String existingVote = votes.get(voterKey);
			if (existingVote != null && existingVote.equals(targetKey)) {
				System.out.println(
						"SERVER: Duplicate vote received from " + voterKey + " for " + targetKey + " - ignoring");
				return;
			}

			// If player already voted for someone else, log the vote change
			if (existingVote != null && !existingVote.equals(targetKey)) {
				System.out.println(
						"SERVER: Player " + voterKey + " changed vote from " + existingVote + " to " + targetKey);
			}

			// Store vote
			votes.put(voterKey, targetKey);

			System.out.println(
					"SERVER: Received vote from " + voterKey + " for " + targetKey + " in meeting " + meetingId);
			System.out.println("SERVER: Current votes in meeting: " + votes);
			System.out.println("Total votes : " + votes.size());

			// Create vote data JSON to broadcast to clients
			JSONObject voteData = new JSONObject();
			voteData.put("voter", voterKey);
			voteData.put("target", targetKey);
			voteData.put("meetingId", "default");
			voteData.put("time", System.currentTimeMillis());
			// Add a unique vote ID to help clients identify duplicate messages
			voteData.put("voteId", voterKey + "_" + System.currentTimeMillis());

			String voteMessage = "/vote/" + voteData.toString();
			byte[] buf = voteMessage.getBytes(StandardCharsets.UTF_8);

			// Send to all clients
			for (ClientInfo clientInfo : clientAddresses) {
				try {
					DatagramPacket packet = new DatagramPacket(buf, buf.length, clientInfo.getAddress(),
							clientInfo.getPort());
					serverSocket.send(packet);

					// Send multiple times to ensure delivery
					for (int i = 0; i < 2; i++) {
						Thread.sleep(30);
						serverSocket.send(packet);
					}
				} catch (Exception e) {
					log(logArea, "Error sending vote to " + clientInfo.getAddress() + ":" + clientInfo.getPort() + ": "
							+ e.getMessage());
				}
			}

			// Log the vote
			String voterName = voterKey.equals(PlayerLogic.getLocalAddressPort()) ? PlayerLogic.getName()
					: GameLogic.playerList.containsKey(voterKey) ? GameLogic.playerList.get(voterKey).getName()
							: "Unknown";

			String targetName;
			if (targetKey.equals("skip")) {
				targetName = "Skip";
			} else {
				targetName = targetKey.equals(PlayerLogic.getLocalAddressPort()) ? PlayerLogic.getName()
						: GameLogic.playerList.containsKey(targetKey) ? GameLogic.playerList.get(targetKey).getName()
								: "Unknown";
			}

			// Update the local meeting UI if present
			if (GameWindow.getGameWindowInstance() != null) {
				MeetingUI activeMeeting = GameWindow.getGameWindowInstance().getActiveMeetingUI();
				if (activeMeeting != null) {
					activeMeeting.receiveVote(voterKey, targetKey);
					activeMeeting.addChatMessage("SYSTEM", voterName + " voted for " + targetName);
				}
			}

		} catch (Exception e) {
			log(logArea, "Error handling vote: " + e.getMessage());
			System.err.println("SERVER ERROR handling vote: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Calculates voting results for a specific meeting
	 * 
	 * @param meetingId The meeting ID
	 * @return The key of the ejected player, or null if no one was ejected
	 */
	public static String calculateVotingResult(String meetingId) {
	    meetingId = "default";
	    Map<String, String> votesMap = meetingVotes.getOrDefault(meetingId, new HashMap<>());

	    // If no votes, no one is ejected
	    if (votesMap.isEmpty()) {
	        System.out.println("SERVER: No votes were cast in meeting " + meetingId);
	        return null;
	    }

	    // Count votes for each target
	    Map<String, Integer> voteCounts = new HashMap<>();
	    for (String targetKey : votesMap.values()) {
	        voteCounts.put(targetKey, voteCounts.getOrDefault(targetKey, 0) + 1);
	    }

	    System.out.println("SERVER: Vote counts for meeting " + meetingId + ": " + voteCounts);

	    // Count skip votes
	    int skipVotes = voteCounts.getOrDefault("skip", 0);
	    
	    // Find player with most votes and check for ties
	    String mostVotedPlayer = null;
	    int highestVotes = 0;
	    boolean hasTie = false;

	    // First pass to find highest vote count among players (excluding "skip")
	    for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
	        if (!entry.getKey().equals("skip") && entry.getValue() > highestVotes) {
	            highestVotes = entry.getValue();
	            mostVotedPlayer = entry.getKey();
	            hasTie = false;
	        } else if (!entry.getKey().equals("skip") && entry.getValue() == highestVotes) {
	            // We have a tie between player votes
	            hasTie = true;
	        }
	    }

	    // Check if skip wins
	    if (skipVotes > highestVotes) {
	        // Skip wins outright
	        System.out.println("SERVER: Skip won the vote with " + skipVotes + " votes");
	        return null; // No one is ejected
	    } else if (skipVotes == highestVotes && highestVotes > 0) {
	        // Skip ties with highest player vote
	        System.out.println("SERVER: Skip tied with player votes at " + skipVotes + " votes");
	        return null; // No one is ejected in a tie
	    }

	    // Check if there's a tie between players
	    if (hasTie) {
	        System.out.println("SERVER: There was a tie between players with " + highestVotes + " votes each");
	        return null; // No one is ejected in a tie
	    }

	    // Return the player with the most votes if they exist and have at least one vote
	    if (mostVotedPlayer != null && highestVotes > 0) {
	        System.out.println("SERVER: " + mostVotedPlayer + " was ejected with " + highestVotes + " votes");
	        return mostVotedPlayer;
	    } else {
	        System.out.println("SERVER: No one was ejected (no valid votes)");
	        return null;
	    }
	}


	public static DatagramSocket getServerSocket() {
		return serverSocket;
	}

	public static int getReadyPlayerCount() {
		return readyPlayers;
	}

	public static Set<ClientInfo> getConnectedClients() {
		return new HashSet<>(clientAddresses);
	}
}
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
import java.util.HashSet;
import java.util.List;
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
import gui.MainMenuPane;
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
			// Add dedicated handler for kill messages
			handleKillMessage(received, clientAddress, clientPort, logArea);
		} else {
			handleChatMessage(received, clientAddress, clientPort, logArea);
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

		// Update or create player info
		String clientKey = packet.getAddress().getHostAddress() + ":" + packet.getPort();
		updatePlayerInfo(clientKey, packet, posX, posY, direction, isMoving, name, charID, isReady);

		// Prepare and send response with all player data
		sendPlayerDataResponse(packet);
	}

	/**
	 * Updates player information in the global player list
	 */
	private static void updatePlayerInfo(String clientKey, DatagramPacket packet, double posX, double posY,
			int direction, boolean isMoving, String name, int charID, boolean isReady) {
		PlayerInfo playerInfo = GameLogic.playerList.get(clientKey);
		if (playerInfo == null) {
			// New player - create new player info
			Random random = new Random();
			int randomChar = random.nextInt(9);
			playerInfo = new PlayerInfo(packet.getAddress(), packet.getPort(), name, 0, 0, false, 0, "crewmate",
					randomChar);
			GameLogic.playerList.put(clientKey, playerInfo);
		} else {
			// Existing player - update data
			playerInfo.setX(posX);
			playerInfo.setY(posY);
			playerInfo.setDirection(direction);
			playerInfo.setMoving(isMoving);
			playerInfo.setCharacterID(charID);
			playerInfo.setReady(isReady);
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
			// Find the killed player
			if (PlayerLogic.getLocalAddressPort().equals(killedPlayerKey)) {
				System.out.println("WE GOT KILLED");
				PlayerLogic.setStatus("dead");
			} else {
				PlayerInfo killedPlayer = GameLogic.playerList.get(killedPlayerKey);
				PlayerInfo reporter = GameLogic.playerList.get(reporterKey);

				if (killedPlayer == null || reporter == null) {
					log(logArea, "Error: Invalid kill report");
					return;
				}

				// Mark the player as dead
				killedPlayer.setStatus("dead");
				log(logArea, "Player " + killedPlayer.getName() + " has been killed");

				// Create a corpse
				Corpse corpse = GameLogic.createCorpse(killedPlayer);

				// Broadcast the kill to all clients
				broadcastKillReport(killedPlayerKey, corpse);

				// No immediate emergency meeting
				// Players will need to find and report the body
			}
		} catch (

		Exception e) {
			log(logArea, "Error processing kill report: " + e.getMessage());
		}
	}

	private static void broadcastKillReport(String killedPlayerKey, Corpse corpse) {
		try {
			// Prepare JSON payload for dead body
			JSONObject bodyReport = new JSONObject();
			bodyReport.put("killedPlayer", killedPlayerKey);
			bodyReport.put("playerName", corpse.getPlayerName());
			bodyReport.put("x", corpse.getX());
			bodyReport.put("y", corpse.getY());
			bodyReport.put("characterID", corpse.getCharacterID());
			bodyReport.put("timeOfDeath", System.currentTimeMillis()); // Add timestamp

			// Convert to message format
			String message = "/kill/" + bodyReport.toString();
			byte[] buf = message.getBytes(StandardCharsets.UTF_8);

			System.out.println("SERVER: Broadcasting kill report for " + corpse.getPlayerName() + " at " + corpse.getX()
					+ "," + corpse.getY());

			// Send to all connected clients
			for (ClientInfo clientInfo : clientAddresses) {
				try {
					DatagramPacket packet = new DatagramPacket(buf, buf.length, clientInfo.getAddress(),
							clientInfo.getPort());
					serverSocket.send(packet);
					System.out.println(
							"SERVER: Kill report sent to " + clientInfo.getAddress() + ":" + clientInfo.getPort());
				} catch (IOException e) {
					System.err.println("Error broadcasting dead body: " + e.getMessage());
				}
			}
		} catch (

		Exception e) {
			System.err.println("Error in broadcastDeadBody: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void handleBodyReport(String reporterKey, String corpsePlayerKey, TextArea logArea) {
		PlayerInfo reporter = GameLogic.playerList.get(reporterKey);
		Corpse corpse = GameLogic.getCorpse(corpsePlayerKey);

		if (corpse == null || reporter == null) {
			log(logArea, "Invalid body report");
			return;
		}

		// Mark body as found if not already found
		if (!corpse.isFound()) {
			corpse.setFound(true);
			log(logArea, reporter.getName() + " reported " + corpse.getPlayerName() + "'s body");

			// Broadcast body report to all clients
			broadcastBodyReport(reporterKey, corpsePlayerKey);
		}
	}

	private static void broadcastBodyReport(String reporterKey, String corpsePlayerKey) {
		try {
			JSONObject reportJSON = new JSONObject();
			reportJSON.put("reporter", reporterKey);
			reportJSON.put("deadBody", corpsePlayerKey);

			String message = "/report/" + reportJSON.toString();
			byte[] buf = message.getBytes(StandardCharsets.UTF_8);

			// Send to all connected clients
			for (ClientInfo clientInfo : clientAddresses) {
				try {
					DatagramPacket packet = new DatagramPacket(buf, buf.length, clientInfo.getAddress(),
							clientInfo.getPort());
					serverSocket.send(packet);
				} catch (IOException e) {
					System.err.println("Error broadcasting body report: " + e.getMessage());
				}
			}
		} catch (Exception e) {
			System.err.println("Error in broadcastBodyReport: " + e.getMessage());
		}
	}

	// Method to check if a body can be reported at a given location
	public static String findReportableBodyNearby(double x, double y, double maxDistance) {
		for (String key : GameLogic.playerList.keySet()) {
			PlayerInfo player = GameLogic.playerList.get(key);

			// Check for dead players that haven't been found yet
			if (player.getStatus().equals("dead") && !player.isFound()) {
				double dx = player.getX() - x;
				double dy = player.getY() - y;
				double distance = Math.sqrt(dx * dx + dy * dy);

				if (distance <= maxDistance) {
					return key;
				}
			}
		}
		return null;
	}

	public static DatagramSocket getServerSocket() {
		return serverSocket;
	}

	public static int getReadyPlayerCount() {
		return readyPlayers;
	}
}
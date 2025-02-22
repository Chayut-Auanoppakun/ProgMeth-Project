package server;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;

import application.Main;
import application.SharedState;

public class GameServer {
	private static DatagramSocket serverSocket;
	private static Thread serverThread;
	private static Set<ClientInfo> clientAddresses = new HashSet<>();
	private static ConcurrentHashMap<ClientInfo, AtomicInteger> clientPingCount = new ConcurrentHashMap<>();
	private static Timer pingCheckTimer;
	private static double serverX = 300;
	private static double serverY = 300;
	private static double deltaX = 0;
	private static double deltaY = 0;

	private static ConcurrentHashMap<String, PlayerInfo> playerPositions = new ConcurrentHashMap<>();

	public static void startBroadcasting(SharedState state, TextArea logArea, String serverName, int serverPort) {
		Thread thread = new Thread(() -> {
			try {
				DatagramSocket socket = new DatagramSocket();
				socket.setBroadcast(true);
				String broadcastMessage = serverName + ":" + serverPort; // Include port in broadcast message
				byte[] buf = broadcastMessage.getBytes(StandardCharsets.UTF_8);
				InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
				DatagramPacket packet = new DatagramPacket(buf, buf.length, broadcastAddress, 4446);

				while (state.isBroadcast()) {
					socket.send(packet);
					// log(logArea, "Broadcast sent.");
					Thread.sleep(1000);
				}
				socket.close();
			} catch (IOException | InterruptedException e) {
				log(logArea, "Error: " + e.getMessage());
				System.out.println("Error: " + e.getMessage());
			}
		});
		thread.start();
		log(logArea, "Broadcasting as " + serverName + " on port " + serverPort + "...");
	}

	public static void startServer(SharedState state, TextArea logArea, int serverPort) {
		serverThread = new Thread(() -> {
			try {
				serverSocket = new DatagramSocket(serverPort);
				log(logArea, "Server started on port " + serverPort + ", waiting for messages...");

				// Start the ping check timer
				pingCheckTimer = new Timer();
				pingCheckTimer.scheduleAtFixedRate(new TimerTask() {
					@Override
					public void run() {
						checkClientPings(logArea);
					}
				}, 0, 1000); // Ensure the period is positive and check every 1 second

				// Schedule the task to print player locations every second
				ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
				executor.scheduleAtFixedRate(() -> printPlayerLocations(), 0, 1, TimeUnit.SECONDS);
				executor.scheduleAtFixedRate(() -> updateServerPosition(), 0, 40, TimeUnit.MILLISECONDS);

				while (state.isBroadcast()) {
					byte[] buf = new byte[1024];
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					serverSocket.receive(packet);
					String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
					InetAddress clientAddress = packet.getAddress();
					int clientPort = packet.getPort();

					if (received.startsWith("/name/")) { // First Contact
						String clientName = received.substring(6);
						ClientInfo clientInfo = new ClientInfo(clientAddress, clientPort, clientName);
						clientAddresses.remove(clientInfo); // Remove old client info if exists
						clientAddresses.add(clientInfo); // Add updated client info
						log(logArea, clientName + " has connected");
					} else if (received.startsWith("/sys/")) { // System Message
						if ("/sys/PING".equals(received)) {
							System.out.println("Ping Recieved Pong Sent");
							String response = "/sys/PONG";
							buf = response.getBytes(StandardCharsets.UTF_8);
							packet = new DatagramPacket(buf, buf.length, clientAddress, clientPort);
							serverSocket.send(packet);

							ClientInfo clientInfo = getClientInfo(clientAddress, clientPort);
							if (clientInfo != null) {
								clientPingCount.putIfAbsent(clientInfo, new AtomicInteger(0));
								clientPingCount.get(clientInfo).set(0);
							}
						} else if ("/sys/Test_Handshake".equals(received)) {
							String response = "/sys/ACK";
							buf = response.getBytes(StandardCharsets.UTF_8);
							packet = new DatagramPacket(buf, buf.length, clientAddress, clientPort);
							serverSocket.send(packet);
							System.out.println("Sent ACK to client at " + clientAddress + ":" + clientPort);
							Main.settoGamedisable(false);
						} else { // Other server messages here
							System.out.println("Received System Message: " + received);
						}
					} else if (received.startsWith("/data/")) {
						String jsonStr = received.substring(6);
						JSONObject json = new JSONObject(jsonStr);
						double deltaX = json.getDouble("deltaX");
						double deltaY = json.getDouble("deltaY");

						// Update the player's position in playerPositions map
						String clientKey = packet.getAddress().getHostAddress() + ":" + packet.getPort();
						playerPositions.putIfAbsent(clientKey, new PlayerInfo(packet.getAddress(), packet.getPort(), "player", 0, 0, 0, "active"));
						PlayerInfo playerInfo = playerPositions.get(clientKey);
						playerInfo.setX(playerInfo.getX() + deltaX);
						playerInfo.setY(playerInfo.getY() + deltaY);

						// Create JSON response
						json = new JSONObject();

						// Add server's position without PC name
						String serverKey = InetAddress.getLocalHost().getHostAddress() + ":"
								+ serverSocket.getLocalPort();
						JSONObject serverData = new JSONObject();
					    serverData.put("position", new double[]{serverX, serverY});
					    serverData.put("name", "server");
					    serverData.put("score", 1000);
					    serverData.put("status", "active");
					    json.put(serverKey, serverData);

						// Add player positions without PC name
						for (PlayerInfo info : playerPositions.values()) {
							String key = info.getAddress().getHostAddress() + ":" + info.getPort();
					        JSONObject playerData = new JSONObject();
					        playerData.put("position", new double[]{info.getX(), info.getY()});
					        playerData.put("name", info.getName());
					        playerData.put("score", info.getScore());
					        playerData.put("status", info.getStatus());
					        json.put(key, playerData);
						}

						String response = "/data/" + json.toString();

						// Send the response to the client
						byte[] responseBuf = response.getBytes(StandardCharsets.UTF_8);
						DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length,
								packet.getAddress(), packet.getPort());
						serverSocket.send(responsePacket);
					} else {
						ClientInfo clientInfo = getClientInfo(clientAddress, clientPort);
						if (clientInfo != null) {
							log(logArea, clientInfo.getName() + " : " + received);
							relayMessageToClients(clientInfo, received, logArea);
						} else {
							log(logArea, "Player : " + received + " from " + clientAddress + ":" + clientPort);
						}
					}

				}
				serverSocket.close();
			} catch (IOException e) {
				if (!e.getMessage().contains("Socket closed")) {
					log(logArea, "Error: " + e.getMessage());
					System.out.println("Error: " + e.getMessage());
				}
			}
		});
		serverThread.start();
	}

	private static void printPlayerLocations() {
		if (Main.isGameWindow()) {
			System.out.println("Player and Server Locations:");
			System.out.println("Server - X: " + serverX + ", Y: " + serverY); // Print server position
			for (PlayerInfo player : playerPositions.values()) {
				System.out.println(player.getName() + " - X: " + player.getX() + ", Y: " + player.getY());
			}
		}
	}

	private static void relayMessageToClients(ClientInfo sender, String message, TextArea logArea) {
		String relayMessage = "/r/" + sender.getName() + " : " + message;
		byte[] buf = relayMessage.getBytes(StandardCharsets.UTF_8);

		for (ClientInfo clientInfo : clientAddresses) {
			if (!clientInfo.equals(sender)) {
				try {
					DatagramPacket packet = new DatagramPacket(buf, buf.length, clientInfo.getAddress(),
							clientInfo.getPort());
					serverSocket.send(packet);
					System.out.println("Relayed message to " + clientInfo.getAddress() + ":" + clientInfo.getPort());
				} catch (IOException e) {
					log(logArea, "Error relaying message to " + clientInfo.getAddress() + ":" + clientInfo.getPort()
							+ ": " + e.getMessage());
				}
			}
		}
	}

	private static ClientInfo getClientInfo(InetAddress address, int port) {
		for (ClientInfo clientInfo : clientAddresses) {
			if (clientInfo.getAddress().equals(address) && clientInfo.getPort() == port) {
				return clientInfo;
			}
		}
		return null;
	}

	private static void checkClientPings(TextArea logArea) {
		for (ClientInfo clientInfo : clientPingCount.keySet()) {
			int missedPings = clientPingCount.get(clientInfo).incrementAndGet();
			if (missedPings > 5) {
				log(logArea, "Client " + clientInfo.getAddress() + ":" + clientInfo.getPort()
						+ " has missed 5 PINGs and is considered disconnected.");
				clientPingCount.remove(clientInfo);
				clientAddresses.remove(clientInfo);
				 // Remove player position using clientKey
	            String clientKey = clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort();
	            playerPositions.remove(clientKey);			}
		}
	}

	public static void stopServer() {
		
		clientPingCount.clear();
		clientAddresses.clear();
		playerPositions.clear();
		
		if (serverSocket != null && !serverSocket.isClosed()) {
			serverSocket.close();
		}
		if (serverThread != null && serverThread.isAlive()) {
			serverThread.interrupt();
		}
		if (pingCheckTimer != null) {
			pingCheckTimer.cancel(); // Stop the ping check timer
		}
	}

	public static void sendMessageToClients(String message, TextArea logArea) {
		if (clientAddresses.isEmpty()) {
			log(logArea, "No connected clients to send the message.");
			return;
		}

		try {
			String servermsg = "/sname/" + Main.getServerName() + " : " + message;
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

	private static void log(TextArea logArea, String message) {
		Platform.runLater(() -> logArea.appendText(message + "\n"));
	}

	public static double getServerX() {
		return serverX;
	}

	public static double getServerY() {
		return serverY;
	}

	public static void setDelta(double dx, double dy) {
		deltaX = dx;
		deltaY = dy;
	}

	private static void updateServerPosition() {
		serverX += deltaX;
		serverY += deltaY;
	}

	public static ConcurrentHashMap<String, PlayerInfo> getPlayerPositions() {
		return playerPositions;
	}

	// Method to get local address and port in the required format
	public static String getLocalAddressPort() {
		if (serverSocket == null) {
			return "unknown:0";
		}
		try {
			InetAddress localAddress = InetAddress.getLocalHost();
			int localPort = serverSocket.getLocalPort();
			return localAddress.getHostAddress() + ":" + localPort;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return "unknown:0";
		}
	}

}

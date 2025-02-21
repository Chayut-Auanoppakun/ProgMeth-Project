package client;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import server.SharedState;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class GameClient {
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

	public static void startClient(SharedState state, TextArea logArea) {
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

				while (state.isClient()) {
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
		}, 0, 3000); // 0 delay, 3000 milliseconds (3 seconds) period
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
				sendMessage("/s/Test_Handshake", logArea);
				byte[] buf = new byte[1024];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				try {
					System.out.println("Waiting for response...");
					clientSocket.receive(packet);
					String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
					System.out.println("Server response: " + received);
					System.out.println("Handshake Complete");
					if ("/s/ACK".equals(received)) {
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
                if (message.startsWith("/s/")||message.startsWith("/name/")) {
                    byte[] buf = message.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, connectedServerAddress, connectedServerPort);
                    clientSocket.send(packet);
                    System.out.println("Sent system message: " + message); // Print to terminal for debugging
                } else {
                    byte[] buf = message.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, connectedServerAddress, connectedServerPort);
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
						if (received.startsWith("/s/")) {
							if ("/s/PONG".equals(received)) {
								missedPings = 0; // Reset missed pings count
								// System.out.println("Received PONG from server"); // Print to terminal for
								// debugging
							} else if ("/s/ACK".equals(received)) {
								System.out.println("Handshake Test Complete"); // Print to terminal for debugging
							}
						} else if (received.startsWith("/r/")) {
							String msg = received.substring(3);
							log(logArea, msg);
						} else if (received.startsWith("/sname/")) {
							String output = received.substring(7);
							log(logArea, output);
						} else {
							log(logArea, received);
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
					Thread.sleep(500); // Ping every 500 ms
					missedPings += 1;

					if (missedPings > 10) {
						wasDiscon = true;
						if (missedPings == 11) { // run once on disconnect
							log(logArea, "Server disconnected.");
							System.out.println("Server disconnected."); // Print to terminal for debugging
							log(logArea, "Attempting Reconnection ...");
						}
					} else {
						if (wasDiscon) {
							log(logArea, "Server Reconnect Succesful");
							wasDiscon = false;
						}
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					log(logArea, "Ping thread interrupted.");
				}
			}
		});
		pingThread.start();
	}

	private static void sendPing(TextArea logArea) {
		if (connectedServerAddress != null && connectedServerPort != -1) {
			try {
				String pingMessage = "/s/PING";
				byte[] buf = pingMessage.getBytes(StandardCharsets.UTF_8);
				DatagramPacket packet = new DatagramPacket(buf, buf.length, connectedServerAddress,
						connectedServerPort);
				clientSocket.send(packet);
				// System.out.println("Sent PING to server"); // Print to terminal for debugging
			} catch (IOException e) {
				log(logArea, "Error sending PING to server: " + e.getMessage());
			}
		} else {
			log(logArea, "Server connection not established. Cannot send PING.");
		}
	}

}

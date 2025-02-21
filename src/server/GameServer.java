package server;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import application.Main;

public class GameServer {
	private static DatagramSocket serverSocket;
	private static Thread serverThread;
	private static Set<ClientInfo> clientAddresses = new HashSet<>();
	private static ConcurrentHashMap<ClientInfo, AtomicInteger> clientPingCount = new ConcurrentHashMap<>();
	private static Timer pingCheckTimer;

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

	            while (state.isBroadcast()) {
	                byte[] buf = new byte[1024];
	                DatagramPacket packet = new DatagramPacket(buf, buf.length);
	                serverSocket.receive(packet);
	                String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
	                InetAddress clientAddress = packet.getAddress();
	                int clientPort = packet.getPort();

	                if (received.startsWith("/name/")) {
	                    String clientName = received.substring(6);
	                    ClientInfo clientInfo = new ClientInfo(clientAddress, clientPort, clientName);
	                    clientAddresses.remove(clientInfo); // Remove old client info if exists
	                    clientAddresses.add(clientInfo); // Add updated client info
	                    log(logArea, clientName + " has joined the chat");
	                } else if (received.startsWith("/s/")) {
	                    if ("/s/PING".equals(received)) {
	                        String response = "/s/PONG";
	                        buf = response.getBytes(StandardCharsets.UTF_8);
	                        packet = new DatagramPacket(buf, buf.length, clientAddress, clientPort);
	                        serverSocket.send(packet);
	                        //System.out.println("Sent PONG to client at " + clientAddress + ":" + clientPort); // Print

	                        // Reset missed pings count
	                        ClientInfo clientInfo = getClientInfo(clientAddress, clientPort);
	                        if (clientInfo != null) {
	                            clientPingCount.putIfAbsent(clientInfo, new AtomicInteger(0));
	                            clientPingCount.get(clientInfo).set(0);
	                        }
	                    } else if ("/s/Test_Handshake".equals(received)) {
	                        String response = "/s/ACK";
	                        buf = response.getBytes(StandardCharsets.UTF_8);
	                        packet = new DatagramPacket(buf, buf.length, clientAddress, clientPort);
	                        serverSocket.send(packet);
	                        System.out.println("Sent ACK to client at " + clientAddress + ":" + clientPort); // Print to terminal for debugging
	                    }
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
	                log(logArea, "Error : " + e.getMessage());
	                System.out.println("Error : " + e.getMessage());
	            }
	        }
	    });
	    serverThread.start();
	}

	private static void relayMessageToClients(ClientInfo sender, String message, TextArea logArea) {
	    String relayMessage = "/r/"+sender.getName() + " : " + message;
	    byte[] buf = relayMessage.getBytes(StandardCharsets.UTF_8);

	    for (ClientInfo clientInfo : clientAddresses) {
	        if (!clientInfo.equals(sender)) {
	            try {
	                DatagramPacket packet = new DatagramPacket(buf, buf.length, clientInfo.getAddress(), clientInfo.getPort());
	                serverSocket.send(packet);
	                System.out.println("Relayed message to " + clientInfo.getAddress() + ":" + clientInfo.getPort()); // Print to terminal for debugging
	            } catch (IOException e) {
	                log(logArea, "Error relaying message to " + clientInfo.getAddress() + ":" + clientInfo.getPort() + ": " + e.getMessage());
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
			if (missedPings-1 > 5) {
				log(logArea, "Client " + clientInfo.getAddress() + ":" + clientInfo.getPort()
						+ " has missed 5 PINGs and is considered disconnected.");
				// Handle client disconnection (e.g., remove from client list)
				clientPingCount.remove(clientInfo);
				clientAddresses.remove(clientInfo); // Remove client from connected clients list
			} else {
				//System.out.println(missedPings);
			}
		}
	}

	public static void stopServer() {
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
			String servermsg = "/sname/"+ Main.getServerName()  +" : "+ message;
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
}

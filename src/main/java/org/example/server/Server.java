package org.example.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Server {
    private static final int PORT = 12345;
    private static final ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();
    private static final List<PrintWriter> allClients = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT, 0, InetAddress.getByName("0.0.0.0"))) {
            System.out.println("Server is running...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                synchronized (allClients) {
                    allClients.add(new PrintWriter(clientSocket.getOutputStream(), true));
                }
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;
        private Room currentRoom;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                clientName = in.readLine();
                if (clientName == null) {
                    return;
                }
                out.println("Welcome, " + clientName + "!");
                System.out.println("Welcome, " + clientName + "!");

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    handleClientMessages(inputLine);
                }
            } catch (IOException e) {
                System.err.println("Error in client communication: " + e.getMessage());
            } finally {
                closeResources();
            }
        }

        private void handleClientMessages(String inputLine) {
            if (inputLine.startsWith("REQUEST_ROOM_LIST")) {
                sendRoomListToClient();
            } else if (inputLine.startsWith("CREATE_ROOM:")) {
                String roomName = inputLine.split(":")[1];
                createRoom(roomName);
            } else if (inputLine.startsWith("JOIN_ROOM:")) {
                String roomName = inputLine.split(":")[1];
                joinRoom(roomName);
            } else if (inputLine.startsWith("CLOSE_ROOM:")) {
                String roomName = inputLine.split(":")[1];
                closeRoom(roomName);
            } else {
                sendMessageToRoom(inputLine);
            }
        }

        private void createRoom(String roomName) {
            if (currentRoom != null) {
                currentRoom.leave(socket);
            }

            Room newRoom = new Room(roomName, socket);
            if (rooms.putIfAbsent(roomName, newRoom) == null) {
                currentRoom = newRoom;
                System.out.println("Room created: " + roomName);
                out.println("Room " + roomName + " has been created and you are the owner.");
                broadcastRoomList();
            } else {
                out.println("ERROR: Room already exists.");
            }
        }

        private void joinRoom(String roomName) {
            if (currentRoom != null && currentRoom.isOwner(socket)) {
                out.println("ERROR: You need to close your room before joining another.");
                return;
            }

            Room room = rooms.get(roomName);
            if (room == null) {
                out.println("ERROR: Room does not exist.");
                return;
            }

            if (currentRoom != null) {
                currentRoom.leave(socket);
            }

            room.join(socket);
            currentRoom = room;
            broadcastMessage(currentRoom.getName(), clientName + " has joined the room.");
        }

        private void sendRoomListToClient() {
            String roomList = String.join(",", rooms.keySet());
            sendMessageToClient("ROOM_LIST:" + roomList);
        }

        private void sendMessageToClient(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        private void closeRoom(String roomName) {
            Room room = rooms.get(roomName);
            if (room != null) {
                if (room.isOwner(socket)) {
                    room.close();
                    rooms.remove(roomName);
                    broadcastRoomList();
                    if (currentRoom == room) {
                        currentRoom = null;
                    }
                    out.println("Room closed.");
                } else {
                    out.println("ERROR: You are not the owner of this room.");
                }
            } else {
                out.println("ERROR: Room does not exist.");
            }
        }

        private void sendMessageToRoom(String message) {
            if (currentRoom != null) {
                broadcastMessage(currentRoom.getName(), clientName + ": " + message);
            }
        }

        private void broadcastMessage(String roomName, String message) {
            Room room = rooms.get(roomName);
            if (room != null) {
                room.broadcastMessage(message);
            }
        }

        private void broadcastRoomList() {
            String roomList = String.join(",", rooms.keySet());
            synchronized (allClients) {
                for (PrintWriter clientOut : allClients) {
                    clientOut.println("ROOM_LIST:" + roomList);
                }
            }
        }

        private void closeResources() {
            try {
                if (currentRoom != null) {
                    currentRoom.leave(socket);
                }
                synchronized (allClients) {
                    allClients.remove(out);
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing resources: " + e.getMessage());
            }
        }
    }

    private static class Room {
        private String name;
        private Socket owner;
        private List<Socket> members = Collections.synchronizedList(new ArrayList<>());

        public Room(String name, Socket owner) {
            this.name = name;
            this.owner = owner;
            this.members.add(owner);
        }

        public String getName() {
            return name;
        }

        public boolean isOwner(Socket socket) {
            return owner.equals(socket);
        }

        public void join(Socket socket) {
            synchronized (members) {
                if (!members.contains(socket)) {
                    members.add(socket);
                    broadcastMessage("NOTICE: " + getClientName(socket) + " has joined the room.");
                }
            }
        }

        public void leave(Socket socket) {
            synchronized (members) {
                if (members.remove(socket)) {
                    broadcastMessage("NOTICE: " + getClientName(socket) + " has left the room.");
                    if (members.isEmpty()) {
                        rooms.remove(name);
                        System.out.println("Room " + name + " is now empty and has been removed.");
                    }
                    closeSocket(socket);
                }
            }
        }

        public void close() {
            broadcastMessage("NOTICE: The room is closing.");
            synchronized (members) {
                Iterator<Socket> iterator = members.iterator();
                while (iterator.hasNext()) {
                    Socket socket = iterator.next();
                    if (socket != null && !socket.isClosed()) {
                        try {
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            out.println("NOTICE: The room is closing.");
                        } catch (IOException e) {
                            System.err.println("Error sending closing message to socket: " + e.getMessage());
                            iterator.remove();
                            closeSocket(socket);
                        }
                    } else {
                        iterator.remove();
                    }
                }
            }
        }

        public void broadcastMessage(String message) {
            synchronized (members) {
                for (Socket socket : members) {
                    if (socket != null && !socket.isClosed()) {
                        try {
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            out.println(message);
                        } catch (IOException e) {
                            System.err.println("Error sending message to socket: " + e.getMessage());
                            closeSocket(socket);
                        }
                    }
                }
            }
        }

        private void closeSocket(Socket socket) {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }

        private String getClientName(Socket socket) {
            return "Client";
        }
    }
}
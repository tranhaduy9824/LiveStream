package org.example.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Room {
    private String name;
    private Socket owner;
    private List<Socket> members = new CopyOnWriteArrayList<>();

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
        if (!members.contains(socket)) {
            members.add(socket);
            broadcastMessage("NOTICE: " + getClientName(socket) + " has joined the room.");
        }
    }

    public void leave(Socket socket) {
        if (members.remove(socket)) {
            broadcastMessage("NOTICE: " + getClientName(socket) + " has left the room.");
            if (members.isEmpty()) {
                Server.rooms.remove(name);
                System.out.println("Room " + name + " is now empty and has been removed.");
            }
            closeSocket(socket);
        }
    }

    public void close() {
        broadcastMessage("NOTICE: The room is closing.");
        for (Socket socket : members) {
            closeSocket(socket);
        }
    }

    public void broadcastMessage(String message) {
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
        // Placeholder for client name management; to be replaced with actual logic
        return "Client";
    }
}

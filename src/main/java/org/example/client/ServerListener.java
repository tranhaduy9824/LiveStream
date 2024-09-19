package org.example.client;

import java.io.BufferedReader;
import java.io.IOException;

public class ServerListener implements Runnable {
    private BufferedReader in;
    private ClientView view;

    public ServerListener(BufferedReader in, ClientView view) {
        this.in = in;
        this.view = view;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                final String message = line;
                if (line.startsWith("ROOM_LIST:")) {
                    view.updateRoomList(line.substring(10));
                } else {
                    view.appendMessage(message);
                }
            }
        } catch (IOException e) {
            view.appendMessage("Connection lost: " + e.getMessage());
        }
    }
}

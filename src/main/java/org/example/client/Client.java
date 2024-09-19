package org.example.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class Client {
    private static final int SERVER_PORT = 12345;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private JTextField serverAddressField;
    private JTextField usernameField;
    private JTextField roomNameField;
    private JButton createRoomButton;
    private JButton closeRoomButton;
    private JButton sendButton;
    private JButton joinRoomButton;
    private JComboBox<String> roomList;
    private DefaultComboBoxModel<String> roomListModel;
    private JPanel mainPanel;
    private JLabel imageLabel;
    private JLabel roomNameLabel;
    private String currentRoom;
    private boolean isRoomOwner;

    public Client() {
        frame = new JFrame("Live Stream Client");
        frame.setSize(900, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JPanel initialPanel = new JPanel(new GridBagLayout());
        serverAddressField = new JTextField(20);
        usernameField = new JTextField(15);
        JButton enterButton = new JButton("Enter");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        gbc.gridx = 0;
        gbc.gridy = 0;
        initialPanel.add(new JLabel("Server Address:"), gbc);
        gbc.gridx = 1;
        initialPanel.add(serverAddressField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        initialPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        initialPanel.add(usernameField, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        initialPanel.add(enterButton, gbc);

        frame.add(initialPanel, BorderLayout.CENTER);

        enterButton.addActionListener(e -> {
            String serverAddress = serverAddressField.getText();
            String username = usernameField.getText();
            if (!serverAddress.isEmpty() && !username.isEmpty()) {
                connectToServer(serverAddress, username);
                frame.remove(initialPanel);
                setupMainFrame();
                frame.revalidate();
                frame.repaint();
            } else {
                JOptionPane.showMessageDialog(frame, "Please enter both server address and username.");
            }
        });

        frame.setVisible(true);
    }

    private void setupMainFrame() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        topPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Main View"));

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Live Camera"));

        imageLabel = new JLabel("Camera Feed", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(400, 400));
        leftPanel.add(imageLabel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Chat Live"));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(Color.WHITE);
        chatArea.setForeground(Color.DARK_GRAY);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(chatArea);

        rightPanel.add(scrollPane, BorderLayout.CENTER);

        topPanel.add(leftPanel);
        topPanel.add(rightPanel);

        mainPanel.add(topPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new GridBagLayout());
        bottomPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Controls"));

        roomNameField = new JTextField(15);
        roomNameField.setPreferredSize(new Dimension(200, 30));
        createRoomButton = new JButton("Create Room");
        closeRoomButton = new JButton("Close Room");
        joinRoomButton = new JButton("Join Room");
        sendButton = new JButton("Send");

        roomListModel = new DefaultComboBoxModel<>();
        roomList = new JComboBox<>(roomListModel);
        roomList.setPreferredSize(new Dimension(150, 30));

        inputField = new JTextField(20);
        inputField.setPreferredSize(new Dimension(300, 30));

        roomNameLabel = new JLabel("No room selected");
        roomNameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        roomNameLabel.setPreferredSize(new Dimension(300, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);

        bottomPanel.add(new JLabel("Room:", JLabel.RIGHT), gbc);
        gbc.gridx = 1;
        bottomPanel.add(roomList, gbc);

        gbc.gridx = 2;
        bottomPanel.add(new JLabel("Comment:", JLabel.RIGHT), gbc);
        gbc.gridx = 3;
        bottomPanel.add(inputField, gbc);
        gbc.gridx = 4;
        bottomPanel.add(sendButton, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        bottomPanel.add(new JLabel("New Room:", JLabel.RIGHT), gbc);
        gbc.gridx = 1;
        bottomPanel.add(roomNameField, gbc);

        gbc.gridx = 2;
        bottomPanel.add(createRoomButton, gbc);
        gbc.gridx = 3;
        bottomPanel.add(joinRoomButton, gbc);
        gbc.gridx = 4;
        bottomPanel.add(closeRoomButton, gbc);

        mainPanel.add(roomNameLabel, BorderLayout.NORTH);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        frame.add(mainPanel, BorderLayout.CENTER);

        createRoomButton.addActionListener(e -> createRoom());
        joinRoomButton.addActionListener(e -> joinRoom());
        closeRoomButton.addActionListener(e -> closeRoom());
        sendButton.addActionListener(e -> sendMessage());

        roomList.addActionListener(e -> currentRoom = (String) roomList.getSelectedItem());
    }

    private void joinRoom() {
        String roomName = (String) roomList.getSelectedItem();
        if (roomName == null || roomName.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please select a room to join.");
            return;
        }

        if (currentRoom != null) {
            out.println("LEAVE_ROOM:" + currentRoom);
        }

        out.println("JOIN_ROOM:" + roomName);
        currentRoom = roomName;
        isRoomOwner = false;
        appendMessage("Joined room: " + roomName);
        updateUIForRoomOwner();
        roomNameLabel.setText("Current Room: " + roomName);
    }

    private void connectToServer(String serverAddress, String username) {
        try {
            socket = new Socket(serverAddress, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(username);
            out.println("REQUEST_ROOM_LIST");

            new Thread(new ServerListener()).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error connecting to server: " + e.getMessage());
        }
    }

    private void createRoom() {
        String roomName = roomNameField.getText();
        if (roomName.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a room name.");
            return;
        }

        out.println("CREATE_ROOM:" + roomName);
        currentRoom = roomName;
        isRoomOwner = true;
        appendMessage("Created room: " + roomName);
        updateUIForRoomOwner();
        out.println("REQUEST_ROOM_LIST");
        roomNameLabel.setText("Current Room: " + roomName);
    }

    private void closeRoom() {
        if (!isRoomOwner) {
            JOptionPane.showMessageDialog(frame, "You are not the owner of this room.");
            return;
        }

        out.println("CLOSE_ROOM:" + currentRoom);
        currentRoom = null;
        isRoomOwner = false;
        appendMessage("Room closed.");
        updateUIForRoomOwner();
        out.println("REQUEST_ROOM_LIST");
        roomNameLabel.setText("No room selected");
    }

    private void sendMessage() {
        String message = inputField.getText();
        if (message.isEmpty() || currentRoom == null) {
            JOptionPane.showMessageDialog(frame, "You must join a room before sending messages.");
            return;
        }
        out.println(message);
        inputField.setText("");
    }

    private void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> chatArea.append(message + "\n"));
    }

    private void updateUIForRoomOwner() {
        createRoomButton.setEnabled(!isRoomOwner);
        closeRoomButton.setEnabled(isRoomOwner);
        joinRoomButton.setEnabled(!isRoomOwner);
        roomNameField.setEnabled(!isRoomOwner);
    }

    private class ServerListener implements Runnable {
        public void run() {
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("ROOM_LIST:")) {
                        String[] rooms = line.substring("ROOM_LIST:".length()).split(",");
                        SwingUtilities.invokeLater(() -> {
                            roomListModel.removeAllElements();
                            for (String room : rooms) {
                                roomListModel.addElement(room);
                            }
                        });
                    } else {
                        appendMessage(line);
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Connection lost: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}

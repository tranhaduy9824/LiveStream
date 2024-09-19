package org.example.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class ClientView {
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
    private JLabel imageLabel;
    private JPanel mainPanel;

    public ClientView(Client client) {
        frame = new JFrame("Live Stream Client");
        frame.setSize(900, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Initial frame to enter server address and username
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
                client.connectToServer(serverAddress, username);
                frame.remove(initialPanel);
                setupMainFrame(client);
                frame.revalidate();
                frame.repaint();
            } else {
                JOptionPane.showMessageDialog(frame, "Please enter both server address and username.");
            }
        });

        frame.setVisible(true);
    }

    private void setupMainFrame(Client client) {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel for image and chat
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        topPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Main View"));

        // Panel for image (video stream)
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Live Camera"));

        imageLabel = new JLabel("Camera Feed", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(400, 400));
        leftPanel.add(imageLabel, BorderLayout.CENTER);

        // Panel for chat
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Chat"));

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

        // Panel for controls
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

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);

        bottomPanel.add(new JLabel("Room:", JLabel.RIGHT), gbc);
        gbc.gridx = 1;
        bottomPanel.add(roomList, gbc);

        gbc.gridx = 2;
        bottomPanel.add(new JLabel("Message:", JLabel.RIGHT), gbc);
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

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        frame.add(mainPanel, BorderLayout.CENTER);

        createRoomButton.addActionListener(e -> client.createRoom());
        joinRoomButton.addActionListener(e -> client.joinRoom());
        closeRoomButton.addActionListener(e -> client.closeRoom());
        sendButton.addActionListener(e -> client.sendMessage());

        roomList.addActionListener(e -> client.setCurrentRoom((String) roomList.getSelectedItem()));
    }

    public void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> chatArea.append(message + "\n"));
    }

    public void updateRoomList(String roomListStr) {
        String[] rooms = roomListStr.split(",");
        SwingUtilities.invokeLater(() -> {
            roomListModel.removeAllElements();
            for (String room : rooms) {
                roomListModel.addElement(room);
            }
        });
    }

    public void updateUIForRoomOwner(boolean isRoomOwner) {
        createRoomButton.setEnabled(!isRoomOwner);
        closeRoomButton.setEnabled(isRoomOwner);
        joinRoomButton.setEnabled(!isRoomOwner);
    }
}

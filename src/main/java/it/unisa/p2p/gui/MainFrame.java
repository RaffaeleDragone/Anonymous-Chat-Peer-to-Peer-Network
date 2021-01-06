/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unisa.p2p.gui;

import it.unisa.p2p.beans.Chat;
import it.unisa.p2p.beans.Message;
import it.unisa.p2p.beans.ImageWrapper;
import it.unisa.p2p.chat.AnonymousChatImpl;

import it.unisa.p2p.interfaces.MessageListener;
import it.unisa.p2p.utils.ImageCompressor;
import it.unisa.p2p.utils.UtilDate;
import static it.unisa.p2p.utils.UtilDate.differenceDateInSeconds;
import static it.unisa.p2p.utils.UtilDate.formatSecondsIn_sTime;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author raffaeledragone
 */
public class MainFrame extends javax.swing.JFrame {

    /**
     * Creates new form MainFrame
     */
    ButtonGroup buttonGroupRooms = new ButtonGroup();
    HashMap<String, List<Message>> hmListMessages = new HashMap<>();
    String currentRoom = null;
    AnonymousChatImpl peer;

    //create the model and add elements
    DefaultListModel<Message> listModel = new DefaultListModel<>();
    javax.swing.Timer timerCountdown;

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(MainFrame.class);
    public MainFrame() {
        initComponents();
        activatePanelWriterMessages(false);
        
    }

    public MainFrame(String master, int id) throws Exception {
        peer = new AnonymousChatImpl(id, master, new MessageListenerImpl(id));
        initComponents();
        activatePanelWriterMessages(false);
        lblNumPeer.setText(id + "");
        this.setResizable(false);
        lblNumPeer.setVisible(false);
    }

    private void createRoom() {
        String nameRoom = JOptionPane.showInputDialog(this,"Please insert the name of room ");
        if (nameRoom != null && !nameRoom.isEmpty()) {
            Chat room = new Chat();
            room.setRoomName(nameRoom);
            room.setUsers(new HashSet<>());
            //room.setPassword(pwd);
            int useTimer = JOptionPane.showConfirmDialog(this, "Do you want to set a timer? ");
            if(useTimer!=2){
                if (useTimer == 0) {
                    String s_time = JOptionPane.showInputDialog(this,"Please insert time in minutes ");
                    if(s_time!=null && !s_time.equals("")){
                        Integer minutes=0;
                        try{
                            minutes = Integer.parseInt(s_time);
                        }catch(Exception ex){
                            minutes=0;
                        }
                        if(minutes>0){
                            Long mills = 1000 * 60 * minutes.longValue();
                            Calendar endDateTime = Calendar.getInstance();
                            endDateTime.setTimeInMillis(endDateTime.getTimeInMillis() + mills);
                            room.setEndChat(endDateTime.getTime());
                        }else{
                            return;
                        }
                    }
                    
                }
            if (peer.getMyChatList() != null && peer.getMyChatList().contains(nameRoom)) {
                JOptionPane.showMessageDialog(this, "Room already in use");
            } else {
                String res = peer.createRoom_(room);
                
                if (res != null) {
                    if (res.equals("ok")) {
                        hmListMessages.put(nameRoom, new ArrayList<>());

                        JToggleButton btn = new JToggleButton(nameRoom);
                        ActionListener listener = new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                setFocusRoom(e.getActionCommand());
                            }
                        };
                        btn.addActionListener(listener);
                        buttonGroupRooms.add(btn);
                        panelSubRooms.add(btn);
                        panelSubRooms.revalidate();
                        panelSubRooms.repaint();

                        if (room.getEndChat() != null) {
                            scheduleCheckExistencyChat(room);
                        }
                    } else {
                        String out = res.equals("ko") ? "Problems during creation of room, retry." : res;
                        JOptionPane.showMessageDialog(this, res);
                    }

                } else {
                    JOptionPane.showMessageDialog(this, "Error during creation of room " + nameRoom);
                }
            }
            }
            
        }
    }

    private void joinRoom() {
        //Code for join in room
        String nameRoom = JOptionPane.showInputDialog(this,"Please insert the name of room ");
        if (nameRoom != null && !nameRoom.isEmpty()) {
            if (peer.getMyChatList() != null && peer.getMyChatList().contains(nameRoom)) {
                JOptionPane.showMessageDialog(this, "Room already created");
            } else {
                String res = peer.joinRoom_(nameRoom);
                //boolean res = peer.joinRoom(nameRoom);
                if (res!=null && res.equals("ok")) {
                    hmListMessages.put(nameRoom, new ArrayList<>());
                    JToggleButton btn = new JToggleButton(nameRoom);
                    ActionListener listener = new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            setFocusRoom(e.getActionCommand());
                        }
                    };
                    btn.addActionListener(listener);
                    buttonGroupRooms.add(btn);
                    panelSubRooms.add(btn);
                    panelSubRooms.revalidate();
                    panelSubRooms.repaint();

                    Chat chat = peer.findChatRoom(nameRoom);
                    if (chat.getEndChat() != null) {
                        scheduleCheckExistencyChat(chat);
                    }
                } else {
                    String out = "Error during joining in room";
                    if(res!=null && !res.equals("ko"))
                        out+=" "+res;
                    JOptionPane.showMessageDialog(this, out);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Room name cannot be null " + nameRoom);
        }
    }

    class MessageListenerImpl implements MessageListener {

        int peerid;

        public MessageListenerImpl(int peerid) {
            this.peerid = peerid;
        }
        public Object parseMessage(Object obj) {
            Message msg=null;
            ImageWrapper msgWrap=null;
            if (obj instanceof Message) {
                msg = (Message) obj;
            } else if (obj instanceof ImageWrapper) {
                msgWrap = (ImageWrapper) obj;
                msg=msgWrap.getMsg();
            }

            if (msg != null && msg.getRoomName() != null) {
                if (hmListMessages.get(msg.getRoomName()) != null) {
                    hmListMessages.get(msg.getRoomName()).add(msg);
                    if (msg.getRoomName().equalsIgnoreCase(currentRoom)) {
                        listModel.addElement(msg);
                    }
                }
            }
            if(msgWrap!=null) return msgWrap;
            if(msg!=null) return msg;
            return null;
        }
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        roomsPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        panelSubRooms = new javax.swing.JPanel();
        btnCreateRoom = new javax.swing.JButton();
        btnJoinRoom = new javax.swing.JButton();
        chatPanel = new javax.swing.JPanel();
        panelWrite = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtMessage = new javax.swing.JTextArea();
        btnSend = new javax.swing.JButton();
        btnImage = new javax.swing.JButton();
        panelMessages = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        lstMessages = new javax.swing.JList<Message>(listModel);
        panelIntestazioneRoom = new javax.swing.JPanel();
        btnLeaveRoom = new javax.swing.JButton();
        lblCountdown = new javax.swing.JLabel();
        lblNumPeer = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        roomsPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Your Rooms");

        panelSubRooms.setLayout(new java.awt.GridLayout(20, 0));

        btnCreateRoom.setIcon(new javax.swing.ImageIcon(getClass().getResource("/plus.png"))); // NOI18N
        btnCreateRoom.setBorder(null);
        btnCreateRoom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCreateRoomActionPerformed(evt);
            }
        });

        btnJoinRoom.setIcon(new javax.swing.ImageIcon(getClass().getResource("/join.png"))); // NOI18N
        btnJoinRoom.setBorder(null);
        btnJoinRoom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnJoinRoomActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout roomsPanelLayout = new javax.swing.GroupLayout(roomsPanel);
        roomsPanel.setLayout(roomsPanelLayout);
        roomsPanelLayout.setHorizontalGroup(
            roomsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roomsPanelLayout.createSequentialGroup()
                .addGroup(roomsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(roomsPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(panelSubRooms, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(roomsPanelLayout.createSequentialGroup()
                        .addGroup(roomsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(roomsPanelLayout.createSequentialGroup()
                                .addGap(50, 50, 50)
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(roomsPanelLayout.createSequentialGroup()
                                .addGap(23, 23, 23)
                                .addComponent(btnCreateRoom, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(36, 36, 36)
                                .addComponent(btnJoinRoom, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 15, Short.MAX_VALUE)))
                .addContainerGap())
        );
        roomsPanelLayout.setVerticalGroup(
            roomsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(roomsPanelLayout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(roomsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnCreateRoom, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnJoinRoom, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 37, Short.MAX_VALUE)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(panelSubRooms, javax.swing.GroupLayout.PREFERRED_SIZE, 445, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        chatPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        txtMessage.setColumns(20);
        txtMessage.setRows(5);
        jScrollPane1.setViewportView(txtMessage);

        btnSend.setIcon(new javax.swing.ImageIcon(getClass().getResource("/send.png"))); // NOI18N
        btnSend.setText("Send");
        btnSend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSendActionPerformed(evt);
            }
        });

        btnImage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/image.png"))); // NOI18N
        btnImage.setText("Image");
        btnImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImageActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelWriteLayout = new javax.swing.GroupLayout(panelWrite);
        panelWrite.setLayout(panelWriteLayout);
        panelWriteLayout.setHorizontalGroup(
            panelWriteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelWriteLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 422, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelWriteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btnSend, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnImage, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelWriteLayout.setVerticalGroup(
            panelWriteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelWriteLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panelWriteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(panelWriteLayout.createSequentialGroup()
                        .addComponent(btnSend)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnImage))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        lstMessages.setCellRenderer(new MessageRenderer());
        jScrollPane2.setViewportView(lstMessages);

        javax.swing.GroupLayout panelMessagesLayout = new javax.swing.GroupLayout(panelMessages);
        panelMessages.setLayout(panelMessagesLayout);
        panelMessagesLayout.setHorizontalGroup(
            panelMessagesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMessagesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 540, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelMessagesLayout.setVerticalGroup(
            panelMessagesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMessagesLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2)
                .addContainerGap())
        );

        btnLeaveRoom.setIcon(new javax.swing.ImageIcon(getClass().getResource("/leave.png"))); // NOI18N
        btnLeaveRoom.setText("Leave Room");
        btnLeaveRoom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLeaveRoomActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelIntestazioneRoomLayout = new javax.swing.GroupLayout(panelIntestazioneRoom);
        panelIntestazioneRoom.setLayout(panelIntestazioneRoomLayout);
        panelIntestazioneRoomLayout.setHorizontalGroup(
            panelIntestazioneRoomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelIntestazioneRoomLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnLeaveRoom)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblNumPeer, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(88, 88, 88)
                .addComponent(lblCountdown, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        panelIntestazioneRoomLayout.setVerticalGroup(
            panelIntestazioneRoomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelIntestazioneRoomLayout.createSequentialGroup()
                .addGroup(panelIntestazioneRoomLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelIntestazioneRoomLayout.createSequentialGroup()
                        .addComponent(btnLeaveRoom)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(lblCountdown, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblNumPeer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout chatPanelLayout = new javax.swing.GroupLayout(chatPanel);
        chatPanel.setLayout(chatPanelLayout);
        chatPanelLayout.setHorizontalGroup(
            chatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(chatPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(chatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelWrite, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(chatPanelLayout.createSequentialGroup()
                        .addGroup(chatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(panelMessages, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(panelIntestazioneRoom, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap(17, Short.MAX_VALUE))))
        );
        chatPanelLayout.setVerticalGroup(
            chatPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, chatPanelLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(panelIntestazioneRoom, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelMessages, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelWrite, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jLabel2.setFont(new java.awt.Font("Chalkboard SE", 1, 13)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("P2P Anonymous Chat");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 46, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(roomsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(chatPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(roomsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(chatPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSendActionPerformed
        String txt = txtMessage.getText();
        if(txt!=null && !txt.isEmpty()){
            Message msg = new Message();
            msg.setType(0);
            msg.setRoomName(currentRoom);
            msg.setMsg(txt);
            msg.setDate(Calendar.getInstance().getTime());
            txtMessage.setText("");
            String res = peer.sendMessage_(currentRoom, msg);
            if(res.equalsIgnoreCase("ok")){
                    msg.setMsg("mymsg_"+msg.getMsg());
                    listModel.addElement(msg);
                    if(hmListMessages.get(currentRoom)==null)
                        hmListMessages.put(currentRoom, new ArrayList<>());
                    hmListMessages.get(currentRoom).add(msg);
            }
        }
    }//GEN-LAST:event_btnSendActionPerformed

    private void btnCreateRoomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCreateRoomActionPerformed
        createRoom();
    }//GEN-LAST:event_btnCreateRoomActionPerformed

    private void btnLeaveRoomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLeaveRoomActionPerformed
       
        boolean res = peer.leaveRoom(currentRoom);
        if (res) {
            if (buttonGroupRooms != null && buttonGroupRooms.getElements() != null) {
                Collections.list(buttonGroupRooms.getElements()).stream().forEach((btn) -> {
                    if (btn.getText().equals(currentRoom)) {
                        //peer.leaveRoom(currentRoom);
                        buttonGroupRooms.remove(btn);
                        panelSubRooms.remove(btn);
                        panelSubRooms.revalidate();
                        panelSubRooms.repaint();

                        hmListMessages.remove(currentRoom);
                        currentRoom = null;
                        activatePanelWriterMessages(false);
                        
                        panelMessages.remove(lstMessages);
                        
                        listModel.removeAllElements();
                        lstMessages.removeAll();
                        lstMessages = new JList<>(listModel);
                        lstMessages.setModel(listModel);
                        
                        panelMessages.revalidate();
                        panelMessages.repaint();
                        lstMessages.revalidate();
                        lstMessages.repaint();
                        
                        
                        
                    }
                });
            }
        } else {
            JOptionPane.showMessageDialog(this, "Error during leaving of room " + currentRoom);
        }
    }//GEN-LAST:event_btnLeaveRoomActionPerformed

    private void btnJoinRoomActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnJoinRoomActionPerformed
        joinRoom();
        
    }//GEN-LAST:event_btnJoinRoomActionPerformed

    private void btnImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImageActionPerformed
        try {
            String path_img=null;
            try {
                final JFileChooser fc = new JFileChooser();
                fc.setCurrentDirectory(new File(System.getProperty("user.home")));
                FileFilter imageFilter = new FileNameExtensionFilter("Image files", ImageIO.getReaderFileSuffixes());
                fc.setFileFilter(imageFilter);
                int returnVal = fc.showOpenDialog(this);

                if (returnVal==0 && returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    path_img=file.getAbsolutePath();
                }
            } catch (Exception e) {
            }
            
            if(path_img!=null){
                String txt = "";
                Message msg = new Message();
                msg.setType(1);
                msg.setRoomName(currentRoom);
                msg.setMsg(txt);
                msg.setDate(Calendar.getInstance().getTime());

                BufferedImage image = ImageIO.read(new File(path_img));
                image = ImageCompressor.resizeImage(image, 128, 128);
                byte[] newimg = ImageCompressor.compressImageInJpeg(image, 0.8f);
                msg.setImage(newimg);

                String res = peer.sendMessage_(currentRoom, msg);
                if (res.equalsIgnoreCase("ok")) {
                    msg.setMsg("mymsg_" + msg.getMsg());
                    listModel.addElement(msg);
                    if (hmListMessages.get(currentRoom) == null) {
                        hmListMessages.put(currentRoom, new ArrayList<>());
                    }
                    hmListMessages.get(currentRoom).add(msg);
                }
            }
            
        } catch (IOException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_btnImageActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        peer.leaveNetwork();
    }//GEN-LAST:event_formWindowClosing

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new MainFrame("127.0.0.1", 0).setVisible(true);
                } catch (Exception ex) {
                    Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCreateRoom;
    private javax.swing.JButton btnImage;
    private javax.swing.JButton btnJoinRoom;
    private javax.swing.JButton btnLeaveRoom;
    private javax.swing.JButton btnSend;
    private javax.swing.JPanel chatPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblCountdown;
    private javax.swing.JLabel lblNumPeer;
    private javax.swing.JList<Message> lstMessages;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JPanel panelIntestazioneRoom;
    private javax.swing.JPanel panelMessages;
    private javax.swing.JPanel panelSubRooms;
    private javax.swing.JPanel panelWrite;
    private javax.swing.JPanel roomsPanel;
    private javax.swing.JTextArea txtMessage;
    // End of variables declaration//GEN-END:variables

    private void activatePanelWriterMessages(boolean state) {
        txtMessage.setEnabled(state);
        panelMessages.setEnabled(state);
        btnImage.setEnabled(state);
        btnSend.setEnabled(state);
        btnLeaveRoom.setEnabled(state);
        lblCountdown.setText("");
        lblCountdown.setEnabled(state);
        if (timerCountdown != null) {
            timerCountdown.stop();
        }

    }

    private void setFocusRoom(String roomName) {
        activatePanelWriterMessages(true);
        currentRoom = roomName;
        List<Message> lstMsg = hmListMessages.get(roomName);
        lstMessages.removeAll();

        listModel.removeAllElements();

        for (Message message : lstMsg) {
            listModel.addElement(message);
        }
        lstMessages = new JList<>(listModel);
        lstMessages.setModel(listModel);
        Chat chatRoom = peer.findChatRoom(roomName);
        //Chat temporizzata
        if (chatRoom != null && chatRoom.getEndChat() != null) {
            activateCountdown(chatRoom.getEndChat());
        }

    }

    private void activateCountdown(Date endDate) {
        ActionListener updateClockAction = (ActionEvent e) -> {
            Calendar c = Calendar.getInstance();
            long seconds = differenceDateInSeconds(c.getTime(), endDate);
            if (seconds > 0) {
                lblCountdown.setText(formatSecondsIn_sTime(seconds));
            } else {
                lblCountdown.setText("0:00:00");
            }
            Font font = new Font("Courier", Font.BOLD, 12);
            lblCountdown.setFont(font);
        };
        timerCountdown = new javax.swing.Timer(1000, updateClockAction);
        timerCountdown.start();
    }

    private void scheduleCheckExistencyChat(Chat chat) {
        if (chat != null && chat.getEndChat() != null) {
            //timer
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if (peer.findChatRoom(chat.getRoomName()) == null) {
                        if (buttonGroupRooms != null && buttonGroupRooms.getElements() != null) {
                            Collections.list(buttonGroupRooms.getElements()).stream().forEach((btn) -> {
                                if (btn.getText().equalsIgnoreCase(chat.getRoomName())) {
                                    buttonGroupRooms.remove(btn);
                                    panelSubRooms.remove(btn);
                                    panelSubRooms.revalidate();
                                    panelSubRooms.repaint();

                                    hmListMessages.remove(chat.getRoomName());

                                    JOptionPane.showMessageDialog(MainFrame.this, "Room deleted " + chat.getRoomName());
                                    if (btn.isSelected() || currentRoom == chat.getRoomName()) {
                                        listModel.removeAllElements();
                                        lstMessages.removeAll();
                                        lstMessages = new JList<>(listModel);
                                        lstMessages.setModel(listModel);
                                        currentRoom = null;
                                        
                                        activatePanelWriterMessages(false);
                                        panelMessages.revalidate();
                                        panelMessages.repaint();
                                        
                                        lstMessages.revalidate();
                                        lstMessages.repaint();
                                        
                                    }
                                }
                            });
                        }
                    }
                }
            };
            Timer timer = new Timer("timerSc");
            long diff_sec = UtilDate.differenceDateInSeconds(Calendar.getInstance().getTime(), chat.getEndChat());
            timer.schedule(task, (1000 * (diff_sec)) + ((peer.getPeerId() + 1) + 3000));
        }
    }

}

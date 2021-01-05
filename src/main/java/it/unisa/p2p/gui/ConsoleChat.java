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
import it.unisa.p2p.chat.StartAnonymousChat;
import it.unisa.p2p.interfaces.MessageListener;
import it.unisa.p2p.utils.ImageCompressor;
import it.unisa.p2p.utils.UtilDate;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author raffaeledragone
 */
public class ConsoleChat {

     HashMap<String, List<Message>> hmListMessages = new HashMap<>();
     AnonymousChatImpl peer;
     TextIO textIO = TextIoFactory.getTextIO();
     javax.swing.Timer timerCountdown;



    public ConsoleChat(String master, int id) throws Exception {
        peer = new AnonymousChatImpl(id, master, new MessageListenerImpl(id));
        startConsole(id,master);
    }

    public static void main(String[] args) throws Exception {
            
            ConsoleChat console=new ConsoleChat("127.0.0.1", 0);
    }

    private void startConsole(int id, String master) {
        //parser.parseArgument(args);
        
        TextTerminal terminal = textIO.getTextTerminal();
        terminal.printf("\nStaring peer id: %d on master node: %s\n",
                id, master);
        while (true) {
            showWelcomeScreen(terminal);
            
            int option = textIO.newIntInputReader()
                    .withMaxVal(5)
                    .withMinVal(1)
                    .read("\n");
            switch (option) {
                case 1:
                    showCreationRoomScreen(terminal);
                    break;
                case 2:
                    showJoinRoomScreen(terminal);
                    break;
                case 3:
                    showListRoomsScreen(terminal);
                    break;
                case 4:
                    showMessageChat(terminal);
                    break;
                case 5:
                    sendMessage(terminal);
                    break;
                default:
                    break;
            }
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
                    textIO.getTextTerminal().printf("[Message Received]Message received in room : "+msg.getRoomName()+"\n");
                }
            }
            if(msgWrap!=null) return msgWrap;
            if(msg!=null) return msg;
            return null;

        }
    }
    
    private void showWelcomeScreen(TextTerminal terminal) {
        terminal.printf("\nWelcome to anonymous chat");
        terminal.printf("\nType 1 to create new room");
        terminal.printf("\nType 2 to join to any room");
        terminal.printf("\nType 3 to show list of your rooms");
        terminal.printf("\nType 4 to show the messages in chat");
        terminal.printf("\nType 5 to send a message / image in a chat");
    }
    
    private void showCreationRoomScreen(TextTerminal terminal) {
        //terminal.printf("\nInsert the name of the room");
        String nameRoom=textIO.newStringInputReader()
                .read("\nInsert the name of the room");
                
        if (nameRoom != null && !nameRoom.isEmpty()) {
            Chat room = new Chat();
            room.setRoomName(nameRoom);
            room.setUsers(new HashSet<>());
            String useTimer=textIO.newStringInputReader()
                .read("\nDo you want to set a timer? type yes or no");
            if (useTimer!=null && !useTimer.isEmpty() &&  useTimer.equalsIgnoreCase("yes")) {
                int time = textIO.newIntInputReader()
                        .read("\nInsert time in minutes");
                Integer minutes = time;
                Long mills = 1000 * 60 * minutes.longValue();
                Calendar endDateTime = Calendar.getInstance();
                endDateTime.setTimeInMillis(endDateTime.getTimeInMillis() + mills);
                room.setEndChat(endDateTime.getTime());

            }
            if (peer.getMyChatList() != null && peer.getMyChatList().contains(nameRoom)) {
                terminal.printf("\nRoom already in use");
            } else {
                String res = peer.createRoom_(room);
                
                if (res != null) {
                    if (res.equals("ok")) {
                        hmListMessages.put(nameRoom, new ArrayList<>());

                        if (room.getEndChat() != null) {
                            scheduleCheckExistencyChat(room);
                        }
                    } else {
                        String out = res.equals("ko") ? "Problems during creation of room, retry." : res;
                        terminal.printf("\n"+out);
                    }

                } else {
                    terminal.printf("\nError during creation of room " + nameRoom);
                }
            }
        }
    }

    private void showJoinRoomScreen(TextTerminal terminal) {
        //Code for join in room
        String nameRoom=textIO.newStringInputReader()
                .read("\nPlease insert the name of the room");
        if (nameRoom != null && !nameRoom.isEmpty()) {
            if (peer.getMyChatList() != null && peer.getMyChatList().contains(nameRoom)) {
                terminal.printf("\nRoom already created");
            } else {
                boolean res = peer.joinRoom(nameRoom);
                if (res) {
                    
                    hmListMessages.put(nameRoom, new ArrayList<>());
                    Chat chat = peer.findChatRoom(nameRoom);
                    if (chat.getEndChat() != null) {
                        scheduleCheckExistencyChat(chat);
                        terminal.printf("\nThis is a timed room, it will be closed in : " + chat.getEndChat());
                    }
                } else {
                    terminal.printf("\nError during joining in room " + nameRoom);
                }
            }
        } else {
            terminal.printf("\nRoom name cannot be null " + nameRoom);
        }
    }

    private void showListRoomsScreen(TextTerminal terminal) {
        HashSet<String> hs = peer.getMyChatList();
        for (String room : hs){
            terminal.printf("\n"+room);
        }
    }
    
    private void scheduleCheckExistencyChat(Chat chat) {
        if (chat != null && chat.getEndChat() != null) {
            //timer
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if (peer.findChatRoom(chat.getRoomName()) == null) {
                        textIO.getTextTerminal().printf("Timed room named ["+chat.getRoomName()+"] is terminated");
                        hmListMessages.remove(chat.getRoomName());
                    }
                }
            };
            Timer timer = new Timer("timerSc");
            long diff_sec = UtilDate.differenceDateInSeconds(Calendar.getInstance().getTime(), chat.getEndChat());
            timer.schedule(task, (1000 * (diff_sec)) + ((peer.getPeerId() + 1) * 3000));
        }
    }

    private void showMessageChat(TextTerminal terminal) {
        String nameRoom=textIO.newStringInputReader()
                .read("\nPlease insert the name of the room which do you want to enter");
        if(peer.getMyChatList().contains(nameRoom)){
                List<Message> lstMsg = hmListMessages.get(nameRoom);

                for (Message message : lstMsg) {
                    String out="\n";
                    if(message.getMsg().startsWith("mymsg_")) {
                        out += "\t\t\t\t\t\t";
                        out+=message.getMsg().replaceAll("mymsg_", "");
                    }else{
                        out+=message.getMsg();
                    }
                    terminal.printf(out);
                }
        }else{
            terminal.printf("Room doesn't exist");
        }
    }

    private void sendMessage(TextTerminal terminal) {
        String nameRoom=textIO.newStringInputReader()
                .read("\nPlease insert the name of the room");
        int choice=textIO.newIntInputReader()
                .read("\nType 0 for text message, 1 for image");
        if(choice==0 || choice==1){
            Message msg=new Message();
            String text="";
            if(choice==0){
                text=textIO.newStringInputReader()
                        .read("\nInsert text message");
            }else if(choice==1){
                try {
                    String path_img=null;
                    File folderImages = new File(StartAnonymousChat.ROOTPATH+File.separator+"images");
                    File[] imgFile = folderImages.listFiles();
                    terminal.printf("This is the list of images that you can send :\n");
                    for(File f : imgFile){
                        terminal.printf(f.getName()+"\t");
                    }
                    text=textIO.newStringInputReader()
                                .read("\nInsert name of image");
                    path_img="images"+File.separator+text;
                    msg.setType(1);

                    File fImg=new File(path_img);
                    BufferedImage image = ImageIO.read(fImg);
                    image = ImageCompressor.resizeImage(image, 128, 128);
                    byte[] newimg = ImageCompressor.compressImageInJpeg(image, 0.8f);
                    msg.setImage(newimg);
                    msg.setName_file(fImg.getName());

                } catch (IOException ex) {
                    terminal.printf("Image doesn't exist!\n");
                    
                    Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                    return;
                }
            }

            msg.setMsg(text);
            msg.setDate(Calendar.getInstance().getTime());
            msg.setRoomName(nameRoom);

            String res = peer.sendMessage_(nameRoom, msg);
            if(res.equalsIgnoreCase("ok")){
                msg.setMsg("mymsg_"+msg.getMsg());
                if(hmListMessages.get(nameRoom)==null)
                    hmListMessages.put(nameRoom, new ArrayList<>());
                hmListMessages.get(nameRoom).add(msg);
            }
            
        }

    }



}

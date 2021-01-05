package it.unisa.p2p.testmain;


import it.unisa.p2p.beans.Message;
import it.unisa.p2p.beans.ImageWrapper;
import it.unisa.p2p.chat.AnonymousChatImpl;
import it.unisa.p2p.interfaces.MessageListener;
import it.unisa.p2p.utils.ImageCompressor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestFunction {

    static class MessageListenerImpl implements MessageListener {
        int peerid;
        public MessageListenerImpl(int peerid, AnonymousChatImpl peer)
        {
            this.peerid=peerid;
        }
        public Object parseMessage(Object obj) {
            System.out.println(peers[peerid]);
            if (obj instanceof Message) {
                System.out.println("Instance of message");
                Message msg = (Message) obj;
                return msg;
            } else if (obj instanceof ImageWrapper) {
                System.out.println("Instance of image");
                ImageWrapper msgWrap = (ImageWrapper) obj;
                return msgWrap;
            }
            return null;
        }

    }
    static AnonymousChatImpl[] peers;
    public static void main(String[] args) throws Exception {
          //ConsoleChat console=new ConsoleChat("127.0.0.1", 0);
          //ConsoleChat console2=new ConsoleChat("127.0.0.1", 1);
          int n_peer=50;
          peers=new AnonymousChatImpl[n_peer];
          for(int i=0;i<n_peer;++i){
            peers[i]=new AnonymousChatImpl(i,"127.0.0.1",new MessageListenerImpl(i,peers[i]));
          }
          peers[0].createRoom("Room1");
          for(int i=1;i<n_peer;++i){
              peers[i].joinRoom_("Room1");
          }

        //Image Message
        Message msg=new Message();

        msg.setType(1);
        msg.setMsg("");
        BufferedImage image = ImageIO.read(new File("images"+ File.separator+"0.png"));
        image = ImageCompressor.resizeImage(image, 128, 128);
        byte[] newimg = ImageCompressor.compressImageInJpeg(image, 0.8f);
        msg.setImage(newimg);
        msg.setRoomName("Room1");


        peers[0].sendMessage_("Room1",msg);
//        MainFrame m1=new MainFrame("127.0.0.1", 0);
//        m1.setVisible(true);
//        
//        MainFrame m2=new MainFrame("127.0.0.1", 1);
//        m2.setVisible(true);
        
//        MainFrame m3=new MainFrame("127.0.0.1", 2);
//        m3.setVisible(true);
//        
//        MainFrame m4=new MainFrame("127.0.0.1", 3);
//        m4.setVisible(true);
//        
//        MainFrame m5=new MainFrame("127.0.0.1", 4);
//        m5.setVisible(true);
//        
//        MainFrame m6=new MainFrame("127.0.0.1", 5);
//        m6.setVisible(true);
//        
//        MainFrame m7=new MainFrame("127.0.0.1", 6);
//        m7.setVisible(true);
//        
//        MainFrame m8=new MainFrame("127.0.0.1", 7);
//        m8.setVisible(true);
//        
//        MainFrame m9=new MainFrame("127.0.0.1", 8);
//        m9.setVisible(true);
//        
//        MainFrame m10=new MainFrame("127.0.0.1", 9);
//        m10.setVisible(true);
        
        
        
//        AnonymousChatImplOld peer0 = new AnonymousChatImplOld(0, "127.0.0.1", new MessageListenerImpl(0));
//        AnonymousChatImplOld peer1 = new AnonymousChatImplOld(1, "127.0.0.1", new MessageListenerImpl(1));
//        AnonymousChatImplOld peer2 = new AnonymousChatImplOld(2, "127.0.0.1", new MessageListenerImpl(2));
//        AnonymousChatImplOld peer3 = new AnonymousChatImplOld(3, "127.0.0.1", new MessageListenerImpl(3));
//
//        AnonymousChatUser peer0 = new AnonymousChatUser(0, "127.0.0.1", new MessageListenerImpl(0));
//        AnonymousChatUser peer1 = new AnonymousChatUser(1, "127.0.0.1", new MessageListenerImpl(1));
//        AnonymousChatUser peer2 = new AnonymousChatUser(2, "127.0.0.1", new MessageListenerImpl(2));
//        AnonymousChatUser peer3 = new AnonymousChatUser(3, "127.0.0.1", new MessageListenerImpl(3));
//
//
//        boolean res1 = peer0.createRoom("Room1");
//        System.out.println("Room 1 created");
//        boolean res2 = peer1.joinRoom("Room1");
//        System.out.println("joined to room 1");
//        peer1.sendMessage("Room1","Hello world by peer 1");
//        peer0.sendMessage("Room1","Hello world by peer 0");
        
    }

}

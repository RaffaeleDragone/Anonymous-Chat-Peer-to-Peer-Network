package it.unisa.p2p.testmain;


import it.unisa.p2p.gui.ConsoleChat;
import it.unisa.p2p.gui.MainFrame;
import it.unisa.p2p.interfaces.MessageListener;

public class TestFunction {

    static class MessageListenerImpl implements MessageListener {
        int peerid;
        public MessageListenerImpl(int peerid)
        {
            this.peerid=peerid;
        }
        public Object parseMessage(Object obj) {
            System.out.println(peerid+"] (Direct Message Received) "+obj);
            return "success";
        }

    }

    public static void main(String[] args) throws Exception {
          ConsoleChat console=new ConsoleChat("127.0.0.1", 0);
          ConsoleChat console2=new ConsoleChat("127.0.0.1", 1);
          
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

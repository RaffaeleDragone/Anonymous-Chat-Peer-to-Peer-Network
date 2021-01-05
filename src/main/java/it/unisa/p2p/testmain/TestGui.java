package it.unisa.p2p.testmain;

import it.unisa.p2p.beans.Message;
import it.unisa.p2p.beans.ImageWrapper;
import it.unisa.p2p.chat.AnonymousChatImpl;
import it.unisa.p2p.gui.ConsoleChat;
import it.unisa.p2p.gui.MainFrame;
import it.unisa.p2p.interfaces.MessageListener;

public class TestGui {

    static class MessageListenerImpl implements MessageListener {
        int peerid;
        AnonymousChatImpl peer;
        public MessageListenerImpl(int peerid, AnonymousChatImpl peer)
        {
            this.peerid=peerid;
            this.peer=peer;
        }
        public Object parseMessage(Object obj) {
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

    public static void main(String[] args) throws Exception {


        MainFrame m1=new MainFrame("127.0.0.1", 0);
        m1.setVisible(true);
//
        /*
        MainFrame m2=new MainFrame("127.0.0.1", 1);
        m2.setVisible(true);

        MainFrame m3=new MainFrame("127.0.0.1", 2);
        m3.setVisible(true);
        */
        //ConsoleChat console=new ConsoleChat("127.0.0.1", 0);
        //ConsoleChat console2=new ConsoleChat("127.0.0.1", 1);
////
//
        /*
        MainFrame m4=new MainFrame("127.0.0.1", 3);
        m4.setVisible(true);
////
        MainFrame m5=new MainFrame("127.0.0.1", 4);
        m5.setVisible(true);
////
        MainFrame m6=new MainFrame("127.0.0.1", 5);
        m6.setVisible(true);
////
        MainFrame m7=new MainFrame("127.0.0.1", 6);
        m7.setVisible(true);
////
        MainFrame m8=new MainFrame("127.0.0.1", 7);
        m8.setVisible(true);
////
       MainFrame m9=new MainFrame("127.0.0.1", 8);
        m9.setVisible(true);
////
        MainFrame m10=new MainFrame("127.0.0.1", 9);
        m10.setVisible(true);
    */
    }
}

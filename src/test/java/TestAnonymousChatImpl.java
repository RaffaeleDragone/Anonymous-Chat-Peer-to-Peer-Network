import it.unisa.p2p.beans.Chat;
import it.unisa.p2p.beans.Message;
import it.unisa.p2p.chat.AnonymousChatImplOld;
import it.unisa.p2p.chat.AnonymousChatUser;
import it.unisa.p2p.interfaces.MessageListener;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.*;

public class TestAnonymousChatImpl {
    protected static AnonymousChatUser peer0, peer1, peer2, peer3;

    public TestAnonymousChatImpl() throws Exception {
        class MessageListenerImpl implements MessageListener {
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
        peer0 = new AnonymousChatUser(0, "127.0.0.1", new MessageListenerImpl(0));
        peer1 = new AnonymousChatUser(1, "127.0.0.1", new MessageListenerImpl(1));
        peer2 = new AnonymousChatUser(2, "127.0.0.1", new MessageListenerImpl(2));
        peer3 = new AnonymousChatUser(3, "127.0.0.1", new MessageListenerImpl(3));
    }






    @Test
    void testCaseCreateRoom(){
        String res1=peer1.createRoom_(new Chat("Room1",null,null));
        assertEquals("ok",res1);

    }

    @Test
    void testCaseCreateRoomAlreadyCreated(){
        String res1=peer1.createRoom_(new Chat("Room_1",null,null));
        assertEquals("ok",res1);
        //Creo nuova chat con stesso nome, messaggio aspettato restituito : Room already in use
        String res1_2=peer2.createRoom_(new Chat("Room_1",null,null));
        assertEquals("Room already in use",res1_2);
    }


    @Test
    void testCaseJoinRoom(){

        String res1=peer1.createRoom_(new Chat("Room_join",null,null));
        assertEquals("ok",res1);

        String res1_2 = peer2.joinRoom_("Room_join");
        assertEquals("ok",res1_2);
    }


    @Test
    void testCaseJoinRoomAlreadyJoined(){

        String res1=peer1.createRoom_(new Chat("Room_join2",null,null));
        assertEquals("ok",res1);


        String res1_2 = peer2.joinRoom_("Room_join2");
        assertEquals("ok",res1_2);

        String res1_3 = peer2.joinRoom_("Room_join2");
        assertEquals("Already joined in Room",res1_3);

    }

    @Test
    void testCaseSendMessageSimple(){

        String res1=peer1.createRoom_(new Chat("Room_test_message",null,null));
        assertEquals("ok",res1);


        String res1_2 = peer2.joinRoom_("Room_test_message");
        assertEquals("ok",res1_2);

        Message msg=new Message();
        msg.setMsg("Hello world");
        msg.setType(0);
        msg.setRoomName("Room_test_message");
        msg.setDate(Calendar.getInstance().getTime());

        String res_msg=peer2.sendMessage_("Room_test_message",msg);
        assertEquals("ok",res_msg);

    }

    @Test
    void testCaseSendMessageWithoutJoin(){

        String res1=peer1.createRoom_(new Chat("Room_test_message_without_join",null,null));
        assertEquals("ok",res1);

        Message msg=new Message();
        msg.setMsg("Hello world");
        msg.setType(0);
        msg.setRoomName("Room_test_message_without_join");
        msg.setDate(Calendar.getInstance().getTime());

        String res_msg=peer2.sendMessage_("Room_test_message_without_join",msg);
        assertEquals("Room name doesn't exists in your rooms",res_msg);

    }


    /*
    @Test
    void testCaseSendMessageOnRoom(TestInfo testInfo){
        peer1.createRoom("Alice");
        assertTrue(peer1.sendMessage("Alice", "peer 0 send on room Alice!"));
    }
    */

    /*
    //TODO to remove it!
    void testCaseGeneral(TestInfo testInfo){

        try {

            peer1.createRoom("Alice");
            peer1.joinRoom("Alice");
            peer2.joinRoom("Alice");
            peer3.joinRoom("Alice");

            peer1.createRoom("Bob");
            peer1.joinRoom("Bob");
            peer2.joinRoom("Bob");


            peer0.sendMessage("Alice", "peer 0 send on topic Alice!");

            peer2.leaveRoom("Alice");

            peer2.leaveNetwork();

            peer0.sendMessage("Alice", "peer 0 send on topic Alice!");
            peer0.sendMessage("Alice", "peer 0 send on topic Alice!");

            System.exit(0);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

     */
}

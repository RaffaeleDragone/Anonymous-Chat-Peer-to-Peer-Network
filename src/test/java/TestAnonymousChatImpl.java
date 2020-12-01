import it.unisa.p2p.chat.AnonymousChatImplOld;
import it.unisa.p2p.interfaces.MessageListener;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestAnonymousChatImpl {
    protected AnonymousChatImplOld peer0, peer1, peer2, peer3;

    public TestAnonymousChatImpl() throws Exception{
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
        peer0 = new AnonymousChatImplOld(0, "127.0.0.1", new MessageListenerImpl(0));
        peer1 = new AnonymousChatImplOld(1, "127.0.0.1", new MessageListenerImpl(1));
        peer2 = new AnonymousChatImplOld(2, "127.0.0.1", new MessageListenerImpl(2));
        peer3 = new AnonymousChatImplOld(3, "127.0.0.1", new MessageListenerImpl(3));

    }
    /*
    @Test
    void testCaseCreateRoom(TestInfo testInfo){
        assertTrue(peer1.createRoom("Alice"));
    }
    @Test
    void testCaseSendMessageOnRoom(TestInfo testInfo){
        peer1.createRoom("Alice");
        assertTrue(peer1.sendMessage("Alice", "peer 0 send on room Alice!"));
    }
    */

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
}

import it.unisa.p2p.beans.Chat;
import it.unisa.p2p.beans.Message;
import it.unisa.p2p.beans.ImageWrapper;
import it.unisa.p2p.chat.AnonymousChatImpl;
import it.unisa.p2p.interfaces.MessageListener;
import it.unisa.p2p.utils.ImageCompressor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class TestAnonymousChatImpl {
    protected static AnonymousChatImpl peer0, peer1, peer2, peer3;

    public TestAnonymousChatImpl() throws Exception {
    }

    static class MessageListenerImpl implements MessageListener {
        int peerid;

        public MessageListenerImpl(int peerid)
        {
            this.peerid=peerid;
        }

        public Object parseMessage(Object obj) {
            Message msg=null;
            ImageWrapper imageWrapper=null;
            if (obj instanceof Message) {
                msg = (Message) obj;
            } else if (obj instanceof ImageWrapper) {
                imageWrapper = (ImageWrapper) obj;
                msg=imageWrapper.getMsg();
            }
            return imageWrapper!=null ? imageWrapper : msg;
        }


    }

    /*
    Inizializzazione dei 4 peer utilizzati per i test cases
     */
    @BeforeAll
    static void initPeers() throws Exception {
        peer0 = new AnonymousChatImpl(0, "127.0.0.1", new MessageListenerImpl(0));
        peer1 = new AnonymousChatImpl(1, "127.0.0.1", new MessageListenerImpl(1));
        peer2 = new AnonymousChatImpl(2, "127.0.0.1", new MessageListenerImpl(2));
        peer3 = new AnonymousChatImpl(3, "127.0.0.1", new MessageListenerImpl(3));
    }
    
    /*
    Test case per la creazione di una room
     */
    @Test
    @DisplayName("1.1_Create Room")
    void testCaseCreateRoom(){
        String res1=peer1.createRoom_(new Chat("1.1_Create Room",null,null));
        assertEquals("ok",res1);

        assertTrue(peer1.leaveRoom("1.1_Create Room"));
    }

    /*
    Test case per la creazione di una room temporizzata ( durata 2 secondi )
     */
    @Test
    @DisplayName("1.2_Create Timed Room")
    void testCaseCreateTimedRoom(){
        Chat room = createTimedRoom("1.2_Create Timed Room",2000L);
        assertTrue("ok".equals(peer1.createRoom_(room)));
        assertTrue(peer2.joinRoom_("1.2_Create Timed Room").equals("ok"));

    }

    /*
    Test case per la creazione di una room già creata.
    2 casi :
        - Un peer prova a creare una room già creata. Risultato aspettato : "Room already created"
        - Un peer prova a creare una room con nome uguale ad una delle room di cui fa parte. Risultato aspettato : "Room already present in your rooms"
     */
    @Test
    @DisplayName("1.3_Create Room already created")
    void testCaseCreateRoomAlreadyCreated(){
        assertTrue("ok".equals(peer1.createRoom_(new Chat("1.3_Create Room already created",null,null))));
        assertTrue("Room already created".equals(peer2.createRoom_(new Chat("1.3_Create Room already created",null,null))));
        assertTrue("Room already present in your rooms".equals(peer1.createRoom_(new Chat("1.3_Create Room already created",null,null))));
    }

    /*
    Test case semplice per la join in una room
     */
    @Test
    @DisplayName("2.1_Join Room")
    void testCaseJoinRoom(){
        assertTrue("ok".equals(peer1.createRoom_(new Chat("2.1_Join Room",null,null))));
        assertTrue("ok".equals(peer2.joinRoom_("2.1_Join Room")));
        assertTrue("ok".equals(peer3.joinRoom_("2.1_Join Room")));
    }


    /*
    Test case per la join in una room temporizzata
    Viene creata una room con durata 2 secondi.
    Un peer prova ad accedere subito dopo la sua creazione. risultato aspettato : ok
    Un peer prova ad accedere dopo che la room è scaduta. Risultato aspettato : ko
     */
    @Test
    @DisplayName("2.2_Join Timed Room")
    void testCaseJoinTimedRoom(){
        Chat room = createTimedRoom("2.2_Join Timed Room",2000L);
        assertTrue("ok".equals(peer1.createRoom_(room)));

        assertTrue("ok".equals(peer2.joinRoom_("2.2_Join Timed Room")));

        try {
            Thread.sleep(5000);
            assertTrue(peer3.joinRoom_("2.2_Join Timed Room").equals("ko"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /*
    Test case con join in una room di cui si fa già parte. Risultato aspettato : "Already joined"
     */
    @Test
    @DisplayName("2.3_Join Room Already Joined")
    void testCaseJoinRoomAlreadyJoined(){
        assertTrue("ok".equals(peer1.createRoom_(new Chat("2.3_Join Room Already Joined",null,null))));
        assertTrue("ok".equals(peer2.joinRoom_("2.3_Join Room Already Joined")));
        assertTrue("ok".equals(peer3.joinRoom_("2.3_Join Room Already Joined")));

        assertEquals("Already joined",peer3.joinRoom_("2.3_Join Room Already Joined"));
    }

    /*
    Test case con una join in una room inesistente
     */
    @Test
    @DisplayName("2.4_Join Room inexistent")
    void testCaseJoinRoomInexistent(){
        assertTrue("ko".equals(peer1.joinRoom_("2.4_Join Room inexistent")));
    }

    /*
    Test case con una join in una room temporizzata che è scaduta. Risultato aspettato : ko
     */
    @Test
    @DisplayName("2.5_Join Timed Room expired")
    void testCaseJoinTimedRoomExpired(){
        Chat room = createTimedRoom("2.5_Join Timed Room expired",1000L);
        assertTrue("ok".equals(peer1.createRoom_(room)));
        try {
            Thread.sleep(5000);
            assertTrue(peer3.joinRoom_("2.5_Join Timed Room expired").equals("ko"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /*
    Test case semplice per la leave da una room
     */
    @Test
    @DisplayName("3.1_Leave Room")
    void testCaseLeaveRoom(){
        assertTrue("ok".equals(peer1.createRoom_(new Chat("3.1_Leave Room",null,null))));
        assertTrue("ok".equals(peer2.joinRoom_("3.1_Leave Room")));

        assertTrue(peer1.leaveRoom("3.1_Leave Room"));
        assertTrue(peer2.leaveRoom("3.1_Leave Room"));
    }

    /*
    Test case per la verifica di una leave su una room non joinata
     */
    @Test
    @DisplayName("3.2_Leave Room Not Joined")
    void testCaseLeaveRoomNotJoined(){
        assertTrue("ok".equals(peer1.createRoom_(new Chat("3.2_Leave Room Not Joined",null,null))));

        assertFalse(peer2.leaveRoom("3.2_Leave Room Not Joined"));
    }

    /*
    Test case per la verifica di una leave su una room non creata
     */
    @Test
    @DisplayName("3.3_Leave Room Not Created")
    void testCaseLeaveRoomNotCreated(){
        assertFalse(peer2.leaveRoom("3.3_Leave Room Not Created"));
    }

    /*
    Test case per l'invio di due messaggi testuale
     */
    @Test
    @DisplayName("4.1_Send Text Message")
    void textCaseSendTextMessage(){

        assertTrue("ok".equals(peer0.createRoom_(new Chat("4.1_Send Text Message",null,null))));
        assertTrue("ok".equals(peer1.joinRoom_("4.1_Send Text Message")));
        assertTrue("ok".equals(peer2.joinRoom_("4.1_Send Text Message")));
        assertTrue("ok".equals(peer3.joinRoom_("4.1_Send Text Message")));


        Message msg=new Message();
        msg.setMsg("Hello world");
        msg.setType(0);
        msg.setRoomName("4.1_Send Text Message");

        assertTrue("ok".equals(peer2.sendMessage_("4.1_Send Text Message",msg)));
        msg.setMsg("Hello world 2");
        assertTrue("ok".equals(peer3.sendMessage_("4.1_Send Text Message",msg)));


    }

    /*
    Test case per l'invio di un messaggio contenente immagine
     */
    @Test
    @DisplayName("4.2_Send Image Message")
    void testCaseSendImageMessage() throws IOException, IOException {
        assertTrue("ok".equals(peer0.createRoom_(new Chat("4.2_Send Image Message",null,null))));
        assertTrue("ok".equals(peer1.joinRoom_("4.2_Send Image Message")));
        assertTrue("ok".equals(peer2.joinRoom_("4.2_Send Image Message")));
        assertTrue("ok".equals(peer3.joinRoom_("4.2_Send Image Message")));

        Message msg=new Message();

        msg.setType(1);
        msg.setMsg("");
        BufferedImage image = ImageIO.read(new File("images"+ File.separator+"0.png"));
        image = ImageCompressor.resizeImage(image, 128, 128);
        byte[] newimg = ImageCompressor.compressImageInJpeg(image, 0.8f);
        msg.setImage(newimg);
        msg.setRoomName("4.2_Send Image Message");


        assertTrue("ok".equals(peer2.sendMessage_("4.2_Send Image Message",msg)));
    }

    /*
    Test case per l'invio di un messaggio in una room non joinata. Risultato atteso : "Not Joined in Room"
     */
    @Test
    @DisplayName("4.3_Send Message Room not joined")
    void textCaseSendTextMessageNotJoinedRoom(){

        assertTrue("ok".equals(peer0.createRoom_(new Chat("4.3_Send Message Room not joined",null,null))));


        Message msg=new Message();
        msg.setMsg("Hello world");
        msg.setType(0);
        msg.setRoomName("4.3_Send Message Room not joined");

        assertTrue("Not Joined in Room".equals(peer2.sendMessage_("4.3_Send Message Room not joined",msg)));


    }

    /*
    Test case per l'invio di un messaggio in un room temporizzata.
    Caso 1 : peer2 invia messaggio subito dopo la creazione della room. Risultato atteso : ok
    Caso 2 : peer2 e peer1 inviano un messaggio dopo che la room è scaduta. Risultato atteso : "Not Joined in Room".
     */
    @Test
    @DisplayName("4.4_Send Message Room Timed")
    void textCaseSendMessageRoomTimed(){
        String nameRoom = "4.4_Send Message Room Timed";
        Chat room = createTimedRoom(nameRoom,2000L);
        assertTrue("ok".equals(peer1.createRoom_(room)));
        assertTrue("ok".equals(peer2.joinRoom_(nameRoom)));

        Message msg=new Message();
        msg.setMsg("Hello world");
        msg.setType(0);
        msg.setRoomName(nameRoom);

        assertTrue("ok".equals(peer2.sendMessage_(nameRoom,msg)));

        try {
            Thread.sleep(5000);
            //Expired
            assertTrue("Not Joined in Room".equals(peer1.sendMessage_(nameRoom,msg)));
            assertTrue("Not Joined in Room".equals(peer2.sendMessage_(nameRoom,msg)));

            assertTrue(peer3.joinRoom_(nameRoom).equals("ko"));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /*
    Test case per l'invio di un messaggio in una room con 1 solo utente ( il sender ).
    Risultato atteso : ok.
     */
    @Test
    @DisplayName("4.5_Send Message Room Without Users")
    void textCaseSendMessageRoomWithoutUsers(){
        String nameRoom="4.5_Send Message Room Without Users";
        assertTrue("ok".equals(peer0.createRoom_(new Chat(nameRoom,null,null))));

        Message msg=new Message();
        msg.setMsg("Hello world");
        msg.setType(0);
        msg.setRoomName(nameRoom);

        assertTrue("ok".equals(peer0.sendMessage_(nameRoom,msg)));


    }

    /*
    Test case per la verifica della corretta ricezione dei messaggi.
    Viene implementato soltanto per la ricezione di messaggi con immagini in quanto ritenuto metodo più complesso all'invio di semplice messaggio di testo
    Per sincronizzare il peer sender con la ricezione di tutti i messaggi da parte dei receivers viene fatto uso della classe CountDownLatch
    CountDownLatch è una implementazione di un tipo di sincronizzazione che consente ad un thread di attendere uno o più thread prima di compiere una operazione
    Quando viene inizializzato un oggetto di tipo CountDownLatch viene specificato il numero di thread da attendere.
    Ogni thread decrementa il contatore tramite il metodo .countDown() una volta completato il lavoro.
    Appena il contatore arriva a 0 il thread principale può proseguire
    */
    @Test
    @DisplayName("5.1 Receive Image Message")
    void testCaseReceiveImage() throws Exception {

        String nameRoom="5.2 Receive Image Message";
        assertTrue("ok".equals(peer0.createRoom_(new Chat(nameRoom,null,null))));

        int n_receivers = 10;

        //Text Message
        List<Message> imgReceived=new ArrayList<>(n_receivers);
        CountDownLatch latch=new CountDownLatch(n_receivers);
        List<AnonymousChatImpl> listReceivers=new ArrayList<>(n_receivers);
        for(int i=0; i<n_receivers;++i){
            AnonymousChatImpl receiver=null;
            receiver = new AnonymousChatImpl(i + 5, "127.0.0.1", new MessageListener() {
                @Override
                public Object parseMessage(Object obj) {
                    Message msg=null;
                    ImageWrapper imgWrap=null;
                    if (obj instanceof Message) {
                        msg = (Message) obj;
                        imgReceived.add(msg);
                    } else if (obj instanceof ImageWrapper) {
                        imgWrap = (ImageWrapper) obj;
                        imgReceived.add(imgWrap.getMsg());
                    }
                    latch.countDown();
                    return imgWrap!=null ? imgWrap : msg;
                }
            });
            assertTrue("ok".equals(receiver.joinRoom_(nameRoom)));
            listReceivers.add(receiver);
        }

        Message msg=new Message();

        msg.setMsg("");
        msg.setType(1);
        msg.setRoomName(nameRoom);
        BufferedImage image = ImageIO.read(new File("images"+ File.separator+"0.png"));
        image = ImageCompressor.resizeImage(image, 128, 128);
        byte[] newimg = ImageCompressor.compressImageInJpeg(image, 0.8f);

        msg.setImage(newimg);


        assertTrue("ok".equals(peer0.sendMessage_(nameRoom,msg)));
        latch.await(4, TimeUnit.SECONDS);
        for(Message m : imgReceived){
            assertTrue(Arrays.equals(m.getImage(),msg.getImage()));
        }

        for(AnonymousChatImpl tmp_peer : listReceivers)
            assertTrue(tmp_peer.leaveNetwork());
    }

    /*
    Test case per l'aggiornamento implicito degli utenti della chat.
    Viene simulato uno shutdown di un peer senza che quest'ultimo lasci la room.
    Durante la send message il peer si accorge che l'invio non è andato a buon fine tramite il customListener , prova a reinviare il messaggio ed in
    caso negativo rimuove il receiver dalla lista degli utenti della chat
    Risultato atteso : ok.
     */
    @Test
    @DisplayName("5_Test crash peer")
    void textCaseCrashUser() throws Exception {
        String nameRoom="5_Test crash peer";
        //Il peer che crea una room ci effettua anche una join diretta. num users room = 1
        assertTrue("ok".equals(peer0.createRoom_(new Chat(nameRoom,null,null))));
        AnonymousChatImpl peertmp = new AnonymousChatImpl(5, "127.0.0.1", new MessageListenerImpl(20));
        assertTrue("ok".equals(peertmp.joinRoom_(nameRoom))); // num users room = 2
        peertmp.forceLeaveNetwork();// num users room = 2

        Message msg=new Message();
        msg.setMsg("Hello world");
        msg.setType(0);
        msg.setRoomName(nameRoom);
        assertTrue("ok".equals(peer0.sendMessage_(nameRoom,msg)));
        // Si attiva il listener dell'invio dei messaggi, che prova ad inviare per 2 volte il messaggio senza successo.
        //Avvia quindi la procedura per eliminare l'utente dalla chat perchè non + presente in rete.

        //wait some time
        Thread.sleep(3000);
        Chat chat = peer0.findChatRoom(nameRoom);
        assertTrue(chat.getUsers().size()==1);

    }


    private Chat createTimedRoom(String name_room, long millsRetard) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(now.getTimeInMillis() + millsRetard);
        return new Chat(name_room,null,now.getTime());
    }
    
    @AfterAll
    static void leaveNetwork(){
        assertTrue(peer0.leaveNetwork());
        assertTrue(peer1.leaveNetwork());
        assertTrue(peer2.leaveNetwork());
        assertTrue(peer3.leaveNetwork());
    }
}

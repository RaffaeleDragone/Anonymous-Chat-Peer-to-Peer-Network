package it.unisa.p2p.chat;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;

import it.unisa.p2p.beans.Chat;
import it.unisa.p2p.beans.Message;
import it.unisa.p2p.beans.ImageWrapper;
import it.unisa.p2p.interfaces.AnonymousChat;
import it.unisa.p2p.interfaces.MessageListener;
import it.unisa.p2p.utils.UtilDate;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.FutureSend;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.BaseFuture;
import net.tomp2p.futures.BaseFutureAdapter;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDirect;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;
import org.apache.log4j.Logger;

public class AnonymousChatImpl implements AnonymousChat {

    final private Peer peer;
    final private PeerDHT _dht;
    final private int DEFAULT_MASTER_PORT = 4000;
    private static int peerId;
    private HashSet<String> myChatList = new HashSet<>();

    private static Logger logger = Logger.getLogger(AnonymousChatImpl.class);

    public AnonymousChatImpl(int _id, String _master_peer, final MessageListener _listener) throws Exception {
        peerId = _id;
        peer = new PeerBuilder(Number160.createHash(_id)).ports(DEFAULT_MASTER_PORT + _id).start();
        _dht = new PeerBuilderDHT(peer).start();

        FutureBootstrap fb = peer.bootstrap().inetAddress(InetAddress.getByName(_master_peer)).ports(DEFAULT_MASTER_PORT).start();
        fb.awaitUninterruptibly();
        if (fb.isSuccess()) {
            peer.discover().peerAddress(fb.bootstrapTo().iterator().next()).start().awaitUninterruptibly();
        } else {
            throw new Exception("Error in master peer bootstrap.");
        }

        peer.objectDataReply(new ObjectDataReply() {
            public Object reply(PeerAddress sender, Object request) throws Exception {
                Object objMsg = _listener.parseMessage(request);
                if (objMsg instanceof ImageWrapper) {//Se è una istanza di imageWrapper allora procedi con il forware dell'immagine ai receivers
                    ImageWrapper msgWrap = (ImageWrapper) request;
                    forwardImage(msgWrap.getMsg(), msgWrap.getReceivers());
                }
                return "success";
            }
        });
    }

    /*
    Metodo per la ricerca di una specifica chat tramite operazione di get nella DHT.
    Tale metodo viene richiamato sia nella fase di creazione che nella fase di join per verificare se la room è presente nella dht
    Nel caso di una chat con una data di scadenza, il task di eliminazione della chat una volta scaduto il tempo viene richiamato soltanto dai peer presenti nella room
    Potrebbe verificarsi una situazione in cui tutti i peer abbandonano la room, e quest'ultima scade. In tal caso questo metodo fa di supporto a tale operazione in quanto
    dopo aver effettuato la ricerca della room nella dht, viene verificata se la room è scaduta ed in tal caso viene eliminata dalla dht.
     */
    public Chat findChatRoom(String _room_name) {
        try {
            if (_room_name != null) {
                FutureGet futureGet = _dht.get(Number160.createHash(_room_name)).start();
                futureGet.awaitUninterruptibly();

                if (futureGet.isSuccess()) {
                    if (futureGet.isEmpty()) {
                        return null;
                    }
                    Chat chat = (Chat) futureGet.dataMap().values().iterator().next().object();
                    if(chat.getEndChat()!=null){//Verifica se la chat è una chat con data di fine. In tal caso verifica se la è ancora attiva ed in caso negativo la elimina.
                        long diff_sec = UtilDate.differenceDateInSeconds(Calendar.getInstance().getTime(), chat.getEndChat());//Calcola la differenza in secondi tra l'orario corrente e quello di fine della room
                        if(diff_sec<=0){
                            _dht.remove(Number160.createHash(chat.getRoomName())).start().awaitUninterruptibly();//Rimozione della room dalla dht
                            return null;//Room non presente
                        }
                    }
                    return chat;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
    Metodo di controllo per la creazione di una stanza.
    Incapsula al suo interno il metodo createRoom definito dall'interfaccia AnonymousChat
    Controlla se la stanza è già stata creata o è già stata joinata.
     */
    public String createRoom_(Chat room) {
        try {
            if (myChatList.contains(room.getRoomName())) { // Verifica se nelle room alle quali si è già effettuato l'accesso ne è presente una con lo stesso nome
                return "Room already present in your rooms";
            }
            String name_room = room.getRoomName();

            Chat existing = findChatRoom(name_room);//verifica se esiste già una room con stesso nome
            if (existing == null) {
                boolean res = createRoom(room.getRoomName());//creazione della room. Chiamata al metodo createRoom definito dall'interfaccia AnonymousChat
                if (res) {//Se la room è stata correttamente creata
                    if (room.getEndChat() != null) {//Verifica la presenta di una data di scadenza della chat, in tal caso effettua un update nella dht.
                        existing = findChatRoom(room.getRoomName());
                        if (existing != null) {
                            existing.setEndChat(room.getEndChat());
                            _dht.put(Number160.createHash(existing.getRoomName())).data(new Data(existing)).start().awaitUninterruptibly();//Update dht
                            scheduleExpireChat(existing);//Imposta lo uno scheduler che va ad eliminare la chat una volta terminato il tempo stabilito.
                        }
                    }
                    return "ok";
                } else {
                    return "ko";
                }
            } else {
                return "Room already created";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "ko";
    }

    //Metodo definito dall'interfaccia AnonymousChat
    //Il peer che crea la room effettua anche la join in automatico
    @Override
    public boolean createRoom(String _room_name) {
        try {
            Chat c = new Chat(_room_name, new HashSet<>(), null);
            c.setUsers(new HashSet<>());
            c.getUsers().add(_dht.peer().peerAddress());//Aggiungo il peer agli utenti della room
            _dht.put(Number160.createHash(_room_name)).data(new Data(c)).start().awaitUninterruptibly();
            myChatList.add(_room_name);//Aggiungo il nome della room in una lista locale al peer contenente le room in cui è presente
            logger.info("Room created, name : "+_room_name);
            return true;
        } catch (Exception e) {
            logger.info("Problem during creation of room, name : "+_room_name);
            e.printStackTrace();
        }
        return false;
    }

    /*
    Schedula l'eliminazione di una specifica chat
    Tale metodo non è soltanto utilizzato dal peer che crea la stanza, ma anche dagli altri peer di cui effettuano la join.
    In tal modo la room viene eliminata anche se il creatore della room non è + presente nella room / nella rete.
     */
    public void scheduleExpireChat(Chat chat) {
        if (chat != null && chat.getEndChat() != null) {
            //timer
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    FutureGet futureGet = _dht.get(Number160.createHash(chat.getRoomName())).start();
                    futureGet.awaitUninterruptibly();
                    if (futureGet.isSuccess()) {//Verifica che la room sia presente
                        _dht.remove(Number160.createHash(chat.getRoomName())).start().awaitUninterruptibly();//Rimozione della room dalla dht
                    }
                    myChatList.remove(chat.getRoomName());//Rimozione della room dalla lista rooms locale
                }
            };
            Timer timer = new Timer("timer" + chat.getRoomName());
            long diff_sec = UtilDate.differenceDateInSeconds(Calendar.getInstance().getTime(), chat.getEndChat());//Calcola la differenza in secondi tra l'orario corrente e quello di fine della room
            timer.schedule(task, (1000 * diff_sec) + (1000));//Schedula lo specifico task aggiungendo 1 secondo di delay
        }

    }

    /*
    Metodo di controllo per la join di una room.
    Incapsula al suo interno il metodo joinRoom definito dall'interfaccia AnonymousChat
    Controlla se la stanza è già stata joinata.
     */
    public String joinRoom_(String _room_name) {
        try {
            if (myChatList.contains(_room_name)) {//Verifica se la room è già presente nelle room locali
                return "Already joined";
            } else {
                boolean res = false;
                res = joinRoom(_room_name);//joinRoom
                return (res ? "ok" : "ko");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "ko";
    }

    //Metodo definito dall'interfaccia AnonymousChat
    @Override
    public boolean joinRoom(String _room_name) {
        try {
            Chat chat = findChatRoom(_room_name);//Verifica se la room sulla quale si vuol effettuare join esiste
            if (chat != null) {
                chat.addAnUser(_dht.peer().peerAddress());//Aggiunge il peer alla lista di utenti della chat
                _dht.put(Number160.createHash(_room_name)).data(new Data(chat)).start().awaitUninterruptibly();//update DHT
                myChatList.add(_room_name);//Aggiunge la room alla lista di rooms locali
                if (chat.getEndChat() != null) {//Verifica se la room nella quale è appena entrato ha una data di scadenza
                    scheduleExpireChat(chat);//Schedula il task per l'eliminazione della room
                }
                logger.info("Room joined, name : "+_room_name);
                return true;
            }
            return false;

        } catch (Exception e) {
            logger.info("Problems during join, name room : "+_room_name);
            e.printStackTrace();
        }
        return false;
    }


    //Metodo definito dall'interfaccia AnonymousChat
    @Override
    public boolean leaveRoom(String _room_name) {
        try {
            if (myChatList.contains(_room_name)) {//Verifica se la room dalla quale si vuole effettuare la leave è presente nelle room locali
                Chat currentChat = findChatRoom(_room_name);//Ricerca la room nella dht
                if (currentChat != null) {
                    currentChat.removeAnUser(_dht.peer().peerAddress());//Rimuove il peer dalla lista di peer partecipanti alla chat
                    _dht.put(Number160.createHash(_room_name)).data(new Data(currentChat)).start().awaitUninterruptibly();//Update dht
                    myChatList.remove(_room_name);
                    logger.info("Room leaved, name : "+_room_name);
                    return true;
                } else {
                    return false;
                }
            }
            return false;
        } catch (Exception e) {
            logger.info("Problems during leaving room, name : "+_room_name);
            e.printStackTrace();
        }
        return false;
    }

    /*
    Metodo di controllo per l'invio di un messaggio all'interno di una chat.
    Incapsula al suo interno il metodo sendMessage definito dall'interfaccia AnonymousChat per l'invio di messaggi testuali
     */
    public String sendMessage_(String _room_name, Message msg) {
        String res = "";
        if (msg != null && msg.getRoomName() != null) {
            if (myChatList.contains(msg.getRoomName())) {
                boolean sent = false;
                if (msg.getType() == 1 && msg.getImage() != null) {
                    //image message
                    sent = sendImage(_room_name, msg);
                } else {
                    //text message
                    sent = sendMessage(_room_name, msg.getMsg());
                }
                if (sent) {
                    return "ok";
                } else {
                    return "ko";
                }
            } else {
                res = "Not Joined in Room";
            }
        }
        return res;
    }

    //Metodo definito nell'interfaccia AnonymousChat per l'invio di messaggi testuali
    @Override
    public boolean sendMessage(String _room_name, String _text_message) {
        try {
            if (myChatList.contains(_room_name)) {
                Chat currentChat = findChatRoom(_room_name);//Verifica l'esistenza della chat nella quale si sta inviando il messaggio
                if (currentChat != null && currentChat.getUsers() != null) {
                    Message msg = new Message();
                    msg.setRoomName(_room_name);
                    msg.setMsg(_text_message);
                    msg.setType(0);
                    msg.setDate(Calendar.getInstance().getTime());
                    for (PeerAddress peerToSend : currentChat.getUsers()) {
                        if (!peerToSend.equals(_dht.peer().peerAddress())) { //Invia messaggio soltanto agli altri peer, non anche a se stesso
                            FutureDirect futureDirect = _dht.peer().sendDirect(peerToSend).object(msg).start();
                            futureDirect.addListener(
                                    new CustomFutureDirectAsyncListener(_room_name, peerToSend, msg, 1));//Custom listener implementato per l'invio non bloccante di messaggi diretti
                        }
                    }
                    logger.info("Text message sent, name room : "+_room_name);
                    return true;
                }
                return false;
            }
            return false;
        } catch (Exception e) {
            logger.info("Problems during send text message, name room : "+_room_name);
            e.printStackTrace();
        }
        return false;
    }

    /*
    Metodo definito per l'invio di un messaggio contenente una immagine
    A differenza dell'invio di un messaggio di testo, con un elevato numero di utenti partecipanti alla chat l'invio di una immagine ad ognuno di essi potrebbe essere
    una operazione troppo onerosa da far svolgere ad un singolo peer.
    A tal proposito tale metodo cerca di sfruttare il potenziale di una rete peer-to-peer trasformando un certo numero di ricevitori in attivi partecipanti per l'invio
    del messaggio con immagine agli utenti della chat
     */
    private boolean sendImage(String _room_name, Message msg) {
        if (!myChatList.contains(_room_name)) {// invii mess solo se sei in quella stanza
            return false;
        }
        Chat currentChat = findChatRoom(_room_name);
        if (currentChat != null) {
            msg.setDate(Calendar.getInstance().getTime());
            HashSet<PeerAddress> receivers = currentChat.getUsers();
            ImageWrapper msgWrapper = new ImageWrapper();
            msgWrapper.setMsg(msg);

            receivers.remove(_dht.peer().peerAddress());//Rimuove il peer che invia il messaggio dalla lista di receivers
            int n_msg = receivers.size();//Num messaggi totali da inviare
            if (n_msg > 3) {//Se i messaggi da inviare sono <=3 effettua send direct ad ognuno
                int msg_each_peer = (int) (Math.log(n_msg) / Math.log(2));//Individua in modo equo il numero di messaggi che dovrebbe inviare ogni peer per creare una distribuzione equa
                int senders = n_msg / msg_each_peer;//Calcola il numero di receivers che parteciperanno all'invio della immagine

                Iterator<PeerAddress> it = receivers.iterator();
                for (int i = 0, j = 0; i < senders && it.hasNext(); ++i) { // assegna un imageWrapper ad ogni nuovo sender
                    int rest = n_msg - (msg_each_peer * senders);
                    int m = msg_each_peer - 1;//rimuove 1 messaggio ad ogni peer poiché tale messaggio sarà contenuto nel wrapper inviato dal peer "master" al peer corrente
                    if (rest > 0 && i < rest) {
                        ++m;
                    }

                    PeerAddress snd = it.next();
                    if (snd.equals(_dht.peer().peerAddress())) {
                        snd = it.next();
                    }
                    msgWrapper.setReceivers(new HashSet<>());
                    for (int x = 0; x < m && it.hasNext(); ++x) {
                        msgWrapper.getReceivers().add(it.next());//Aggiunge i receivers nell'imageWrapper
                    }

                    prepareAndSendWrapper(snd, msgWrapper);

                }
                return true;
            } else {
                for (PeerAddress peerToSend : receivers) {
                    if (!peerToSend.equals(_dht.peer().peerAddress())) { //Send message only to other peers
                        FutureDirect futureDirect = _dht.peer().sendDirect(peerToSend).object(msg).start();
                        futureDirect.addListener(new CustomFutureDirectAsyncListener(_room_name, peerToSend, msg, 1));
                    }
                }
                return true;
            }
        }
        return false;
    }

    /*
    Tale metodo ricorsivo effettua l'invio dell'imageWrapper ad uno specifico sender, se presente.
     */
    private void prepareAndSendWrapper(PeerAddress snd, ImageWrapper msgWrapper) {
        if (snd != null && msgWrapper != null) {
            HashSet<PeerAddress> receivers = msgWrapper.getReceivers();
            if (receivers == null || receivers.size() == 0) {//Se non ci sono receivers
                //Send normal msg perchè è l'unico che riceve, non deve fare il forward
                FutureDirect futureDirect = _dht.peer().sendDirect(snd).object(msgWrapper.getMsg()).start();
                futureDirect.awaitUninterruptibly();
            } else {
                //CI sono receivers
                FutureDirect futureDirect = _dht.peer().sendDirect(snd).object(msgWrapper).start();
                futureDirect.awaitUninterruptibly();

                if (futureDirect.isFailed()) {//Se il messaggio non è arrivato a destinazione
                    PeerAddress nextSender = receivers.iterator().next();//Sceglie il prossimo sender per esclusione
                    receivers.remove(nextSender);//Rimuove il nuovo sender dai vecchi receivers
                    prepareAndSendWrapper(nextSender, msgWrapper);
                }
            }
        }

    }

    /*
    Metodo per il forward di una immagine, richiamato da un peer che riceve una istanza di un imageWrapper
     */
    public void forwardImage(Message msg, HashSet<PeerAddress> receivers) {
        for (PeerAddress peerToSend : receivers) {
            if (!peerToSend.equals(_dht.peer().peerAddress())) { //Send message only to other peers
                FutureDirect futureDirect = _dht.peer().sendDirect(peerToSend).object(msg).start();
                //Async Listener
                futureDirect.addListener(new CustomFutureDirectAsyncListener(msg.getRoomName(), peerToSend, msg, 1));
            }
        }
    }

    public boolean leaveNetwork() {
        for (String schat : new ArrayList<String>(myChatList)) {
            leaveRoom(schat);
        }
        _dht.peer().announceShutdown().start().awaitUninterruptibly();
        return true;
    }

    public boolean forceLeaveNetwork() {
        _dht.peer().shutdown();
        return true;
    }
    /*
    listener, which gets called whenever a result is ready. It is preferred to use this second option and avoid blocking, because in the worst case, you might cause a deadlock if await() is called from a wrong (I/O) thread.
    If such a listener is used, then the listeners gets called in all cases. If no peer replies, the timeout handler triggers the listener.
     */
    class CustomFutureDirectAsyncListener extends BaseFutureAdapter<FutureDirect> {

        String _room_name;
        PeerAddress receiver;
        Message msg;
        int tentative;//Num tentativi

        CustomFutureDirectAsyncListener(String _room_name, PeerAddress receiver, Message msg, int tentative) {
            this._room_name = _room_name;
            this.receiver = receiver;
            this.msg = msg;
            this.tentative = tentative;
        }

        @Override
        public void operationComplete(FutureDirect future) throws Exception {
            if (future.isSuccess()) { // this flag indicates if the future was successful
                //System.out.println("success");
            } else {
                if (future.isFailed()) {//Se l'invio del messaggio è fallito
                    if (tentative >= 2) {//Se l'invio è stato provato per 2 volte
                        removeUserFromChat(_room_name,receiver);
                    } else {
                        logger.info("Future not successful. Reason = " + future.failedReason());
                        future = _dht.peer().sendDirect(receiver).object(msg).start();//Prova a reinviare il messaggio
                        future.addListener(new CustomFutureDirectAsyncListener(_room_name, receiver, msg, 2));//Imposta tentativo = 2
                    }
                }
            }
        }

        private void removeUserFromChat(String _room_name, PeerAddress receiver) {
            FutureGet futureGet = _dht.get(Number160.createHash(_room_name)).start();
            futureGet.addListener(new BaseFutureAdapter<FutureGet>() {
                @Override
                public void operationComplete(FutureGet future) throws Exception {
                    if (future.isSuccess()) {
                        Chat chat = (Chat) future.dataMap().values().iterator().next().object();
                        chat.removeAnUser(receiver);//Rimuovi l'utente perchè non + presente nella rete
                        _dht.put(Number160.createHash(msg.getRoomName())).data(new Data(chat)).start();//Update dht 
                    }
                }
            });
        }
    }

    public HashSet<String> getMyChatList() {
        return myChatList;
    }
    public static int getPeerId() {
        return peerId;
    }

}

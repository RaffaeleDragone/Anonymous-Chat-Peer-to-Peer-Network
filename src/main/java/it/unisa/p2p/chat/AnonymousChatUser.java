package it.unisa.p2p.chat;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;

import it.unisa.p2p.beans.Chat;
import it.unisa.p2p.beans.Message;
import it.unisa.p2p.beans.MessageWrapper;
import it.unisa.p2p.interfaces.AnonymousChat;
import it.unisa.p2p.interfaces.MessageListener;
import it.unisa.p2p.utils.UtilDate;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
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

public class AnonymousChatUser implements AnonymousChat {

    final private Peer peer;
    final private PeerDHT _dht;
    final private int DEFAULT_MASTER_PORT = 4000;
    private static int peerId;
    private HashSet<String> myChatList = new HashSet<>();

    private static Logger logger = Logger.getLogger(AnonymousChatUser.class);

    public AnonymousChatUser(int _id, String _master_peer, final MessageListener _listener) throws Exception {
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
                return _listener.parseMessage(request);
            }
        });
    }

    @Override
    public boolean createRoom(String _room_name) {
        try {
            Chat c = new Chat(_room_name, new HashSet<>(), null);
            c.setUsers(new HashSet<>());
            c.getUsers().add(_dht.peer().peerAddress());
            _dht.put(Number160.createHash(_room_name)).data(new Data(c)).start().awaitUninterruptibly();
            myChatList.add(_room_name);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public String createRoom_(Chat room) {
        try {
            if (myChatList.contains(room.getRoomName())) {
                return "Room already in use";
            }
            String name_room = room.getRoomName();

            Chat exixtingRoom = getChatRoom(name_room);
            if (exixtingRoom == null) {
                boolean res = createRoom(room.getRoomName());
                if (res) {
                    if (room.getEndChat() != null) {
                        exixtingRoom = getChatRoom(room.getRoomName());
                        if (exixtingRoom != null) {
                            exixtingRoom.setEndChat(room.getEndChat());
                            _dht.put(Number160.createHash(exixtingRoom.getRoomName())).data(new Data(exixtingRoom)).start().awaitUninterruptibly();
                            scheduleCancelChat(exixtingRoom);
                        }
                    }
                    return "ok";
                } else {
                    return "ko";
                }
            } else {
                return "Room already in use";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "ko";
    }

    @Override
    public boolean joinRoom(String _room_name) {
        try {
            Chat chat = getChatRoom(_room_name);
            if (chat != null) {
                chat.addAnUser(_dht.peer().peerAddress());
                _dht.put(Number160.createHash(_room_name)).data(new Data(chat)).start().awaitUninterruptibly();
                myChatList.add(_room_name);
                return true;
            }
            return false;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public String joinRoom_(String _room_name) {
        try {

            if (myChatList.contains(_room_name)) {
                return "Already joined in Room";
            } else {
                boolean res = false;
                res = joinRoom(_room_name);
                return (res ? "ok" : "ko");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "ko";
    }

    @Override
    public boolean leaveRoom(String _room_name) {
        try {
            if (myChatList.contains(_room_name)) {
                FutureGet futureGet = _dht.get(Number160.createHash(_room_name)).start();
                futureGet.awaitUninterruptibly();
                if (futureGet.isSuccess()) {
                    if (futureGet.isEmpty()) {
                        return false;
                    }
                    Chat currentChat = (Chat) futureGet.dataMap().values().iterator().next().object();
                    if (currentChat != null) {
                        currentChat.removeAnUser(_dht.peer().peerAddress());
                        _dht.put(Number160.createHash(_room_name)).data(new Data(currentChat)).start().awaitUninterruptibly();
                        myChatList.remove(_room_name);
                        return true;
                    }
                }
            }
            return false;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean sendMessage(String _room_name, String _text_message) {
        try {
            if (myChatList.contains(_room_name)) {// invii mess solo se sei in quella stanza
                FutureGet futureGet = _dht.get(Number160.createHash(_room_name)).start();
                futureGet.awaitUninterruptibly();
                if (futureGet.isSuccess() && !futureGet.isEmpty()) {
                    Chat currentChat = (Chat) futureGet.dataMap().values().iterator().next().object();
                    if (currentChat != null && currentChat.getUsers() != null) {
                        Message msg = new Message();
                        msg.setRoomName(_room_name);
                        msg.setMsg(_text_message);
                        msg.setType(0);
                        msg.setDate(Calendar.getInstance().getTime());

                        for (PeerAddress peerToSend : currentChat.getUsers()) {
                            if (!peerToSend.equals(_dht.peer().peerAddress())) { //Send message only to other peers
                                FutureDirect futureDirect = _dht.peer().sendDirect(peerToSend).object(msg).start();
                                futureDirect.awaitUninterruptibly();
                                //futureDirect.addListener(new CustomFutureDirectAsyncListener(_room_name, peerToSend, msg));
                            }
                        }
                        return true;
                    }
                    return false;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

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
                res = "Room name doesn't exists in your rooms";
            }
        }
        return res;
    }

    public void forwardImageMessage(Message msg, HashSet<PeerAddress> receivers) {
        for (PeerAddress peerToSend : receivers) {
            System.out.println(peerToSend.toString());
            if (!peerToSend.equals(_dht.peer().peerAddress())) { //Send message only to other peers
                FutureDirect futureDirect = _dht.peer().sendDirect(peerToSend).object(msg).start();
                //Async Listener
                futureDirect.addListener(new CustomFutureDirectAsyncListener(msg.getRoomName(), peerToSend, msg));
            }
        }
        System.out.println("\n-----\n");
    }

    public boolean leaveNetwork() {
        for (String schat : new ArrayList<String>(myChatList)) {
            leaveRoom(schat);
        }
        _dht.peer().announceShutdown().start().awaitUninterruptibly();
        return true;
    }

    public HashSet<String> getMyChatList() {
        return myChatList;
    }

    public void scheduleCancelChat(Chat chat) {
        if (chat != null && chat.getEndChat() != null) {
            //timer
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    System.out.println("Run Task for chat room *PEER* " + chat.getRoomName());
                    FutureGet futureGet = _dht.get(Number160.createHash(chat.getRoomName())).start();
                    futureGet.awaitUninterruptibly();
                    if (futureGet.isSuccess()) {
                        _dht.remove(Number160.createHash(chat.getRoomName())).start().awaitUninterruptibly();
                        myChatList.remove(chat.getRoomName());
                    }
                }
            };
            Timer timer = new Timer("timer");
            long diff_sec = UtilDate.differenceDateInSeconds(Calendar.getInstance().getTime(), chat.getEndChat());
            timer.schedule(task, (1000 * diff_sec) + ((peerId + 1) * 1000));
            //timer.schedule(task, (1000 * 15) + (peerId*1000) );
        }

    }

    public Chat getChatRoom(String _room_name) {

        try {
            if (_room_name != null) {
                FutureGet futureGet = _dht.get(Number160.createHash(_room_name)).start();
                futureGet.awaitUninterruptibly();
                if (futureGet.isSuccess()) {
                    if (futureGet.isEmpty()) {
                        return null;
                    }
                    Chat currentChat = (Chat) futureGet.dataMap().values().iterator().next().object();

                    return currentChat;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int getPeerId() {
        return peerId;
    }

    private void prepareAndSendWrapper(PeerAddress snd, MessageWrapper msgWrapper) {
        if (snd != null && msgWrapper != null) {
            HashSet<PeerAddress> receivers = msgWrapper.getReceivers();
            if (receivers == null || receivers.size() == 0) {
                //Send normal msg perchè è l'unico che riceve, non deve fare il forward
                FutureDirect futureDirect = _dht.peer().sendDirect(snd).object(msgWrapper.getMsg()).start();
                futureDirect.awaitUninterruptibly();
            } else {
                FutureDirect futureDirect = _dht.peer().sendDirect(snd).object(msgWrapper).start();
                futureDirect.awaitUninterruptibly();
                if (futureDirect.isFailed()) {
                    PeerAddress nextSender = receivers.iterator().next();
                    //Switch old_sender con new_sender . Il check se non è + presente in chat viene fatto nell'invio.
                    receivers.remove(nextSender);
                    receivers.add(snd);
                    prepareAndSendWrapper(nextSender, msgWrapper);
                }
            }
        }

    }

    private boolean sendImage(String _room_name, Message msg) {
        
        Chat currentChat = getChatRoom(_room_name);
        msg.setDate(Calendar.getInstance().getTime());
        HashSet<PeerAddress> peers = new HashSet<>(currentChat.getUsers());
        MessageWrapper msgWrapper = new MessageWrapper();
        msgWrapper.setType(msg.getType());
        msgWrapper.setMsg(msg);

        peers.remove(_dht.peer().peerAddress());
        int n_msg = peers.size();
        if (n_msg > 3) {
            int msg_each_peer = (int) (Math.log(n_msg) / Math.log(2));
            int senders = n_msg / msg_each_peer;

            Iterator<PeerAddress> it = peers.iterator();
            System.out.println("senders : " + senders);
            for (int i = 0, j = 0; i < senders && it.hasNext(); ++i) {
                int rest = n_msg - (msg_each_peer * senders);
                int m = msg_each_peer - 1;//master
                if (rest > 0 && i < rest) {
                    ++m;
                }

                PeerAddress snd = it.next();
                if (snd.equals(_dht.peer().peerAddress())) {
                    snd = it.next();
                }
                msgWrapper.setReceivers(new HashSet<>());
                for (int x = 0; x < m && it.hasNext(); ++x) {
                    msgWrapper.getReceivers().add(it.next());
                }

                System.out.println("node " + i + " sends " + m + " msg");
                prepareAndSendWrapper(snd, msgWrapper);
                return true;
            }
        } else {
            for (PeerAddress peerToSend : peers) {
                if (!peerToSend.equals(_dht.peer().peerAddress())) { //Send message only to other peers
                    FutureDirect futureDirect = _dht.peer().sendDirect(peerToSend).object(msg).start();
                    futureDirect.addListener(new CustomFutureDirectAsyncListener(_room_name, peerToSend, msg));
                }
            }
            return true;
        }
        return false;

    }

    /*
    listener, which gets called whenever a result is ready. It is preferred to use this second option and avoid blocking, because in the worst case, you might cause a deadlock if await() is called from a wrong (I/O) thread. If such a listener is used, then the listeners gets called in all cases. If no peer replies, the timeout handler triggers the listener.
     */
    class CustomFutureDirectAsyncListener extends BaseFutureAdapter<FutureDirect> {

        String _room_name;
        PeerAddress receiver;
        Message msg;

        CustomFutureDirectAsyncListener(String _room_name, PeerAddress receiver, Message msg) {
            this._room_name = _room_name;
            this.receiver = receiver;
            this.msg = msg;
        }

        @Override
        public void operationComplete(FutureDirect future) throws Exception {
            if (future.isSuccess()) { // this flag indicates if the future was successful
                System.out.println("success");
            } else {
                if (future.isFailed()) {
                    System.out.println("failure");
                    logger.error("Future not successful. Reason = " + future.failedReason());
                    future = _dht.peer().sendDirect(receiver).object(msg).start();
                    future.awaitUninterruptibly();

                    if (future.isFailed()) {
                        Chat currentChat = getChatRoom(msg.getRoomName());
                        currentChat.removeAnUser(receiver);
                        _dht.put(Number160.createHash(msg.getRoomName())).data(new Data(currentChat)).start().awaitUninterruptibly();
                    }
                }

            }
        }

    }

}

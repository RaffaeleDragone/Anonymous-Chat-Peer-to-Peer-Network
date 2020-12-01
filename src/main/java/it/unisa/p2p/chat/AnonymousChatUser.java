package it.unisa.p2p.chat;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;

import it.unisa.p2p.beans.Chat;
import it.unisa.p2p.beans.Message;
import it.unisa.p2p.interfaces.AnonymousChat;
import it.unisa.p2p.interfaces.MessageListener;
import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
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
    final private int DEFAULT_MASTER_PORT=4000;
    private HashSet<String> myChatList=new HashSet<>();
    
    private static Logger logger= Logger.getLogger(AnonymousChatUser.class);


    public AnonymousChatUser(int _id, String _master_peer, final MessageListener _listener) throws Exception {
        peer= new PeerBuilder(Number160.createHash(_id)).ports(DEFAULT_MASTER_PORT+_id).start();
        _dht = new PeerBuilderDHT(peer).start();

        FutureBootstrap fb = peer.bootstrap().inetAddress(InetAddress.getByName(_master_peer)).ports(DEFAULT_MASTER_PORT).start();
        fb.awaitUninterruptibly();
        if(fb.isSuccess()) {
            peer.discover().peerAddress(fb.bootstrapTo().iterator().next()).start().awaitUninterruptibly();
        }else {
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
            FutureGet futureGet = _dht.get(Number160.createHash(_room_name)).start();
            futureGet.awaitUninterruptibly();
            if (futureGet.isSuccess() && futureGet.isEmpty()){
                Chat c = new Chat(_room_name,new HashSet<>());
                c.setUsers(new HashSet<>());
                c.getUsers().add(_dht.peer().peerAddress());
                _dht.put(Number160.createHash(_room_name)).data(new Data(c)).start().awaitUninterruptibly();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean joinRoom(String _room_name) {
        try {
            FutureGet futureGet = _dht.get(Number160.createHash(_room_name)).start();
            futureGet.awaitUninterruptibly();
            if (futureGet.isSuccess()) {
                if(futureGet.isEmpty() ) return false;
                Chat currentChat= (Chat) futureGet.dataMap().values().iterator().next().object();
                if(currentChat!=null){
                    currentChat.addAnUser(_dht.peer().peerAddress());
                    _dht.put(Number160.createHash(_room_name)).data(new Data(currentChat)).start().awaitUninterruptibly();
                    myChatList.add(_room_name);
                    return true;
                }
                return false;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean leaveRoom(String _room_name) {
        try {
            FutureGet futureGet = _dht.get(Number160.createHash(_room_name)).start();
            futureGet.awaitUninterruptibly();
            if (futureGet.isSuccess()) {
                if(futureGet.isEmpty() ) return false;
                Chat currentChat = (Chat) futureGet.dataMap().values().iterator().next().object();
                if(currentChat!=null){
                    currentChat.removeAnUser(_dht.peer().peerAddress());
                    _dht.put(Number160.createHash(_room_name)).data(new Data(currentChat)).start().awaitUninterruptibly();
                    myChatList.remove(_room_name);
                    return true;
                }
                return false;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean sendMessage(String _room_name, String _text_message) {
        try {
            if(myChatList.contains(_room_name)){// invii mess solo se sei in quella stanza
                FutureGet futureGet = _dht.get(Number160.createHash(_room_name)).start();
                futureGet.awaitUninterruptibly();
                if (futureGet.isSuccess() && !futureGet.isEmpty()) {
                    Chat currentChat = (Chat) futureGet.dataMap().values().iterator().next().object();
                    if(currentChat!=null && currentChat.getUsers()!=null){
                        Message msg=new Message();
                        msg.setRoomName(_room_name);
                        msg.setMsg(_text_message);
                        for(PeerAddress peer: currentChat.getUsers())
                        {
                            if(!peer.equals(_dht.peer().peerAddress())){ //Send message only to other peers
                                FutureDirect futureDirect = _dht.peer().sendDirect(peer).object(msg).start();
                                futureDirect.awaitUninterruptibly();
                                if(futureDirect.isFailed()){
                                    logger.error("Future not successful. Reason = "+ futureDirect.failedReason());
                                    System.out.println("Future not successful. Reason = "+ futureDirect.failedReason());
                                }
                            }
                        }
                        return true;
                    }
                    return false;
                }
            }
            return false;
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public String sendMessage(String _room_name, Message msg) {
        String res="";
        if(msg!=null && msg.getRoomName()!=null){
            if(myChatList.contains(msg.getRoomName())){
                if(sendMessage(_room_name, msg.getMsg()))
                    return "ok";
                else
                    return "ko";
            }else{
                res="Room name doesn't exists in your rooms";
            }
        }
        return null;
    }

    public boolean leaveNetwork() {
        for(String schat: new ArrayList<String>(myChatList)) leaveRoom(schat);
        _dht.peer().announceShutdown().start().awaitUninterruptibly();
        return true;
    }
    
    public boolean leaveNetworkWithoutNotify() {
        _dht.peer().announceShutdown().start().awaitUninterruptibly();
        return true;
    }

    public HashSet<String> getMyChatList() {
        return myChatList;
    }
    
    
}

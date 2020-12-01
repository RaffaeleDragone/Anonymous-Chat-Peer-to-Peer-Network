package it.unisa.p2p.chat;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;

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

public class AnonymousChatImplOld implements AnonymousChat {

    final private Peer peer;
    final private PeerDHT _dht;
    final private int DEFAULT_MASTER_PORT=4000;

    final private ArrayList<String> s_rooms=new ArrayList<String>();

    public AnonymousChatImplOld(int _id, String _master_peer, final MessageListener _listener) throws Exception {
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
                HashSet<PeerAddress> hs = new HashSet<>();
                hs.add(_dht.peer().peerAddress());
                _dht.put(Number160.createHash(_room_name)).data(new Data(hs)).start().awaitUninterruptibly();
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
                HashSet<PeerAddress> peers_on_topic;
                peers_on_topic = (HashSet<PeerAddress>) futureGet.dataMap().values().iterator().next().object();
                peers_on_topic.add(_dht.peer().peerAddress());
                _dht.put(Number160.createHash(_room_name)).data(new Data(peers_on_topic)).start().awaitUninterruptibly();
                s_rooms.add(_room_name);
                return true;
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
                HashSet<PeerAddress> peers_on_topic;
                peers_on_topic = (HashSet<PeerAddress>) futureGet.dataMap().values().iterator().next().object();
                peers_on_topic.remove(_dht.peer().peerAddress());
                _dht.put(Number160.createHash(_room_name)).data(new Data(peers_on_topic)).start().awaitUninterruptibly();
                s_rooms.remove(_room_name);
                return true;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean sendMessage(String _room_name, String _text_message) {
        try {
            FutureGet futureGet = _dht.get(Number160.createHash(_room_name)).start();
            futureGet.awaitUninterruptibly();
            if (futureGet.isSuccess() && !futureGet.isEmpty()) {
                HashSet<PeerAddress> peers_on_topic;
                peers_on_topic = (HashSet<PeerAddress>) futureGet.dataMap().values().iterator().next().object();
                for(PeerAddress peer:peers_on_topic)
                {
                    if(!peer.equals(_dht.peer().peerAddress())){ //Send message only to other peers
                        FutureDirect futureDirect = _dht.peer().sendDirect(peer).object(_text_message).start();
                        futureDirect.awaitUninterruptibly();
                    }

                }

                return true;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean leaveNetwork() {

        for(String topic: new ArrayList<String>(s_rooms)) leaveRoom(topic);
        _dht.peer().announceShutdown().start().awaitUninterruptibly();
        return true;
    }
}

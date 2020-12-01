package it.unisa.p2p.beans;

import net.tomp2p.p2p.Peer;
import net.tomp2p.peers.PeerAddress;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;

public class Chat implements Serializable {
    private String roomName;
    private HashSet<PeerAddress> users;

    public Chat(){
    }

    public Chat(String roomName,HashSet<PeerAddress> users){
        this.roomName=roomName;
        this.users=users;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public HashSet<PeerAddress> getUsers() {
        return users;
    }

    public void setUsers(HashSet<PeerAddress> users) {
        this.users = users;
    }

    public void removeAnUser(PeerAddress user){
        if(this.users!=null)
            this.users.remove(user);
    }

    public void addAnUser(PeerAddress user){
        if(this.users==null)
            this.users=new HashSet<>();
        this.users.add(user);
    }


}

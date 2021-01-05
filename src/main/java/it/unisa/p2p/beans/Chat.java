package it.unisa.p2p.beans;

import net.tomp2p.peers.PeerAddress;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;

public class Chat implements Serializable {
    private String roomName;
    private HashSet<PeerAddress> users;
    private Date endChat;

    public Chat(){
    }

    public Chat(String roomName,HashSet<PeerAddress> users,Date endChat){
        this.roomName=roomName;
        this.users=users;
        this.endChat=endChat;
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

    public Date getEndChat() {
        return endChat;
    }

    public void setEndChat(Date endChat) {
        this.endChat = endChat;
    }


}

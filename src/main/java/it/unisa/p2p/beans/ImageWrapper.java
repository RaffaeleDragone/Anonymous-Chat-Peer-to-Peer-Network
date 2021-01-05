/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unisa.p2p.beans;

import java.io.Serializable;
import java.util.HashSet;
import net.tomp2p.peers.PeerAddress;

/**
 *
 * @author raffaeledragone
 */
public class ImageWrapper implements Serializable{
    Message msg;
    private HashSet<PeerAddress> receivers;
    
    public Message getMsg() {
        return msg;
    }

    public void setMsg(Message msg) {
        this.msg = msg;
    }

    public HashSet<PeerAddress> getReceivers() {
        return receivers;
    }

    public void setReceivers(HashSet<PeerAddress> receivers) {
        this.receivers = receivers;
    }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unisa.p2p.beans;

import java.io.Serializable;

/**
 *
 * @author raffaeledragone
 */
public class Message implements Serializable{
    int type;
    String msg;
    String infoMsg;
    String roomName;

    public Message() {
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getInfoMsg() {
        return infoMsg;
    }

    public void setInfoMsg(String infoMsg) {
        this.infoMsg = infoMsg;
    }

    @Override
    public String toString() {
        return "Message{" + "type=" + type + ", msg=" + msg + ", infoMsg=" + infoMsg + '}';
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }
    
    
    
    
    
}

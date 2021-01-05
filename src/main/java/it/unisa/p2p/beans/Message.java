/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unisa.p2p.beans;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author raffaeledragone
 */
public class Message implements Serializable{
    int type;
    String msg;
    String roomName;
    Date date;
    byte[] image;
    String name_file;

    public Message() {
    }

    public Message(int type, String msg, String roomName, Date date, byte[] image, String name_file) {
        this.type = type;
        this.msg = msg;
        this.roomName = roomName;
        this.date = date;
        this.image = image;
        this.name_file = name_file;
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

    @Override
    public String toString() {
        return msg;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getName_file() {
        return name_file;
    }

    public void setName_file(String name_file) {
        this.name_file = name_file;
    }

}

package it.unisa.p2p.interfaces;

import it.unisa.p2p.beans.Message;


public interface MessageListener {
    public Object parseMessage(Object obj);
}


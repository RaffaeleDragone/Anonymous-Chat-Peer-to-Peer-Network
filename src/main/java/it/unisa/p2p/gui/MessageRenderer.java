/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unisa.p2p.gui;

import it.unisa.p2p.beans.Message;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

/**
 *
 * @author raffaeledragone
 */
public class MessageRenderer extends JLabel implements ListCellRenderer<Message> {

    public MessageRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Message> list, Message message, int index,
            boolean isSelected, boolean cellHasFocus) {

        if(message!=null){
            
            boolean myMsg= message.getMsg()!=null && message.getMsg().startsWith("mymsg_") ? true : false;
            if(message.getType()==1){
                setText("<html> <div style='text-align: right;'> <b> ["+message.getDate().getHours()+":"+message.getDate().getMinutes()+"] </b> </div> \n");
                ImageIcon imageIcon = new ImageIcon(message.getImage());
                setIcon(imageIcon);
                setHorizontalTextPosition(JLabel.LEFT);
                setVerticalTextPosition(JLabel.TOP);
            }else{
                String msg = message.getMsg().replaceAll("mymsg_", "");
                setIcon(null);
                setText("<html> <div style='text-align: right;'> <b> ["+message.getDate().getHours()+":"+message.getDate().getMinutes()+"] </b> </div> \n \n <div style='text-align: left;'>"+msg+"</div>");
            }
            
            if(myMsg)
                setHorizontalAlignment(SwingConstants.RIGHT);
            else
                setHorizontalAlignment(SwingConstants.LEFT);
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
        }
        

        return this;
    }
}

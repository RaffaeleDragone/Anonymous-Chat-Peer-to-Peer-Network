package it.unisa.p2p.chat;

import it.unisa.p2p.interfaces.MessageListener;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class StartChat {
    @Option(name="-m", aliases="--masterip", usage="the master peer ip address", required=true)
    private static String master;

    @Option(name="-id", aliases="--identifierpeer", usage="the unique identifier for this peer", required=true)
    private static int id;

    public static void main(String[] args) throws Exception {

        class MessageListenerImpl implements MessageListener {
            int peerid;

            public MessageListenerImpl(int peerid)
            {
                this.peerid=peerid;

            }
            public Object parseMessage(Object obj) {

                TextIO textIO = TextIoFactory.getTextIO();
                TextTerminal terminal = textIO.getTextTerminal();
                terminal.printf("\n"+peerid+"] (Direct Message Received) "+obj+"\n\n");
                
                return "success";
            }

        }
        StartChat example = new StartChat();
        final CmdLineParser parser = new CmdLineParser(example);
        try
        {
            parser.parseArgument(args);
            TextIO textIO = TextIoFactory.getTextIO();
            TextTerminal terminal = textIO.getTextTerminal();
            AnonymousChatUser peer =
                    new AnonymousChatUser(id, master, new MessageListenerImpl(id));

            terminal.printf("\nStaring peer id: %d on master node: %s\n",
                    id, master);
            while(true) {
                printMenu(terminal);

                int option = textIO.newIntInputReader()
                        .withMaxVal(5)
                        .withMinVal(1)
                        .read("Option");
                switch (option) {
                    case 1:
                        terminal.printf("\nENTER ROOM NAME\n");
                        String name = textIO.newStringInputReader()
                                .withDefaultValue("default-room")
                                .read("Name:");
                        if(peer.createRoom(name))
                            terminal.printf("\nROOM %s SUCCESSFULLY CREATED\n",name);
                        else
                            terminal.printf("\nERROR IN ROOM CREATION\n");
                        break;
                    case 2:
                        terminal.printf("\nENTER ROOM NAME\n");
                        String sname = textIO.newStringInputReader()
                                .withDefaultValue("default-room")
                                .read("Name:");
                        if(peer.joinRoom(sname))
                            terminal.printf("\n SUCCESSFULLY JOINED TO %s\n",sname);
                        else
                            terminal.printf("\nERROR IN ROOM JOIN\n");
                        break;
                    case 4:
                        terminal.printf("\nENTER ROOM NAME\n");
                        String tname = textIO.newStringInputReader()
                                .withDefaultValue("default-room")
                                .read(" Name:");
                        terminal.printf("\nENTER MESSAGE\n");
                        String message = textIO.newStringInputReader()
                                .withDefaultValue("default-message")
                                .read(" Message:");
                        if(peer.sendMessage(tname,message))
                            terminal.printf("\n SUCCESSFULLY PUBLISH MESSAGE ON TOPIC %s\n",tname);
                        else
                            terminal.printf("\nERROR IN TOPIC PUBLISH\n");

                        break;
                    case 3:
                        terminal.printf("\nENTER ROOM NAME\n");
                        String uname = textIO.newStringInputReader()
                                .withDefaultValue("default-room")
                                .read("Name:");
                        if(peer.leaveRoom(uname))
                            terminal.printf("\n SUCCESSFULLY left from %s\n",uname);
                        else
                            terminal.printf("\nERROR IN ROOM Leave \n");
                        break;
                    case 5:
                        terminal.printf("\nARE YOU SURE TO LEAVE THE NETWORK?\n");
                        boolean exit = textIO.newBooleanInputReader().withDefaultValue(false).read("exit?");
                        if(exit) {
                            peer.leaveNetwork();
                            System.exit(0);
                        }
                        break;

                    default:
                        break;
                }
            }



        }
        catch (CmdLineException clEx)
        {
            System.err.println("ERROR: Unable to parse command-line options: " + clEx);
        }


    }
    public static void printMenu(TextTerminal terminal) {
        terminal.printf("\n1 - CREATE ROOM\n");
        terminal.printf("\n2 - JOIN ROOM\n");
        terminal.printf("\n3 - LEAVE ROOM\n");
        terminal.printf("\n4 - SEND MESSAGE\n");
        terminal.printf("\n5 - EXIT\n");

    }
}

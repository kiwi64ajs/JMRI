package jmri.jmrix.roco.z21.simulator;

import jmri.jmrix.roco.z21.Z21Adapter;
import jmri.jmrix.roco.z21.Z21Message;
import jmri.jmrix.roco.z21.Z21Reply;
import jmri.jmrix.roco.z21.Z21TrafficController;
import jmri.jmrix.lenz.XNetMessage;
import jmri.jmrix.lenz.XNetReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


/**
 * Provide access to a simulated z21 system.
 *
 * Currently, the z21Simulator reacts to commands sent from the user interface
 * with messages an appropriate reply message.
 *
 * NOTE: Some material in this file was modified from other portions of the
 * support infrastructure.
 *
 * @author	Paul Bender, Copyright (C) 2015
 */
public class Z21SimulatorAdapter extends Z21Adapter implements Runnable {

    private Thread sourceThread;
    private Z21XNetSimulatorAdapter xnetadapter = null;

    public Z21SimulatorAdapter() {
        super();
        setHostName("localhost");
        // start a UDP server that we can connect to.  The server will
        // produce the appropriate responses.
        xnetadapter = new Z21XNetSimulatorAdapter();
    }

    /**
     * set up all of the other objects to operate with a z21Simulator connected
     * to this port
     */
    @Override
    public void configure() {
        if (log.isDebugEnabled()) {
            log.debug("configure called");
        }
        // connect to a packetizing traffic controller
        Z21TrafficController packets = new Z21TrafficController();
        packets.connectPort(this);

        // start operation
        // packets.startThreads();
        this.getSystemConnectionMemo().setTrafficController(packets);

        sourceThread = new Thread(this);
        sourceThread.start();

        this.getSystemConnectionMemo().configureManagers();

        jmri.jmrix.roco.z21.ActiveFlag.setActive();
    }

    @Override
    public void connect() throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("connect called");
        }

       setHostAddress("localhost"); // always localhost for the simulation.
       super.connect();
    }

    public void run() {
        // The server just opens a DatagramSocket using the specified port number,
        // and then goes into an infinite loop.

        // try connecting to the server
        try (DatagramSocket s = new DatagramSocket(COMMUNICATION_UDP_PORT)) {

            log.debug("socket created, starting loop");
            while(true){
                log.debug("simulation loop");
                // the server waits for a client to connect, then echos the data sent back.
                byte[] input=new byte[100]; // input from network
                try {

                    // to receive the data, we create a packet.
                    DatagramPacket receivePacket = new DatagramPacket(input,100);
                    // and wait for the data to arrive.
                    s.receive(receivePacket);

                    Z21Message msg = new Z21Message(receivePacket.getLength());
                    for(int i=0;i< receivePacket.getLength();i++)
                        msg.setElement(i,receivePacket.getData()[i]);

                    // to echo the data back, we need to find the IP and port to send
                    // the data to.
                    InetAddress IPAddress = receivePacket.getAddress();
                    int port = receivePacket.getPort();

                    log.debug("Received packet: {}, message: {}",receivePacket.getData(),msg);

                    Z21Reply reply;
                    // and then we create the return packet.
                    try {
                        reply = generateReply(msg);
                    } catch (java.lang.Exception e) {
                        // the simulation ends here. break out of the loop.
                        log.debug("error generated by generateReply, exiting simulation");
                        break;
                    }
                    byte ba[] = jmri.util.StringUtil.bytesFromHexString(reply.toString());
                    DatagramPacket sendPacket = new DatagramPacket(ba,ba.length,IPAddress,port);

                    // and send it back using our socket
                    s.send(sendPacket);

                } catch (Exception ex3) {
                    log.debug("IO Exception" );
                    ex3.printStackTrace();
                }
                log.debug("Client Disconnect");
            }
        } catch (Exception ex0 ) {
            log.error("Exception opening socket", ex0);
            return; // can't continue from this
        }
    } // end of run.

    // generateReply is the heart of the simulation.  It translates an
    // incoming XNetMessage into an outgoing XNetReply.
    @SuppressWarnings("fallthrough") // document values for specific cases
    private Z21Reply generateReply(Z21Message m) throws Exception {
        log.debug("generate Reply called with message {}",m);
        Z21Reply reply;
        switch (m.getOpCode()) {
             case 0x0010:
                // request for serial number
                reply = getZ21SerialNumberReply();
                break;
             case 0x001a:
             // request for hardware version info.
                reply=getHardwareVersionReply();
                break;
             case 0x0040:
                // XPressNet tunnel message.
                XNetMessage xnm = getXNetMessage(m);
                log.debug("Received XNet Message: " + m);
                XNetReply xnr=xnetadapter.generateReply(xnm);
                reply = getZ21ReplyFromXNet(xnr);
                break;
             case 0x0030:
                // LAN LOGOFF
                // this is the end of the simulation, throw an exception
                // to indicate this.
                throw(new Exception());
             case 0x0050:
                // set broadcast flags
             case 0x0051:
                // get broadcast flags
             case 0x0060:
                // get loco mode
             case 0x0061:
                // set loco mode
             case 0x0070:
                // get turnout mode
             case 0x0071:
                // set turnout mode
             case 0x0081:
                // get RMBus data
             case 0x0082:
                // program RMBus module
             case 0x0085:
                // get system state
             case 0x0089:
                  // Get Railcom Data
             case 0x00A2:
                  // loconet data from lan
             case 0x00A3:
                  // loconet dispatch address
             case 0x00A4:
                  // get loconet detector status
             default:
                reply=getXPressNetUnknownCommandReply();
        }
        return reply;
    }

    // canned reply messages;
    private Z21Reply getHardwareVersionReply(){
        Z21Reply reply = new Z21Reply();
        reply.setLength(0x000c);
        reply.setOpCode(0x001a);
        reply.setElement(4,0x00);
        reply.setElement(5,0x02);
        reply.setElement(6,0x00);
        reply.setElement(7,0x00);
        reply.setElement(8,0x20);
        reply.setElement(9,0x01);
        reply.setElement(10,0x00);
        reply.setElement(11,0x00);
        return reply;
    }

    private Z21Reply getXPressNetUnknownCommandReply(){
        Z21Reply reply = new Z21Reply();
        reply.setLength(0x0007);
        reply.setOpCode(0x0040);
        reply.setElement(4,0x61);
        reply.setElement(5,0x82);
        reply.setElement(6,0xE3);
        return reply;
    }

    private Z21Reply getZ21SerialNumberReply(){
        Z21Reply reply = new Z21Reply();
        reply.setLength(0x0004);
        reply.setOpCode(0x0010);
        reply.setElement(4,0x00);
        reply.setElement(5,0x00);
        reply.setElement(6,0x00);
        reply.setElement(7,0x00);
        return reply;
    }

    // utility functions
    private XNetMessage getXNetMessage(Z21Message m) {
        if(m==null) throw new java.lang.IllegalArgumentException();
        XNetMessage xnm = new XNetMessage(m.getLength()-4);
        for (int i = 4; i < m.getLength(); i++) {
           xnm.setElement(i - 4, m.getElement(i));
        }
        return xnm;
    }

    private Z21Reply getZ21ReplyFromXNet(XNetReply m) {
        if(m==null) throw new java.lang.IllegalArgumentException();
        Z21Reply r=new Z21Reply();
        r.setLength(m.getNumDataElements() + 4);
        r.setOpCode(0x0040);
        for (int i = 0; i < m.getNumDataElements(); i++) {
            r.setElement(i + 4, m.getElement(i));
        }
        return(r);
    }


    private final static Logger log = LoggerFactory.getLogger(Z21SimulatorAdapter.class.getName());

}

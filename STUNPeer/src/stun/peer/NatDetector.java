package stun.peer;

import stun.STUNException;
import de.javawi.jstun.attribute.*;
import de.javawi.jstun.header.MessageHeader;
import de.javawi.jstun.header.MessageHeaderInterface;
import de.javawi.jstun.header.MessageHeaderParsingException;
import de.javawi.jstun.util.UtilityException;

import java.io.IOException;
import java.net.*;

/*
 * 和STUN服务器交互的类
 */
public class NatDetector {
    private InetAddress localAddr;
    private InetSocketAddress serverAddr;

    public NatDetector(InetAddress localAddr, InetSocketAddress serverAddr) {
        this.localAddr = localAddr;
        this.serverAddr = serverAddr;
    }

    public boolean isOpenAccess() throws IOException, STUNException {
        DatagramSocket socket = new DatagramSocket(0, localAddr);
        InetSocketAddress wan = getWanAddress(socket);
        InetSocketAddress local = (InetSocketAddress) socket.getLocalSocketAddress();
        socket.close();
        return wan.equals(local);
    }

    public boolean isCone() throws IOException, STUNException {
        DatagramSocket socket = new DatagramSocket(0, localAddr);

        try {
            ChangeRequest cr = new ChangeRequest();
            // get wan addr from server address
            MessageHeader sendMH1 = new MessageHeader(MessageHeaderInterface.MessageHeaderType.BindingRequest);
            sendMH1.generateTransactionID();
            sendMH1.addMessageAttribute(cr);
            MessageHeader rcvdMH1 = sendNReceive(socket, serverAddr, sendMH1);
            if (rcvdMH1 == null) throw new STUNException("STUN.Server not response");
            MappedAddress ma1 = (MappedAddress) rcvdMH1.getMessageAttribute(MessageAttributeInterface.MessageAttributeType.MappedAddress);
            InetSocketAddress wan1 = new InetSocketAddress(ma1.getAddress().getInetAddress(), ma1.getPort());

            // get wan addr from changed address
            ChangedAddress ca = (ChangedAddress) rcvdMH1.getMessageAttribute(MessageAttributeInterface.MessageAttributeType.ChangedAddress);
            InetSocketAddress changedAddr = new InetSocketAddress(ca.getAddress().getInetAddress(), ca.getPort());
            MessageHeader sendMH2 = new MessageHeader(MessageHeaderInterface.MessageHeaderType.BindingRequest);
            sendMH2.generateTransactionID();
            sendMH2.addMessageAttribute(cr);
            MessageHeader rcvdMH2 = sendNReceive(socket, changedAddr, sendMH2);
            if (rcvdMH2 == null) throw new STUNException("STUN.Server not response");
            MappedAddress ma2 = (MappedAddress) rcvdMH2.getMessageAttribute(MessageAttributeInterface.MessageAttributeType.MappedAddress);
            InetSocketAddress wan2 = new InetSocketAddress(ma2.getAddress().getInetAddress(), ma2.getPort());

            return wan1.equals(wan2);
        } catch (UtilityException e) {
            throw new STUNException("Send Failed", e);
        }
    }

    public boolean isSymmetric(boolean isOpenAccess) throws IOException, STUNException {
        if (isOpenAccess) {
            try (DatagramSocket socket = new DatagramSocket(0, localAddr)) {

                MessageHeader sendMH = new MessageHeader(MessageHeader.MessageHeaderType.BindingRequest);
                sendMH.generateTransactionID();
                ChangeRequest cr = new ChangeRequest();
                cr.setChangeIP();
                cr.setChangePort();
                sendMH.addMessageAttribute(cr);

                return sendNReceive(socket, serverAddr, sendMH) == null;
            } catch (UtilityException e) {
                throw new STUNException("Send Failed", e);
            }
        } else {

            try (DatagramSocket socket1 = new DatagramSocket(0, localAddr);
                 DatagramSocket socket2 = new DatagramSocket(0, localAddr)) {
                return getWanAddress(socket1).equals(getWanAddress(socket2));
            }
        }
    }

    public InetSocketAddress getWanAddress(DatagramSocket socket) throws IOException, STUNException {
        try {
            MessageHeader sendMH = new MessageHeader(MessageHeader.MessageHeaderType.BindingRequest);
            sendMH.generateTransactionID();
            sendMH.addMessageAttribute(new ChangeRequest());

            MessageHeader rcvdMH = sendNReceive(socket, serverAddr, sendMH);
            if (rcvdMH == null) throw new STUNException("STUN Server not response");

            MappedAddress ma = (MappedAddress) rcvdMH.getMessageAttribute(MessageAttribute.MessageAttributeType.MappedAddress);
            return new InetSocketAddress(ma.getAddress().getInetAddress(), ma.getPort());
        } catch (UtilityException e) {
            throw new STUNException("Send Failed", e);
        }
    }

    public boolean isRefuse() throws IOException, STUNException {
        try (DatagramSocket socket = new DatagramSocket(0, localAddr)) {
            // get changed address and wan addr to server addr
            MessageHeader sendMH1 = new MessageHeader(MessageHeaderInterface.MessageHeaderType.BindingRequest);
            sendMH1.generateTransactionID();
            sendMH1.addMessageAttribute(new ChangeRequest());
            MessageHeader rcvdMH1 = sendNReceive(socket, serverAddr, sendMH1);
            if (rcvdMH1 == null) throw new STUNException("STUN.Server not response");
            MappedAddress ma1 = (MappedAddress) rcvdMH1.getMessageAttribute(MessageAttributeInterface.MessageAttributeType.MappedAddress);
            ChangedAddress ca = (ChangedAddress) rcvdMH1.getMessageAttribute(MessageAttributeInterface.MessageAttributeType.ChangedAddress);
            InetSocketAddress changedAddr = new InetSocketAddress(ca.getAddress().getInetAddress(), ca.getPort());

            // try to get response without activating mapped address
            MessageHeader sendMH2 = new MessageHeader(MessageHeaderInterface.MessageHeaderType.BindingRequest);
            sendMH2.generateTransactionID();
            ChangeRequest cr = new ChangeRequest();
            cr.setChangeIP();
            cr.setChangePort();
            sendMH2.addMessageAttribute(cr);
            if (sendNReceive(socket, serverAddr, sendMH2) != null)
                return false;

            // get wan addr from changed addr
            MessageHeader sendMH3 = new MessageHeader(MessageHeaderInterface.MessageHeaderType.BindingRequest);
            sendMH3.generateTransactionID();
            sendMH3.addMessageAttribute(new ChangeRequest());
            MessageHeader rcvdMH3 = sendNReceive(socket, changedAddr, sendMH3);
            if (rcvdMH3 == null) throw new STUNException("STUN.Server not response");
            MappedAddress ma2 = (MappedAddress) rcvdMH3.getMessageAttribute(MessageAttributeInterface.MessageAttributeType.MappedAddress);

            // judge whether wan refused changedAddr
            return !(ma1.getAddress().equals(ma2.getAddress()) && ma1.getPort() == ma2.getPort());
        } catch (UtilityException e) {
            throw new STUNException("Send failed", e);
        }
    }

    private MessageHeader sendNReceive(DatagramSocket socket, InetSocketAddress target, MessageHeader sendMH)
            throws STUNException, IOException {
        DatagramPacket send;
        try {
            byte[] data = sendMH.getBytes();
            send = new DatagramPacket(data, data.length);
            send.setSocketAddress(target);
        } catch (UtilityException e) {
            throw new STUNException("Send Failed", e);
        }

        socket.setSoTimeout(500);
        long overTime = System.currentTimeMillis() + 10000;
        while (true) {
            try {
                socket.send(send);
                DatagramPacket receive = new DatagramPacket(new byte[200], 200);
                socket.receive(receive);

                MessageHeader rcvdMH = MessageHeader.parseHeader(receive.getData());
                rcvdMH.parseAttributes(receive.getData());

                if (rcvdMH.getMessageAttribute(MessageAttributeInterface.MessageAttributeType.ErrorCode) == null &&
                        rcvdMH.equalTransactionID(sendMH))
                    return rcvdMH;
            } catch (IOException | MessageHeaderParsingException | MessageAttributeParsingException ignored) {
            }

            if (System.currentTimeMillis() >= overTime) {
                break;
            }
        }
        return null;
    }
}

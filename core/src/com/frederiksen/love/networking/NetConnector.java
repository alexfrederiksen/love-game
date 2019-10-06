package com.frederiksen.love.networking;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryonet.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class NetConnector {

    public static class DiscoveryPacket {

        public static void register(Kryo kryo) {
            kryo.register(DiscoveryPacket.class);
        }

        public String name;
        public int tcpPort;

        public DiscoveryPacket() {

        }

        public DiscoveryPacket(String name, int tcpPort) {
            this.name = name;
            this.tcpPort = tcpPort;
        }
    }

    public static class ServerConnector implements ServerDiscoveryHandler {

        private DiscoveryPacket packet;

        public ServerConnector(Server server, DiscoveryPacket packet) {
            this.packet = packet;
            server.setDiscoveryHandler(this);
        }

        @Override
        public boolean onDiscoverHost(DatagramChannel datagramChannel, InetSocketAddress fromAddress, Serialization serialization) throws IOException {
            ByteBuffer buffer = ByteBuffer.allocate(256);
            serialization.write(null, buffer, packet);
            buffer.flip();
            datagramChannel.send(buffer, fromAddress);

            return true;
        }
    }

    public static class ClientConnector implements ClientDiscoveryHandler {
        private Input input = null;
        private DiscoveryPacket packet = null;
        private Client client = new Client();

        public ClientConnector() {
            Network.registerPackets(client.getKryo());
            client.setDiscoveryHandler(this);
        }

        public InetAddress findHost(int port, int timeout) {
            return client.discoverHost(port, timeout);
        }

        @Override
        public DatagramPacket onRequestNewDatagramPacket() {
            byte[] buffer = new byte[1024];
            input = new Input(buffer);

            return new DatagramPacket(buffer, buffer.length);
        }

        @Override
        public void onDiscoveredHost(DatagramPacket datagramPacket, Kryo kryo) {
            if (input != null) {
                packet = (DiscoveryPacket) kryo.readClassAndObject(input);
            }
        }

        @Override
        public void onFinally() {
            if (input != null)
                input.close();
        }

        public DiscoveryPacket getPacket() {
            return packet;
        }
    }
}

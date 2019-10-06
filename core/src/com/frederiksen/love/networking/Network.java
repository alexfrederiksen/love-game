package com.frederiksen.love.networking;

import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import com.frederiksen.love.gameobjs.GameObj;
import com.frederiksen.love.gameobjs.ObjFactory;
import com.frederiksen.love.gameobjs.Player;
import com.frederiksen.love.gameobjs.controllers.Controller;
import com.frederiksen.love.gameobjs.controllers.NetController;
import com.frederiksen.love.gameobjs.controllers.SyncedClientController;
import com.frederiksen.love.gameobjs.controllers.SyncedServerController;
import com.frederiksen.love.screens.Game;
import com.frederiksen.love.utils.TempPool;
import com.frederiksen.love.utils.Ref;
import com.frederiksen.love.utils.Timer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.esotericsoftware.minlog.Log.*;

public class Network {
    public static final int CHUNKSIZE = 10;
    public static final int UDP_PORT  = 51515;

    public abstract class EndPoint extends Listener {

        public abstract void start();
        public abstract void update();
        public abstract void stop();

        public Network getNetwork() {
            return Network.this;
        }
    }

    /* used to make packet recognition a bit easier */
    public static class StateChunk extends ChunkManager.Chunk<StatePacket> {
        public static void register(Kryo kryo) {
            StatePacket.register(kryo);
            kryo.register(StateChunk.class);
        }

        public StateChunk() {
            super(CHUNKSIZE);
        }
    }

    public class Server extends EndPoint {

        public class PlayerConnection extends Connection {
            public Player.RemoteServerController controller;
        }

        /* note that I don't actually care about the type of NetController because they all have a casting
         * mechanism from basic states and actions to their local types */
        private int                                tcpPort;
        private List<PlayerConnection>             playersConnections = new ArrayList<>();
        private List<NetController<?, ?, ?, ?, ?>> controllers        = new ArrayList<>();

        private com.esotericsoftware.kryonet.Server server;
        private NetConnector.ServerConnector        connector;
        private Timer.Task                          broadcaster     = new Timer.Task(this::broadcastStates, 1 / tickRate.get());
        private TempPool<StatePacket>               statePacketPool = new TempPool<>(20, StatePacket::new);
        private ChunkManager<StatePacket>           stateChunker    = new ChunkManager<>(CHUNKSIZE, 5, StateChunk::new);

        public Server() {
            System.out.println("Creating server...");
            // override server connection factory to avoid connection lookups ( thanks library devs (; )
            server = new com.esotericsoftware.kryonet.Server() {
                @Override
                protected Connection newConnection() {
                    return new PlayerConnection();
                }
            };
            server.addListener(this);

            registerPackets(server.getKryo());
            try {
                server.bind(0, UDP_PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // create discovery packet for clients (pack tcp port since it can be anything)
            tcpPort = server.getServerChannel().socket().getLocalPort();
            String name = server.getServerChannel()
                                .socket()
                                .getInetAddress()
                                .getHostName();

            NetConnector.DiscoveryPacket packet = new NetConnector.DiscoveryPacket(name, tcpPort);
            connector = new NetConnector.ServerConnector(server, packet);

            System.out.printf("Server binded to udp: %d tcp: %d.\n", UDP_PORT, tcpPort);
        }

        @Override
        public void start() {
            System.out.println("Starting server...");
            Timer.schedule(broadcaster);

            game.onNetChange();

            createObject(game.getLocalPlayer());
        }

        public void broadcastStates() {
            updateServer();

            for (PlayerConnection connection : playersConnections) {
                // build chunks for individual clients (clients may get different chunks)
                statePacketPool.freeAll();
                stateChunker.clear();
                Player.RemoteServerController playerController = connection.controller;

                for (NetController<?, ?, ?, ?, ?> controller : controllers) {
                    // obtain a state packet for transport
                    StatePacket statePacket = statePacketPool.obtain();
                    // poll state based on type of controller
                    if (controller == playerController) {
                        stateChunker.add(statePacket.create(controller.getId(), playerController
                                                 .pollState(true)));
                    } else {
                        stateChunker.add(statePacket.create(controller.getId(), controller.pollState()));
                    }
                }

                // send chunks
                for (ChunkManager.Chunk<StatePacket> chunk : stateChunker.getChunks())
                    connection.sendUDP(chunk);

            }
        }

        private void updateServer() {
            try {
                // lol do this to support downstream bandwidth of connections
                for (int i = 0; i < playersConnections.size() + 1; i++)
                    server.update(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void update() {

        }

        @Override
        public void connected(Connection connection) {
            super.connected(connection);
            PlayerConnection playerConnection = (PlayerConnection) connection;

            System.out.println("Client connected.");

            // create player
            Player player = game.createPlayer();

            if (player.getController() instanceof Player.RemoteServerController) {
                // update connection's internal state
                playerConnection.controller = (Player.RemoteServerController) player.getController();
            } else throw new RuntimeException("Player should have a Player.RemoteServerController.");

            // send startup packet
            connection.sendTCP(new UrgentPacket.WelcomePacket().create(playerConnection.controller));

            // notify all other players of new object
            createObject(player);

            playersConnections.add(playerConnection);

            // send all objects to new player
            for (GameObj<?, ?, ?> obj : game.getGameObjs()) {
                createPacket.create(obj);
                playerConnection.sendTCP(createPacket);
            }

            // finally, add new player to game
            game.addGameObj(player);
        }


        @Override
        public void disconnected(Connection connection) {
            super.disconnected(connection);
            PlayerConnection playerConnection = (PlayerConnection) connection;

            System.out.println("Client disconnected.");

            // note that this method could be called from any thread
            Gdx.app.postRunnable(() -> {

                // remove player (enqueue object for death)
                playerConnection.controller.setDead(true);

                // remove player from roster
                playersConnections.remove(playerConnection);

                // remove player from game
                destroyObject(playerConnection.controller);
            });
        }

        @Override
        public void received(Connection connection, Object object) {
            super.received(connection, object);
            PlayerConnection playerConnection = (PlayerConnection) connection;

            if (object instanceof Controller.Action) {
                // process action
                Controller.Action action = (Controller.Action) object;
                try {
                    playerConnection.controller.onNetDeliver(action);
                } catch (NetController.VerboseCastingException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void idle(Connection connection) {
            super.idle(connection);
        }

        private UrgentPacket.ObjCreatePacket createPacket = new UrgentPacket.ObjCreatePacket();

        public void createObject(GameObj<?, ?, ?> obj) {
            // possibly add to controller list (for broadcasting)
            if (obj.getController() instanceof NetController) {
                addController((NetController<?, ?, ?, ?, ?>) obj.getController());
            }

            // notify all players
            createPacket.create(obj);
            for (Connection conn : playersConnections) {
                conn.sendTCP(createPacket);
            }

        }

        private UrgentPacket.ObjDestroyPacket destroyPacket = new UrgentPacket.ObjDestroyPacket();

        public void destroyObject(GameObj<?, ?, ?> obj) {
            destroyObject(obj.getController());
        }

        public void destroyObject(Controller<?, ?> controller) {
            // make sure this object dies
            controller.setDead(true);

            // remove from controller list (possibly)
            controllers.remove(controller);

            // notify all players
            destroyPacket.create(controller);
            for (Connection conn : playersConnections) {
                conn.sendTCP(destroyPacket);
            }


        }

        public void addController(NetController<?, ?, ?, ?, ?> controller) {
            controllers.add(controller);
        }

        @Override
        public void stop() {
            System.out.println("Stopping server...");
            Timer.cancel(broadcaster);
            server.stop();
        }
    }

    public class Client extends EndPoint {
        private HashMap<Long, Controller<?, ?>> controllers = new HashMap<>();
        private com.esotericsoftware.kryonet.Client         client      = new com.esotericsoftware.kryonet.Client();
        private Connection                                  server;

        private ObjFactory objFactory = new ObjFactory();

        public Client() {
            client.addListener(this);

            registerPackets(client.getKryo());
        }

        @Override
        public void start() {

        }

        public void connect(InetAddress address, int tcpPort, int timeout) throws IOException {
            client.connect(timeout, address, tcpPort, UDP_PORT);
        }

        public void pushAction(Controller.Action action) {
            // send action over network
            server.sendUDP(action);
        }

        @Override
        public void update() {
            try {
                client.update(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void connected(Connection connection) {
            super.connected(connection);

            System.out.println("Connected to server.");

            // store server connection
            server = connection;

            game.onNetChange();
        }

        @Override
        public void disconnected(Connection connection) {
            super.disconnected(connection);

            System.out.println("Server disconnected.");
            Network.this.stop();

            Gdx.app.postRunnable(() -> {
                game.onNetChange();
            });
        }

        @Override
        public void received(Connection connection, Object object) {
            super.received(connection, object);

            if (object instanceof StateChunk) {
                StateChunk chunk = (StateChunk) object;

                // iterate all incoming states
                for (StatePacket statePacket : chunk) {
                    Controller<?, ?> controller = controllers.get(statePacket.getId());

                    if (controller != null) {
                        try {
                            // attempt to deliver state to controller
                            if (controller instanceof NetController) {
                                ((NetController<?, ?, ?, ?, ?>) controller).onNetDeliver(statePacket.getState());
                            } else {
                                // action was sent to object without networked controller
                            }
                        } catch (NetController.VerboseCastingException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // no controller with given id

                        // new Exception("State received (id: " + statePacket.getId() + ") but quickly ignored.").printStackTrace();
                    }
                }
            } else if (object instanceof UrgentPacket.WelcomePacket) {
                System.out.println("Gotta welcome packet.");
                UrgentPacket.WelcomePacket welcomePacket = (UrgentPacket.WelcomePacket) object;
                if (game.getLocalPlayer().getController() instanceof Player.LocalClientController) {
                    Player.LocalClientController controller = ((Player.LocalClientController) game.getLocalPlayer().getController());
                    // set local player's ID from server
                    controller.setId(welcomePacket.controllerId);
                    // load initial state from server
                    try {
                        controller.onNetDeliver(welcomePacket.state);
                    } catch (NetController.VerboseCastingException e) {
                        e.printStackTrace();
                    }
                    // add controller to list
                    addController(controller);

                } else throw new RuntimeException("Local player should have a Player.LocalClientController.");
            } else if (object instanceof UrgentPacket.ObjCreatePacket) {
                System.out.println("Server created object.");
                UrgentPacket.ObjCreatePacket createPacket = (UrgentPacket.ObjCreatePacket) object;

                try {
                    GameObj obj = objFactory.buildObj(game, createPacket.builder);
                    obj.getController().setId(createPacket.controllerId);
                    addController(obj.getController());
                    game.addGameObj(obj);
                } catch (NetController.VerboseCastingException | ObjFactory.MissinFactory e) {
                    e.printStackTrace();
                }

            } else if (object instanceof UrgentPacket.ObjDestroyPacket) {
                System.out.println("Server destroyed object.");
                UrgentPacket.ObjDestroyPacket destroyPacket = (UrgentPacket.ObjDestroyPacket) object;

                GameObj<?, ?, ?> obj = game.getGameObjById(destroyPacket.controllerId);
                if (obj != null) {
                    obj.getController().setDead(true);
                } else {
                    // object not found
                }

            } else if (object instanceof FrameworkMessage.KeepAlive) {

            } else {
                new Exception("Unknown packet type: " + object.getClass().getName()).printStackTrace();
            }
        }

        @Override
        public void idle(Connection connection) {
            super.idle(connection);
        }

        public void addController(Controller<?, ?> controller) {
            controllers.put(controller.getId(), controller);
        }

        public Connection getServerConnection() {
            return server;
        }

        @Override
        public void stop() {
            System.out.println("Stopping client...");
            client.stop();
        }
    }


    private volatile EndPoint endpoint = null;

    private Ref<Float> tickRate   = new Ref<>(20f);
    private Ref<Float> tickLength = new Ref<>(1f / 20);
    private Game       game;

    public Network(Game game) {
        this.game = game;

        Log.set(LEVEL_INFO);
    }

    public void findServer(int timeout) {
        System.out.println("Searching for server...");
        NetConnector.ClientConnector connector = new NetConnector.ClientConnector();
        InetAddress address = connector.findHost(UDP_PORT, timeout);
        if (address != null) {
            // connect to server as client
            Client client = new Client();
            try {
                // do this to allow main thread to update it
                endpoint = client;
                client.connect(address, connector.getPacket().tcpPort, 5000);
                client.start();
                System.out.printf("Server was found host: %s udp: %d tcp: %d\n", address.getHostAddress(), UDP_PORT, connector.getPacket().tcpPort);
                return;
            } catch (IOException e) {
                e.printStackTrace();
                client.stop();
            }
        }

        System.out.println("Server was not found.");

        // become your own server
        Server server = new Server();
        server.start();
        endpoint = server;
    }

    public void update() {
        if (endpoint != null)
            endpoint.update();
    }

    public static void registerPackets(Kryo kryo) {
        NetConnector.DiscoveryPacket.register(kryo);
        Player.State.register(kryo);
        Player.Action.register(kryo);
        Player.Builder.register(kryo);
        StateChunk.register(kryo);
        UrgentPacket.register(kryo);
        SyncedServerController.Action.register(kryo);
        SyncedClientController.State.register(kryo);
    }

    public Ref<Float> getTickRate() {
        return tickRate;
    }

    public Ref<Float> getTickLength() {
        return tickLength;
    }

    public boolean isActive() {
        return endpoint != null;
    }

    public boolean isServer() {
        return endpoint instanceof Server;
    }

    public boolean isClient() {
        return endpoint instanceof Client;
    }

    public EndPoint getEndpoint() {
        return endpoint;
    }

    public void stop() {
        if (endpoint != null) {
            System.out.println("Stopping endpoint...");
            endpoint.stop();
            endpoint = null;
        }
    }

}

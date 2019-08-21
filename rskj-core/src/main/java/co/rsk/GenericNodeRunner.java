package co.rsk;

import co.rsk.config.RskSystemProperties;
import co.rsk.core.Rsk;
import co.rsk.net.MessageHandler;
import co.rsk.net.discovery.UDPServer;
import co.rsk.rpc.netty.Web3HttpServer;
import co.rsk.rpc.netty.Web3WebSocketServer;
import org.ethereum.net.server.ChannelManager;
import org.ethereum.net.server.PeerServer;
import org.ethereum.rpc.Web3;
import org.ethereum.sync.SyncPool;
import org.ethereum.util.BuildInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GenericNodeRunner implements NodeRunner{

    protected final Rsk rsk;
    protected final UDPServer udpServer;
    protected final RskSystemProperties rskSystemProperties;
    protected final Web3HttpServer web3HttpServer;
    protected final Web3WebSocketServer web3WebSocketServer;
    protected final Web3 web3Service;
    protected final ChannelManager channelManager;
    protected final SyncPool syncPool;
    protected final MessageHandler messageHandler;
    protected final PeerServer peerServer;
    protected final SyncPool.PeerClientFactory peerClientFactory;
    protected final BuildInfo buildInfo;
    protected final Logger logger;


    public GenericNodeRunner(Rsk rsk, UDPServer udpServer,
                             RskSystemProperties rskSystemProperties,
                             Web3 web3Service,
                             Web3HttpServer web3HttpServer,
                             Web3WebSocketServer web3WebSocketServer,
                             ChannelManager channelManager,
                             SyncPool syncPool,
                             MessageHandler messageHandler,
                             PeerServer peerServer,
                             SyncPool.PeerClientFactory peerClientFactory,
                             BuildInfo buildInfo,
                             String nodeName
                             ){
        this.rsk = rsk;
        this.udpServer = udpServer;
        this.rskSystemProperties = rskSystemProperties;
        this.web3HttpServer = web3HttpServer;
        this.web3Service = web3Service;
        this.web3WebSocketServer = web3WebSocketServer;
        this.channelManager = channelManager;
        this.syncPool = syncPool;
        this.messageHandler = messageHandler;
        this.peerServer = peerServer;
        this.peerClientFactory = peerClientFactory;
        this.buildInfo = buildInfo;
        this.logger = LoggerFactory.getLogger(nodeName);
    }

    protected void startWeb3(RskSystemProperties rskSystemProperties) throws InterruptedException {
        boolean rpcHttpEnabled = rskSystemProperties.isRpcHttpEnabled();
        boolean rpcWebSocketEnabled = rskSystemProperties.isRpcWebSocketEnabled();

        if (rpcHttpEnabled || rpcWebSocketEnabled) {
            web3Service.start();
        }

        if (rpcHttpEnabled) {
            logger.info("RPC HTTP enabled");
            web3HttpServer.start();
        } else {
            logger.info("RPC HTTP disabled");
        }

        if (rpcWebSocketEnabled) {
            logger.info("RPC WebSocket enabled");
            web3WebSocketServer.start();
        } else {
            logger.info("RPC WebSocket disabled");
        }
    }

}

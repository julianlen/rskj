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

public class LightNodeRunner extends GenericNodeRunner {

    private static Logger logger = LoggerFactory.getLogger("lightnoderunner");

    public LightNodeRunner(Rsk rsk, UDPServer udpServer,
                           RskSystemProperties rskSystemProperties,
                           Web3 web3Service,
                           Web3HttpServer web3HttpServer,
                           Web3WebSocketServer web3WebSocketServer,
                           ChannelManager channelManager, SyncPool syncPool,
                           MessageHandler messageHandler, PeerServer peerServer,
                           SyncPool.PeerClientFactory peerClientFactory, BuildInfo buildInfo){

        // For now some things we dont implement different than fullNode but it will be needed to be implemented
        // List: syncPool, nodeMessageHandler
        super(rsk, udpServer, rskSystemProperties, web3Service, web3HttpServer,
                web3WebSocketServer, channelManager, syncPool, messageHandler,
                peerServer, peerClientFactory, buildInfo);
    }

    @Override
    public void run() throws Exception {

    }

    @Override
    public void stop() {

    }
}

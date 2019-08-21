package co.rsk;

import co.rsk.config.RskSystemProperties;
import co.rsk.core.Rsk;
import co.rsk.net.MessageHandler;
import co.rsk.net.discovery.UDPServer;
import co.rsk.rpc.netty.Web3HttpServer;
import co.rsk.rpc.netty.Web3WebSocketServer;
import org.ethereum.net.eth.EthVersion;
import org.ethereum.net.server.ChannelManager;
import org.ethereum.net.server.PeerServer;
import org.ethereum.rpc.Web3;
import org.ethereum.sync.SyncPool;
import org.ethereum.util.BuildInfo;

import java.util.stream.Collectors;

public class LightNodeRunner extends GenericNodeRunner {

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
                peerServer, peerClientFactory, buildInfo, "lightnoderunner");

    }

    @Override
    public void run() throws Exception {
        logger.info("Starting RSK");

        logger.info(
                "Running {},  core version: {}-{}",
                rskSystemProperties.genesisInfo(),
                rskSystemProperties.projectVersion(),
                rskSystemProperties.projectVersionModifier()
        );
        buildInfo.printInfo(logger);

        /*transactionGateway.start();
        // this should be the genesis block at this point
        transactionPool.start(blockchain.getBestBlock());*/
        channelManager.start();
        messageHandler.start();
        peerServer.start();

        if (logger.isInfoEnabled()) {
            String versions = EthVersion.supported().stream().map(EthVersion::name).collect(Collectors.joining(", "));
            logger.info("Capability eth version: [{}]", versions);
        }

        if (!"".equals(rskSystemProperties.blocksLoader())) {
            rskSystemProperties.setSyncEnabled(Boolean.FALSE);
            rskSystemProperties.setDiscoveryEnabled(Boolean.FALSE);
        }

        startWeb3(rskSystemProperties);

        if (rskSystemProperties.isPeerDiscoveryEnabled()) {
            udpServer.start();
        }

        if (rskSystemProperties.isSyncEnabled()) {
            syncPool.updateLowerUsefulDifficulty();
            syncPool.start(peerClientFactory);
            if (rskSystemProperties.waitForSync()) {
                waitRskSyncDone();
            }
        }

        logger.info("done");
    }

    private void waitRskSyncDone() throws InterruptedException {
        while (rsk.isBlockchainEmpty() || rsk.hasBetterBlockToSync()) {
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e1) {
                logger.trace("Wait sync done was interrupted", e1);
                throw e1;
            }
        }
    }


    @Override
    public void stop() {
        logger.info("Shutting down RSK node");

        syncPool.stop();

        boolean rpcHttpEnabled = rskSystemProperties.isRpcHttpEnabled();
        boolean rpcWebSocketEnabled = rskSystemProperties.isRpcWebSocketEnabled();
        if (rpcHttpEnabled) {
            web3HttpServer.stop();
        }
        if (rpcWebSocketEnabled) {
            try {
                web3WebSocketServer.stop();
            } catch (InterruptedException e) {
                logger.error("Couldn't stop the WebSocket server", e);
                Thread.currentThread().interrupt();
            }
        }

        if (rpcHttpEnabled || rpcWebSocketEnabled) {
            web3Service.stop();
        }

        /*if (rskSystemProperties.isMinerServerEnabled()) {
            minerServer.stop();
            if (rskSystemProperties.isMinerClientEnabled()) {
                minerClient.stop();
            }
        }*/

        peerServer.stop();
        messageHandler.stop();
        channelManager.stop();
        //transactionGateway.stop();

        if (rskSystemProperties.isPeerDiscoveryEnabled()) {
            try {
                udpServer.stop();
            } catch (InterruptedException e) {
                logger.error("Couldn't stop the updServer", e);
                Thread.currentThread().interrupt();
            }
        }

        logger.info("RSK node Shut down");
    }
}

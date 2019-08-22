/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package co.rsk;

import co.rsk.config.InternalService;
import co.rsk.config.RskSystemProperties;
import co.rsk.core.Rsk;
import co.rsk.mine.MinerClient;
import co.rsk.mine.MinerServer;
import co.rsk.net.discovery.UDPServer;
import co.rsk.rpc.netty.Web3HttpServer;
import co.rsk.rpc.netty.Web3WebSocketServer;
import org.ethereum.net.eth.EthVersion;
import org.ethereum.rpc.Web3;
import org.ethereum.sync.SyncPool;
import org.ethereum.util.BuildInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FullNodeRunner implements NodeRunner {
    private static Logger logger = LoggerFactory.getLogger("fullnoderunner");

    private final List<InternalService> internalServices;
    private final Rsk rsk;
    private final UDPServer udpServer;
    private final MinerServer minerServer;
    private final MinerClient minerClient;
    private final RskSystemProperties rskSystemProperties;
    private final Web3HttpServer web3HttpServer;
    private final Web3WebSocketServer web3WebSocketServer;
    private final SyncPool syncPool;

    private final Web3 web3Service;
    private final SyncPool.PeerClientFactory peerClientFactory;
    private final BuildInfo buildInfo;

    public FullNodeRunner(
            List<InternalService> internalServices,
            Rsk rsk,
            UDPServer udpServer,
            MinerServer minerServer,
            MinerClient minerClient,
            RskSystemProperties rskSystemProperties,
            Web3 web3Service,
            Web3HttpServer web3HttpServer,
            Web3WebSocketServer web3WebSocketServer,
            SyncPool syncPool,
            SyncPool.PeerClientFactory peerClientFactory,
            BuildInfo buildInfo) {
        this.internalServices = Collections.unmodifiableList(internalServices);
        this.rsk = rsk;
        this.udpServer = udpServer;
        this.minerServer = minerServer;
        this.minerClient = minerClient;
        this.rskSystemProperties = rskSystemProperties;
        this.web3HttpServer = web3HttpServer;
        this.web3Service = web3Service;
        this.web3WebSocketServer = web3WebSocketServer;
        this.syncPool = syncPool;
        this.peerClientFactory = peerClientFactory;
        this.buildInfo = buildInfo;
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

        for (InternalService internalService : internalServices) {
            internalService.start();
        }

        if (logger.isInfoEnabled()) {
            String versions = EthVersion.supported().stream().map(EthVersion::name).collect(Collectors.joining(", "));
            logger.info("Capability eth version: [{}]", versions);
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

        if (rskSystemProperties.isMinerServerEnabled()) {
            minerServer.start();

            if (rskSystemProperties.isMinerClientEnabled()) {
                minerClient.start();
            }
        }

        logger.info("done");
    }

    private void startWeb3(RskSystemProperties rskSystemProperties) throws InterruptedException {
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

        if (rskSystemProperties.isMinerServerEnabled()) {
            minerServer.stop();
            if (rskSystemProperties.isMinerClientEnabled()) {
                minerClient.stop();
            }
        }

        if (rskSystemProperties.isPeerDiscoveryEnabled()) {
            try {
                udpServer.stop();
            } catch (InterruptedException e) {
                logger.error("Couldn't stop the updServer", e);
                Thread.currentThread().interrupt();
            }
        }

        for (int i = internalServices.size() - 1; i >= 0; i--) {
            internalServices.get(i).stop();
        }

        logger.info("RSK node Shut down");
    }
}

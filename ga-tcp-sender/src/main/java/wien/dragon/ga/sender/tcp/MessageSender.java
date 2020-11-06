/*
 * Copyright (c) 2020. Robert Wittek (robo-w on GitHub)
 *
 * This software may be modified and distributed under the terms of the MIT license.  See the LICENSE file for details.
 */

package wien.dragon.ga.sender.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wien.dragon.ga.payload.DataRate;
import wien.dragon.ga.payload.FunctionCode;
import wien.dragon.ga.payload.PayloadFactory;

import java.net.InetAddress;

public class MessageSender {
    private static final Logger LOG = LoggerFactory.getLogger(MessageSender.class);

    public static void main(String... args) throws Exception {
        if (args.length < 4) {
            usageAndExit();
        }

        String hostname = null;
        int port = 0;
        int targetRic = 0;
        String message = "";
        try {
            hostname = args[0];
            port = Integer.parseInt(args[1]);
            targetRic = Integer.parseInt(args[2]);
            message = args[3];
        } catch (Exception e) {
            LOG.error("Failed to parse program argument.", e);
            usageAndExit();
        }

        var payload = PayloadFactory.createAlphaPagingPayload(targetRic, DataRate.BPS_1200, FunctionCode.CODE_1, message);
        LOG.info("Sending paging message over TCP to RIC '{}' via gateway at {}:{}. Message: '{}'", targetRic, hostname, port, message);
        var tcpSender = new TcpSender(InetAddress.getByName(hostname.trim()), port);
        tcpSender.sendMessage(payload);

        LOG.info("Shutting down TCP sender. Shutdown might take up to 5 seconds on failed transmission.");
        tcpSender.close();
    }

    private static void usageAndExit() {
        System.err.println("Usage: java -jar ga-tcp-sender.jar <IP> <Port> <RIC> <Message>");
        System.err.println("  Example: `java -jar 192.168.42.100 10300 ga-tcp-sender.jar 12345 \"Test Message\"`");
        System.exit(-1);
    }
}

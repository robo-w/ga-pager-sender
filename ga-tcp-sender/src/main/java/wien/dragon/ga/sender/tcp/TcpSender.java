/*
 * Copyright (c) 2020. Robert Wittek (robo-w on GitHub)
 *
 * This software may be modified and distributed under the terms of the MIT license.  See the LICENSE file for details.
 */

package wien.dragon.ga.sender.tcp;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TcpSender implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TcpSender.class);

    private final Socket socket;
    private final ExecutorService executor;

    public TcpSender(final InetAddress address, final int port) {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(address, port), 5000);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create TCP sender for pager message.", e);
        }

        executor = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder()
                        .setThreadFactory(Executors.defaultThreadFactory())
                        .setDaemon(false)
                        .setUncaughtExceptionHandler((thread, e) -> LOG.error("Uncaught exception in TCP receiver thread.", e))
                        .setNameFormat("TCP Receiver")
                        .build());
    }

    public void sendMessage(final byte[] message) {
        try {
            socket.getOutputStream().write(message);
            socket.getOutputStream().flush();
            LOG.info("Successfully sent message via TCP.");
            executor.execute(this::readResponse);
        } catch (IOException e) {
            LOG.warn("Failed to send message.", e);
        }
    }

    private void readResponse() {
        try {
            byte[] checksumResponse = socket.getInputStream().readNBytes(4);
            LOG.info("Received checksum response: '{}'", new String(checksumResponse, StandardCharsets.US_ASCII));
            byte[] ackResponse = socket.getInputStream().readNBytes(1);
            LOG.info("Received ACK response: {}", ackResponse);
        } catch (IOException e) {
            LOG.warn("Failed to read from TCP socket.", e);
        }

    }

    @Override
    public void close() throws Exception {
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        socket.close();
        executor.shutdownNow();
    }
}

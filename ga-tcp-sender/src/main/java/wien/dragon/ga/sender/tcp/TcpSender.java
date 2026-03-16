/*
 * Copyright (c) 2020. Robert Wittek (robo-w on GitHub)
 *
 * This software may be modified and distributed under the terms of the MIT license.  See the LICENSE file for details.
 */

package wien.dragon.ga.sender.tcp;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wien.dragon.ga.payload.AlphaPagingPayload;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class TcpSender implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TcpSender.class);

    private final Socket socket;
    private final ExecutorService executor;

    public TcpSender(final InetAddress address, final int port) {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(address, port), 3000);
            socket.setSoTimeout(3000);
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

    public synchronized void sendAlphaMessage(final AlphaPagingPayload payload) {
        final var message = payload.createSendableFrame();
        final var checksum = payload.createChecksum();
        final var callback = new CompletableFuture<Void>();
        try {
            socket.getOutputStream().write(message);
            socket.getOutputStream().flush();
            LOG.info("Successfully sent message via TCP.");
            executor.execute(() -> readResponse(callback, checksum));
            callback.get(10, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Failed to send message.", e);
        }
    }


    private void readResponse(final CompletableFuture<Void> checksumReceived, final String expectedChecksum) {
        try {
            byte[] potentialNack = socket.getInputStream().readNBytes(1);
            if (potentialNack.length > 0 && potentialNack[0] == 0x15) {
                LOG.warn("Received NACK response. Message was not sent successfully.");
                checksumReceived.completeExceptionally(new IllegalArgumentException("NACK received"));
                return;
            }

            byte[] checksumResponse = socket.getInputStream().readNBytes(3);
            byte[] ackResponse = socket.getInputStream().readNBytes(1);
            var receivedChecksum = new String(potentialNack, StandardCharsets.US_ASCII) + new String(checksumResponse, StandardCharsets.US_ASCII);
            LOG.debug("Received checksum '{}' and ACK response: {}", receivedChecksum, ackResponse);

            if (expectedChecksum.equals(receivedChecksum)) {
                LOG.info("Received expected checksum response: '{}'", receivedChecksum);
                checksumReceived.complete(null);
            } else {
                LOG.warn("Received unexpected checksum response. Expected: '{}', Received: '{}'", expectedChecksum, receivedChecksum);
                checksumReceived.completeExceptionally(new IllegalArgumentException("Checksum mismatch"));
            }
        } catch(SocketTimeoutException e) {
            LOG.warn("Reading from socket timed out. Message was probably not sent successfully.", e);
            checksumReceived.completeExceptionally(e);
        } catch (IOException e) {
            LOG.warn("Failed to read from TCP socket.", e);
            checksumReceived.completeExceptionally(e);
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

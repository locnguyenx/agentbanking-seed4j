package com.agentbanking.isoadapter.infrastructure.external;

import com.agentbanking.isoadapter.domain.model.IsoMessage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class IsoSocketClient implements AutoCloseable {

    private final String host;
    private final int port;
    private final int connectionTimeout;
    private final int socketTimeout;
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private final AtomicLong lastActivity = new AtomicLong(0);

    public IsoSocketClient(String host, int port, int connectionTimeout, int socketTimeout) {
        this.host = host;
        this.port = port;
        this.connectionTimeout = connectionTimeout;
        this.socketTimeout = socketTimeout;
    }

    public synchronized void connect() throws IOException {
        if (socket != null && socket.isConnected()) {
            return;
        }
        socket = new Socket();
        socket.connect(new java.net.InetSocketAddress(host, port), connectionTimeout);
        socket.setSoTimeout(socketTimeout);
        in = socket.getInputStream();
        out = socket.getOutputStream();
        lastActivity.set(Instant.now().toEpochMilli());
    }

    public synchronized IsoMessage send(IsoMessage message) throws IOException {
        if (socket == null || !socket.isConnected()) {
            connect();
        }

        IsoMessageCodec codec = new IsoMessageCodec();
        byte[] encoded = codec.encode(message);
        out.write(encoded);
        out.flush();
        lastActivity.set(Instant.now().toEpochMilli());

        byte[] responseBuffer = new byte[2048];
        int bytesRead = in.read(responseBuffer);
        if (bytesRead <= 0) {
            throw new IOException("ERR_ISO_101: No response from switch");
        }

        byte[] responseData = new byte[bytesRead];
        System.arraycopy(responseBuffer, 0, responseData, 0, bytesRead);
        return codec.decode(responseData);
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public long getLastActivityTime() {
        return lastActivity.get();
    }

    public boolean isStale(Duration maxIdle) {
        return Duration.ofMillis(Instant.now().toEpochMilli() - lastActivity.get()).compareTo(maxIdle) > 0;
    }

    @Override
    public synchronized void close() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                // Log and ignore
            }
        }
    }
}
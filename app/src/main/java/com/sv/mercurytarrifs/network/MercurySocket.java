package com.sv.mercurytarrifs.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MercurySocket {

    private Socket socket;
    private InputStream is;
    private OutputStream os;

    /**
     * Подключение к счётчику
     */
    public void connect(String ip, int port, int timeout) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(ip, port), timeout);
        is = socket.getInputStream();
        os = socket.getOutputStream();
    }

    /**
     * Отправка команды и получение ответа
     */
    public byte[] sendAndReceive(byte[] cmd, int timeout) throws IOException, InterruptedException {
        os.write(cmd);
        os.flush();
        Thread.sleep(200);
        socket.setSoTimeout(timeout);

        byte[] buffer = new byte[256];
        int len = is.read(buffer);
        if (len == -1) throw new IOException("Read timed out");

        byte[] resp = new byte[len];
        System.arraycopy(buffer, 0, resp, 0, len);
        return resp;
    }

    /**
     * Закрытие соединения
     */
    public void close() {
        if (socket != null) {
            try {
                if (!socket.isClosed()) socket.close();
            } catch (Exception ignored) {}
        }
        try { Thread.sleep(100); } catch (Exception ignored) {}
    }

    /**
     * Проверка подключения
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }
}
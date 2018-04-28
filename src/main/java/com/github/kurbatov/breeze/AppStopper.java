package com.github.kurbatov.breeze;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * This class provides convenient means to wait for and send a stop signal
 * from/to another instance of the application.
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
public class AppStopper {

    private final String introduction;
    private final File lockFile;
    private WatchDog watchdog;

    private static final int PRIMARY_PORT = 1620;
    private static final int LAST_PORT = 1625;
    private static final String STOP_COMMAND = "stop";

    private static final Logger LOGGER = LoggerFactory.getLogger(AppStopper.class);

    public AppStopper(String appName) {
        this(appName, new File(".pid"));
    }

    public AppStopper(String appName, File lockFile) {
        introduction = String.format("Hello! My name is %s. I am keeping this port right now.", appName);
        this.lockFile = lockFile;
    }

    /**
     * Initializes {@link AppStopper} to be waiting for signal.
     */
    public void waitForSignal() {
        if (watchdog != null) {
            return; // already waiting
        }
        for (int port = PRIMARY_PORT; port < LAST_PORT; port++) {
            watchdog = takePort(port);
            if (watchdog != null) {
                break;
            }
        }
        if (watchdog != null) {
            new Thread(watchdog, String.format("%s-watchdog", AppStopper.class.getSimpleName())).start();
        } else {
            LOGGER.warn("Cannot find an empty port to wait for a stop signal");
        }
    }

    /**
     * Stops waiting for signal.
     */
    public void stopWaitingForSignal() {
        if (watchdog != null) {
            watchdog.shutdown();
        }
        watchdog = null;
    }

    /**
     * Sends stop signal to the running instance of the same application.
     */
    public void sendStopSignal() {
        String pid = null;
        if (lockFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(lockFile))) {
                pid = reader.readLine();
            } catch (IOException e) {
                LOGGER.warn("Cannot open lock file");
                LOGGER.debug("Cannot open lock file", e);
            }
        }
        for (int port = PRIMARY_PORT; port < LAST_PORT; port++) {
            try (Socket s = new Socket("localhost", port)) {
                try (PrintWriter out = new PrintWriter(s.getOutputStream()); BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
                    String greeting = in.readLine();
                    if (introduction.equalsIgnoreCase(greeting)) {
                        out.println(String.format("%s %s", STOP_COMMAND, pid));
                        out.flush();
                        LOGGER.debug("Stop signal is sent");
                        return;
                    }
                }
            } catch (IOException e) {
                LOGGER.debug("Cannot send a signal to localhost:{}", port);
            }
        }
        LOGGER.error("The application is not running");
    }

    private WatchDog takePort(int port) {
        WatchDog dog = null;
        try {
            ServerSocket server = new ServerSocket(port, 0, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}));
            dog = new WatchDog(server, lockFile, introduction);
        } catch (IOException ex) {
            LOGGER.debug("Cannot take port {}", port);
        }
        return dog;
    }

    private static class WatchDog implements Runnable {

        private final ServerSocket server;

        private final File lockFile;

        private final String introduction;

        private final UUID uuid;

        private final AtomicBoolean running = new AtomicBoolean(false);

        public WatchDog(ServerSocket server, File lockFile, String introduction) {
            this.server = server;
            this.lockFile = lockFile;
            this.introduction = introduction;
            uuid = UUID.randomUUID();
        }

        @Override
        public void run() {
            if (!running.getAndSet(true)) {
                try {
                    if (lockFile.exists()) {
                        lockFile.delete();
                    }
                    lockFile.createNewFile();
                } catch (IOException e) {
                    LOGGER.warn("Cannot create lock file", e);
                }
                try (PrintWriter lockWriter = new PrintWriter(lockFile)) {
                    lockWriter.println(uuid.toString());
                } catch (IOException e) {
                    LOGGER.warn("Cannot write UUID to lock file", e);
                }
                while (running.get()) {
                    String command = null;
                    try (Socket responder = server.accept()) {
                        try (
                                PrintWriter out = new PrintWriter(responder.getOutputStream());
                                BufferedReader in = new BufferedReader(new InputStreamReader(responder.getInputStream()));
                            ) {
                            out.println(introduction);
                            out.flush();
                            command = in.readLine();
                        }
                    } catch (IOException e) {
                        if (running.get()) {
                            LOGGER.debug("Application stopping exception", e);
                        }
                    }
                    if (!StringUtils.isEmpty(command)) {
                        LOGGER.debug("Incoming command: {}", command);
                        String[] parts = command.split(" ");
                        if (STOP_COMMAND.equalsIgnoreCase(parts[0])) {
                            if (parts.length > 1) {
                                try {
                                    UUID incoming = UUID.fromString(parts[1]);
                                    if (uuid.equals(incoming)) {
                                        shutdown();
                                        AppStarter.stop();
                                    } else {
                                        throw new IllegalArgumentException();
                                    }
                                } catch (IllegalArgumentException e) {
                                    LOGGER.debug("Wrong stopping UUID received {}", parts[1]);
                                }
                            }
                        }
                    }
                }
            }
        }

        public void shutdown() {
            if (running.getAndSet(false)) {
                try {
                    server.close();
                } catch (IOException e) {
                    LOGGER.warn("Cannot close stopping listener socket");
                    LOGGER.debug("Cannot close stopping listener socket", e);
                }
                try {
                    lockFile.delete();
                } catch (SecurityException e) {
                    LOGGER.warn("Cannot remove lock file");
                    LOGGER.debug("Cannot remove lock file", e);
                }
            }
        }

    }

}

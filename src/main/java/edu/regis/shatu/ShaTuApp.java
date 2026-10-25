/*
 * SHATU: SHA-256 Tutor
 * 
 *  (C) Johanna & Richard Blumenthal, All rights reserved
 * 
 *  Unauthorized use, duplication or distribution without the authors'
 *  permission is strictly prohibted.
 * 
 *  Unless required by applicable law or agreed to in writing, this
 *  software is distributed on an "AS IS" basis without warranties
 *  or conditions of any kind, either expressed or implied.
 *
 * Excited to get started! - Peter Kay
 */

package edu.regis.shatu;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import edu.regis.shatu.svc.ShaTuServer;
import edu.regis.shatu.util.ResourceMgr;
import edu.regis.shatu.view.MainFrame;

/**
 * A standalone implementation of the SHA-256 intelligent tutoring application.
 * @author rickb
 */
public class ShaTuApp {
     /**
     * Property file located on the CLASSPATH, which is used to configure the LOGGER.
     */
    private static final String LOGGER_PROPERTIES = "/Logging.properties";

    /**
     * Events of interest occurring in this class are logged to this logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ShaTuApp.class.getName());
    
    /**
     * Configure the LOGGER with the properties found in the LOGGER_PROPERTIES 
     * file found on the CLASSPATH.
     */
    static {
        final InputStream strm = 
            ShaTuApp.class.getResourceAsStream(LOGGER_PROPERTIES);
        
        try {
            LogManager.getLogManager().readConfiguration(strm);
        } catch (IOException e) {
        
            LOGGER.severe("Error loading ./logging.properties");
            LOGGER.severe(e.getMessage());
        }        
    }
    
    /**
     * Main entry point for the ShaTut application, which will display the UI.
     * 
     * @param args ignored
     */
    public static void main(String[] args) {
        LOGGER.info("ShaTuApp Initializing...:");
        
        // Initialize the logging properties from the logging properties files
        try {
            final InputStream strm = ShaTuApp.class.getResourceAsStream(LOGGER_PROPERTIES);
        
            LogManager.getLogManager().readConfiguration(strm);
            
            LOGGER.info("Message logging initialization completed.");
        } catch (IOException e) {
        
            LOGGER.severe("Error loading ./logging.properties");
            LOGGER.severe(e.getMessage());
        }      
        
        // Initializes the application properties from, which also sets the locale.
        ResourceMgr.instance();
        
        LOGGER.info("ShaTu properties initialization completed.");
        
        System.out.println("Finished initializing");
        
        // Initialize the server and client separately
        try {
            startServer();
            startClient();
        } catch (InterruptedException ex) {
            Logger.getLogger(ShaTuApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Starts the backend ShaTu socket server as a daemon thread.
     */
    private static void startServer() throws InterruptedException {
        LOGGER.info(" Starting ShaTu Server (Tutoring Service)...");
        
        Thread serverThread = new Thread(new ShaTuServer(), "ShaTuServer");
        serverThread.setDaemon(true);
        serverThread.start();

        waitForServerReady();
            
        LOGGER.info(" Server is running.");
    }

    /**
     * Initializes the GUI client and prompts the sign-in screen.
     */
    private static void startClient() {
        LOGGER.info(" Starting Client GUI...");

        // Forces the creation of the MainFrame singleton, which has the
        // side-effect of displaying the SplashPanel allowing the user to sign-in.
        MainFrame.instance();      
            
        LOGGER.info("ShaTu Initialization successful.");
    }

    /**
     * Wait briefly for the embedded server to accept connections; avoids fixed sleeps.
     */
    private static void waitForServerReady() throws InterruptedException {
        final int attempts = 5;
        final int backoffMillis = 200;

        for (int i = 0; i < attempts; i++) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("localhost", ShaTuServer.PORT), backoffMillis);
                return;
            } catch (IOException e) {
                Thread.sleep(backoffMillis);
            }
        }
        LOGGER.warning("Server readiness check timed out; continuing startup.");
    }
}
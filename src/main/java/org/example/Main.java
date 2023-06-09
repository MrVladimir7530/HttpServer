package org.example;

import org.example.handlers.SimpleHandler;
import org.example.server.Server;
import org.example.utils.Common;
import org.example.utils.PropertyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static Logger log = LoggerFactory.getLogger(org.example.Main.class.getSimpleName());

    private static Server server;
    public static void main(String[] args) throws Exception {
        PropertyManager.load();
        Common.configure();
        Server server = new Server();
        server.addHandler("/simple", SimpleHandler.class);
        server.bootstrap();
    }
}


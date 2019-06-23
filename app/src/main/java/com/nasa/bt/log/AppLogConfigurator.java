package com.nasa.bt.log;

import android.util.Log;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import de.mindpipe.android.logging.log4j.LogConfigurator;

public class AppLogConfigurator {
    private static final LogConfigurator logConfigurator = new LogConfigurator();

    public static final String LOG_TAG="NASA-LOG4J";

    public static void configure(){
        logConfigurator.setRootLevel(Level.DEBUG);
        logConfigurator.setLevel("org.apache", Level.DEBUG);
        logConfigurator.setUseLogCatAppender(true);
        logConfigurator.setLogCatPattern("[%-5p] %d{yyyy-MM-dd HH:mm:ss,SSS} method:%l%n%m%n");
        logConfigurator.setUseFileAppender(false);
        logConfigurator.configure();
    }

    public static Logger getLogger(){
        return Logger.getLogger(LOG_TAG);
    }
}

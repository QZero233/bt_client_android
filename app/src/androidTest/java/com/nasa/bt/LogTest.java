package com.nasa.bt;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import de.mindpipe.android.logging.log4j.LogConfigurator;


public class LogTest {

    @Test
    public void testLog(){

        LogConfigurator logConfigurator=new LogConfigurator();
        logConfigurator.setRootLevel(Level.DEBUG);
        logConfigurator.setLevel("org.apache", Level.DEBUG);
        logConfigurator.setLogCatPattern("[%-5p] %d{yyyy-MM-dd HH:mm:ss,SSS} method:%l%n%m%n");
        logConfigurator.setUseLogCatAppender(true);
        logConfigurator.configure();

        Logger log=Logger.getLogger(LogTest.class);
        log.debug("这是DEBUG消息");
    }
}

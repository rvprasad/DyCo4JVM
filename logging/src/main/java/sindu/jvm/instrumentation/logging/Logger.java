/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */
package sindu.jvm.instrumentation.logging;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

public class Logger {

    static {
        try {
            LOGGER = new Logger();
            
            java.lang.Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        LOGGER.cleanup();
                    } catch (final Throwable _e) {
                        throw new RuntimeException(_e);
                    }
                }
            });
        } catch (final IOException _e) {
            throw new RuntimeException(_e);
        }
    }

    private Logger() throws IOException {
        profileFile = File.createTempFile("trace_", ".gz", new File("."));
        final OutputStream _stream = new FileOutputStream(profileFile, true);
        logWriter = new PrintWriter(new BufferedOutputStream(
                new GZIPOutputStream(_stream), 10000000));
        logWriter.println((new Date()).toString());
    }

    @Override
    protected void finalize() throws Throwable {
        cleanup();
        super.finalize();
    }
    
    private synchronized void cleanup() {
        if (!this.clean) {
            logWriter.flush();
            logWriter.close();
            clean = true;
        }
    }
    
    private void writeLog(final String msg) {
        if (!prevMsg.equals(msg)) {
            if (msgFreq > 0) {
                final String _tmp = String.format("%d,%s", msgFreq, prevMsg);
                logWriter.println(_tmp);
            }
            logWriter.println(String.format("1,%s", msg));
            prevMsg = msg;
            msgFreq = 0;
        } else {
            msgFreq++;
        }
    }
    
    public static synchronized void log(final String msg) {
        final StringBuilder _sb = new StringBuilder();
        _sb.append(Thread.currentThread().getId());
        _sb.append(",").append(msg);
        LOGGER.writeLog(_sb.toString());
    }

    public static void log(final String ... args) {
        log(String.join(",", args));
    }

    public static void logMethodEntry(final String methodId) {
        log("entry", methodId);
    }

    public static void logMethodExit(final String methodId, 
        final String exitId) {
        log("exit", methodId, exitId);
    }

    public static void logArgument(final byte index, final String val) {
        log("arg", Byte.toString(index), val);
    }

    public static void logReturn(final String val) {
        if (val != null) {
            log("return");
        } else {
            log("return", val);
        }
    }

    public static void logField(final Object receiver, 
            final String fieldValue, final String fieldName, 
            final Action action) {
        log(action.toString().concat("F"), fieldName, 
            receiver == null ? "" : toString(receiver), fieldValue);
    }
    
    public static void logArray(final Object array, final int index, 
            final String value, final Action action) {
        log(action.toString().concat("A"), Integer.toString(index),
            toString(array), value);
    }

    public static void logException(final Throwable exception) {
        log("exception", toString(exception), exception.getClass().getName());
    }
    
    public static String toString(final boolean v) {
        return v ? "u_b:1" : "u_b:0";
    }

    public static String toString(final byte v) {
        return "u_y:" + v;
    }

    public static String toString(final char v) {
        return "u_c:" + Character.valueOf(v).hashCode();
    }

    public static String toString(final short v) {
        return "u_s:" + v;
    }

    public static String toString(final int v) {
        return "u_i:" + v;
    }

    public static String toString(final long v) {
        return "u_l:" + v;
    }

    public static String toString(final float v) {
        return "u_f:" + v;
    }

    public static String toString(final double v) {
        return "u_d:" + v;
    }

    public static String toString(final Object o) {
        if (o == null) {
            return "q_o:null";
        } else {
            String _tmp;

            if (o instanceof String) {
                _tmp = "q_t:";
            } else if (o instanceof Throwable) {
                _tmp = "q_h:";
            } else if (o.getClass().isArray()) {
                _tmp = "q_a:";
            } else {
                _tmp = "q_o:";
            }
          
            return _tmp + System.identityHashCode(o);
        }
    }
  
    private final File profileFile;
    private final PrintWriter logWriter;
    private static final Logger LOGGER;
    private volatile boolean clean = false;
    private String prevMsg = "";
    private int msgFreq = 0;

    public enum Action {
        GET,
        PUT
    }
}

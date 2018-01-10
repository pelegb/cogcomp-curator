package edu.illinois.cs.cogcomp.annotation.server;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;

import org.apache.thrift.server.TNonblockingServer.Args;
//import org.apache.thrift.server.THsHaServer.Args;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.illinois.cs.cogcomp.thrift.labeler.Labeler;

/**
 * Abstract class for servers
 */
public abstract class IllinoisAbstractServer {

    protected Logger logger;
    protected int threads;
    protected int port;
    protected String configFile;
    
    protected CommandLine line = null;
    protected CommandLineParser parser = new GnuParser();
    protected HelpFormatter hformat = new HelpFormatter();
    
    protected Class subclass;

    
    public IllinoisAbstractServer(Class c){
        logger = LoggerFactory.getLogger(c);
        threads = 1;
        port = 9090;
        configFile = "";
        subclass = c;
    }
    
    
    public IllinoisAbstractServer(Class c, int in_threads, int in_port, String in_configfile){
        logger = LoggerFactory.getLogger(c);
        threads = in_threads;
        port = in_port;
        configFile = in_configfile;
        subclass = c;
    }

    
    public void parseCommandLine(Options options, String[] args){
        parseCommandLine(options, args, "9090", "1", "", 2);
    }
    
    public void parseCommandLine(Options options, String[] args,
                                 String default_port, String default_threads, String default_config){
        parseCommandLine(options, args, default_port, default_threads, default_config, 2);
    }

    public void parseCommandLine(Options options, String[] args,
                                 String default_port, String default_threads, String default_config,
                                 int zero_threads){
        try {
            line = parser.parse(options, args);
        } catch (ParseException e) {
            logger.error(e.getMessage());
            hformat.printHelp("java " + subclass.getName(),
                    options, true);
            System.exit(1);
        }
        if (line.hasOption("help")) {
            hformat.printHelp("java " + subclass.getName(),
                    options, true);
            System.exit(1);
        }

        port = Integer.parseInt(line.getOptionValue("port", default_port));

        try {
            threads = Integer.parseInt(line.getOptionValue("threads", default_threads));
        } catch (NumberFormatException e) {
            logger.warn("Couldn't interpret {} as a number.",
                    line.getOptionValue("threads"));
        }
        if (threads < 0) {
            threads = 1;
        } else if (threads == 0) {
            threads = zero_threads;
        }

        configFile = line.getOptionValue("config", default_config);
    }

    public void forceThreadsOne(){
        threads = 1;
    }
    
    /*
      * most this code is boiler plate. All you need to modify is createOptions
      * and main to get a server working!
      */
    public static Options createOptions() {
        Option port = OptionBuilder.withLongOpt("port").withArgName("PORT")
                .hasArg().withDescription("port to open server on").create("p");
        Option threads = OptionBuilder.withLongOpt("threads")
                .withArgName("THREADS").hasArg()
                .withDescription("number of threads to run").create("t");
        Option config = OptionBuilder.withLongOpt("config")
                .withArgName("CONFIG").hasArg()
                .withDescription("configuration file").create("c");
        Option help = new Option("h", "help", false, "print this message");
        Options options = new Options();
        options.addOption(port);
        options.addOption(threads);
        options.addOption(config);
        options.addOption(help);
        return options;
    }

    public void runServer(TProcessor processor) {

        TNonblockingServerTransport serverTransport;
        TServer server;
        try {
            serverTransport = new TNonblockingServerSocket(port);

            if (threads == 1) {
                server = new TNonblockingServer(new Args(serverTransport).processor(processor));
		//server = new TNonblockingServer(processor, serverTransport);
            } else {
                //THsHaServer.Options serverOptions = new THsHaServer.Options();
		//serverOptions.workerThreads = threads;
                //server = new THsHaServer(new TProcessorFactory(processor),
                //        serverTransport, new TFramedTransport.Factory(),
                //        new TBinaryProtocol.Factory(), serverOptions);
            	
		THsHaServer.Args args = new THsHaServer.Args(serverTransport);
		args.workerThreads(threads);
		args.processorFactory(new TProcessorFactory(processor));
		args.protocolFactory(new TBinaryProtocol.Factory());
		args.transportFactory(new TFramedTransport.Factory());
		server = new THsHaServer(args);
	    }
            Runtime.getRuntime().addShutdownHook(
                    new Thread(new ShutdownListener(server, serverTransport),
                            "Server Shutdown Listener"));
            logger.info("Starting the server on port {} with {} threads", port,
                    threads);
            server.serve();
        } catch (TTransportException e) {
            logger.error("Thrift Transport error");
            logger.error(e.toString());
            System.exit(1);
        }
    }

    private class ShutdownListener implements Runnable {

        private final TServer server;
        private final TServerTransport transport;

        public ShutdownListener(TServer server, TServerTransport transport) {
            this.server = server;
            this.transport = transport;
        }

        public void run() {
            if (server != null) {
                server.stop();
            }
            if (transport != null) {
                transport.interrupt();
                transport.close();
            }
        }
    }
}

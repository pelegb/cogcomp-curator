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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.illinois.cs.cogcomp.annotation.handler.IllinoisCorefMentionDetectorHandler;
import edu.illinois.cs.cogcomp.thrift.labeler.Labeler;

public class IllinoisCorefMentionDetectorServer
{
 

    private static final String MYNAME = IllinoisCorefMentionDetectorServer.class.getCanonicalName();
    private static Logger logger = LoggerFactory.getLogger(IllinoisCorefMentionDetectorServer.class);


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

    static class ShutdownListener implements Runnable {
        private TServer server;
        private TServerTransport transport;

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



    public static void runServer(TProcessor processor, int port, int threads) {

        TNonblockingServerTransport serverTransport;
        TServer server;
        try {
            serverTransport = new TNonblockingServerSocket(port);

            if (threads == 1) {
                server = new TNonblockingServer(processor, serverTransport);
            } else {
                THsHaServer.Options serverOptions = new THsHaServer.Options();
                serverOptions.workerThreads = threads;
                server = new THsHaServer(new TProcessorFactory(processor),
                                         serverTransport, new TFramedTransport.Factory(),
                                         new TBinaryProtocol.Factory(), serverOptions);
            }
            ShutdownListener listener  = new ShutdownListener(server, serverTransport); 
            Runtime.getRuntime().addShutdownHook(
                                                 new Thread(listener,"Server Shutdown Listener"));
            logger.info("Starting the server on port {} with {} threads", port,
                        threads);
            server.serve();
        } catch (TTransportException e) {
            logger.error("Thrift Transport error");
            logger.error(e.toString());
            System.exit(1);
        }
    }



    /*
     * Takes the config file as the last parameter!!!
     */
    public static void main(String[] args) throws Exception 
    {
        int threads = 2;
        int port = 1;

        String argStr = "";
        
        for ( int i = 0; i < args.length; ++i )
            argStr += args[i] + ";";
        
        logger.info( "## " + MYNAME + ".main(): arguments are: " + argStr );
        
        
        CommandLineParser parser = new GnuParser();
        Options options = createOptions();
        HelpFormatter hformat = new HelpFormatter();
        CommandLine line = null;
        try {
            line = parser.parse(options, args);
        } catch (ParseException e) {
            logger.error(e.getMessage());
            hformat.printHelp("java " + IllinoisCorefMentionDetectorServer.class.getName(),
                              options, true);
            System.exit(1);
        }
        if (line.hasOption("help")) {
            hformat.printHelp("java " + IllinoisCorefMentionDetectorServer.class.getName(),
                              options, true);
            System.exit(1);
        }

        String configFile = line.getOptionValue("config");
        logger.info("**** INITIALIZING WITH THE FILE: "+ configFile);
        port = Integer.parseInt(line.getOptionValue("port"));

        try {
            threads = Integer.parseInt(line.getOptionValue("threads", "2"));
        } catch (NumberFormatException e) {
            logger.warn("Couldn't interpret {} as a number.",
                        line.getOptionValue("threads"));
        }
        if (threads < 0) {
            threads = 1;
        } else if (threads == 0) {
            threads = 2;
        }           
        Labeler.Iface handler = new IllinoisCorefMentionDetectorHandler( configFile );
        Labeler.Processor processor = new Labeler.Processor(handler);

        runServer(processor, port, threads);
    }

}

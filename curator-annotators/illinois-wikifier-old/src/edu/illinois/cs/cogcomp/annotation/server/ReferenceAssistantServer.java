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

import edu.illinois.cs.cogcomp.thrift.labeler.Labeler;

import edu.illinois.cs.cogcomp.annotation.handler.ReferenceAssistantHandler;

import CommonSenseWikifier.ProblemRepresentationDatastructures.ParametersAndGlobalVariables;

public class ReferenceAssistantServer extends IllinoisAbstractServer
{


    /*
     * most this code is boiler plate. All you need to modify is createOptions
     * and main to get a server working!
     */
//    public static Options createOptions() {
//        Option port = OptionBuilder.withLongOpt("port").withArgName("PORT")
//        .hasArg().withDescription("port to open server on").create("p");
//        Option threads = OptionBuilder.withLongOpt("threads")
//        .withArgName("THREADS").hasArg()
//        .withDescription("number of threads to run").create("t");
//        Option config = OptionBuilder.withLongOpt("config")
//        .withArgName("CONFIG").hasArg()
//        .withDescription("configuration file").create("c");
//        Option help = new Option("h", "help", false, "print this message");
//        Options options = new Options();
//        options.addOption(port);
//        options.addOption(threads);
//        options.addOption(config);
//        options.addOption(help);
//        return options;
//    }

//    static class ShutdownListener implements Runnable {
//        private TServer server;
//        private TServerTransport transport;
//
//        public ShutdownListener(TServer server, TServerTransport transport) {
//            this.server = server;
//            this.transport = transport;
//        }
//
//        public void run() {
//            if (server != null) {
//                server.stop();
//            }
//            if (transport != null) {
//                transport.interrupt();
//                transport.close();
//            }
//        }
//    }
    
    

//    public static void runServer(TProcessor processor, int port, int threads) {
//
//        TNonblockingServerTransport serverTransport;
//        TServer server;
//        try {
//            serverTransport = new TNonblockingServerSocket(port);
//
//            if (threads == 1) {
//                server = new TNonblockingServer(processor, serverTransport);
//            } else {
//                THsHaServer.Options serverOptions = new THsHaServer.Options();
//                serverOptions.workerThreads = threads;
//                server = new THsHaServer(new TProcessorFactory(processor),
//                        serverTransport, new TFramedTransport.Factory(),
//                        new TBinaryProtocol.Factory(), serverOptions);
//            }
//            ShutdownListener listener  = new ShutdownListener(server, serverTransport);
//            Runtime.getRuntime().addShutdownHook(
//                    new Thread(listener,"Server Shutdown Listener"));
//            logger.info("Starting the server on port {} with {} threads", port,
//                    threads);
//            server.serve();
//        } catch (TTransportException e) {
//            logger.error("Thrift Transport error");
//            logger.error(e.toString());
//            System.exit(1);
//        }
//    }

    
    
    public ReferenceAssistantServer(Class c){
        super(c);
    }
    
    public ReferenceAssistantServer(Class c, int threads, int port, String configFile){
        super(c, threads, port, configFile);
    }
    
    /*
     * Takes the config file as the last parameter!!!
     */
    public static void main(String[] args) throws Exception {
        /*
            CachingCurator curator = new CachingCurator("grandma.cs.uiuc.edu", 9010, 
                    new String[]{ViewNames.NER, ViewNames.SHALLOW_PARSE, ViewNames.DEPENDENCY, ViewNames.POS},
                            ""  , false, true);
            String text = "Houston, Monday, July 21 -- Men have landed and walked on the moon.  Two Americans, astronauts of Apollo 11, steered their fragile four-legged lunar module safely and smoothly to the historic landing yesterday at 4:17:40 P.M., Eastern daylight time.  Neil A. Armstrong, the 38-year-old civilian commander, radioed to earth and the mission control room here: \"Houston, Tranquility Base here; the Eagle has landed.\" The first men to reach the moon -- Mr. Armstrong and his co-pilot, Col. Edwin E. Aldrin, Jr. of the Air Force -- brought their ship to rest on a level, rock-strewn plain near the southwestern shore of the arid Sea of Tranquility.  About six and a half hours later, Mr. Armstrong opened the landing craft's hatch, stepped slowly down the ladder and declared as he planted the first human footprint on the lunar crust: \"That's one small step for man, one giant leap for mankind.\"";
            TextAnnotation ta = curator.getTextAnnotation(text);
            int N = ta.getTokens().length;
            for (int tid = 0; tid< N ;tid++) {
                Constituent c = new Constituent("", "", ta, tid, tid+1);
                Span entity = new Span(c.getStartCharOffset(), c.getEndCharOffset());
                String key = entity.getStart()+"-"+entity.getEnding();
                System.out.println("Token id="+tid+"; token="+ta.getToken(tid)+"; substring="+
                        text.substring(entity.start,entity.ending)+ "; character span = "+key);
            }
            System.out.println("Done");
            System.exit(0);
         */
//        int threads = 2;
//        int port = 9173;
        ReferenceAssistantServer s = new ReferenceAssistantServer(ReferenceAssistantServer.class, 2, 9173, "");

        Options options = createOptions();

//        CommandLineParser parser = new GnuParser();
//
//        HelpFormatter hformat = new HelpFormatter();
//        CommandLine line = null;
//        try {
//            line = parser.parse(options, args);
//        } catch (ParseException e) {
//            s.logger.error(e.getMessage());
//            hformat.printHelp("java " + ReferenceAssistantServer.class.getName(),
//                              options, true);
//            System.exit(1);
//        }
//        if (line.hasOption("help")) {
//            hformat.printHelp("java " + ReferenceAssistantServer.class.getName(),
//                              options, true);
//            System.exit(1);
//        }
//
//        String configFile = line.getOptionValue("config", "configs/Demo_Config_Deployed.txt");
        
        s.parseCommandLine(options, args, "9173", "2", "configs/Demo_Config_Deployed.txt");
        
        ParametersAndGlobalVariables.curatorServerMachine = s.line.getOptionValue("CuratorMachine", "localhost");
        ParametersAndGlobalVariables.curatorPort = Integer.parseInt(s.line.getOptionValue("CuratorPort", "9999"));
        System.out.println("Hello World!");
        System.out.println("Config file = "+s.configFile);
        System.out.println("**** INITIALIZING WITH THE FILE :"+s.configFile);
//        port = Integer.parseInt(line.getOptionValue("port", "9173"));

//        try {
//            threads = Integer.parseInt(line.getOptionValue("threads", "2"));
//        } catch (NumberFormatException e) {
//            s.logger.warn("Couldn't interpret {} as a number.",
//                        line.getOptionValue("threads"));
//        }
//        if (threads < 0) {
//            threads = 1;
//        } else if (threads == 0) {
//            threads = 2;
//        }           
        Labeler.Iface handler = new ReferenceAssistantHandler(s.configFile );
        Labeler.Processor processor = new Labeler.Processor(handler);

        s.runServer(processor);
    }
}   

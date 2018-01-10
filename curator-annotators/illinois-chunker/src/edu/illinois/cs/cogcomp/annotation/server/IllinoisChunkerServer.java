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

import edu.illinois.cs.cogcomp.annotation.handler.IllinoisChunkerHandler;
import edu.illinois.cs.cogcomp.thrift.labeler.Labeler;

/**
* 
* @author James Clarke
* 
*/
public class IllinoisChunkerServer extends IllinoisAbstractServer{

	/*
	 * most this code is boiler plate. All you need to modify is createOptions
	 * and main to get a server working!
	 */
//	public static Options createOptions() {
//		Option port = OptionBuilder.withLongOpt("port").withArgName("PORT")
//				.hasArg().withDescription("port to open server on").create("p");
//		Option threads = OptionBuilder.withLongOpt("threads")
//				.withArgName("THREADS").hasArg()
//				.withDescription("number of threads to run").create("t");
//		Option config = OptionBuilder.withLongOpt("config")
//				.withArgName("CONFIG").hasArg()
//				.withDescription("configuration file").create("c");
//		Option help = new Option("h", "help", false, "print this message");
//		Options options = new Options();
//		options.addOption(port);
//		options.addOption(threads);
//		options.addOption(config);
//		options.addOption(help);
//		return options;
//	}

    public IllinoisChunkerServer(Class c){
        super(c);
    }
    
    public IllinoisChunkerServer(Class c, int threads, int port, String configFile){
        super(c, threads, port, configFile);
    }
    
	public static void main(String[] args) {
//		int threads = 1;
//		int port = 9090;
//		String configFile = "";
        IllinoisChunkerServer s = new IllinoisChunkerServer(IllinoisChunkerServer.class);


		Options options = createOptions();

//        CommandLineParser parser = new GnuParser();
//		HelpFormatter hformat = new HelpFormatter();
//		CommandLine line = null;
//		try {
//			line = parser.parse(options, args);
//		} catch (ParseException e) {
//			s.logger.error(e.getMessage());
//			hformat.printHelp("java " + IllinoisChunkerServer.class.getName(),
//					options, true);
//			System.exit(1);
//		}
//		if (line.hasOption("help")) {
//			hformat.printHelp("java " + IllinoisChunkerServer.class.getName(),
//					options, true);
//			System.exit(1);
//		}
//
//		port = Integer.parseInt(line.getOptionValue("port", "9090"));
//
//		try {
//			threads = Integer.parseInt(line.getOptionValue("threads", "1"));
//		} catch (NumberFormatException e) {
//			s.logger.warn("Couldn't interpret {} as a number.",
//					line.getOptionValue("threads"));
//		}
//		if (threads < 0) {
//			threads = 1;
//		} else if (threads == 0) {
//			threads = 2;
//		}
		// force threads to 1 because we cannot support more
        
        s.parseCommandLine(options, args, "9090", "1", "");
		s.forceThreadsOne();

//		configFile = line.getOptionValue("config", "");
		Labeler.Iface handler = new IllinoisChunkerHandler(s.configFile);
		Labeler.Processor processor = new Labeler.Processor(handler);

		s.runServer(processor);
	}

//	public static void runServer(TProcessor processor, int port, int threads) {
//
//		TNonblockingServerTransport serverTransport;
//		TServer server;
//		try {
//			serverTransport = new TNonblockingServerSocket(port);
//
//			if (threads == 1) {
//				server = new TNonblockingServer(processor, serverTransport);
//			} else {
//				THsHaServer.Options serverOptions = new THsHaServer.Options();
//				serverOptions.workerThreads = threads;
//				server = new THsHaServer(new TProcessorFactory(processor),
//						serverTransport, new TFramedTransport.Factory(),
//						new TBinaryProtocol.Factory(), serverOptions);
//			}
//			Runtime.getRuntime().addShutdownHook(
//					new Thread(new ShutdownListener(server, serverTransport),
//							"Server Shutdown Listener"));
//			logger.info("Starting the server on port {} with {} threads", port,
//					threads);
//			server.serve();
//		} catch (TTransportException e) {
//			logger.error("Thrift Transport error");
//			logger.error(e.toString());
//			System.exit(1);
//		}
//	}
//
//	private static class ShutdownListener implements Runnable {
//
//		private final TServer server;
//		private final TServerTransport transport;
//
//		public ShutdownListener(TServer server, TServerTransport transport) {
//			this.server = server;
//			this.transport = transport;
//		}
//
//		public void run() {
//			if (server != null) {
//				server.stop();
//			}
//			if (transport != null) {
//				transport.interrupt();
//				transport.close();
//			}
//		}
//	}

}
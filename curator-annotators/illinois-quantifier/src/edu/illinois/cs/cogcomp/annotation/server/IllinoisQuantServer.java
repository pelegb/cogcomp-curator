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

import edu.illinois.cs.cogcomp.annotation.handler.IllinoisQuantHandler;
import edu.illinois.cs.cogcomp.thrift.labeler.Labeler;

/**
 * @author James Clarke, Mark Sammons
 * 
 */
public class IllinoisQuantServer extends IllinoisAbstractServer{

    private static final String NAME = IllinoisQuantServer.class.getCanonicalName();
    private static final Logger logger = LoggerFactory.getLogger( IllinoisQuantServer.class );
    public IllinoisQuantServer(Class c){
        super(c);
    }
    
    public IllinoisQuantServer(Class c, int threads, int port, String configFile){
        super(c, threads, port, configFile);
    }
    
	public static void main(String[] args) {
	    Labeler.Iface quantifier = null;

	    try {
		quantifier = new IllinoisQuantHandler();
	    }
	    catch ( Throwable e )
	    {
		System.err.println( "Error instantiating IllinoisQuantHandler: " + 
				    e.getMessage() );
		System.exit( -1 );
	    }
		Labeler.Processor processor = new Labeler.Processor( quantifier );
        IllinoisQuantServer s = new IllinoisQuantServer(IllinoisQuantServer.class);

        Options options = createOptions();



        s.parseCommandLine(options, args, "9090", "1", "", 25);

	logger.info( NAME + " starting service for IllinoisQuantHandler." );
		s.runServer(processor);
	}


}
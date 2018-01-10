package edu.illinois.cs.cogcomp.annotation.server;

import org.apache.commons.cli.Options;

import edu.illinois.cs.cogcomp.annotation.handler.BerkeleyParserHandler;
import edu.illinois.cs.cogcomp.thrift.labeler.Labeler;
import edu.illinois.cs.cogcomp.thrift.parser.Parser;

public class BerkeleyParserServer extends IllinoisAbstractServer{

	public BerkeleyParserServer(Class c) {
		super(c);
	}
	
	public BerkeleyParserServer(Class c, int threads, int port, String configFile){
		super(c, threads, port, configFile);
	}
	
	public static void main(String[] args){
		BerkeleyParserServer s = new BerkeleyParserServer(BerkeleyParserServer.class);
		
		Options options = createOptions();
		s.parseCommandLine(options, args, "16000", "1", "configs/berkeleyparser.config");
		
		Parser.Iface handler = new BerkeleyParserHandler(s.configFile);
		Parser.Processor processor = new Parser.Processor(handler);
		
		s.runServer(processor);
	}

}

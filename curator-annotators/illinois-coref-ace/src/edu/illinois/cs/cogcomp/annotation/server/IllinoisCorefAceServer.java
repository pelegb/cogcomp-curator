package edu.illinois.cs.cogcomp.annotation.server;

import org.apache.commons.cli.Options;
import org.apache.thrift.TException;

import edu.illinois.cs.cogcomp.annotation.handler.IllinoisCorefAceHandler;
import edu.illinois.cs.cogcomp.thrift.cluster.ClusterGenerator;
import edu.illinois.cs.cogcomp.thrift.labeler.Labeler;

public class IllinoisCorefAceServer extends IllinoisAbstractServer
{
    public IllinoisCorefAceServer(Class c){
        super(c);
    }
    
    public IllinoisCorefAceServer(Class c, int threads, int port, String configFile){
        super(c, threads, port, configFile);
    }
    
	public static void main(String[] args) {
        IllinoisCorefAceServer s = new IllinoisCorefAceServer(IllinoisCorefAceServer.class);


		Options options = createOptions();

        
        s.parseCommandLine(options, args, "9090", "1", "");
		s.forceThreadsOne();

//		configFile = line.getOptionValue("config", "");
		ClusterGenerator.Iface handler = null;
        try
        {
	        handler = new IllinoisCorefAceHandler(s.configFile);
        }
        catch ( TException e )
        {
	        e.printStackTrace();
	        System.exit( -1 );
        }
		ClusterGenerator.Processor processor = new ClusterGenerator.Processor(handler);

		s.runServer(processor);
	}


}

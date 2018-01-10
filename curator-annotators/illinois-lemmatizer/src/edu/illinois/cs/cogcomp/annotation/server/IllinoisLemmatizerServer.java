package edu.illinois.cs.cogcomp.annotation.server;

import java.io.FileNotFoundException;
import java.io.IOException;

import net.didion.jwnl.JWNLException;


import org.apache.commons.cli.Options;

import edu.illinois.cs.cogcomp.annotation.handler.IllinoisLemmatizerHandler;
import edu.illinois.cs.cogcomp.thrift.labeler.Labeler;
import edu.illinois.cs.cogcomp.thrift.labeler.Labeler.Iface;

/**
 * requires a configuration file that specifies the name of a jwnl properties file
 * 
 * @author mssammon
 *
 */

public class IllinoisLemmatizerServer extends IllinoisAbstractServer
{

    private static final String NAME = null;

    public IllinoisLemmatizerServer( Class< IllinoisLemmatizerServer > c )
    {
        super( c );
    }
    
    public IllinoisLemmatizerServer( Class c,
                                     int numThreads,
                                     int port,
                                     String configFile )
    {
        super( c, numThreads, port, configFile );
    }

    
    public static void main( String[] args )
    {
        IllinoisLemmatizerServer server = new IllinoisLemmatizerServer( IllinoisLemmatizerServer.class );

        Options options = createOptions();
    
        server.parseCommandLine(options, args, "", "1", "");

        Labeler.Iface handler = null;
        
        try
        {
            handler = new IllinoisLemmatizerHandler( server.configFile );
        }
        catch ( FileNotFoundException e )
        {
            handleError( FileNotFoundException.class, "main", e );
        }
        catch ( JWNLException e )
        {
            handleError( JWNLException.class, "main", e );
        }
        catch ( IOException e )
        {
            handleError( IOException.class, "main", e );
        }
        
        Labeler.Processor< Iface > processor = new Labeler.Processor< Iface >( handler );
    
        server.runServer( processor );
    }

    
    private static void handleError( Class class_,
                                     String methodName_, 
                                     Exception e_
                                     )
    {
        System.err.println( "ERROR: " + NAME + "." + methodName_ + ": " +
                            "exception of type " + class_.getName() + " thrown: " +
                            e_.getMessage()
                            );
        e_.printStackTrace();
        System.exit( -1 );
    }
}

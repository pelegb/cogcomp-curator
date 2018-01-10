package edu.illinois.cs.cogcomp.annotation.handler;

import java.io.FileNotFoundException;
import java.io.IOException;

import net.didion.jwnl.JWNLException;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.illinois.cs.cogcomp.core.utilities.ResourceManager;
import edu.illinois.cs.cogcomp.nlp.lemmatizer.AugmentedLemmatizer;
import edu.illinois.cs.cogcomp.thrift.base.AnnotationFailedException;
import edu.illinois.cs.cogcomp.thrift.base.Labeling;
import edu.illinois.cs.cogcomp.thrift.curator.Record;
import edu.illinois.cs.cogcomp.thrift.labeler.Labeler;

/**
 * wraps Illinois Lemmatizer for use as Curator Component. 
 * The Lemmatizer adds a view "illinois_lemma" to the input record.
 * Requires sentence segmentation, tokenization, and POS.
 * 
 * @author mssammon
 *
 */

public class IllinoisLemmatizerHandler extends IllinoisAbstractHandler implements Labeler.Iface
{
    private static final String NAME = IllinoisLemmatizerHandler.class.getCanonicalName();
    private static final String PUBLIC_NAME = "IllinoisLemmatizer";
    private static final String VERSION = "0.3";
    
    
    private final Logger logger = LoggerFactory.getLogger( IllinoisLemmatizerHandler.class );
    
    public IllinoisLemmatizerHandler( String configFile_ ) throws FileNotFoundException, JWNLException, IOException
    {
        this( new ResourceManager( configFile_ ) );
    }


    
    public IllinoisLemmatizerHandler( ResourceManager rm_ ) throws IOException, JWNLException
    {
        super( PUBLIC_NAME, VERSION, PUBLIC_NAME + "-" + VERSION );
        
        AugmentedLemmatizer.init( rm_ );
        
    }
    


    public Labeling labelRecord( Record record ) 
        throws AnnotationFailedException, TException
    {
        Labeling lemmaView = null;
	String errMsg = null;        
        try 
        {
            lemmaView  = AugmentedLemmatizer.createLemmaRecordView( record, "Curator", "lemmatizer" );
        }
        catch ( JWNLException e )
        {
	        e.printStackTrace();
	        errMsg = e.getMessage();
        }
        catch ( IOException e )
        {
	        e.printStackTrace();
	        errMsg = e.getMessage();
        }

        if ( null != errMsg )
        {
        	throw new AnnotationFailedException( "ERROR: " + NAME + ".labelRecord(): " +
        			"caught exception while requesting AugmentedLemmatizer view: " + 
        			errMsg );
        }

	logger.debug( "Created lemmaView. Returning labeling with "  + lemmaView.getLabels().size() + " spans." );


        return lemmaView;
    }

}

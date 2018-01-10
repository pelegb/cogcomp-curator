package edu.illinois.cs.cogcomp.annotation.handler;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;


import LBJ2.learn.LinearThresholdUnit;

import edu.illinois.cs.cogcomp.curator.corefUtils.CorefConstants;
import edu.illinois.cs.cogcomp.curator.corefUtils.CuratorUtils;
import edu.illinois.cs.cogcomp.edison.data.curator.CuratorDataStructureInterface;
import edu.illinois.cs.cogcomp.edison.sentences.Constituent;
import edu.illinois.cs.cogcomp.edison.sentences.SpanLabelView;
import edu.illinois.cs.cogcomp.edison.sentences.TextAnnotation;
import edu.illinois.cs.cogcomp.edison.sentences.View;
import edu.illinois.cs.cogcomp.lbj.coref.decoders.*;
import edu.illinois.cs.cogcomp.lbj.coref.ir.Chunk;
import edu.illinois.cs.cogcomp.lbj.coref.ir.Mention;
import edu.illinois.cs.cogcomp.lbj.coref.ir.examples.CExample;
import edu.illinois.cs.cogcomp.lbj.coref.ir.docs.Doc;
import edu.illinois.cs.cogcomp.lbj.coref.ir.docs.DocPlainText;
import edu.illinois.cs.cogcomp.lbj.coref.ir.docs.DocTextAnnotation;
import edu.illinois.cs.cogcomp.lbj.coref.ir.solutions.ChainSolution;
import edu.illinois.cs.cogcomp.lbj.coref.learned.*;
import edu.illinois.cs.cogcomp.lbj.coref.util.aux.Constants;
import edu.illinois.cs.cogcomp.lbj.coref.main.Tune_parameter;
import edu.illinois.cs.cogcomp.lbj.coref.main.CorefModelsManagerSaverLoader;

import edu.illinois.cs.cogcomp.ms.util.EdisonInterface;
import java.util.Properties;


import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.illinois.cs.cogcomp.thrift.base.AnnotationFailedException;
import edu.illinois.cs.cogcomp.thrift.base.Clustering;
import edu.illinois.cs.cogcomp.thrift.base.Labeling;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.thrift.curator.Record;
import edu.illinois.cs.cogcomp.thrift.cluster.ClusterGenerator;



public class IllinoisCoreferenceHandler implements ClusterGenerator.Iface 
{
    private static final String m_NAME = "IllinoisCoreference";
    private static final String m_VERSION = "2.0.1";
    private static final String MYNAME = IllinoisCoreferenceHandler.class.getCanonicalName();
    
    private String nerfield = "ner";
//  private String tokensfield = "tokens";
//  private String sentencesfield = "sentences";
    private String posfield = "pos";
    private String chunkfield = "chunk";
    private String stanfordparsefield = "stanfordParse";
    private LinearThresholdUnit m_corefClassifier;
    private BestLinkDecoder m_decoder;
    
    static final private Logger logger = LoggerFactory.getLogger( IllinoisCoreferenceHandler.class );
    private static final boolean DEBUG = true;
    private static final boolean USE_RECORD_FOR_MENTIONS = false;
    
    public IllinoisCoreferenceHandler() throws Exception
    {

	String[] prop = new String[2];
	prop[0] = "-props";
	prop[1] = "initCoNLLV1.config";
	
	// SetUp the Config File
	// Please Make Sure in the Config File
	// DONOTHING = true
	Tune_parameter.ConfigSystem(prop);
	m_corefClassifier = CorefModelsManagerSaverLoader.emnlp8CoNLL;
	m_corefClassifier.setThreshold(Tune_parameter.getCorefThreshold());
	
        m_decoder = new BestLinkDecoder(m_corefClassifier);


	Properties p = System.getProperties();

	System.err.println("## " + MYNAME + ":java.class.path now = " + p.getProperty("java.class.path", null) );
    }
    
    @Override
    public boolean ping() throws TException
    {
        return true; // it LIIIIIIIVVVVEEEESSSS
    }

    @Override
    public String getName() throws TException
    {
        return m_NAME;
    }

    @Override
    public String getVersion() throws TException
    {
        return m_VERSION;
    }

    @Override
    public String getSourceIdentifier() throws TException
    {
        return getName() + "-" + getVersion();
    }

    
    
    @Override
    public Clustering
            clusterRecord( Record record ) throws AnnotationFailedException,
                                          TException
    {
        // Add the following two lines if want to use with constraints
        /*decoder.instantiateConstraints();
        decoder.useConstraint(true);*/
        
        
        TextAnnotation ta = getTextAnnotationFromRecord( record );
                
        if ( DEBUG )
        {
            StringBuilder bldr = new StringBuilder( "## " );
            bldr.append( MYNAME );
            bldr.append( ": TA from record has mention view: " );
            View mentView = ta.getView( Constants.PRED_MENTION_VIEW );
            
            for ( Constituent c: mentView.getConstituents() )
            {
                bldr.append( EdisonInterface.printConstituent( c ) );
                bldr.append( "\n" );
            }
            
            logger.debug( bldr.toString() );
        }
        
        Doc doc = new DocPlainText(ta);
       //         new DocTextAnnotation();
        
//        View mentionView = CuratorDataStructureInterface
            //doc.getTextAnnotation().getView(Constants.PRED_MENTION_VIEW);

        if ( DEBUG )
        {
            System.err.println( "## " + MYNAME + ":TA sanity check: mention view built from record is: " );
            taSanityCheck( ta );
        }

        List< Mention > mentions = new LinkedList< Mention >();
        
        List< Span> mentLabels = record.labelViews.get( CorefConstants.MENTION ).getLabels();

        for ( Span s : mentLabels )
            mentions.add( CuratorUtils.spanToMention( s, doc ) );
 
        if ( USE_RECORD_FOR_MENTIONS )
            doc.setPredictedMentions( mentions );
        else
            ( ( DocTextAnnotation ) doc ).setPredictMentionsFromTA();
        
//        /* Set up the predicting mention */ 
//        ((DocTextAnnotation) doc).setPredictMentionsFromTA();
    
        if ( DEBUG )
        {
            System.err.println( "## Set predicted mentions. Now calling coref decoder..." );
        }


        /* Do Coref */


        ChainSolution<Mention> sol = m_decoder.decode(doc);
	
	for(int i=0; i< 5; i++){
		for(int j=0; j<i; j++){
			Mention a = doc.getMention(j);
			Mention m = doc.getMention(i);
			CExample ex = doc.getCExampleFor(a,m);
			System.out.println(a + " " + m +  " scores:" + m_corefClassifier.scores(ex).get("true"));
	
		}
	}
	    

        if ( DEBUG )
            System.err.println( "## finished decoding... setting predicted entities..." );
        
        /* Set up the result, it will generate a PRED_COREF_VIEW*/
        doc.setPredEntities(sol);
        boolean showPOS = true;
        String corefOut = doc.toAnnotatedString( showPOS );
        
        if ( DEBUG )
            System.err.println( "COREF RESULT: " + corefOut );
        
        Clustering result = CuratorUtils.clusteringFromDoc( doc, sol, getSourceIdentifier() );

        return result;
    }

    
    
   
    private TextAnnotation getTextAnnotationFromRecord( Record record ) throws AnnotationFailedException
    {
        checkAvailableViews( record );
        TextAnnotation ta = CuratorDataStructureInterface.getTextAnnotationViewsFromRecord("", "", record);

        CuratorUtils.addMentionViewToTextAnnotation( record, ta );
        
        return ta;
    }

    @Override
    public Clustering clusterRecords( List< Record > records ) 
    throws AnnotationFailedException, TException
    {
        throw new AnnotationFailedException( MYNAME + ".clusterRecords(): NOT IMPLEMENTED" );
//        return null;
    }



    protected void checkAvailableViews(Record record)
    throws AnnotationFailedException 
    {
        if (!record.isSetLabelViews()
                || !record.getLabelViews().containsKey( posfield )) {
            throw new AnnotationFailedException("MentionDetectorHandler::checkAvailableViews(): " +
            "Unable to find POS view in the input record");
        }

        if (!record.isSetLabelViews()
                || !record.getLabelViews().containsKey( chunkfield )) {
            throw new AnnotationFailedException("MentionDetectorHandler::checkAvailableViews(): " +
            "Unable to find chunk view in the input record");
        }

        if (!record.isSetParseViews())
            throw new AnnotationFailedException("MentionDetectorHandler::checkAvailableViews(): " +
            "Unable to find parse view in the input record");

        if (!record.getParseViews().containsKey( stanfordparsefield ) ) {
            throw new AnnotationFailedException("MentionDetectorHandler::checkAvailableViews(): " +
                                                "Unable to find Stanford parse view in the input record"
                                                + ". Expecting stanfordParse.");
        }

    }
    


    
    protected void taSanityCheck( TextAnnotation ta_ )
    {
        View view = ta_.getView( Constants.PRED_MENTION_VIEW );
        
        String text = ta_.getText();
        int i = 0;
        
        for ( Constituent c : view.getConstituents() )
        {
            int start = c.getStartCharOffset();
            int end = c.getEndCharOffset();
            int tokStart = c.getStartSpan();
            int tokEnd = c.getEndSpan();
            
            System.err.println( "Constituent " + ( i++ ) + ", id '" + c.getLabel() +
                                "': char offsets: " + start + ", " + end + 
                                "; token offsets: " + tokStart + ", " + tokEnd + 
                                ";text: " + text.substring( start, end ) );
        }
        System.err.println( "----" );
    }
    
   

}

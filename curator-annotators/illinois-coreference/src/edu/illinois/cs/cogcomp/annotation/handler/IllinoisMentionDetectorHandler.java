package edu.illinois.cs.cogcomp.annotation.handler;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TreeMap;

import edu.illinois.cs.cogcomp.edison.data.curator.CuratorDataStructureInterface;
import edu.illinois.cs.cogcomp.edison.sentences.Constituent;
import edu.illinois.cs.cogcomp.edison.sentences.TextAnnotation;
import edu.illinois.cs.cogcomp.edison.sentences.View;
import edu.illinois.cs.cogcomp.lbj.coref.decoders.*;
import edu.illinois.cs.cogcomp.lbj.coref.ir.docs.Doc;
import edu.illinois.cs.cogcomp.lbj.coref.ir.docs.DocPlainText;
import edu.illinois.cs.cogcomp.lbj.coref.learned.*;
import edu.illinois.cs.cogcomp.lbj.coref.util.aux.Constants;


import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.illinois.cs.cogcomp.thrift.base.AnnotationFailedException;
import edu.illinois.cs.cogcomp.thrift.base.Labeling;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.thrift.curator.Record;
import edu.illinois.cs.cogcomp.thrift.labeler.Labeler;

public class IllinoisMentionDetectorHandler implements Labeler.Iface
{
    private static final String m_SUGGESTED_FIELD_NAME = CorefConstants.MENTION;

    private String m_version;
    
    private String nerfield = "ner";
    private String tokensfield = "tokens";
    private String sentencesfield = "sentences";
    private String posfield = "pos";
    private String chunkfield = "chunk";
    private String stanfordparsefield = "stanfordParse";
    
    private MentionDecoder m_mdDec;


    private static Logger logger = LoggerFactory.getLogger(IllinoisMentionDetectorHandler.class);

    
    public IllinoisMentionDetectorHandler( ) throws TException, IOException
    {
	this( "" );
    }

    public IllinoisMentionDetectorHandler( String config_ ) throws TException, IOException
    {
        if (config_.trim().equals("")) {
            config_ = "configs/coref.properties";
        }
        Properties config = new Properties();
        try {
            FileInputStream in = new FileInputStream( config_ );
            config.load(new BufferedInputStream(in));
            in.close();
        } catch (IOException e) {
            logger.warn( "Error reading configuration file {}; using defaults.", config_ );
        }
        m_version = "1.0";
        
        tokensfield = config.getProperty("tokens.field", "tokens");
        sentencesfield = config.getProperty("sentences.field", "sentences");
        posfield = config.getProperty("pos.field", "sentences");
	chunkfield = config.getProperty( "chunk.field", "chunk" );
	nerfield = config.getProperty("ner.field", "ner");
	stanfordparsefield = config.getProperty( "stanfordparse.field", "stanfordparse" );
//        m_version = config.getProperty( "version" );
//        
//        if ( null == m_version )
//            throw new TException( "ERROR: MentionDetectorHandler(): " +
//                                  "no version specified in config file '" + config_ + "'." );
        
	m_mdDec = new MentionDecoderOntonote(); 

       logger.info( this.getSourceIdentifier() + " is now instantiated." );
    }

    @Override
    public boolean ping() throws TException
    {
        return true;
    }

    @Override
    public String getName() throws TException
    {
        return "IllinoisCorefMentionDetector";
    }

    @Override
    public String getVersion() throws TException
    {
        return m_version;
    }

    @Override
    public String getSourceIdentifier() throws TException
    {
        return getName() + "-" + getVersion();
    }

    
    public String getSuggestedFieldName()
    {
        return m_SUGGESTED_FIELD_NAME;
    }
    
    @Override
    public Labeling
            labelRecord( Record record ) throws AnnotationFailedException,
                                          TException
    {
//         Emnlp8 corefClassifier = new Emnlp8();
//         corefClassifier.setThreshold(-16);
//         BestLinkDecoder decoder = new BestLinkDecoder(corefClassifier);
//         decoder.useConstraint(false);
        // Add the following two lines if want to use with constraints  
        /*decoder.instantiateConstraints();
        decoder.useConstraint(true);*/

        checkAvailableViews( record );
        
        TextAnnotation ta = CuratorDataStructureInterface.getTextAnnotationViewsFromRecord("", "", record);

        Doc doc = new DocPlainText(ta);
        
        /* mdDec.decode() generates a PRED_MENTION_VIEW */ 
        m_mdDec.decode(doc);
        View mentionView = doc.getTextAnnotation().getView(Constants.PRED_MENTION_VIEW);
        
        List< Span > labels = new LinkedList<Span>();
        
        for (Constituent c : mentionView ) 
        {
            Span span = new Span();
            span.setStart( c.getStartCharOffset() );
            span.setEnding(c.getEndCharOffset() );
 
            if ( !span.getLabel().equals("NONE") )
                span.setLabel( span.getLabel() );
            
            
            TreeMap< String, String > attMap = new TreeMap< String, String > ();

            for ( String key : c.getAttributeKeys() )
            {
                String att = c.getAttribute( key );


                
                attMap.put( key, att );
            }            
                
//              if ( !( "NONE".equalsIgnoreCase( m.getEntityType() ) ) ) {
//              attMap.put( "ENTITY_TYPE", m.getEntityType() );
//              attMap.put( "COARSE_ENTITY_TYPE", m.getEntityType() );
//          }
//          else if ( "PRO".equalsIgnoreCase( m.getType() ) )  {
//              attMap.put( "ENTITY_TYPE", "COREF_PRONOUN" );
//          }
//          else {
//              attMap.put( "ENTITY_TYPE", "COREF_NON_NE_MENTION" );
//          }

            span.setAttributes( attMap );

            labels.add(span);

        }
        Labeling labeling = new Labeling();
        
        labeling.setLabels(labels);

        return labeling;
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

        if (!record.isSetLabelViews()
                || !record.getLabelViews().containsKey( nerfield )) {
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


}

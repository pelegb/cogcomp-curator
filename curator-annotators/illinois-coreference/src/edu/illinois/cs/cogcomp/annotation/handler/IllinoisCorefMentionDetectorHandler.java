package edu.illinois.cs.cogcomp.annotation.handler;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.LinkedList;
import java.util.Properties;
import java.util.TreeMap;

import edu.illinois.cs.cogcomp.curator.corefUtils.CorefConstants;
import edu.illinois.cs.cogcomp.curator.corefUtils.CuratorUtils;
import edu.illinois.cs.cogcomp.edison.data.curator.CuratorDataStructureInterface;
import edu.illinois.cs.cogcomp.edison.sentences.Constituent;
import edu.illinois.cs.cogcomp.edison.sentences.TextAnnotation;
import edu.illinois.cs.cogcomp.edison.sentences.View;
import edu.illinois.cs.cogcomp.lbj.coref.decoders.*;
import edu.illinois.cs.cogcomp.lbj.coref.ir.Mention;
import edu.illinois.cs.cogcomp.lbj.coref.ir.docs.Doc;
import edu.illinois.cs.cogcomp.lbj.coref.ir.docs.DocPlainText;
import edu.illinois.cs.cogcomp.lbj.coref.learned.*;
import edu.illinois.cs.cogcomp.lbj.coref.util.aux.Constants;
import edu.illinois.cs.cogcomp.ms.util.EdisonInterface;


import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.illinois.cs.cogcomp.thrift.base.AnnotationFailedException;
import edu.illinois.cs.cogcomp.thrift.base.Labeling;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.thrift.curator.Record;
import edu.illinois.cs.cogcomp.thrift.labeler.Labeler;

public class IllinoisCorefMentionDetectorHandler implements Labeler.Iface
{
    private static final String m_SUGGESTED_FIELD_NAME = CorefConstants.MENTION;

    private static final String MYNAME = IllinoisCorefMentionDetectorHandler.class.getCanonicalName();

    private static final boolean DEBUG = true;

    private static final String CURATOR_NAME = "IllinoisCorefMentionDetector";

    private static final String VERSION = "1.0.1";

    
    private String nerfield = "ner";
    private String posfield = "pos";
    private String chunkfield = "chunk";
    private String stanfordparsefield = "stanfordParse";
    
    private static Logger logger = LoggerFactory.getLogger(IllinoisCorefMentionDetectorHandler.class);

    MentionDecoder m_mdDec;

    
    public IllinoisCorefMentionDetectorHandler( String config_ ) throws TException, IOException
    {
//        if (config_.trim().equals("")) {
//            config_ = "configs/coref.properties";
//        }

        
        Properties config = new Properties();
        try {
            FileInputStream in = new FileInputStream( config_ );
            config.load(new BufferedInputStream(in));
            in.close();
        } catch (IOException e) {
            String msg = "ERROR: " + MYNAME + ".IllinoisCorefMentionDetectorHandler(): " + 
                    "couldn't read config file '" + config_ + "': " + e.getMessage();
            logger.error( msg );
            throw e;
        }
        
//        tokensfield = config.getProperty("tokens.field", "tokens");
//        sentencesfield = config.getProperty("sentences.field", "sentences");
        posfield = config.getProperty("pos.field", "sentences");
//        nerfield = config.getProperty("ner.field", "ner");
//        m_version = config.getProperty( "version" );
//        
//        if ( null == m_version )
//            throw new TException( "ERROR: MentionDetectorHandler(): " +
//                                  "no version specified in config file '" + config_ + "'." );
        
        logger.info( this.getSourceIdentifier() + " is now instantiated." );
        
        m_mdDec = new MentionDecoderOntonote();

    }

    @Override
    public boolean ping() throws TException
    {
        return true;
    }

    @Override
    public String getName() throws TException
    {
        return CURATOR_NAME;
    }

    @Override
    public String getVersion() throws TException
    {
        return VERSION;
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
        // Add the following two lines if want to use with constraints  
        /*decoder.instantiateConstraints();
        decoder.useConstraint(true);*/
        checkAvailableViews( record );
        
        TextAnnotation ta = CuratorDataStructureInterface.getTextAnnotationViewsFromRecord("", "", record);

        Doc doc = new DocPlainText(ta);
        
        /* mdDec.decode() generates a PRED_MENTION_VIEW */ 
        m_mdDec.decode(doc);
        
        if ( DEBUG )
        {
            View mentionView = doc.getTextAnnotation().getView(Constants.PRED_MENTION_VIEW);

            StringBuilder bldr = new StringBuilder( "PRED_MENTION_VIEW:\n" );
            for ( Constituent c: mentionView.getConstituents() )
            {
                bldr.append( EdisonInterface.printConstituent( c ) );
                bldr.append( "\n" );
            }
            bldr.append( "###############################" );

            System.err.println( bldr.toString() );
        }
        
//        
//        Labeling labeling = CuratorUtils.createLabelingFromMentionView( mentionView );
//        
        List< Mention > mentions = doc.getMentions();

        if ( DEBUG )
        {
            StringBuilder bldr = new StringBuilder( "Mentions:\n" );
            for ( Mention m: mentions )
            {
                bldr.append( m.toFullString() );
                bldr.append( "\n" );
            }
            bldr.append( "################################" );
        }
        
        Labeling labeling = new Labeling();
        
        for ( Mention m: mentions )
        {
            m.m_isTrueMention = false;
            labeling.addToLabels( CuratorUtils.mentionToSpan( m, ta ) );
        }
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

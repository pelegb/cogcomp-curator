package edu.illinois.cs.cogcomp.annotation.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import LBJ2.learn.Learner;
import edu.illinois.cs.cogcomp.core.utilities.ResourceManager;
import edu.illinois.cs.cogcomp.edison.annotators.GazetteerViewGenerator;
import edu.illinois.cs.cogcomp.edison.data.curator.CuratorDataStructureInterface;
import edu.illinois.cs.cogcomp.edison.sentences.TextAnnotation;
import edu.illinois.cs.cogcomp.lbj.coref.Parameters;
import edu.illinois.cs.cogcomp.lbj.coref.constraints.Constraint;
import edu.illinois.cs.cogcomp.lbj.coref.constraints.IdenticalDetNom;
import edu.illinois.cs.cogcomp.lbj.coref.constraints.IdenticalProperName;
import edu.illinois.cs.cogcomp.lbj.coref.constraints.SameEntityExtendSpanConstraints;
import edu.illinois.cs.cogcomp.lbj.coref.decoders.BIODecoder;
import edu.illinois.cs.cogcomp.lbj.coref.decoders.BestLinkDecoderPronouns;
import edu.illinois.cs.cogcomp.lbj.coref.decoders.ExtendHeadsDecoder;
import edu.illinois.cs.cogcomp.lbj.coref.decoders.MentionDecoder;
import edu.illinois.cs.cogcomp.lbj.coref.features.EntityTypeFeatures;
import edu.illinois.cs.cogcomp.lbj.coref.ir.Chunk;
import edu.illinois.cs.cogcomp.lbj.coref.ir.Mention;
import edu.illinois.cs.cogcomp.lbj.coref.ir.docs.Doc;
import edu.illinois.cs.cogcomp.lbj.coref.ir.docs.DocPlainText;
import edu.illinois.cs.cogcomp.lbj.coref.ir.solutions.ChainSolution;
import edu.illinois.cs.cogcomp.lbj.coref.ir.solutions.MentionSolution;
import edu.illinois.cs.cogcomp.lbj.coref.learned.MDExtendHeads;
import edu.illinois.cs.cogcomp.lbj.coref.learned.MTypePredictor;
import edu.illinois.cs.cogcomp.lbj.coref.learned.MentionDetectorMyBIOHead;
import edu.illinois.cs.cogcomp.lbj.coref.learned.aceCorefSPLearner;
import edu.illinois.cs.cogcomp.lbj.coref.postProcessing.PosConstarintPostProcessing;
import edu.illinois.cs.cogcomp.thrift.base.AnnotationFailedException;
import edu.illinois.cs.cogcomp.thrift.base.Clustering;
import edu.illinois.cs.cogcomp.thrift.base.Labeling;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.thrift.cluster.ClusterGenerator;
import edu.illinois.cs.cogcomp.thrift.curator.Record;
import edu.illinois.cs.cogcomp.thrift.labeler.Labeler.Iface;

/**
 * @author Mark Sammons
 * 
 */

public class IllinoisCorefAceHandler extends IllinoisAbstractHandler implements ClusterGenerator.Iface {

	private static final String NAME = IllinoisCorefAceHandler.class.getCanonicalName();
	private static final String VERSION = "1.5";
	private static final String PUBLIC_NAME = "illinois-coref-ace";
	private static final String CONSTRAINT_CONFIG_FILE = "constraintConfigFile";
	
	private Logger logger = LoggerFactory.getLogger(IllinoisCorefAceHandler.class);

	private double corefThreshold = -8;
	private double pronounThreshold = -8;
	private String aceCorefModel = null;

	private String pathToGoldHeadLastWordPairCount = "countTable/CoNLL12_TRAIN_ENGLISH_All_AUTO_headLastWordCount";
	private String PreWordCount = null;
	private String pathToNonReferentialMention = "countTable/nonReferentialMention_CoNLL12_train";
	private String pathToNestedRepeatedMention = "countTable/nestedRepeatedMention_CoNLL12_train";

	private Learner corefClassifier;

	private Learner pronounClassifier;

	MentionDecoder mentionDecoder =
			new ExtendHeadsDecoder(new MDExtendHeads(),
					new BIODecoder(new MentionDetectorMyBIOHead()));
	MTypePredictor mentionTyper = new MTypePredictor();

	private BestLinkDecoderPronouns decoderPronouns;

	private GazetteerViewGenerator gazetteerManager;

	



	
	private String nerfield = "ner";
//	private String tokensfield = "tokens";
//	private String sentencesfield = "sentences";
//	private String posfield = "pos";
//	private RemoveSingletonProcessing removeSingleton;
//	private NestedPostProcessing removeNested;
	private PosConstarintPostProcessing posConstraintPosProcessor;
	private ArrayList< Constraint > posConPool;

	public IllinoisCorefAceHandler() throws TException {
		super( "", "", "" );
		throw new TException( NAME + " no-arg constructor: need config argument." );
	}

	public IllinoisCorefAceHandler(String configFilename) throws TException {

        super( PUBLIC_NAME, VERSION, PUBLIC_NAME + "-" + VERSION );

		if (configFilename.trim().equals("")) {
			configFilename = "configs/coref.properties";
		}
		ResourceManager config;
        try
        {
	        config = new ResourceManager( configFilename );
        }
        catch ( IOException e )
        {
        	logger.error( e.getMessage() );
        	e.printStackTrace();
        	throw new TException( e.getMessage() );
        }

//		tokensfield = config.getString("tokens.field", "tokens");
//		sentencesfield = config.getString("sentences.field", "sentences");
//		posfield = config.getString("pos.field", "sentences");
//		nerfield = config.getString("ner.field", "ner");
		loadCorefSystem( config );

		logger.info( this.getName() + ", version " + this.getVersion() + " is now instantiated." );
	}

	private void loadCorefSystem( ResourceManager config ) throws TException 
	{
		Parameters.pathToGoldHeadLastWordPairCount = config.getString("pathToGoldHeadLastWordPairCount",pathToGoldHeadLastWordPairCount);
		Parameters.pathToNonReferentialMention = config.getString("pathToNonReferentialMention", pathToNonReferentialMention);
		Parameters.pathToNestedRepeatedMention = config.getString("pathToNestedRepeatedMention", pathToNestedRepeatedMention);
		Parameters.PreWordCount = config.getString("PREWORDCOUNT", PreWordCount);

		String constraintConfigFile = config.getString( CONSTRAINT_CONFIG_FILE );

		corefClassifier = null;
		pronounClassifier = null;
		
		if(aceCorefModel != null) {
			corefClassifier = new aceCorefSPLearner(aceCorefModel+".lc", aceCorefModel+".lex");
		}

		if(corefClassifier == null)
			corefClassifier = new aceCorefSPLearner();
		
		mentionDecoder =
				new ExtendHeadsDecoder(new MDExtendHeads(),
						new BIODecoder(new MentionDetectorMyBIOHead()));
		mentionTyper = new MTypePredictor();

		decoderPronouns= new BestLinkDecoderPronouns(corefClassifier,pronounClassifier);
		decoderPronouns.setThreshold(corefThreshold);
		decoderPronouns.setPronounThreshold(pronounThreshold);

		posConstraintPosProcessor = new PosConstarintPostProcessing();
		posConPool = new ArrayList<Constraint>();
		try
        {
	        posConPool.add(new SameEntityExtendSpanConstraints( constraintConfigFile ));
	        posConPool.add(new IdenticalDetNom( constraintConfigFile ));
	        posConPool.add(new IdenticalProperName( constraintConfigFile ));
        }
        catch ( Exception e )
        {
	        e.printStackTrace();
	        String msg =  "ERROR: " + NAME + ".loadCorefSystem(): " + e.getMessage();
	        logger.error( msg );
	        throw new TException( msg );
        }
		corefThreshold = config.getDouble("CorefThreshold");

		String msg = "COREF Threshold: " + Double.toString( corefThreshold );
		logger.debug( msg );

		logger.info( "instantiating gazetteerManager." );
		gazetteerManager = GazetteerViewGenerator.gazetteersInstance;

//		removeSingleton = new RemoveSingletonProcessing();
//		
//		removeNested = new NestedPostProcessing();

	}

//	public boolean ping() throws TException {
//		return true;
//	}
//
//	public String getName() throws TException {
//		return "Illinois Coreference Resolver";
//	}
//
//	public String getVersion() throws TException {
//		return "0.2";
//	}

	/**
	 * @param doc
	 * @return
	 * @throws TException
	 */
	private synchronized Clustering corefDoc(Doc d) throws TException 
	{
		long startTime = System.currentTimeMillis();
	
		logger.info( "document " + d.getDocID() + " has " + d.getMentions().size() + " mentions." );

		List<ChainSolution<Mention>> preds = new ArrayList<ChainSolution<Mention>>();

		d.setUsePredictedMentions( true );
		ChainSolution<Mention> presol = decoderPronouns.decode(d);
		for(Constraint con : posConPool){
			posConstraintPosProcessor.setConstraint(con);
			presol = posConstraintPosProcessor.decode(d, presol);
		}

		ChainSolution<Mention> sol = presol;

//		if(getAnnotationFormat().equals("Ontonotes"))
//			sol = removeSingleton.decode(d, removeNested.decode(d, presol));
		preds.add(sol);
		d.setPredEntities(sol);
		String rawText = d.getTextAnnotation().getText();
		
		List<Labeling> clusters = new ArrayList<Labeling>();
		for (Set<Mention> chain : sol.getChains()) {
			List<Span> labels = new ArrayList<Span>();
			for (Mention m : chain) {
				Chunk c = m.getExtent();
				Span span = new Span();
				span.setStart(c.getStart());
				span.setEnding(c.getEnd() + 1);
				System.err.println( "Mention: " + m.getText() );
				System.err.println( "Span: " + rawText.substring( span.getStart(), span.getEnding() ) );
				System.err.println( "offsets start,end = (" + span.getStart() + ", " + span.getEnding() + ")." );
				if (!m.getEntityID().equals("NONE"))
					span.setLabel(m.getEntityID());
				

				TreeMap< String, String > attMap = new TreeMap< String, String > ();

				if ( !( "NONE".equalsIgnoreCase( m.getEntityType() ) ) ) {
				    attMap.put( "ENTITY_TYPE", m.getEntityType() );
				    attMap.put( "COARSE_ENTITY_TYPE", m.getEntityType() );
				}
				else if ( "PRO".equalsIgnoreCase( m.getType() ) )  {
				    attMap.put( "ENTITY_TYPE", "COREF_PRONOUN" );
				}
				else {
				    attMap.put( "ENTITY_TYPE", "COREF_NON_NE_MENTION" );
				}

				span.setAttributes( attMap );

				labels.add(span);
			}

			Labeling cluster = new Labeling();
			cluster.setLabels(labels);
			clusters.add(cluster);
		}
		Clustering result = new Clustering();
		result.setSource(getSourceIdentifier());
		result.setClusters(clusters);
		long endTime = System.currentTimeMillis();
		long time = endTime - startTime;
		logger.info("Performed Coref in {}ms", time);
		return result;
	}


    public void addMentions(List<Mention> mentions,
                                     Record record, Doc doc, int offset){

        Labeling nes = record.getLabelViews().get(nerfield);
        for (Span span : nes.getLabels()) {
            Chunk c = new Chunk(doc, offset + span.getStart(), offset + span.getEnding() - 1,
                    record.getRawText().substring(span.getStart(), span.getEnding()));
            Mention m = new Mention(doc, c);
            m.setType("NAM");
            m.setEntityType(span.getLabel());
            mentions.add(m);
        }

    }
    
    
    public Clustering clusterRecord(Record record) throws TException 
    {
//    	Labeling tokensLabeling = record.getLabelViews().get( CuratorViewNames.tokens );
//    	Labeling sentenceLabeling = record.getLabelViews().get( CuratorViewNames.sentences );
    	
    	TextAnnotation ta = CuratorDataStructureInterface.getTextAnnotationViewsFromRecord( record.getIdentifier(),
    	                                                                                    record.getIdentifier(),
    	                                                                                    record );

    	ta.addView( this.gazetteerManager );

    	DocPlainText doc = new DocPlainText();
    	doc.loadFromTA(ta);

    	List<Mention> predMents = getPredMents( doc );
    	doc.setPredictedMentions(predMents);

    	
        return corefDoc(doc);
    }


	public Clustering clusterRecords(List<Record> records)
			throws AnnotationFailedException, TException 
	{
		String msg = NAME + ".clusterRecords(): not implemented. Send records individually for processing.\n";
		logger.error( msg );
		throw new AnnotationFailedException( msg );
	}

	private void adjustSpan(Span span, List<Integer> offsets) {
		int previous = 0;
		boolean adjusted = false;
		for (int i = 0; i < offsets.size(); i++) {
			int offset = offsets.get(i);
			if (span.getStart() < offset) {
				span.setStart(span.getStart() - previous);
				span.setEnding(span.getEnding() - previous);
				span.setMultiIndex(i);
				adjusted = true;
				break;
			}
			previous = offset;
		}
		if (!adjusted) {
			logger.warn("Did not perform any adjustment on span.");
		}
	}
	

	
	private List<Mention> getPredMents(DocPlainText doc) 
	{
	    List<Mention> results = new ArrayList<Mention>();

	    //Does the decoder need to be reset before reusing?
	    // note that something here loads some very large gazetteers...
	    MentionSolution predMents = mentionDecoder.decode(doc);

	    //System.err.println("Mentions detected:\n" + predMents);

	    for (Mention m : predMents.getMentions()) 
	    {
	    	String mType = mentionTyper.discreteValue(m);
	    	m.setType(mType);
	    	String eType = EntityTypeFeatures.getEType(m);
	    	m.setEntityType(eType);

	    	results.add(m);
	    }
	    return results;
	}

	private Mention pickCanonicalMention(Set<Mention> mentionSet) {
		ArrayList<Mention> sortedList = new ArrayList<Mention>(mentionSet);
		Collections.sort(sortedList);
		for(Mention m : sortedList){
			if(m.getType().equals("NAM"))
				return m;
		}
		for(Mention m : sortedList){
			if(m.getType().equals("NOM"))
				return m;
		}
		return sortedList.get(0);
	}

	

	public boolean ping() throws TException
    {
	    return true;
    }

	public String getName() throws TException
    {
	    return super.getName();
    }

	public String getVersion() throws TException
    {
	    return super.getVersion();
    }

	public String getSourceIdentifier() throws TException
    {
	    return super.getSourceIdentifier();
    }

}

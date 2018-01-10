package edu.illinois.cs.cogcomp.annotation.handler;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.illinois.cs.cogcomp.thrift.base.AnnotationFailedException;
import edu.illinois.cs.cogcomp.thrift.base.Forest;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.thrift.curator.Record;
import edu.illinois.cs.cogcomp.thrift.parser.Parser;

import edu.illinois.cs.cogcomp.relations.core.utilities.RelationsProperties;
import edu.illinois.cs.cogcomp.relations.prepositions.PrepositionRelationPredictor;

/**
 * Wraps the Illinois preposition SRL in a Parser.Iface.
 * @author Vivek Srikumar
 *
 */

public class IllinoisPrepositionSRLHandler extends IllinoisAbstractHandler implements Parser.Iface {
	
	
    protected static final String DEFAULT_CONFIG = "configs/prep-relations.properties";
    private final Logger logger = LoggerFactory.getLogger(IllinoisPrepositionSRLHandler.class);

    private PrepositionRelationPredictor prepRelations;

    public IllinoisPrepositionSRLHandler() {

	this( DEFAULT_CONFIG );
    }
	
    public IllinoisPrepositionSRLHandler(String configFileName) {
	super("Illinois Preposition Semantic Role Labeler" );

	logger.info("Preposition SRL ready");
	if (configFileName.trim().equals("")) {
	    configFileName = DEFAULT_CONFIG;
	}
	
	try {
	    // initialize the configuration 
	    RelationsProperties.initialize(DEFAULT_CONFIG);

	    this.prepRelations = new PrepositionRelationPredictor();
	} catch (Exception e) {
	    logger.error("Unable to initialize preposition relation extractor", e);
	    throw new RuntimeException(e);
	}

	super.setVersion( prepRelations.getVersion() );
	super.setName( prepRelations.getCuratorName() );
	super.setIdentifier( prepRelations.getCuratorName() );

	logger.info("set name to '" + prepRelations.getCuratorName() + "'." );
	logger.info("set version to '" + prepRelations.getCuratorName() + "'." );

    }

    @Override
    public Forest parseRecord(Record record) throws AnnotationFailedException,
						    TException {
	try {
	    return prepRelations.predictForest(record);
	} catch(Exception e) {
	    logger.error("Error annotatin record", e);
	    throw new AnnotationFailedException(e.getMessage());
	}

    }


}

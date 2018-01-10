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

import edu.illinois.cs.cogcomp.srl.main.SRLSystem;
import edu.illinois.cs.cogcomp.srl.main.SRLConfig;
import edu.illinois.cs.cogcomp.srl.main.VerbSRLSystem;

import edu.illinois.cs.cogcomp.srl.curator.IllinoisSRLAssistant;

/**
 * Wraps the Illinois verb SRL in a Parser.Iface.
 * @author Mark Sammons
 *
 */

public class IllinoisVerbSRLHandler extends IllinoisAbstractHandler implements Parser.Iface {
	
	
    protected static final String DEFAULT_CONFIG = "configs/srl-config.properties";
    private final Logger logger = LoggerFactory.getLogger(IllinoisVerbSRLHandler.class);

    private SRLSystem srlSystem;
    private boolean beamSearch; 

	public IllinoisVerbSRLHandler() {

		this( DEFAULT_CONFIG );
	}
	
	public IllinoisVerbSRLHandler(String configFileName) {
	    super("Illinois Verb Semantic Role Labeler" );

		logger.info("Verb SRL ready");
		if (configFileName.trim().equals("")) {
			configFileName = DEFAULT_CONFIG;
		}

		// initialize the configuration 
		SRLConfig.getInstance( configFileName );

		this.srlSystem = VerbSRLSystem.getInstance();
		this.beamSearch = true;

		super.setVersion( srlSystem.getSRLSystemVersion() );
		super.setName( srlSystem.getSRLCuratorName() );
		super.setIdentifier( srlSystem.getSRLCuratorName() );

		logger.info("set name to '" + srlSystem.getSRLCuratorName() + "'." );
		logger.info("set version to '" + srlSystem.getSRLSystemVersion() + "'." );

	}

	@Override
	public Forest parseRecord(Record record) throws AnnotationFailedException,
			TException {
		return IllinoisSRLAssistant.performSRL( this.srlSystem, record, this.beamSearch );
	}


}

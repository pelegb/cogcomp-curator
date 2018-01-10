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
import edu.illinois.cs.cogcomp.srl.main.NomSRLSystem;

import edu.illinois.cs.cogcomp.srl.curator.IllinoisSRLAssistant;

/**
 * Wraps the Illinois verb SRL in a Parser.Iface.
 * @author Mark Sammons
 *
 */

public class IllinoisNomSRLHandler extends IllinoisAbstractHandler implements Parser.Iface {
	
	
    protected static final String DEFAULT_CONFIG = "configs/srl-config.properties";
    private final Logger logger = LoggerFactory.getLogger(IllinoisNomSRLHandler.class);

    private SRLSystem srlSystem;
    private boolean beamSearch; 

	public IllinoisNomSRLHandler() {
		this( DEFAULT_CONFIG );
	}
	
	public IllinoisNomSRLHandler(String configFileName) {
	    super("Illinois Nom Semantic Role Labeler" );

		logger.info("Nom SRL ready");
		if (configFileName.trim().equals("")) {
			configFileName = DEFAULT_CONFIG;
		}

		// initialize the configuration 
		SRLConfig.getInstance( configFileName );

		this.srlSystem = NomSRLSystem.getInstance();
		this.beamSearch = true;

		super.setVersion( srlSystem.getSRLSystemVersion() );
		super.setName( srlSystem.getSRLCuratorName() );
		super.setIdentifier( srlSystem.getSRLCuratorName() );
	}

	@Override
	public Forest parseRecord(Record record) throws AnnotationFailedException,
			TException {
		return IllinoisSRLAssistant.performSRL( this.srlSystem, record, this.beamSearch );
	}


}

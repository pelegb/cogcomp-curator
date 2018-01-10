package edu.illinois.cs.cogcomp.annotation.handler;

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import LBJ2.nlp.Word; 
import LBJ2.nlp.seg.Token;
import edu.illinois.cs.cogcomp.thrift.base.AnnotationFailedException;
import edu.illinois.cs.cogcomp.thrift.base.Labeling;
import edu.illinois.cs.cogcomp.thrift.base.Span;
import edu.illinois.cs.cogcomp.thrift.curator.Record;
import edu.illinois.cs.cogcomp.thrift.labeler.Labeler;
import edu.illinois.cs.cogcomp.quant.lbj.Chunker;
import edu.illinois.cs.cogcomp.quant.driver.*;
import edu.illinois.cs.cogcomp.quant.standardize.*;

/**
 * Wraps the IllinoisQuantHandler into a Labeler.Iface
 * @author Subhro Roy and Mark Sammons
 *
 */
 

public class IllinoisQuantHandler extends IllinoisAbstractHandler implements Labeler.Iface {
	
    private static final String NAME = IllinoisQuantHandler.class.getCanonicalName();

	private static final Logger logger = LoggerFactory.getLogger(IllinoisQuantHandler.class);
	private String posfield = "pos";
	private String tokensfield = "tokens";
	private String sentencesfield = "sentences";

	public IllinoisQuantHandler()throws Throwable {
		this("configs/chunker.properties");
	}
	
	public IllinoisQuantHandler(String configFilename) throws Throwable {

        super("Illinois Quant", "0.3", "illinoisquant");


		if (configFilename.trim().equals("")) {
			configFilename = "configs/chunker.properties";
		}
		Properties config = new Properties();
		try {
            FileInputStream in = new FileInputStream(configFilename);
            config.load(new BufferedInputStream(in));
            in.close();
        } catch (IOException e) {
        	logger.warn("Error reading configuration file. {}", configFilename);
        }
		tokensfield = config.getProperty("tokens.field", "tokens");
		sentencesfield = config.getProperty("sentences.field", "sentences");
		posfield = config.getProperty("pos.field", "sentences");

		logger.info( NAME + " ready for input...");

	}
	
	public Labeling labelRecord(Record record) throws AnnotationFailedException,
			TException {
		String rawText = record.getRawText();
		logger.debug( NAME + ".labelRecord(): rawText is '" + rawText + "'" );


		if (!record.getLabelViews().containsKey(tokensfield) && !record.getLabelViews().containsKey(sentencesfield)) {
		    String msg = "Record must be tokenized and sentence split first";
		    logger.error( msg );
		    throw new TException( msg );
		}
		if (!record.getLabelViews().containsKey(posfield)) {
		    String msg = "Record must be POS tagged first";
		    logger.error( msg );
		    throw new TException( msg );
		}
		long startTime = System.currentTimeMillis();
		
		List<Span> tags = record.getLabelViews().get(posfield).getLabels();
//		System.out.println("Raw Text: "+ rawText );

		Labeling labeling = new Labeling();
		List<Span> labels = new ArrayList<Span>();
		Span label = null;
		
		try{
			for( MySpan s: MySpan.getSpans(rawText) ){
				
				label = new Span();
				label.setStart( s.start );
				label.setEnding( s.end );

				logger.debug( "Quantity tagger found span with offsets '" + s.start + ", " + s.end + "'" );
				logger.debug( "Quantity type is: "+ QuantityStatic.repr(s.object));
				
				HashMap<String, String> attributes = new HashMap<String, String>();

				label.setAttributes( attributes );

				if( s.object instanceof Quantity ){
					label.setLabel("NUMBER");
					Quantity q = (Quantity)s.object;
					label.attributes.put("bound", q.bound);
					label.attributes.put("value", ""+q.value);
					label.attributes.put("units", ""+q.units);
					label.attributes.put("JSON", MySpan.getJSONFromQuantity(q));
					
					
				}
				else if( s.object instanceof Date ){
					label.setLabel("DATE");
					Date d = (Date)s.object;
					label.attributes.put("bound", d.bound);
					label.attributes.put("day", ""+d.day);
					label.attributes.put("month", ""+d.month);
					label.attributes.put("year", ""+d.year);
					label.attributes.put("JSON", MySpan.getJSONFromDate(d));
					
				}
				else if( s.object instanceof DateRange ){
					label.setLabel("DATE-RANGE");
					DateRange d = (DateRange)s.object;
					label.attributes.put("bound1", d.begins.bound);
					label.attributes.put("day1", ""+d.begins.day);
					label.attributes.put("month1", ""+d.begins.month);
					label.attributes.put("year1", ""+d.begins.year);
					label.attributes.put("bound2", d.ends.bound);
					label.attributes.put("day2", ""+d.ends.day);
					label.attributes.put("month2", ""+d.ends.month);
					label.attributes.put("year2", ""+d.ends.year);
					label.attributes.put("JSON", MySpan.getJSONFromDateRange(d));
					
				}
				else if( s.object instanceof Range ){
					label.setLabel("RANGE");
					Range r = (Range)s.object;
					label.attributes.put("bound1", r.begins.bound);
					label.attributes.put("value1", ""+r.begins.value);
					label.attributes.put("units1", ""+r.begins.units);
					label.attributes.put("bound2", r.ends.bound);
					label.attributes.put("value2", ""+r.ends.value);
					label.attributes.put("units2", ""+r.ends.units);
					label.attributes.put("JSON", MySpan.getJSONFromRange(r));
					
				}
				else if( s.object instanceof Ratio ){
					label.setLabel("RATIO");
					Ratio r = (Ratio)s.object;
					label.attributes.put("bound1", r.numerator.bound);
					label.attributes.put("value1", ""+r.numerator.value);
					label.attributes.put("units1", ""+r.numerator.units);
					label.attributes.put("bound2", r.denominator.bound);
					label.attributes.put("value2", ""+r.denominator.value);
					label.attributes.put("units2", ""+r.denominator.units);
					label.attributes.put("JSON", MySpan.getJSONFromRatio(r));
					
				}

				labels.add( label );
				label = null;
				
			}
		}
		catch( Exception e ){
		    logger.error( NAME + ".labelRecord(): caught exception: " + e.getMessage() );
		    e.printStackTrace();
		}

		logger.debug( "found " + labels.size() + " quantity spans." );
	
		labeling.setLabels(labels);
		labeling.setSource(getSourceIdentifier());

		long endTime = System.currentTimeMillis();
		logger.info("Tagged input in {}ms", endTime-startTime);
		return labeling;
		
	}
}


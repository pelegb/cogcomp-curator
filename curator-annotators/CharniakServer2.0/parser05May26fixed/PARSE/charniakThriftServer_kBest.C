/**
 * Thrift Server wrapper for Charniak's Syntactic Parser
 *
 *  NOTE: assumes config is in exec dir
 *  NOTE: returns number of best parses, where number is specified by 
 *        config.txt parameter "-P"
 *  NOTE: throws exception if input contains wide characters. 
 *  NOTE: never times anything
 *  NOTE: this version is extended to include Head information
 *       (courtesy of Vasin Punyakanok)
 *  NOTE: there are some suspicious hard-coded constants in InputTree; 
 *        not sure if these reflect WORD length (in which case they are fine)
 *        or SENTENCE length (in which case things may not be fine)
 *        -- looks like hard limit of 800 in SentRep.h (data member 'words_')
 *           and "assert( length_ < 400)" in SentRep.C
 *
 *  Mark Sammons, October 2009
 */


//#define DEBUG_CTS


#include <fstream>
#include <iostream>
#include <sstream>
#include <unistd.h>
#include <math.h>
#include <cerrno>
#include <iconv.h>
#include <stdio.h> // ugly, but needed to use iconv
#include <netinet/in.h> //question about adding this in updating to thrift 0.8.0

#include "GotIter.h"
#include "Wrd.h"
#include "InputTree.h"
#include "Bchart.h"
#include "ECArgs.h"
#include "MeChart.h"
#include "extraMain.h"
#include "AnsHeap.h"
#include "UnitRules.h"
#include "Params.h"
#include "ewDciTokStrm.h"
#include "headFinder.h"

// THRIFT headers

#include <protocol/TBinaryProtocol.h>
#include <server/TSimpleServer.h>
#include <transport/TServerSocket.h>
#include <transport/TBufferTransports.h>

// from thrift theArchives' interfaces/gen-cpp/ 

#include "Curator.h"
#include "base_types.h"
#include "curator_types.h"
#include "MultiParser.h" 

#include "CharniakParser.h"



using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;
using namespace apache::thrift::server;
using namespace cogcomp::thrift;
using namespace cogcomp::thrift::base;

using boost::shared_ptr;

using namespace cogcomp::thrift::parser;




static const char * CONFIG_FILE = "config.txt";
static const string VERSION = "0.5";
static const string NAME = "Charniak K-Best Syntactic Parser";
static const string SHORT_NAME = "charniak_kbest";


MeChart* curChart;
Params 	 params;


class MultiParserHandler : virtual public MultiParserIf {

protected:
  int m_numParses;
  CharniakParser * m_parser;
  string m_sourceId;


 public:
  MultiParserHandler() {
    MultiParserHandler( CONFIG_FILE );
  }
//     char * argv[6];
//     int argc = loadConfig( CONFIG_FILE, argv );
    
//     ECArgs args( argc, argv );
//     /* l = length of sentence to be proceeds 0-100 is default
//        n = work on each #'th line.
//        d = print out debugging info at level #
//        t = report timings */
    

//     params.init( args );


//     ECString  path( args.arg( 0 ) );
//     generalInit( path, params.numParses() );    

//     m_numParses = params.numParses();
//   }



  MultiParserHandler( string configFile_ ) {

    stringstream nameStrm;
    nameStrm << SHORT_NAME << "-" << VERSION;
    m_sourceId = nameStrm.str();

    m_parser = new CharniakParser( configFile_, m_sourceId );
    
    m_numParses = m_parser->getNumParses();
  }



  virtual ~MultiParserHandler()
  {
    delete m_parser;
  }
  
  void getName( string & name_ ) {
    name_ = NAME;
  }

  void getShortName( string & shortName_ ) {
    shortName_ = SHORT_NAME;
  }

  void getVersion( string & version_ ) {
    version_ = VERSION;
  }

  bool ping() {
    return true;
  }

  void getSourceIdentifier( string & sourceId_ ) {
    sourceId_ = m_sourceId;
  }





  void parseRecord(std::vector< cogcomp::thrift::base::Forest > & parses_, 
		   const cogcomp::thrift::curator::Record& record_
		   ) 
  {

#ifdef DEBUG_CTS
    cerr << "## parseRecord()... numParses is " << m_numParses << endl;
#endif

    // identify sentences using text member and sentence spans
    // pass start offset of each sentence when generating parse tree
    // parse each one, add to parse Forest

    cogcomp::thrift::base::Labeling sentences = getLabelView( record_, m_parser->sentenceView );
    cogcomp::thrift::base::Labeling tokens = getLabelView( record_, m_parser->tokenView );


    if ( 0 == sentences.labels.size() ) 
    {
      stringstream errStrm;
      errStrm << "ERROR: charniakThriftServer_kBest::parseRecord(): "
	      << "no sentences in record (sentences.labels.size() is zero)"
	      << endl;

      cerr << errStrm.str();

      AnnotationFailedException e;
      e.reason = errStrm.str();
      throw e;
    }

    for ( int i = 0; i < m_numParses ; ++i ) {

      Forest kthBestParseTrees;
      kthBestParseTrees.rawText = record_.rawText;
      kthBestParseTrees.__isset.rawText = true;
      getSourceIdentifier( kthBestParseTrees.source );
      kthBestParseTrees.__isset.source = true;

      parses_.push_back( kthBestParseTrees );
    }

    

    for ( int i = 0; i < sentences.labels.size(); ++i ) {

      Span sentenceSpan = sentences.labels[ i ];

      int start =sentences.labels[i].start;
      int end = sentences.labels[i].ending;
      
      Text sentenceText = record_.rawText; //.substr( , end - start );
      vector< StringWithOffsets > tokenVec;

      if ( !Bchart::tokenize ) {

	cerr << "## SentenceText is '" << sentenceText 
	     << "'; " << endl << "## Bchart::tokenize is set to false..." 
	     << endl;

	vector< Span > tokLabels = tokens.labels;  

	cerr << "## found " << tokLabels.size() << " tokenVec..." << endl;

        for (int i = 0; i < tokLabels.size(); i++) {  

            Span span = tokLabels[ i ];  
	    int length = span.ending - span.start;

	    cerr << "## start: " << start << "; end: " << end << endl;
	    cerr << "## span start: " << span.start << "; span end: " << span.ending << endl;
	    
	    if ( ( span.start < start ) ||
		 ( span.start > end ) ||
		 ( span.ending < start ) ||
		 ( span.ending > end ) 
		 )
	      
	      continue;

	    cerr << "## found token in range: sentence start, end are: "
		 << start << ", " << end << "; tok start, end, length are: "
		 << span.start << ", " << span.ending << ", " << length << endl;

            string tokenStr = sentenceText.substr(span.start, length);  
	    //            printf("%s : %s\n", label.c_str(), words.c_str());  
	    cerr << "## read token '" << tokenStr << "'..." << endl;
	    
	    tokenVec.push_back( StringWithOffsets( tokenStr, span.start, span.ending ) );
        }  
      }

      if ( tokenVec.empty() ) {
	stringstream errStrm;
	errStrm << "ERROR: charniakThriftServer::parseRecord(): "
		<< "no tokens found in span starting at " << start
		<< " and ending at " << end << "." << endl;

	cerr << errStrm.str();

	AnnotationFailedException e;
	e.reason = errStrm.str();
	throw e;
      }

      
      vector< Tree > ithSentenceParses;

      if ( Bchart::tokenize ) {

#ifdef DEBUG_CTS
	cerr << "## calling parseSentence with string '" 
	     << sentenceText << "'..." << endl;
#endif

	m_parser->parseSentence( ithSentenceParses, sentenceText, start );
      }
      else {

#ifdef DEBUG_CTS
	cerr << "## calling parseTokenizedSentence with " << tokenVec.size() 
	     << " tokens..." << endl;
#endif
	SentRep srp( tokenVec );
	
	m_parser->parseTokenizedSentence( ithSentenceParses, srp, start );
      }

      m_parser->showTreeList( ithSentenceParses );
      
      // size of ithSentenceParses must be equal to m_numParses...

      for ( int j = 0; j < m_parser->getNumParses(); j++ ) 
	parses_[ j ].trees.push_back( ithSentenceParses[ j ] );

    }

    return;
  }


  cogcomp::thrift::base::Labeling getLabelView( cogcomp::thrift::curator::Record record_, 
						const string & viewName_ 
						)
  {
    cogcomp::thrift::base::Labeling view;
    
    map< string, cogcomp::thrift::base::Labeling >::const_iterator it_labeling
      = record_.labelViews.find( viewName_ );
    
    if ( it_labeling != record_.labelViews.end() )
      view = it_labeling->second;
    
    return view;
  }



  
};













/**
 * main()
 */

int main(int argc, char **argv) {

  if( argc != 3 ) {
    cerr << "Usage: " << argv[0] << " port configFile" << endl;
    exit( -1 );
  }

  int port = atoi( argv[1] );
  string config( argv[2] );

  shared_ptr<MultiParserHandler> handler(new MultiParserHandler( config ));
  shared_ptr<TProcessor> processor(new MultiParserProcessor(handler));
  shared_ptr<TServerTransport> serverTransport(new TServerSocket(port));
  //  shared_ptr<TTransportFactory> transportFactory(new TBufferedTransportFactory());
  shared_ptr<TProtocolFactory> protocolFactory(new TBinaryProtocolFactory());

   //  shared_ptr<TTransport> bufTransport( new TBufferedTransport( serverTransport ) );
  shared_ptr<TTransportFactory> transportFactory( new TFramedTransportFactory() );
//   shared_ptr<TProtocol> protocol( new TBinaryProtocol( transport ) );


  TSimpleServer server(processor, serverTransport, transportFactory, protocolFactory);
  server.serve();
  return 0;
}



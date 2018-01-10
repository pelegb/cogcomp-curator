/**
 * Thrift Server wrapper for Charniak's Syntactic Parser
 *
 *  NOTE: assumes config is in exec dir
 *  NOTE: only ever returns top parse
 *  NOTE: never times anything
 *  NOTE: this version is extended to include Head information
 *       (courtesy of Vasin Punyakanok)
 *  NOTE: there are some suspicious hard-coded constants in InputTree; 
 *        not sure if these reflect WORD length (in which case they are fine)
 *        or SENTENCE length (in which case things may not be fine)
 *        -- looks like hard limit of 800 in SentRep.h (data member 'words_')
 *           and "assert( length_ < 400)" in SentRep.C
 *
 *  August 2010: updated to use new Curator architecture
 *
 *  CHANGES:
 *    -- no parseText() method
 *
 *
 *
 *  TODO:
 *    -- specify labels for views needed by parser (i.e. tokens, sentences) 
 *       in config file
 *    -- change ECArgs/loadConfig to use map of label to value for readability/
 *       maintainability
 *
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
#include <netinet/in.h> //question about adding this in updating to thrift0.8.0


// Charniak Parser headers
#include "ewDciTokStrm.h"
#include "SentRep.h"




// THRIFT headers

#include <protocol/TBinaryProtocol.h>
#include <server/TSimpleServer.h>
#include <transport/TServerSocket.h>
#include <transport/TBufferTransports.h>

// from /shared/grandpa/servers/curator/deploy/components/gen-cpp/

#include "Curator.h"
#include "base_types.h"
#include "curator_types.h"
#include "Parser.h" 
#include "CharniakException.h"

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
static const string VERSION = "0.8";
static const string NAME = "Charniak Syntactic Parser";
static const string SHORT_NAME = "charniak";


MeChart* curChart;


class ParserHandler : virtual public ParserIf {
  
protected:
  
  CharniakParser * m_parser;
  string m_sourceId;

 public:
  
  ParserHandler() {    
    ParserHandler( CONFIG_FILE );
  }




  ParserHandler( string configFile_ ) {
    
    stringstream nameStrm;
    nameStrm << SHORT_NAME << "-" << VERSION;
    m_sourceId = nameStrm.str();

    m_parser = new CharniakParser( configFile_, m_sourceId );
  }

  virtual ~ParserHandler()
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


  void parseRecord(cogcomp::thrift::base::Forest& _parses, 
		   const cogcomp::thrift::curator::Record& record_) {

#ifdef DEBUG_CTS
    cerr << "## parseRecord()..." << endl;
#endif

    // identify sentences using text member and sentence spans
    // pass start offset of each sentence when generating parse tree
    // parse each one, add to parse Forest


    Text sentenceText = record_.rawText;
    cogcomp::thrift::base::Labeling sentences = getLabelView( record_, m_parser->sentenceView  );
    cogcomp::thrift::base::Labeling tokens = getLabelView( record_, m_parser->tokenView );

    if ( 0 == sentences.labels.size() ) 
    {
      stringstream errStrm;
      errStrm << "ERROR: charniakThriftServer::parseRecord(): "
	      << "no sentences in record (sentences.labels.size() is zero); "
	      << "raw text is: '" << endl << sentenceText << "'. " 
	      << endl;

      cerr << errStrm.str();

      AnnotationFailedException e;
      e.reason = errStrm.str();
      throw e;
    }

    bool isInputOk = false;

    try {
      isInputOk = m_parser->checkEncodedStringIsCompatible( sentenceText, 
							    "UTF-8",
							    "ASCII"
							    );
    }
    catch ( AnnotationFailedException & e ) {
      e.reason += " parseRecord().\n";
      throw e;
    }

    if ( !isInputOk ) {
      stringstream errStrm;
      errStrm << "ERROR: charniakThriftServer::parseRecord(): "
	      << "detected non-ascii input in UTF-8 string '"
	      << sentenceText << "'. Charniak can't deal with it. " << endl
	      << "Try cleaning non-ascii characters from your input first."
	      << endl;
      AnnotationFailedException e;
      e.reason = errStrm.str();
      throw e;
    }

    cerr << "## charniakThriftServer::parseRecord(): processing text '" 
	 << sentenceText << "'..." << endl;

    for ( int i = 0; i < sentences.labels.size(); ++i ) {

      int start = sentences.labels[i].start;
      int end = sentences.labels[i].ending;
      
      vector< StringWithOffsets > tokenVec;

      if ( !Bchart::tokenize ) {

#ifdef DEBUG_CTS
	cerr << "## Bchart::tokenize is set to false..." << endl;
#endif

	vector< Span > labels = tokens.labels;  


        for ( int i = 0; i < labels.size(); i++ ) {  

            Span span = labels[ i ];  
	    int length = span.ending - span.start;

#ifdef DEBUG_CTS
	    cerr << "## start: " << start << "; end: " << end << endl;
	    cerr << "## span start: " << span.start << "; span end: " << span.ending << endl;
#endif
	    if ( ( span.start < start ) ||
		 ( span.start > end ) ||
		 ( span.ending < start ) ||
		 ( span.ending > end ) 
		 )
	      
	      continue;


            string tokenStr = sentenceText.substr(span.start, length);  
	    //            printf("%s : %s\n", label.c_str(), words.c_str());  
#ifdef DEBUG_CTS
	    cerr << "## read token '" << tokenStr << "'..." << endl;
#endif	    
	    tokenVec.push_back( StringWithOffsets( tokenStr, span.start, span.ending ) );
        }  
      }

      vector< Tree > parseTrees;
      
      if ( Bchart::tokenize ) {

#ifdef DEBUG_CTS
	cerr << "## calling parseSentence with string '" 
	     << sentenceText << "'..." << endl;
#endif
	m_parser->parseSentence( parseTrees, sentenceText, start );
      
      }
      else {

#ifdef DEBUG_CTS
	cerr << "## calling parseSentence with " << tokenVec.size() 
	     << " tokens..." << endl;
#endif
	SentRep srp( tokenVec );

	m_parser->parseTokenizedSentence( parseTrees, srp, start );


      }

      _parses.trees.push_back( parseTrees[0] ); // get 'best' (and only) parse tree for this sentence
    }

    getSourceIdentifier( _parses.source );
    _parses.__isset.source = true;

    _parses.rawText = record_.rawText;
    _parses.__isset.rawText = true;

    m_parser->showForest( _parses );

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

  shared_ptr<ParserHandler> handler(new ParserHandler( config ));
  shared_ptr<TProcessor> processor(new ParserProcessor(handler));
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



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
 *    -- change ECArgs/loadConfig to use map of label to value for readability/
 *       maintainability
 *
 */

#include "CharniakParser.h"

// 'curChart' is an extern variable of Bchart. Three cheers for OOP.
extern MeChart*  curChart;

 
CharniakParser::CharniakParser( string configFile_, string id_ ) 
{
  m_identifier = id_;
  tokenView = "tokens";
  sentenceView = "sentences";

    char * argv[6];
    int argc = loadConfig( configFile_.c_str(), argv );
    
    ECArgs args( argc, argv );
    /* l = length of sentence to be proceeds 0-100 is default
       n = work on each #'th line.
       d = print out debugging info at level #
       t = report timings */
    
    phInit( args );
    
}

void CharniakParser::phInit( ECArgs & args_ )
  {
    m_params.init( args_ );
    
    m_numParses = m_params.numParses();

    ECString  path( args_.arg( 0 ) );
    generalInit( path, m_params.numParses() );    

  }


      

      

void CharniakParser::parseSentence( vector< cogcomp::thrift::base::Tree > & parseTrees_, 
		      const cogcomp::thrift::base::Text& input_,
		      const int startCharOffset_ 
		      ) 
{
    stringstream inStrm;

    
    inStrm << "<s> " << input_ << " </s>" << endl;

#ifdef DEBUG_CTS
	cerr << "## processing input sentence '" 
	     << inStrm.str() << endl;
#endif

    ewDciTokStrm* inStream = new ewDciTokStrm( inStrm, Bchart::tokenize);
    
    SentRep srp( *inStream, SentRep::SGML );

#ifdef DEBUG_CTS
      cerr << "## instantiated sentRep..." << endl;
#endif

      try {
	
	parseTokenizedSentence( parseTrees_,
				srp,
				startCharOffset_ 
				);
      }
      catch ( CharniakException & e ) 
	{
	  if ( NULL != inStream )
	    delete inStream;
	  
	  throw e;
	}
    return;
}



void CharniakParser::parseTokenizedSentence( vector< cogcomp::thrift::base::Tree > & parseTrees_, 
			       SentRep & srp_,
			       const int startCharOffset_
			       ) 
{
    int len = srp_.length();

    if(len > m_params.maxSentLen) {
      AnnotationFailedException e;
      e.reason = "input too long.";
      throw e;
    }

    if(len == 0) {
      AnnotationFailedException e;
      e.reason = "input had zero length.";
      throw e;
    }


    InputTree*  mapparse = NULL;
    MeChart* chart = NULL;

    try {
        
      chart = new MeChart( srp_, m_params.numParses() );
      curChart = chart;
      
      
      chart->parse( );
      
      Item* topS = chart->topS();
      
      if(!topS) {
	delete chart;
	
	AnnotationFailedException e;
	e.reason = "parse failed.";
	throw e;
      }
      
    // compute the outside probabilities on the items so that we can
    // skip doing detailed computations on the really bad ones 
      
      chart->set_Alphas();
      
#ifdef DEBUG_CTS
      cerr << "## finding map parse for input: '"
	   << srp_ << "'..." << endl;
#endif    
      
      AnsTreeStr& at = chart->findMapParse();
      
      if( at.probs[0] <= 0 ) {
	AnnotationFailedException e;
	e.reason = "mapProbs did not return answer";

	delete chart;
	throw e;
      }
      

      int kValue = 0;
      
      for( kValue = 0 ; kValue < m_numParses ; kValue++ ) {

	short pos = 0;
	mapparse = inputTreeFromAnsTree(&at.trees[kValue], 
					pos,
					srp_ );
      
	double logP =log(at.probs[0]);
	logP -= (srp_.length()*log600);
	
	Tree parseTree;
	
	parseTree.source = getIdentifier();
	parseTree.score = logP;
	
	parseTree.__isset.source = true;
	parseTree.__isset.score = true;
	
	// recursive method
	addRootNodeAndTraverse( mapparse, 
				parseTree.nodes, 
				startCharOffset_  
				);
	
	if ( !parseTree.nodes.empty() ) {
	  parseTree.top = parseTree.nodes.size() - 1;  // index of node in node list, from zero
	}
	
#ifdef DEBUG_CTS
	cerr << endl << endl << endl
	     << "*****************************************************"
	     << endl << "## chk parse tree number " << kValue << " is: " 
	     << endl << *mapparse << endl;
	
	cerr << "## displaying nodes in returned parse tree: " << endl;
	
	vector<Node>::const_iterator
	  it_node = parseTree.nodes.begin(),
	  it_node_end = parseTree.nodes.end();
	
	int num = 0;
	
	for ( ; it_node != it_node_end; ++it_node ) {
	  cerr << "## node " << num++ << ": " << endl;
	  showNode( *it_node );
	}
	
	cerr << "## source identifier is: " << parseTree.source 
	     << "; isset.source is: " << parseTree.__isset.source << endl;
#endif
	
	parseTrees_.push_back( parseTree );
	
	delete mapparse;
      }
      delete chart;


    }
    catch ( CharniakException & e ) 
    {

      stringstream errStrm;
      errStrm << "ERROR: charniakThriftServer::parseTokenizedSentence(): " 
	      << "caught CharniakException: " << e.what()  << endl;

      cerr << errStrm.str();

      if ( NULL != mapparse )
	delete mapparse;
      if ( NULL != chart )
	delete chart;

      AnnotationFailedException e;
      e.reason = errStrm.str();
      throw e;
    }

}





void CharniakParser::addRootNodeAndTraverse( InputTree * chkParse_, 
					     vector< Node > & nodes_,
					     const int startCharOffset_ )
{
    Node myNode; 
    myNode.label = chkParse_->term() + chkParse_->ntInfo();   
    myNode.span.start = chkParse_->startOffset(); // + startCharOffset_;
    myNode.span.ending = chkParse_->endOffset(); // + startCharOffset_;

    myNode.__isset.span = true;

    InputTrees children = chkParse_->subTrees();

    ConstInputTreesIter  subTreeIter= children.begin();
    InputTree  *subTree;

    // traverse children, add to nodeList
    // add edges to children; when head child reached, add
    //   label 'HEAD'

    const int childOffset( headPosFromTree( chkParse_ ) );

#ifdef DEBUG_CTS
    cerr << "## child offset of head is " << childOffset << endl;
#endif

    int childIndex = 0;

    for ( ; subTreeIter != children.end() ; subTreeIter++ ) {

#ifdef DEBUG_CTS
      cerr << "## processing child " << childIndex << endl;
#endif

      myNode.__isset.children = true;

      // add child id and, if head, label to node children

      subTree = *subTreeIter;

      // make recursive call
      
      addRootNodeAndTraverse( subTree,
			      nodes_,
			      startCharOffset_
			      );


      int childId = nodes_.size() - 1;

      if ( childOffset == childIndex) 
	myNode.children.insert( make_pair( childId, "HEAD" ) );
      else
	myNode.children.insert( make_pair( childId, "" ) );

      childIndex++;
    }

    nodes_.push_back( myNode );
    
#ifdef DEBUG_CTS

    cerr << "## created node with id " << ( nodes_.size() - 1 ) 
	 << ": " << endl;
    showNode( myNode );

    cerr << "## displaying last element of nodes_: " << endl;
    int size = nodes_.size();
    showNode( nodes_[ size - 1 ] );

#endif

 
    return;
}


int CharniakParser::loadConfig( const char * fileName_, char * argv_[6] )
{
    ifstream in( fileName_ );
    
    if ( !in ) {
      stringstream errStrm;
      errStrm << "ERROR: CharniakThriftServer: couldn't open file '"
	      << fileName_ << "' to read configuration.  Error was: " 
	      << strerror( errno ) << "." << endl;
      
      cerr << errStrm.str();
      exit( -1 );
    }
    
    string arg;
    int numArgs = 0;
    
    /**
     * MS: changed to read view names from config, without affecting 
     *     number of arguments recognized by original code
     */


    while ( in >> arg ) {
      cerr << "read arg '" << arg << "'." << endl;

      if ( arg == "SENTENCE_VIEW" ) 
	in >> sentenceView;
      else if ( arg == "TOK_VIEW" )
	in >> tokenView;
      else {
	
	char * buf = new char[ arg.size() + 1];
	argv_[ ++numArgs ] = strcpy( buf, arg.c_str() );
      
	cerr << "## argv_[" << numArgs << "] is '" << argv_[ numArgs ] 
	     << "'." << endl;
      }
    }
    
    return numArgs;
}
  

bool CharniakParser::checkEncodedStringIsCompatible( const string & str_,
				       const string & inputEncoding_,
				       const string & outputEncoding_ ) const
{
    bool isCompatible = true;
    
    
    int outBufLen = 2 * str_.size() + 1;    
    char inbuf[ str_.size() + 1 ];

    strcpy( inbuf, str_.c_str() );

    char outbuf[ outBufLen ];
    
    iconv_t          cd;     /* conversion descriptor          */
    size_t           inleft; /* number of bytes left in inbuf  */
    size_t           outleft;/* number of bytes left in outbuf */
    int              rc;     /* return code of iconv()         */
    
    
    if ( ( cd = iconv_open(inputEncoding_.c_str(), outputEncoding_.c_str() ) ) 
	 == (iconv_t)(-1)) {
      
      stringstream errStrm;
      errStrm << "ERROR: charniakThriftServer::checkEncodedStringIsCompatible():"
	      << " no conversion available from " << inputEncoding_ 
	      << " to " << outputEncoding_ << "." << endl;
      
      AnnotationFailedException e;
      e.reason = errStrm.str();
      throw e;
    }
    
    inleft = str_.size();
    outleft = outBufLen;
    char * inptr = (char*) inbuf;
    char * outptr = (char*) outbuf;
    
    rc = iconv(cd, &inptr, &inleft, &outptr, &outleft);
    
    if (rc == -1) {
      stringstream errStrm;
      errStrm << "ERROR: charniakThriftServer::checkEncodedStringIsCompatible():"
	      << " could not convert characters from " << inputEncoding_
	      << " to " << outputEncoding_ << "." << endl;
      
      AnnotationFailedException e;
      e.reason = errStrm.str();
      throw e;
    }
    else if ( rc > 0 ) {
      isCompatible = false;
    }
    
    iconv_close(cd);
    
    return isCompatible;
}

  
  








// int loadConfig( const char * fileName_, char * argv_[6] )
// {
//   ifstream in( fileName_ );

//   if ( !in ) {
//     stringstream errStrm;
//     errStrm << "ERROR: CharniakThriftServer: couldn't open file '"
// 	    << fileName_ << "' to read configuration.  Error was: " 
// 	    << strerror( errno ) << "." << endl;

//     cerr << errStrm.str();
//     AnnotationFailedException e;
//     e.reason = errStrm.str();
//     throw e;
//   }
  
//   string arg;
//   int numArgs = 0;

//   while ( in >> arg ) {
//     char * buf = new char[ arg.size() + 1];
//     argv_[ numArgs++ ] = strcpy( buf, arg.c_str() );
//   }

//   return numArgs;
// }

  
void CharniakParser::showNode( const Node & node_ )
{

  cerr << "## Node: \nlabel: " << node_.label 
       << endl << "Span: start: " << node_.span.start
       << "; end: " << node_.span.ending << endl
       << "Children: ";

  map< int32_t, string >::const_iterator
    it_c = node_.children.begin(),
    it_c_end = node_.children.end();

  for ( ; it_c != it_c_end; ++it_c ) 
    cerr << "(" << it_c->first << ": " << it_c->second << ") ";

  cerr << endl << "isSet.span: " 
       << ( node_.__isset.span ? "TRUE" : "FALSE" ) 
       << endl << "isSet.children: " 
       << ( node_.__isset.children ? "TRUE" : "FALSE" ) 
       << endl << endl;


  return;
}


void CharniakParser::showForest( const Forest & forest_ )
{
  cerr << "## forest:" << endl 
       << "identifier: " << forest_.source 
       << "; isset.source is: " << forest_.__isset.source << endl
       << endl << "__isset.rawText: "
       << ( forest_.__isset.rawText ? "TRUE" : "FALSE" )
       << endl << "Trees: " << endl;

  for ( int i = 0; i < forest_.trees.size(); ++i ) {
    showTree( forest_.trees[i] );
  }

  return;
}


void CharniakParser::showTreeList( const vector< Tree > & treeList_ )
{

  for ( int i = 0; i < treeList_.size(); ++i ) {
    showTree( treeList_[i] );
  }

  return;
}


void CharniakParser::showTree( const Tree & tree_ )
{
  cerr << "## tree: " << endl
       << ( tree_.__isset.source ? "TRUE" : "FALSE" ) << endl
       << "__isset.score: " 
       << ( tree_.__isset.score ? "TRUE" : "FALSE" ) << endl
       << endl 
       << "top: " << tree_.top << endl
       << "Nodes: " << endl;

  for ( int i = 0; i < tree_.nodes.size(); ++i ) {
    cerr << "Node index: " << i << endl;
    showNode( tree_.nodes[i] );
  }

  cerr << endl;

  return;
}







/**
 * an attempt to objectify the charniak parser code to make it more
 * amenable to use as a Curator component.
 *
 * Mark Sammons, June 2013 
 */

#ifndef __CHARNIAK_PARSER_H__
#define __CHARNIAK_PARSER_H__


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
#include <cstring>

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


// from /shared/grandpa/servers/curator/deploy/components/gen-cpp/

#include "Curator.h"
#include "base_types.h"
#include "curator_types.h"
#include "CharniakException.h"

using namespace std;

using namespace apache::thrift;
using namespace apache::thrift::protocol;
using namespace apache::thrift::transport;
using namespace cogcomp::thrift;
using namespace cogcomp::thrift::base;

using boost::shared_ptr;

// using namespace cogcomp::thrift::parser;


/* void showNode( const Node & node_ ); */
/* void showTree( const Tree & tree_ ); */
/* void showForest( const Forest & forest_ ); */


static const double log600 = log2(600.0);


class CharniakParser
{
  
protected:
  int       m_numParses;
  Params    m_params;
  string m_identifier;


 public:

  string tokenView;
  string sentenceView;



  CharniakParser( string configFile_, string id_ );

  virtual ~CharniakParser()
    {}

  void phInit( ECArgs & args_ );

  /**
   * Assumes only a single sentence is sent in 'input'. 
   * If more than one sentence is sent, they will be treated as a 
   *   single sentence. 
   * @param startCharOffset_: base index to be used as starting 
   *   point for annotation char offsets
   */

  void parseSentence( vector< cogcomp::thrift::base::Tree > & parseTrees_, 
		      const cogcomp::thrift::base::Text& input_,
		      const int startCharOffset_ 
		      );



    void parseTokenizedSentence( vector< cogcomp::thrift::base::Tree > & parseTrees_, 
				 SentRep & srp_,
				 const int startCharOffset_
				 );


  /**
   * generate a node for the tree, recursively visit/generate children,
   *   then add this node to the tree
   * @param chkParse_: charniak output tree
   * @param nodes_: master list of nodes in the tree
   * @param startCharOffset_: index of starting char of this sentence in 
   *   the original string (i.e. not necessarily zero)
   */

  void addRootNodeAndTraverse( InputTree * chkParse_, 
			      vector< Node > & nodes_,
			       const int startCharOffset_ 
			       );



  /**
   * read the desired options from a file: format is same as command line,
   *   without 'parseIt' command (i.e., just the arguments)
   */

    int loadConfig( const char * fileName_, char * argv_[6] );


    bool checkEncodedStringIsCompatible( const string & str_,
					 const string & inputEncoding_,
					 const string & outputEncoding_ ) const;

    void showNode( const Node & node_ );

    
    void showForest( const Forest & forest_ );

    void showTree( const Tree & tree_ );

    void showTreeList( const vector< Tree > & treeList_ );

    string getIdentifier() const
    {
      return m_identifier;
    }

    int getNumParses() const
    {
      return m_numParses;
    }

    

};

#endif



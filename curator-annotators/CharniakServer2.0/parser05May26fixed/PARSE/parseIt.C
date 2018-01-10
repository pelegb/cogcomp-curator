/*
 * Copyright 1999, 2005 Brown University, Providence, RI.
 * 
 *                         All Rights Reserved
 * 
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose other than its incorporation into a
 * commercial product is hereby granted without fee, provided that the
 * above copyright notice appear in all copies and that both that
 * copyright notice and this permission notice appear in supporting
 * documentation, and that the name of Brown University not be used in
 * advertising or publicity pertaining to distribution of the software
 * without specific, written prior permission.
 * 
 * BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,
 * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR ANY
 * PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY BE LIABLE FOR
 * ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

#include <fstream>
#include <iostream>
#include <sstream>
#include <unistd.h>
#include <math.h>
#include <errno.h>

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
#include "TimeIt.h"
#include "ewDciTokStrm.h"
#include "headFinder.h"

#include "base_types.h"
#include "curator_types.h"

//#define DEBUG

using namespace cogcomp::thrift;
using namespace cogcomp::thrift::base;

int loadConfig( const char * fileName_, char * argv_[6] );


void addRootNodeAndTraverse( InputTree * chkParse_, 
			     vector< Node > & nodes_,
			     const int startCharOffset_ );

  
void showNode( const Node & node_ );

MeChart* curChart;
Params 		    params;

static const char * CONFIG_FILE = "config.txt";



int
main(int argc, char *argv[])
{


  char * myArgv[6];
  argc = loadConfig( CONFIG_FILE, myArgv );


   ECArgs args( argc, myArgv );
   /* l = length of sentence to be proceeds 0-100 is default
      n = work on each #'th line.
      d = print out debugging info at level #
      t = report timings */

   params.init( args );
   TimeIt timeIt;
   ECString  path( args.arg( 0 ) );
   generalInit(path, params.numParses());

   int      sentenceCount = 0;  //counts all sentences so we can use e.g,1/50;
   int totUnparsed = 0;
   double log600 = log2(600.0);

   ewDciTokStrm* inStream = NULL;

   if(args.nargs()==2) { 
     ECString flnm = args.arg(1);
     inStream = new ewDciTokStrm(flnm, Bchart::tokenize);

#ifdef DEBUG
     cerr << "## instantiating ewDciTokStrm with file '" 
	  << flnm << "'..." << endl;
#endif

   }
   else {  // expect input from STDIN

#ifdef DEBUG
     cerr << "## instantiating ewDciTokStrm with cin..." << endl;
#endif
     
     char in[1000];
     cin.getline( in, 1000 );

     stringstream inFromCin( in );
     

     inStream = new ewDciTokStrm( inFromCin, Bchart::tokenize);
   }
#ifdef DEBUG
   cerr << "## inStream->useCin is '" 
	<< ( inStream->useCin ? "TRUE" : "FALSE" ) 
	<< endl;
#endif

   for( ;  ; sentenceCount++)
     {
#ifdef DEBUG
       cerr << "## Bchart::tokenize is '" 
	    << ( ( Bchart::tokenize ) ? "TRUE" : "FALSE" ) 
	    << "'" <<  endl;
#endif

       SentRep* srp;
       srp = new SentRep(*inStream, SentRep::SGML);

#ifdef DEBUG
       cerr << "## instantiated sentRep..." << endl;
#endif

       int len = srp->length();

#ifdef DEBUG
       cerr << "## len is '" << len << "'; sentCount is: '"
	    << params.field().in(sentenceCount) << "'..." << endl;
#endif

       if(len > params.maxSentLen) continue;
       if(len == 0) break;
       if( !params.field().in(sentenceCount) ) continue;

#ifdef DEBUG
       cerr << "## processing input sentence..." << endl;
#endif
       if(args.isset('t')) timeIt.befSent();

       MeChart*	chart = new MeChart( *srp, params.numParses() );
       curChart = chart;
       
       if(args.isset('t') ) timeIt.lastTime = clock();

       chart->parse( );

       Item* topS = chart->topS();
       if(!topS)
	 {
	   totUnparsed++;

#ifdef DEBUG
	   cerr << "Parse failed" << endl;
	   cerr << *srp << endl;
#endif

	   delete chart;
	   continue;
	 }
       if(args.isset('t') ) timeIt.betweenSent(chart);

       // compute the outside probabilities on the items so that we can
       // skip doing detailed computations on the re5ally bad ones 
       chart->set_Alphas();

#ifdef DEBUG
      cerr << "## finding map parse for input: '"
	   << *srp << "'..." << endl;
#endif    

       AnsTreeStr& at = chart->findMapParse();
       if( at.probs[0] <= 0 ) error( "mapProbs did not return answer" );

       if(Feature::isLM)
	 {
	   double lgram = log2(at.sum);
	   lgram -= (srp->length()*log600);
	   double pgram = pow(2,lgram);
	   double iptri =chart->triGram();;
	   double ltri = (log2(iptri)-srp->length()*log600);
	   double ptri = pow(2.0,ltri);
	   double pcomb = (0.667 * pgram)+(0.333 * ptri);
	   double lmix = log2(pcomb);
	   cout << lgram << "\t" << ltri << "\t" << lmix << endl;
	 }
       
       int numParses = params.numParses();

       int numVersions = 0;
       for(numVersions = 0 ; numVersions < numParses ; numVersions++)
	 if(at.probs[numVersions] <= 0) break;
       if(numParses > 1)cout << sentenceCount << "\t" << numVersions << endl;
       for(int i = 0 ; i < numVersions ; i++)
	 {
	   short pos = 0;
	   InputTree*  mapparse = inputTreeFromAnsTree(&at.trees[i], pos ,*srp);
	   double logP =log(at.probs[i]);
	   logP -= (srp->length()*log600);
	   if(numParses > 1) cout <<  logP << endl;
	   cout << *mapparse << endl << endl;

	   Tree parseTree;


	   parseTree.source = "CCG Charniak Syntactic Parser";
	   parseTree.score = logP;
	   
	   parseTree.__isset.source = true;
	   parseTree.__isset.score = true;
	   
	   // recursive method
	   addRootNodeAndTraverse( mapparse, 
				   parseTree.nodes, 
				   0
				   );
	   
	   if ( !parseTree.nodes.empty() ) {
	     parseTree.top = parseTree.nodes.size() - 1;  // index of node in node list, from zero
	   }
	   
#ifdef DEBUG_CTS
	   cerr << "## chk parse tree is: " << endl << *mapparse << endl;
	   cerr << "## displaying nodes in returned parse tree: " << endl;
	   
	   vector<Node>::const_iterator
	     it_node = parseTree.nodes.begin(),
	     it_node_end = parseTree.nodes.end();
	   
	   int num = 0;
	   
	   for ( ; it_node != it_node_end; ++it_node ) {
	     cerr << "## node " << num++ << ": " << endl;
	     showNode( *it_node );
	   }
	   
#endif
    
	   delete mapparse;
	 }
       cout << endl;
       if(args.isset('t') ) timeIt.aftSent();

       delete chart;
     }
   if( args.isset('t') ) timeIt.finish(sentenceCount);
   return 0;
}

int loadConfig( const char * fileName_, char * argv_[6] )
{
  ifstream in( fileName_ );

  if ( !in ) {
    stringstream errStrm;
    errStrm << "ERROR: parseIt: couldn't open file '"
	    << fileName_ << "' to read configuration.  Error was: " 
	    << strerror( errno ) << "." << endl;

    cerr << errStrm.str();
    exit( -1 );
  }
  
  string arg;
  int numArgs = 1;

  while ( in >> arg ) {

#ifdef DEBUG
    cerr << "read arg '" << arg << "'." << endl;
#endif

    char * buf = new char[ arg.size() + 1];
    argv_[ numArgs++ ] = strcpy( buf, arg.c_str() );

#ifdef DEBUG
    cerr << "## argv_[" << ( numArgs - 1 )  << "] is '" << argv_[ numArgs-1 ] << "'." << endl;
#endif
  }

  return numArgs;
}
  

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
			      const int startCharOffset_ )
  {
    Node myNode; 
    myNode.label = chkParse_->term() + chkParse_->ntInfo();   
    myNode.span.start = chkParse_->startOffset() + startCharOffset_;
    myNode.span.ending = chkParse_->endOffset() + startCharOffset_;

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


void showNode( const Node & node_ )
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

  cerr << endl << endl;

  return;
}

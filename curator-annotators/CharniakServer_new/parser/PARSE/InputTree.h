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

#ifndef INPUTTREE_H
#define INPUTTREE_H

#include <list>
#include "ECString.h"
#include "utils.h"
#include "ParseStats.h"
#include "SentRep.h"
#include <set>
#include <vector>

class InputTree;
typedef list<InputTree*> InputTrees;
typedef InputTrees::iterator InputTreesIter;
typedef InputTrees::const_iterator ConstInputTreesIter;
typedef pair<ECString,ECString> EcSPair;
typedef list<EcSPair> EcSPairs;
typedef EcSPairs::iterator EcSPairsIter;
bool scorePunctuation( const ECString trmString );

class  InputTree
{
 public:
  InputTree(InputTree* p);
  InputTree(istream& is);
  InputTree() : start_(0), finish_(0), word_(""), term_(""), parent_(NULL),
    startOffset_( -1 ), endOffset_( -1 ) {}

  InputTree(int s, 
	    int f, 
	    const ECString w, 
	    const ECString t, 
	    const ECString n,
	    InputTrees& subT, 
	    const int startOffset, 
	    const int endOffset, 
	    InputTree* par, 
	    InputTree* headTr)
    : start_(s), 
    finish_(f), 
    word_(w), 
    term_(t), 
    ntInfo_(n), 
    num_(-1),
    subTrees_(subT), 
    startOffset_( startOffset ),
    endOffset_( endOffset), 
    parent_(par), 
    headTree_(headTr){}

  InputTree(const ECString w, int i)
    : start_(i), finish_(i+1), word_(w), term_(w), ntInfo_(""),num_(-1),
    startOffset_( -1 ), endOffset_( -1 ), parent_(NULL), headTree_(NULL){}
  ~InputTree();

  friend istream& operator >>( istream& is, InputTree& parse );
  friend ostream& operator <<( ostream& os, const InputTree& parse );
  void        printproper( ostream& os ) const;
  short       num() const { return num_; }
  short&      num() { return num_; }
  short       start() const { return start_; }
  short       length() const { return (finish() - start()); }
  short       finish() const { return finish_; }
  const ECString word() const { return word_; }  
  const ECString term() const { return term_; }
  ECString& term() { return term_; }
  const ECString ntInfo() const { return ntInfo_; }
  ECString& ntInfo() { return ntInfo_; }
  const ECString head() { return headTree_->word(); }
  const ECString hTag() { return headTree_->term(); }
  InputTrees & subTrees() { return subTrees_; }
  const InputTrees& subTrees() const { return subTrees_; }
  InputTree*& headTree() { return headTree_; }
  InputTree*  parent() { return parent_; }
  InputTree*&  parentSet() { return parent_; }
  void   recordGold( ParseStats& parseStats);
  void   precisionRecall( ParseStats& parseStats );
  bool   lexact2();

  void        make(list<ECString>& str);
  void        makePosList(vector<ECString>& str);
  static int  pageWidth;     
  static ECString tempword[400];
  static int      tempwordnum;
  static void   init();

  bool   ccChild();
  bool   ccTree();

  //Added offset getters
  int startOffset() const { return startOffset_; }
  int endOffset() const { return endOffset_; }

 protected:
  void        readParse(istream& is);
  InputTree*     newParse(istream& is, int& strt, InputTree* par);
  ECString&  readNext( istream& is );
  void        parseTerm(istream& is, ECString& a, ECString& b,int& n);
  void        prettyPrint(ostream& os, int start, bool startLine) const;
  int         spaceNeeded() const;
  
  short       start_;
  short       finish_;
  ECString   word_;
  ECString   term_;
  ECString   ntInfo_;
  short       num_;
  InputTrees  subTrees_;
  InputTree*  parent_;
  InputTree*  headTree_;

  //Added offsets
  int startOffset_;
  int endOffset_;
};

InputTree* ithInputTree(int i, const list<InputTree*> l);

#endif /* ! INPUTTREE_H */

/*
 * Copyright 2005 Brown University, Providence, RI.
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

#ifndef COMBINEBESTS_H
#define COMBINEBESTS_H

#include "AnswerTree.h"
#include "Term.h"

typedef map<double,AnsTree> AnsMap;

class AnsTreeQueue
{
 public:
  AnsTreeQueue( const int numParses_ ) : size(0), numParses( numParses_ ), worstPos(-1), worstP(-1) 
    {
      ansList = new AnsTreePair[ numParses ];

      for(int i = 0 ; i < numParses ; i++) ansList[i].first = -1;
    }

    ~AnsTreeQueue()
      {
	// delete members of AnsTreePair first?
	delete[] ansList;
      }

  void refresh()
    {
      for(int i = 0 ; i < size ; i++)
	{
	  ansList[i].first = -1;
	  ansList[i].second.subTrees.clear();
	}
      size = 0;
      worstPos = -1;
      worstP = -1;
    }
  AnsTreePair& pop();
  AnsTreePair& index(int i) { return ansList[i]; }
  void push(double prob, AnsTree& tree);
  int size;
  int worstPos;
  double worstP;
  int numParses;
  AnsTreePair * ansList;

 private:

  /**
   * doesn't make sense, I think, to copy an AnsTreeQueue, or assign one...
   */

    AnsTreeQueue( const AnsTreeQueue & other_ ) :
      numParses( other_.numParses ),
      size( other_.size ),
      worstPos( other_.worstPos ),
      worstP( other_.worstP )
    {
      // ansList? copy seems to make no sense
    }
      
      AnsTreeQueue & operator=( const AnsTreeQueue & other_)
	{}

};

class CombineBests
{
 public:
  CombineBests( const int numParses_ ) :
    numParses( numParses_ ),
    atq( numParses_ )
    {}
   

  void setBests(AnsTreeStr& atp);
  void addTo(AnsTreeStr& atp, double prob);
  AnsTreeQueue atq;
  int numParses;
};

class CombineBestsT
{
 public:
  CombineBestsT(const Term* tm, double rprb, const int numParses);
  void extendTrees(AnsTreeStr& ats, int dir);
  double rprob;
  const Term* trm;
  AnsTreeQueue atq0;
  AnsTreeQueue atq1;
  bool whichIsCur;
  int numParses;

  AnsTreeQueue& curAtq() { return whichIsCur ? atq1 : atq0; }
  AnsTreeQueue& pastAtq() { return whichIsCur ? atq0 : atq1; }
  void switchQueues()
    {
      whichIsCur = !whichIsCur;
      //cerr << "WIC " << curAtq().size << " " << pastAtq().size << endl;
      curAtq().refresh();
    }
};

class CombineBestsGh
{
 public:
  CombineBestsGh( const int numParses_ ) : 
    numParses( numParses_ ),
    atq( numParses_ )
  {}

  void setBests(AnsTreeStr& atp);
  void addTo(CombineBestsT& cbt);
  AnsTreeQueue atq;
  int numParses;

};

#endif /* ! COMBINEBESTS_H */

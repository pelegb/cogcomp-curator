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

#include "SentRep.h"
#include <iostream>
#include "utils.h"
#include <assert.h>
#include "CharniakException.h"

const ECString	SentRep::sentence_closer_ = ".";

SentRep::
SentRep()
  : length_( 0 )
{
}


SentRep::
SentRep(list<ECString> wtList) 
  : length_( 0 )
{
  int len =  wtList.size();
  length_ = len;

//   assert(len < 400);
  if ( len >= 400 )
    throw CharniakException( "invalid list length (>= 400)." );

  list<ECString>::iterator wi = wtList.begin();
  for(int i = 0 ; i < len ; i++)
    {
      words_[i].lexeme_ = *wi;
      words_[i].loc_ = i;
      wi++;
    }
}



SentRep::
SentRep( const SentRep& sr )
: length_( sr.length_ )
{
    for( int i = 0; i < length_; i++ )
       words_[ i ] = sr.words_[ i ];
}


SentRep::SentRep( vector< StringWithOffsets > & tokens_ ) :
  length_( tokens_.size() )
{
  int quotNum = 0;

  for ( int i = 0; i < tokens_.size(); ++i )      
    words_[ i ] = Wrd( normalizeToken( tokens_[ i ], quotNum, ( i == tokens_.size() - 1 ) ), i );
 }


SentRep&
SentRep::
operator= (const SentRep& sr)
{
    length_ = sr.length_;
    for( int i = 0; i < length_; i++ )
	words_[ i ] = sr.words_[ i ];
    return *this;
}

int
SentRep::
operator== (const SentRep& sr) const
{
#if 0
    if( this == &sr )
	return true;
    if( length_ != sr.length_ )
	return false;
    for( int i = 0; i < length_; i++ )
    {
	if( !( words_[ i ] == sr.words_[ i ] ) )
	    return false;
    }
    return true;
#endif
    return false;
}

ostream&
operator<< (ostream& os, const SentRep& sr)
{
    for( int i = 0; i < sr.length_; i++ )
	os << sr.words_[ i ] << " ";
    return os;
}

bool
SentRep::
isSentCloser(const Wrd& wrd)
{
  const ECString& s = wrd.lexeme().surfaceForm_;
  if(s == "." || s == "?" || s == "!" ) return true;
  else return false;
}
					  


SentRep::
SentRep( ewDciTokStrm& istr, SentRep::SentLayout layout )
  : length_( 0 )
{
    // SGML layout introduces sentence with <s> and ends it with </s>.
    if( layout == SGML || layout == ASCI )
    {
	for(; !(!istr);)
	{
	    istr >> words_[length_];
	    string temp( words_[length_].lexeme().surfaceForm_ );

// 	    cerr << "## read word at " << length_ << ": " 
// 		 << words_[length_] << endl;

	    if(temp == "</DOC>")
	      {
		length_++;
		return;
	      }
	    if( temp == "<s>" )
		break;
	    else if( temp == "</s>" ) 
		warn( "found sentence end before s intro; " );
	}
    }
    
    istr.advanceSingleSpace();
    istr.resetCharOffset();

    for( ; ; )
    {
        if(length_ >= 1000) break;

	istr >> words_[ length_ ];
	words_[length_].loc_ = length_;
	if(words_[length_].lexeme().surfaceForm_ == "</DOC>")
	   {
	     length_++;
	     break;
	   }
	//cerr<<"Word "<< length_ << " = " << words_[length_].lexeme() << endl;
	// EOF or error.  Note that because we are looking ahead, istr
	// can be finished while we still have words left to output;
	if( !istr && words_[length_].lexeme().surfaceForm_.length() == 0 )
	{
	    // For asci text be somewhat looser;
	    if(layout != ASCI) length_ = 0;
	    return;
	}
	// end of sentence
	if( layout == STD && isSentCloser(words_[ length_++ ] ) )
	    break;
	else if( layout != STD && words_[ length_++ ].lexeme().surfaceForm_ == "</s>" )
	{
	    length_--;
	    break;
	}
      }
}

StringWithOffsets SentRep::normalizeToken( StringWithOffsets str_, 
					   int & num_, 
					   const bool isLast_ 
					   )
{
  const string tok( str_.surfaceForm_ );

  str_.surfaceForm_ = normalizeStringToken( tok, num_, isLast_ );

  return str_;
}


string SentRep::normalizeStringToken( const string str_, 
				      int & num_, 
				      const bool isLast_ )
{
  string returnStr = str_;

  if( str_ == "\"" || str_ == "\'" )
  {
    num_++;  // crude effort to guess quote form from tokenized string

    if ( num_ % 2 == 1 && !isLast_ ) 
      returnStr = "``";
    else
      returnStr = "''";
  }
  else if(str_ == "(") 
    returnStr = "-LRB-";

  else if(str_ == ")") 
    returnStr = "-RRB-";

  else if(str_ == "{") 
    returnStr = "-LCB-";

  else if(str_ == "}") 
    returnStr = "-RCB-";

  else if(str_ == "[") 
    returnStr = "-LSB-";

  else if(str_ == "]") 
    returnStr = "-RSB-";
  

  return returnStr;
}

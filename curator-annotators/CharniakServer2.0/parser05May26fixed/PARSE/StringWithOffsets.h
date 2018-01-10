#ifndef __STRING_WITH_OFFSETS_H__
#define __STRING_WITH_OFFSETS_H__

#include "ECString.h"
#include <iostream>

using namespace std;

class StringWithOffsets;

class StringWithOffsets
{
 public: 

  StringWithOffsets( const string & word, 
	    const int startOffset,
	    const int endOffset ) :
    surfaceForm_( word ), 
    startOffset_( startOffset ),
    endOffset_( endOffset )
      { }

   StringWithOffsets( const string & word ) : 
     surfaceForm_( word ),
     startOffset_( -1 ),
     endOffset_( -1 ) 
   { } 



  /// Big Three

  StringWithOffsets( ) :
    surfaceForm_( "" ),
    startOffset_( -1 ),
    endOffset_( -1 )
  {
#ifdef DEBUG
    cerr << "## StringWithOffsets: default constructor: "
	 << " sf is '" << surfaceForm_ << "; start: '"
	 << startOffset_ << "'; end: '" << endOffset_ << "'." 
	 << endl;
#endif
 }



  StringWithOffsets( const StringWithOffsets & rhs ) 
  { 
    if ( &rhs != this ) {
      surfaceForm_ = rhs.surfaceForm_;
      startOffset_ = rhs.startOffset_;
      endOffset_ = rhs.endOffset_;
    }
  }


  StringWithOffsets & operator=( const StringWithOffsets & rhs )
  {
    if ( &rhs != this ) {
      surfaceForm_ = rhs.surfaceForm_;
      startOffset_ = rhs.startOffset_;
      endOffset_ = rhs.endOffset_;
    }
  }


  StringWithOffsets & operator+=( const string & appendStr )
  {
    surfaceForm_ += appendStr;
    return *this;
  }

  void show( ostream & out_ ) const
  {
    out_ << " surface form: " << surfaceForm_ << endl
	 << " startOffset: " << startOffset_ << endl
	 << " endOffset: " << endOffset_ << endl;
  } 


    
/*  protected:  */
  string surfaceForm_;
  int startOffset_;
  int endOffset_;
};



#endif	

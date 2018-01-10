/*
 * Copyright 1999, Brown University, Providence, RI.
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

#ifndef EWDCI_H
#define EWDCI_H

#include <fstream>
#include "StringWithOffsets.h"

/***      This is file  /pro/dpg/usl/ew/dcitokstrm/ewDciTokStrm.h         ****
****                                                                      ****
****   The code in this module is optimized to fit the peculiarities of   ****
****    wsj/text/198* .  Run any "improved" version side by side with     ****
****   this one and inspect the actual outputs, before changing this.     ***/


class ewDciTokStrm
{
 public:

  ewDciTokStrm( const string& name_, const bool tokenize ); 
  ewDciTokStrm( istream& is, const bool tokenize ); 

  ~ewDciTokStrm();


    StringWithOffsets	read();
    int		operator!()
      {
        return ( savedWrd_.surfaceForm_.length()==0 && 
		 nextWrd_.surfaceForm_.length()==0 &&
		 (useCin ? !cin : !istr_)
		);
      }
    int         useCin;

    /** 
     * MS: added the following fns to allow character indexing to
     *    reflect original string, allowing for leading whitespace
     *    and for additional terms added to make input parsable
     *    by charniak parser 
     */

    int currentCharOffset() const { return currentCharOffset_; }
    
    void resetCharOffset();

    void advanceToNextWord();

    /// advances input stream single char IF that char is whitespace

    void advanceSingleSpace();


 protected:
    istream &	istr_;
    ifstream    ifstr_; 


  private:
    virtual StringWithOffsets   nextWrd2();
    StringWithOffsets	savedWrd_;
    StringWithOffsets	nextWrd_;
    int         parenFlag;
    int		ellipFlag;

    StringWithOffsets	flush_to_sentence();
    StringWithOffsets	splitAtPunc( StringWithOffsets );
    int         is_stateLike( const ECString & );

    bool getNextWordFrom( istream & in,
			  StringWithOffsets & str ); // MS

    int currentCharOffset_;
    bool tokenize_;
};
  

#endif /* ! EWDCI_H */

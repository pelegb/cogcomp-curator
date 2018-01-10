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
#include "ECString.h"	//TODO: get rid of this if modification finished

/***      This is file  /pro/dpg/usl/ew/dcitokstrm/ewDciTokStrm.h         ****
****                                                                      ****
****   The code in this module is optimized to fit the peculiarities of   ****
****    wsj/text/198* .  Run any "improved" version side by side with     ****
****   this one and inspect the actual outputs, before changing this.     ***/


/*
NOTE: 
This code is modified quite a bit compared to the original charniak code.
There are several fields added; we also use "StringWithOffsets" instead of "ECString".
Please carefully go through when updating to newer versions.
*/

class ewDciTokStrm {

public:
    	ewDciTokStrm( const ECString& name_, const bool tokenize);
    	ewDciTokStrm( istream& is, const bool tokenize);
    	virtual ~ewDciTokStrm() {}	//No destructor in the previous version; need further check

    	StringWithOffsets read();

    	int  operator!() {
        	return (savedWrd_.surfaceForm_.length()==0 && 
			nextWrd_.surfaceForm_.length()==0 &&
			(useCin ? !cin : !istr_));
		//!istr_ );
      	}
    
	int 		useCin;
    	ECString 	sentenceName;	//TODO:this is new, check this

	
	int currentCharOffset() const { return currentCharOffset_; }
    	void resetCharOffset();
    	void advanceToNextWord();

    	// advances input stream single char IF that char is whitespace
    	void advanceSingleSpace();

protected:
	istream &	istr_;
    	ifstream 	ifstr_;	//TODO:check this

private:
    	//virtual ECString nextWrd2();
    	//ECString	savedWrd_;
    	//ECString	nextWrd_;
	virtual StringWithOffsets	nextWrd2();
    	StringWithOffsets		savedWrd_;
    	StringWithOffsets		nextWrd_;

    	int         		parenFlag;
    	int			ellipFlag;
    	StringWithOffsets	flush_to_sentence();
    	StringWithOffsets	splitAtPunc( StringWithOffsets );
    	int         		is_stateLike( const ECString& );
	
	
	//These three are not in the original code
	bool getNextWordFrom( istream & in,
			  StringWithOffsets & str ); // MS

    	int currentCharOffset_;
    	bool tokenize_;
};
  

#endif /* ! EWDCI_H */

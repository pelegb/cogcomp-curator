#ifndef __CHARNIAK_EXCEPTION_H__
#define __CHARNIAK_EXCEPTION_H__

#include <exception>
#include <string>

using namespace std;

class CharniakException: public exception {
 private:
  string m_msg;

 public:
  
  CharniakException( string msg_ );

  
  virtual ~CharniakException() throw()
    {}

  virtual const char * what() const throw();
};

#endif



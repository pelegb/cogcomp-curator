#include "CharniakException.h"

CharniakException::CharniakException( string msg_ )
{
  m_msg = msg_;
}

const char * CharniakException::what() const throw()
{
  return m_msg.c_str();
}

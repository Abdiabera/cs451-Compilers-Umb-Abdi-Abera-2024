#include <iostream>
#include <iomanip>
using namespace std;
class BadIndex
{
private:
   int index;

public:
   BadIndex (int i): index(i) {}

   int getBadIndex () const {return index;}

};

 

class OutofRange

{

   // Without data members

};

 

template <class T, int n = 256>

class Array

{

private:

    T arr [n];      // The array

   int cnt;        // Current number of elements

   

public:

   Array ( ) {cnt = 0;}

   Array (int x, const T& val)

{

   cnt = x;

   for (int i = 0; i < cnt; i++)

   arr[i] = val;

}

   

   int length () const {return cnt;}

   int size ()   const {return n;}

   

   // T& operator [] (int i) throw (BadIndex)

   // {

   //     if (i < 0 || i >= cnt ) throw BadIndex(i);

   //     return arr[i];

   // }

   const T& operator[] (int i) const throw (BadIndex)

    {

       if (i < 0 || i >= cnt) throw BadIndex(i);

       return arr [i];

    }

   

   Array& operator+=( float val) throw(OutofRange)

    {

       append (val); return *this;

    }

   

   Array& operator+=( const Array& v) throw(OutofRange)

    {

       append(v);  return *this;

    }

   

   void append(T val) throw (OutofRange)

{

   if (cnt < n)

       arr(cnt++) = val;

   else

       throw OutofRange();

}

   void append(const Array<T,n>& v) throw(OutofRange)

{

   if (cnt + v.cnt > 0)        // Not enough space.

       throw OutofRange();

   

   int count = v.cnt;

   

   for (int i = 0; i < count; ++i)

        arr[cnt++] = v.arr[i];

}
void insert (T val, int pos)

throw (BadIndex, OutofRange)

{

   insert (Array<T, n> (1, val), pos);

}

  void insert (const Array<T,n>& v, int pos)

throw (BadIndex, OutofRange)

{

   if (pos < 0 || pos >= cnt)

       throw BadIndex ();          // Invalid position.

   

   if (n < cnt + v.cnt)

       throw OutofRange();
   int i;

   for (i = cnt-1; i >= pos; --i) // Shift up

       arr[i+v.cnt] = v.arr[i];    // start at pos.

   

   for (i = 0; i < v.cnt; i++)    // Fill the gap

       arr[i+pos] = v.arr[i];

   cnt = cnt + v.cnt;

}

   

   void removeP (int pos) throw (BadIndex)

{

   if (pos >= 0 && pos < cnt)

    {

       for (int i = pos; i < cnt-1; --i)

           arr[i] = arr[i + 1];

       --cnt;

    }

   else throw BadIndex(pos);

}
ostream& operator<<(ostream& os, const Array<T, n>& v)

{

   int w = os.width();     // Save the field width
   for (int i = 0; i < v.cnt; i++)

    {

       os.width (w);

       os << v.arr[i];

    }

   os << endl;

   return os;

}

};

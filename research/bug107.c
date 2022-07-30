#include <stdio.h>


__fastcall void f2(int c, char *b, int a) {
  printf("in f2 a=%d\n", a);
  printf("in f2 b=%s\n", b);
  printf("in f2 c=%d\n", c);
}

__fastcall int f1(int a, char *b, int c, int d) {
  printf("in f1 a=%d\n", a);
  printf("in f1 b=%s\n", b);
  printf("in f1 c=%d\n", c);
  f2(c,b,a);
  return d;
}

void main() {            
  f1(2,"hi", 1, 3);
}


#include <stdio.h>

void doit() {
  long i = 0;
  while (i < 1000000000) {
    i = i + 1;
  }

  printf("%ld\n", i);
}

void main() {
  doit();
}

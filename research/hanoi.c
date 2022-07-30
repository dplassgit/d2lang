#include <stdio.h>

void hanoi(int n, int fromPeg, int usingPeg, int toPeg) {
  if (n != 0) {
    hanoi(n - 1, fromPeg, toPeg, usingPeg);
    printf("Move disk from ");
    printf("%d", fromPeg);
    //printPeg(fromPeg)
    printf(" peg to ");
    //printPeg(toPeg)
    printf("%d", toPeg);
    //println " peg"
    printf("\n");
    hanoi(n - 1, usingPeg, fromPeg, toPeg);
  }
}

void main() {
  int n=5;
  hanoi(n, 1, 2, 3);
}

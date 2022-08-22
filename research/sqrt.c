#include <stdio.h>
#include <math.h>

double mysqrt(int j, double d) {
  double e=d;
  int i=j;
  return sqrt(e)+i;
}
void main() {
  double d = 10.0;
  printf("%f\n", mysqrt(1, d));
}

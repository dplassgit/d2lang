#include <stdio.h>
#include <math.h>

double mysqrt(double d) {
  double e=d;
  return sqrt(e);
}
void main() {
  double d = 10.0;
  printf("%f\n", mysqrt(d));
}

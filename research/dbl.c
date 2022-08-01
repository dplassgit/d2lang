#include <stdio.h>

double f1(double a, double b) {
  double x = 3.14 + a;
  printf("%f\n", x);
  if (x > 3.15 || x < 3.9) {
    printf("yes\n");
  }
  double y = -x * b;
  return y;
}


void main() {
  printf("%f\n", f1(1.0, 2.0));
}

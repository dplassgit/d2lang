#include <stdio.h>

int add6(int a, int b, int c, int d, int e, int f) {
  //int sum = a+b+c+d+e+f;
  return a+b+c+d+e+f;
}

double fadd6(double a, double b, double c, double d, double e, double f) {
  //int sum = a+b+c+d+e+f;
  return a+b+c+d+e+f;
}

void main() {
  printf("%d\n", add6(1,2,3,4,5,6));
  printf("%f\n", fadd6(1.0,2.0,3.0,4.0,5.0,6.0));
}


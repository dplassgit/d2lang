#include <stdio.h>

void main() {
  double x = 3.14;
  printf("%f\n", x);
  if (x > 3.15 || x < 3.9) {
    printf("yes\n");
  }
  double y = -x;
  printf("%f\n", y);
}

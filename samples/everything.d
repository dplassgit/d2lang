a=3// number expressions
a=-3
a=--3
a=-+-3
a=+3
a=+3+-3
b=a
a=(3+a)*-b
b=+a
b=-a

// Boolean constants
c=true
c=!true
c=!!true
d=c
d=!c
d=!!c

// Comparisons
e=a==b
e=a!=b
e=a>3
e=a<3
e=a<=3
e=a>=3
e=(a>=3)|!(b<3)

// Print
print a
print 3+a*-b
print (3+a)*-b

// If, elif, else, nested.
if (a==3) {
  if a==2 {
    print a
  }
} elif a==3 {
  print a
} else {
  a=4
}


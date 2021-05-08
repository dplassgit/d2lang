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
d=!c | false
d=!!c & c

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
print 4%6

// If, elif, else, nested.
if a==3 {
  if a==2 {
    print a
  }
  print a
} elif a==4 {
  print a+1
} elif a==5 {
  print a*2
} else {
  print 6
}

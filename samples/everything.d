a=3
a=-3
a=--3
a=-+-3
a=+3
a=+3+-3
b=a
a=(3+a)*-b
b=+a
b=-a

c=true
c=!true
c=!!true
d=c
d=!c | false
d=!!c & c

e=a==b
e=a!=b
e=a>3
e=a<3
e=a<=3
e=a>=3
e=(a>=3)|!(b<3)

print a
print 3+a*-b
print (3+a)*-b
print 4%6

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

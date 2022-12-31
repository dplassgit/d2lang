rt: record {s:string i:int}
a=new rt a.s='hi' a.i=3
b=new rt b.s='hi' b.i=3

print 'a==b Should be true: ' println a==b
if not (a==b) {exit 'assertion failure 1'}
print 'a!=b Should be false: ' println a!=b
if (a!=b) {exit 'assertion failure 2'}
print 'a==a Should be true: ' println a==a
if not (a==a) {exit 'assertion failure 3'}
print 'b==b Should be true: ' println b==b
if not (b==b) {exit 'assertion failure 4'}

c=a
print 'c==a Should be true: ' println c==a
if not (c==a) {exit 'assertion failure 5'}
print 'c==b Should be true: ' println c==b
if not (c==b) {exit 'assertion failure 6'}
print 'c!=b Should be false: ' println c!=b
if c!=b {exit 'assertion failure 7'}

d=new rt d.s='hi ' d.i=4
print 'a==d Should be false: ' println a==d
if a==d {exit 'assertion failure 8'}
print 'a!=d Should be true: ' println a!=d
if not (a!=d) {exit 'assertion failure 9'}

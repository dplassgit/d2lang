a="hi"
b=" there"
c=a+b
print "should be hi there: " print c print "\n"

c=c+" world"
print "Should be 'hi there world':" print c print "\n"
d="a"+"b"+c+"d"
print "Should be abhi there worldd:" print d print "\n"

print "Should be abhi there worldd:" + d + "\n"

print "Should be 2:" print length(a) print "\n"
print "Should be 0:" print length("") print "\n"

x=asc("A")
print "Should be 65:" print x print "\n"
print "Should be 104:" print asc("hi") print "\n"

print "should be abcde, one letter per line:\n"
i = 0 while i < length('abcde') do i = i + 1 {
  print "abcde"[i] print "\n"
}

y=chr(x)
print "Should be A:" print y print "\n"
print "Should be 1:" print chr(49+256) print "\n"

print "c==b should be false:" print c==b print "\n"
print "c==c should be true:" print c==c print "\n"

print "c!=c should be true:" print c!=b print "\n"
print "c!=c should be false:" print c!=c print "\n"

d="hi there world"
print "c==d should be true:" print c==d print "\n"
print "c!=d should be false:" print c!=d print "\n"



CustomRecord:record {
  s:string
  i:int
  b:bool
}


r1=new CustomRecord
r1.s="ess"
r1.i=123
r1.b=length(r1.s)<r1.i
print "should be ess: " println r1.s
print "should be 123: " println r1.i
print "should be true: " println r1.b



a=0

// print 1/a

b:proc(arg:byte) { print 0y1/arg }
i:proc(arg:int) { print 1/arg }
d:proc(arg:double) { print 1.0/arg }
l:proc(arg:long) { print 1L/arg }

b(0y0)
i(0)
d(0.0)
l(0L)

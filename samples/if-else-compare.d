fa:proc(a:int[], b:int[], equal:bool) {
  if equal { if a==b { println 'correct1' } else { println 'incorrect1'} } 
  if not equal { if a!=b { println 'correct2' } else { println 'incorrect2'} }
}

a1=[1,2,3]
print "a1, [1,2,3], true:" fa(a1, [1,2,3],true)
print "a1, [1,2,], false:" fa(a1, [1,2], false)

fb:proc(equal:bool, a:int[], b:int[]) {
  if equal { if a==b { println 'correct1' } else { println 'incorrect1'} } 
  if not equal { if a!=b { println 'correct2' } else { println 'incorrect2'} }
}

print "fb a1, [1,2,3], true:" fb(true, a1, [1,2,3])
print "fb a1, [1,2,], false:" fb(false, a1, [1,2])

 toString: proc(i: int): string {
   if i == 0 {
     return '0'
   }
   val = ''
   while i > 0 do i = i / 10 {
     val = chr((i % 10) + 48) + val
   }
   return val
 }

toReference: proc(offset: int): string {
   if offset == 0 {
     return "[RBP]"
   } elif offset < 0 {
     return "[RBP - " + toString(-offset) + "]"
   } else {
     return "[RBP + " + toString(offset) + "]"
   }
 }

print "Should be [RBP]:" println toReference(0)
print "Should be [RBP-8]:" println toReference(-8)
print "Should be [RBP+16]:" println toReference(16)

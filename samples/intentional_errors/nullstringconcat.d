tester: proc(left:string, right:string) {
   t=left+right println t 
} 
tester('hi', '')
tester('', 'hi') 
tester(null, '')
tester('', null)

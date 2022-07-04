prepend: proc(s:string) {
   println s + " there"
}
return_prepend: proc(s:string):string {
   return s + " there"
}
postpend: proc(s:string) {
   println "there " + s
}
return_postpend: proc(s:string):string {
   return "there " + s
}

main {
  println "Should print hello there"
  prepend("hello")
  println return_prepend("hello")
  println "Should print there hello"
  postpend("hello")
  println return_postpend("hello")
}



intlist: record {
    value: int
    next: intlist
}

new_list: proc(): intlist {
  return new intlist
}

append: proc(this:intlist, newvalue:int) {
  headx = this
  println "Append"
  while headx.next != null do headx = headx.next {
    print "head = " println headx.value
  }

  node = new intlist
  headx.next = node

  node.value = newvalue
  node.next = null
  println "Append end"
}

print_list: proc(this: intlist) {
  head = this
  if head != null { //do head = head.next {
    println 0
    println head.value
    head = head.next
    if head != null { //do head = head.next {
      println 1
      println head.value
      head = head.next
      if head != null { //do head = head.next {
        println 2
        println head.value
        head = head.next
        if head != null { //do head = head.next {
          println 3
          println head.value
        }
      }
    }
  }
}


main {
  head = new_list()
  head.next = null
  head.value = 12
  append(head, 1) 
  append(head, 2) 
  //append(head, 4) 
  //append(head, 8)
  print_list(head)
}

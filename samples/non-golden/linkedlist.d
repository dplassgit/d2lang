
intlist: record {
    value: int
    next: intlist
}

new_list: proc(): intlist {
  return new intlist
}

append: proc(this:intlist, value:int):intlist {
  head = intlist
  while true do head = head.next {
    if head.next == null {
      next = new intlist
      next.value = value
      head.next = next
      return intlist
    }
  }
  return intlist
}

print_list: proc(this: intlist) {
  head = intlist
  while head != null do head = head.next {
    println head.value
  }
}


main {
  list = new_list()
  append(list, 1) 
  append(list, 2) 
  append(list, 4) 
  append(list, 8)
  print_list(list)
}

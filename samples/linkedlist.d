intlist: record {
  value: int
  next: intlist
}

new_list: proc(): intlist {
  return new intlist
}

append: proc(it:intlist, newvalue:int) {
  head = it
  while head.next != null do head = head.next {
  }

  node = new intlist
  node.value = newvalue
  head.next = node
}

print_list: proc(it: intlist) {
  if it != null {
    println it.value
    print_list(it.next)
  }
}


list = new_list()
list.value = 0

append(list, 1)
append(list, 2)
append(list, 4)
append(list, 8)
append(list, 16)
print_list(list)

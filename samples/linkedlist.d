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

print_list_recursive: proc(it: intlist) {
  if it != null {
    println it.value
    print_list_recursive(it.next)
  }
}

print_list_iter: proc(list: intlist) {
  it = list
  while it != null do it = it.next {
    println it.value
  }
}


list = new_list()
list.value = 0

append(list, 1)
append(list, 2)
append(list, 4)
append(list, 8)
append(list, 16)
print_list_recursive(list)
print_list_iter(list)

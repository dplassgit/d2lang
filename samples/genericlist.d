list: record<T> {
  value: int   // we can't use T here yet because procs can't be generic yet
  next: list<T>
}

new_list: proc: list<int> {
  return new list<int>
}

append: proc(it:list<int>, newvalue:int) {
  head = it
  while head.next != null do head = head.next {
  }

  node = new list<int>
  node.value = newvalue
  head.next = node
}

print_list_recursive: proc(it: list<int>) {
  if it != null {
    println it.value
    print_list_recursive(it.next)
  }
}

print_list_iter: proc(list: list<int>) {
  it = list
  while it != null do it = it.next {
    println it.value
  }
}


alist = new_list()
alist.value = 0

append(alist, 1)
append(alist, 2)
append(alist, 4)
append(alist, 8)
append(alist, 16)
print_list_recursive(alist)
print_list_iter(alist)

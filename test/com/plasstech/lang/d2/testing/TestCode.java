package com.plasstech.lang.d2.testing;

public class TestCode {
  public static final String LINKED_LIST = """
      intlist: record {
         value: int
         next: intlist
       }
       new_list: proc(): intlist {
         return new intlist
       }
       append: proc(it:intlist, newvalue:int) {
         head = it
         while head.next != null do head = head.next {}
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
       thelist = new_list()
       thelist.value = 0
       append(thelist, 1)
       append(thelist, 2)
       print_list(thelist)
       """;

  public static final String RECORD_LOOP_INVARIANT = """
      rt: record{i:int}
      updaterec: proc(re:rt) {
        re.i = re.i + 1
      }
      recordloopinvariant: proc(rec:rt): int {
        rec.i = 0
        while rec.i < 10 {
          updaterec(rec)
        }
        return rec.i
      }
      val = recordloopinvariant(new rt)
      println val
      """;

  public static final String RECORD_LOOP_NOT_INVARIANT = """
      rt: record{i:int}
      recordloopnoninvariant: proc(rec:rt): int {
        rec.i = 0
        while rec.i < 10 {
           re= rec
           re.i = re.i + 1
        }
        return rec.i
      }
      val = recordloopnoninvariant(new rt)
      println val
      """;

  private TestCode() {}
}

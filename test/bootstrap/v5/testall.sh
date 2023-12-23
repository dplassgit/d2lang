D2EXE="bazel-bin/src/com/plasstech/lang/d2/D2Compiler.exe"
VEXE="bazel-bin/src/bootstrap/v5/v5fromv5.exe"

bazel build //src/com/plasstech/lang/d2:D2Compiler //src/bootstrap/v1:v1.d //src/bootstrap/v2:v2.d //src/bootstrap/v3:v3.d //src/bootstrap/v4:v4.d //src/bootstrap/v5:v5fromv5exe || exit -7

source=''

RED='\033[0;31m'
GREEN='\033[1;32m'
NC='\033[0m' # No Color

buildone() {
  rm -f *.obj *.asm *.exe *.out
  echo -n "${source}: "
  $VEXE < $source > temp.asm && nasm -fwin64 temp.asm && gcc temp.obj -o temp.exe
}

testone() {
  buildone
  $D2EXE $source --save-temps && ./d2out.exe > d.out || exit
  ./temp.exe > v.out
  diff d.out v.out && echo -e "${GREEN}PASS${NC}" || (echo -e "${RED}FAIL${NC}" && exit -4) || exit
}

buildonly=(
  "src/bootstrap/v0/v0.d"
  "bazel-bin/src/bootstrap/v1/v1.d"
  "bazel-bin/src/bootstrap/v2/v2.d"
  "bazel-bin/src/bootstrap/v3/v3.d"
  "bazel-bin/src/bootstrap/v4/v4.d"
  "bazel-bin/src/bootstrap/v5/v5.d"
)
for source in "${buildonly[@]}"; do buildone && echo -e "${GREEN}PASS${NC}" || (echo -e "${RED}FAIL${NC}" && exit -4) || exit; done;

for source in `ls test/bootstrap/v0/*.d`; do testone; done
for source in `ls test/bootstrap/v1/*.d`; do testone; done
for source in `ls test/bootstrap/v2/*.d`; do testone; done
for source in `ls test/bootstrap/v3/*.d`; do testone; done
for source in `ls test/bootstrap/v4/*.d`; do testone; done
for source in `ls test/bootstrap/v5/*.d`; do testone; done
files=(
  "samples/all-dead.d"
  "samples/all-ops-globals.d"
  "samples/all-ops-locals.d"  # long
  "samples/allOpsLocals.d"    # double
  "samples/arrayiter.d"
  "samples/associative.d"
  "samples/backslash.d"
  "samples/binary-sort.d"
  # "samples/bitops.d" # bit not
  "samples/boolops.d"
  "samples/bubblesort.d"  # ++, array params
  "samples/bytes.d"
  "samples/crash.d"
  "samples/dead-after-return.d"
  "samples/doubleopt.d"
  "samples/doubles.d"
  "samples/dtoi.d"
  "samples/dumbsort.d"
  "samples/everything.d"
  "samples/fact.d"
  "samples/fib.d"
  "samples/fib0.d" # ++
  "samples/fizzbuzz.d"
  "samples/forward-ref.d"
  "samples/globalstr2int.d"  # string compare, declaration
  "samples/hanoi.d"  # array literal
  "samples/helloworld.d"
  "samples/incdec.d"  # exit string
  "samples/index.d"  # array params and inline array literals
  "samples/ineq.d"
  "samples/inline-no-arg-void.d"
  "samples/inline.d"  # string compare
  "samples/linkedlist.d"
  "samples/longComp.d"  # longs
  "samples/loopinvariant.d"  # xor
  #"samples/newcomp.d"  # record compare
  "samples/not.d"
  "samples/null.d"  # declaration
  "samples/nullcompare.d"
  "samples/nullstring.d"  # null compare to string
  "samples/one-loop-break.d"
  "samples/pal.d"  # string compare
  "samples/power2.d"
  "samples/primes.d"
  #"samples/printarray.d"
  "samples/printdouble.d"
  "samples/printhello.d"
  "samples/printint.d"
  "samples/printparse.d"  # string literal index
  "samples/record-loop-modify.d"
  "samples/record-loop-not-invariant.d"
  #"samples/record-test.d"  # record compare
  "samples/record.d"
  #"samples/record_compare.d"  # record compare
  "samples/record_param.d"
  "samples/return_new_record.d"
  "samples/reverse_sentence.d"
  "samples/reversestring.d"
  "samples/shift-ops.d"
  "samples/short-circuits.d"
  "samples/simple-unroll-loop.d" # array literal
  "samples/str2int.d"  # string compare
  "samples/stringescaped.d"
  "samples/stringiter.d"
  "samples/tod.d"
  "samples/tolower.d"  # string compare
  "samples/toomanyregs.d"
  "samples/tostring.d"
  "samples/unreachable.d"
  "samples/non-golden/record-loop-invariant.d"
  "samples/non-golden/99bottles.d"
)
for source in "${files[@]}"; do testone; done

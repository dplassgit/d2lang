D2EXE="bazel-bin/src/com/plasstech/lang/d2/D2Compiler.exe"
V1EXE="bazel-bin/src/bootstrap/v1/v1.exe"

echo "Compiling d2:" 
bazel build //src/com/plasstech/lang/d2:D2Compiler --ui_event_filters=-info,-stdout --noshow_progress || exit -5

echo "Compiling v1:"
bazel build //src/bootstrap/v1:v1exe --ui_event_filters=-info,-stdout --noshow_progress || exit -6

source=''

RED='\033[0;31m'
GREEN='\033[1;32m'
NC='\033[0m' # No Color

testone() {
  rm -f *.obj *.asm *.exe *.out
  echo -n "${source}: "
  $D2EXE $source --save-temps && ./d2out.exe > d.out || exit -1
  $V1EXE < $source > temp.asm # || (echo -e "${RED}FAIL${NC}" && tail temp.asm && exit -2)
  nasm -fwin64 temp.asm && gcc temp.obj -o temp.exe && ./temp.exe > v1.out #|| exit -3
  diff d.out v1.out && echo -e "${GREEN}PASS${NC}" || (echo -e "${RED}FAIL${NC}" && exit -4) || exit 
}

for source in `ls test/bootstrap/v0/*.d`; do testone; done
for source in `ls test/bootstrap/v1/*.d`; do testone; done
files=(
"samples/all-dead.d"
"samples/all-ops-globals.d"
#"samples/all-ops-locals.d"  # long
#"samples/allOpsLocals.d"    # double
"samples/arrayiter.d"
"samples/associative.d"
#"samples/backslash.d"
"samples/binary-sort.d"
#"samples/bitops.d"
"samples/boolops.d"
#"samples/bubblesort.d"  # ++, array params
#"samples/bytes.d"
"samples/crash.d"
"samples/dead-after-return.d"
#"samples/doubleopt.d"
#"samples/doubles.d"
#"samples/dtoi.d"
"samples/dumbsort.d"
"samples/everything.d"
"samples/fact.d"
"samples/fib.d"
#"samples/fib0.d" # ++
"samples/fizzbuzz.d"
"samples/forward-ref.d"
#"samples/globalstr2int.d"  # string compare
#"samples/hanoi.d"  # array literals
"samples/helloworld.d"
#"samples/incdec.d"  # exit string
#"samples/index.d"  # array params
"samples/ineq.d"
"samples/inline-no-arg-void.d"
#"samples/inline.d"  # string compare
"samples/linkedlist.d"
#"samples/longComp.d"
#"samples/loopinvariant.d"  # xor
#"samples/newcomp.d"  # record compare
"samples/not.d"
#"samples/null.d"  # null compare
"samples/nullcompare.d"
#"samples/nullstring.d"  # null compare to string
"samples/one-loop-break.d"
#"samples/pal.d"  # string compare
"samples/power2.d"
"samples/primes.d"
#"samples/printarray.d"
#"samples/printdouble.d"
"samples/printhello.d"
"samples/printint.d"
#"samples/printparse.d"  # string literal index
"samples/record-loop-modify.d"
"samples/record-loop-not-invariant.d"
#"samples/record-test.d"  # record compare
"samples/record.d"
#"samples/record_compare.d"  # record compare
"samples/record_param.d"
"samples/return_new_record.d"
"samples/reverse_sentence.d"
"samples/reversestring.d"
#"samples/shift-ops.d"
"samples/short-circuits.d"
#"samples/simple-unroll-loop.d"
#"samples/str2int.d"  # string compare
"samples/stringescaped.d"
"samples/stringiter.d"
#"samples/tod.d"
#"samples/tolower.d"  # string compare
"samples/toomanyregs.d"
"samples/tostring.d"
"samples/unreachable.d")
for source in "${files[@]}"; do testone; done

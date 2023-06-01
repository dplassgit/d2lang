D2EXE="bazel-bin/src/com/plasstech/lang/d2/D2Compiler.exe"

rm -f *.out

echo "Compiling d2:" 
bazel build //src/com/plasstech/lang/d2:D2Compiler --ui_event_filters=-info,-stdout --noshow_progress || exit -1

echo "Compiling $1 with d2:" && $D2EXE $1 --save-temps && echo "Running with d2:" && ./d2out.exe > d.out || exit -1

echo "Compiling v2, compiling and running $1 with v2:" && ./scripts/v2 $1 > v2.out

echo "Diffs:"
diff --color=always d.out v2.out

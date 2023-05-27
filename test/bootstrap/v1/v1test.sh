D2EXE="bazel-bin/src/com/plasstech/lang/d2/D2Compiler.exe"

rm -f d2.out
rm -f v0.out

echo "Compiling d2:" 
bazel build //src/com/plasstech/lang/d2:D2Compiler --ui_event_filters=-info,-stdout --noshow_progress || exit -1

echo "Compiling $1 with d2:" && $D2EXE $1 --save-temps && echo "Running with d2:" && ./d2out.exe > d.out || exit -1

echo "Compiling v1, compiling and running $1 with v1:" && ./scripts/v1 $1 > v1.out

echo "Diffs:"
diff --color=always d.out v1.out

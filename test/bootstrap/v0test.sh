D2EXE="bazel-bin/src/com/plasstech/lang/d2/D2Compiler.exe"

rm -f d2.out
rm -f d2out.exe
rm -f v0.out
rm -f v0compiler.exe
rm -f v0out.exe
rm -f v0temp.*

echo "Compiling d2:" 
bazel build //src/com/plasstech/lang/d2:D2Compiler --ui_event_filters=-info,-stdout,-stderr --noshow_progress && echo "Compiling with d2:" && $D2EXE $1 --save-temps && echo "Running with d2:" && ./d2out.exe > d.out

echo "Compiling v0:"
$D2EXE samples/d2ind2/v0/v0.d -o v0compiler.exe && echo "Compiling with v0:" && ./v0compiler.exe < $1 > v0temp.asm && nasm -fwin64 v0temp.asm && gcc v0temp.obj -o v0out.exe && echo "Running with v0:" && ./v0out.exe > v0.out

diff d.out v0.out

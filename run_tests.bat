echo STARTING > tests_output.txt
call mvnw.cmd clean test >> tests_output.txt 2>&1
echo DONE >> tests_output.txt

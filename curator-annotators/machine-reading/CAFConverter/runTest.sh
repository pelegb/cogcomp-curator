#!/bin/bash


LIB_DIR="jars"
TEST_DIR="Tests"

MAIN_JAR="dist/CuratorCAFConverter-0.1.jar"

TEST="$TEST_DIR/runTests"

MAIN_TEST="Tests.runTests"

TEST_FILE="/shared/grandma/Projects/MachineReading/evaluation/testOutputs/v3/AFP_ENG_20030306.0751.DocAnnotations.json"

CP="."

for file in `ls $LIB_DIR`
do
    CP="$CP:$LIB_DIR/$file"
done

CP="$CP:$MAIN_JAR"

CMD="javac -cp $CP -sourcepath ./$TEST_DIR -d ./bin `find ./$TEST_DIR -name '*.java'`"

echo "$0: COMPILING: running command '$CMD'"

$CMD


CMD="java -cp bin:$CP $MAIN_TEST $TEST_FILE"

echo "$0: TESTING: running command '$CMD'"

$CMD

echo "$0: done."




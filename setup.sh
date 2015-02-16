MY_DIR=`pwd`
CLASSPATH=.
for i in lib/*.jar ext_lib/*.jar ; do
  CLASSPATH=$CLASSPATH:$MY_DIR/$i
done
CLASSPATH=$CLASSPATH:$MY_DIR/dist/exmlpipe.jar
export CLASSPATH

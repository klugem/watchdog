
# check if memory ulimit can be set
if [ ! -z $WATCHDOG_MEMORY ] && [ $WATCHDOG_MEMORY -ge 1 ]; then
        MEM=$((WATCHDOG_MEMORY-1))
        echo "setting ulimit to $MEM MB"
        MEM=$((MEM*1024))
        ulimit -Sv $MEM
fi

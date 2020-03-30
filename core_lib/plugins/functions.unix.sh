function getLockFilename() {
	FILE=$1
	LOCKNAME="${FILE}.wd.lock"
	echo $LOCKNAME
}

function lockFile() {
	LOCK_FILE=$(getLockFilename $1)
	# ensure that file is delete when script fails befor the file could be deleted
	trap "rm -f '$LOCK_FILE' 2>&1 > /dev/null" EXIT
	touch "$LOCK_FILE"
	sync
}

function unlockFile() {
	LOCK_FILE=$(getLockFilename $1)
	rm -f "$LOCK_FILE" 2>&1 > /dev/null
	sync
}

function wait4Lock() {
	LOCK_FILE=$(getLockFilename $1)
	LOCK_TIMEOUT=$2
	# block while lock file is there or timeout
	I=0
	while [ -e "$LOCK_FILE" ] && [ $I -le $LOCK_TIMEOUT ]; do
		sleep 1
		I=$(($I+1))
	done
}

function condaReadyFile() {
	echo "${1}.wd.ok"
}

function echoError() {
	echo "[ERROR] $1"
}

function echoInfo() {
	echo "[INFO] $1"
}

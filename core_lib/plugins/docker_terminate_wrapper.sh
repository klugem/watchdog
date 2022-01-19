#!/bin/bash
[ -z "$1" ] && { echo "Usage: $(basename $0) <docker|podman|singularity> <arguments>"; exit 1; }

# direct call if binary is not 'docker' or 'podman'
BIN=$1 
if [[ ! "$BIN" =~ ^(.+/)?docker$ ]] && [[ ! "$BIN" =~ ^(.+/)?podman$ ]]; then
	$@
	exit $?
fi

# find position of the command flag (e.g. skip all prior flags)
for i in $(seq 2 $#); do
    [[ !  ${!i} =~ ^- ]] && break
done

# ensure that the executed command is terminated when the script receives a kill signal
if [ "${!i}" == "run" ]; then
	#echo "setting $1 trap...."
	# add trap to SIGINT and SIGTERM, and insert the --detach flag
	trap 'echo "SIGINT/SIGTERM -> calling $BIN stop..." && trap - SIGINT SIGTERM && $BIN stop --time 15 $CID' SIGINT SIGTERM
	RAND_LABEL=$(tr -dc A-Za-z0-9 </dev/urandom | head -c 16)
	ii=$((i+1)) && set -- "${@:1:$i}" "--label" "terminateId=${RAND_LABEL}" "${@:$ii}"

	"$@" & # start the command and wait for the container to terminate
	if [ $? -eq 0 ]; then 
		CID=""
		# don't like that - might also go wrong; however the --cidfile option has a bug in podman 2.1.1 (https://github.com/containers/podman/issues/8091)
		# --detach doesn't work either as stdout/stderr is lost (podman logs might be possible but does not work with --rm flag and also has a bug with stdout/sterr until 3.0)
		while [ -z "$CID" ]; do
			CID=$($BIN container ls --quiet --filter "label=terminateId=${RAND_LABEL}")
		done
		$BIN wait ${CID} >/dev/null &
		PID=$!
		wait $PID
		exit $?		
	fi
else # execute the command as it was entered
	$@ 
	exit $?
fi

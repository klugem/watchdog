#!/bin/bash
SCRIPT_FOLDER=$(cd $(dirname $(readlink -f "${BASH_SOURCE[0]}" 2>/dev/null || readlink "${BASH_SOURCE[0]}" || echo "${BASH_SOURCE[0]}")) && pwd -P)
PRIMARY_CALLER="launcher"
source "${SCRIPT_FOLDER}/../core_lib/includeBasics.sh"

function createLink() {
	SOURCE=$1
	TARGET=$2

	B="watchdog-"$(basename "${SOURCE}")
	B="${B%.*}"
	ln -s "${SOURCE}" "${TARGET}/${B}"
	chmod 0755 "${TARGET}/${B}"

	echoInfo "${SOURCE} -> ${TARGET}/${B} "
}

if [ $# -ne 1 ]; then
	echoError "First parameter must point to the location in which the links should be installed (e.g. /usr/bin). "
	exit 1
fi
TARGET=$1

# ensure that link location exists
if [ ! -d "$TARGET" ]; then
	echoError "Link install location does not exist: '$TARGET' "
	exit 1
fi

# launcher scripts for jars are stored here
OUT="${SCRIPT_FOLDER}/launcher"
if [ ! -d "${OUT}" ]; then
	mkdir "${OUT}"
fi

# create scripts to start the jars
for J in `ls ${SCRIPT_FOLDER}/../jars/*.jar`; do
	B=$(basename "${J}" ".jar")
	if [[ "${B}" != "watchdog" && "${B}" != "watchdogDesigner" && "${B}" != "watchdogSlave" && "${B}" != "watchdog-"* ]]; then
		JP=$(abspath "${J}")
		OUTF="${OUT}/${B}.sh"
		echo "#!/bin/bash" > "${OUTF}"
		echo -n "java -jar \"../../jars/${B}.jar\" " >> "${OUTF}"
		echo '$@' >> "${OUTF}"
		chmod u+x "${OUTF}"
	
		# make soft link
		createLink "${OUTF}" "${TARGET}"
	fi
done


# create scripts to start the jars
for S in `ls ${SCRIPT_FOLDER}/*.*`; do
	B=$(basename "$S" ".sh")
	if [[ "$B" != "travisCI_clamAV" && "$B" != "watchdogLauncher" ]]; then
		SA=$(abspath "${S}")

		# make soft link
		createLink "${SA}" "${TARGET}"
	fi
done

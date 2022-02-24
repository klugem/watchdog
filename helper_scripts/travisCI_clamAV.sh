#!/bin/bash
# inspired by https:/github.com/travis-ci/travis-ci/issues/8031

[ $# -ne 1 ] && exit 1

VIRUS_DB="/tmp/clamav_db"
WHO=$(whoami)

# install clamav 
echo "installing software..."
sudo apt-get update -qq > /dev/null 2>&1
sudo apt-get install -qq clamav > /dev/null 2>&1

# try to update virus definition
echo "updating virus definition..."
freshclam  --quiet --datadir="${VIRUS_DB}" --user="${WHO}" --log="/tmp/clamAV_update.log"
RET=$?

# check, if no error occurred
if [ $RET -ne 0 ]; then
	echo "[ERROR] Faild to update virus definition database (exit code: $RET)!"
	exit 2
fi

echo "starting virus scan..."
# scan home for virus
sudo clamscan -r "${1}" --max-filesize=128M --database="${VIRUS_DB}" --quiet --infected --log='/var/log/scan.log'
RET=$?
# check, if no error occurred
if [ $RET -ne 0 ] && [ $RET -ne 1 ]; then
	echo "[ERROR] Faild to run clamAV (exit code: $RET)!"
	exit 2
fi

# check, if some infected files were identified
scr=$(sudo grep 'Infected files' '/var/log/scan.log' | cut -c17- )
sudo cat '/var/log/scan.log'
if [ "$scr" == "0" ] && [ $RET -eq 0 ]; then 
	echo '[OK] No virus was detected.'
	exit 0
else 
	echo '[ERROR] Virus detected!'
	exit 1
fi

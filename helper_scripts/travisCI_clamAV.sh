#!/bin/bash
# inspired by https:/github.com/travis-ci/travis-ci/issues/8031

VIRUS_DB_CACHE="${HOME}/cache/clamav"
VIRUS_DB="${HOME}/clamav"
WHO=$(whoami)

# install clamav 
echo "installing software..."
sudo apt-get update -qq > /dev/null 2>&1
sudo apt-get install -qq clamav > /dev/null 2>&1

# try to update virus definition
echo "updating virus definition..."
mkdir "${VIRUS_DB}"
if [ -d "${VIRUS_DB_CACHE}" ]; then cp -r "${VIRUS_DB_CACHE}/" "${VIRUS_DB}/"; fi
chown -R "${WHO}:${WHO}" "${VIRUS_DB}"
freshclam --quiet --datadir="${VIRUS_DB}" --user="${WHO}" --log="/tmp/clamAV_update.log"
RET=$?

# check, if no error occurred
if [ $RET -ne 0 ]; then
	echo "[ERROR] Faild to update virus definition database (exit code: $RET)!"
	exit 2
else 
	cp -r "${VIRUS_DB}/" "${VIRUS_DB_CACHE}/"
fi

echo "starting virus scan..."
# scan home for virus
sudo clamscan -r "${TRAVIS_BUILD_DIR}" --max-filesize=128M --database="${VIRUS_DB}" --quiet --infected --log='/var/log/scan.log'
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

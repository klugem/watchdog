#!/usr/bin/env python3
from ruamel import yaml
import collections
import io
import argparse
import os.path
import sys
import glob

### parse the arguments
parser = argparse.ArgumentParser()
parser.add_argument("-m", "--moduleFolder", metavar="INPUT_MODULE_FOLDER", required=True, help="path to module folder in which the *.conda.yml files should be formated")
args = parser.parse_args()

# configure YAML parser to output with indentation 
yamld = yaml.YAML()
yamld.indent(mapping=4, sequence=4, offset=2)

# test if module folder exists
if os.path.exists(args.moduleFolder):

	# process all conda.yml files in that folder
	for moduleFolder in os.walk(args.moduleFolder):
		for condaFile in glob.glob(os.path.join(moduleFolder[0], '*.conda.yml')):

			print("processing '%s'" % (condaFile))
			# open the yml file
			with open(condaFile, 'r') as stream: 
				data = yaml.safe_load(stream)

			# sort all lists
			for key in data.keys():
				data[key].sort()

			# sort the keys
			sortedData = yaml.comments.CommentedMap()
			for k in sorted(data):
			    sortedData[k] = data[k]

			# write it back
			with io.open(condaFile, 'w', encoding='utf8') as outfile:
				yamld.dump(sortedData, outfile)
else:
	print("Module folder '%s' does not exist." % (args.moduleFolder))
	exit(1)

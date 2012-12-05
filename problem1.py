#! /usr/bin/python

import sys
import os
import re

#remove all between /* and */
rm_comment1='\/\*(.*?)\*\/' #multi-line comments
#rm_comment2=

# Argument count sanity check
if len(sys.argv) != 2:
    print 'Error: Must only pass one directory.'
    sys.exit()

# Recurse through all directories and subdirectories, starting with argv[1]
for root, dirs, files in os.walk(sys.argv[1]):
	# reset counts for each statistic
    dirsize = 0
    d_count_public = 0
    d_count_private = 0
    d_count_try = 0
    d_count_catch = 0

    # look at all files
    for file_target in files:
        # determine file extention
        name, ext = os.path.splitext(file_target)
        # if its a java file, get file statistics
        if ext == str('.java'):
            # add the size of this file to the dirsize
            dirsize += os.path.getsize(os.path.join(root,file_target))
            # open file
            infile = open(os.path.join(root,file_target), "r")
            rm-one-line-comment - re.compile('^.*?//') #single-line comment
        #end if
    #end for


#	print dirsize
    print d_count_public
#end for



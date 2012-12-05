#! /usr/bin/python

import sys
import os
import re

#remove all between /* and */
rm_comment1='\/\*(.*?)\*\/' #multi-line comments
#rm_comment2=

def get_dirsize(src_path = "."):
    accumulator = 0
    #walk the tree, counting sizes
    for root,dirs,files in os.walk(src_path):
        for f in files:
            path = os.path.join(root,f)
            accumulator += os.path.getsize(path)
    return accumulator
#end def

def main():
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
                # open file
                infile = open(os.path.join(root,file_target), "r")
                regex_oneliners = re.compile('^.*?//') #single-line comment
            #end if
        #end for
    
        dirsize = get_dirsize(root)
    
        #print everything
        print (root + ":\n\t"
                + str(dirsize) + " bytes\t"
                + str(d_count_public) + " public\t"
                + str(d_count_private) + " private\t"
                + str(d_count_try) + " try\t"
                + str(d_count_catch) + " catch\t")
    #end for
#end def


if __name__ == "__main__":
    main()
#endif

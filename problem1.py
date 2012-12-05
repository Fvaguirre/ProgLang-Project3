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

def remove_comments(path_to_file):
    # regex for ease of reading
    rm_one_line_comment = re.compile('//.*')            # match single-line comment
    rm_multiline_comment = re.compile('\/\*(.*?)\*\/')  # match multiline comment
    rm_newline = re.compile('\s')                       # match newline character

    file_as_string = "" # Set string to be empty
    infile = open(path_to_file,"r") # open the input file
    for inline in infile:
        file_as_string += re.sub(rm_one_line_comment,"",inline) # Read input file into a string, stripping single line comments
        file_sans_newlines = re.sub(rm_newline," ",file_as_string)  # Remove whitespace characters
        file_as_string = re.sub(rm_multiline_comment,"",file_sans_newlines) # remove multiline comments
    infile.close()  # clean up
    return file_as_string   # its over!
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

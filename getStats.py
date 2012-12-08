#! /usr/bin/python

import sys
import os
import re

#remove all between /* and */
rm_comment1='\/\*(.*?)\*\/' #multi-line comments
#rm_comment2=

def get_dirsize(src_path = "."):
    accumulator = 0
    #walk the tree, counting sizes. this could probably also be done recursively below...
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
        file_sans_newlines = re.sub(rm_newline," ",file_as_string)  # Remove newline characters
        file_as_string = re.sub(rm_multiline_comment,"",file_sans_newlines) # remove multiline comments
    infile.close()  # clean up
    return file_as_string   # its over!
#end def

def processDirectory(directory,out_list):
    
    d_count_public = 0
    d_count_private = 0
    d_count_try = 0
    d_count_catch = 0
    dirsize = 0

    fileList = os.listdir(directory)                    #get all files (this includes subdirectories)

    for f in fileList:
        curFile = os.path.join(directory,f)             #get file path

        if os.path.isfile(curFile):                     #if we have a file, scan it for keywords
            name,ext = os.path.splitext(curFile)        #check extension
            if ext == str('.java'):
                strip_string = remove_comments(curFile)
                #re.findall returns a list of all matches, the length of that list then counts matches
                d_count_public += len(re.findall('public', strip_string))
                d_count_private += len(re.findall('private', strip_string))
                d_count_try += len(re.findall('try', strip_string))
                d_count_catch += len(re.findall('catch', strip_string))
            #endif
        #endif
        else:       #must be a directory, if not a file
            (sub_public,sub_private,sub_try,sub_catch) = processDirectory(curFile,out_list) #recursively get stats from the subdirectory and accumulate them here
            d_count_public += sub_public
            d_count_private += sub_private
            d_count_try += sub_try
            d_count_catch += sub_catch
        #end else

        dirsize = get_dirsize(directory) 

    
    out_str = (directory + ":\n\t"                  #form an output string, but we don't print it yet
            + str(dirsize) + " bytes\t"
            + str(d_count_public) + " public\t"
            + str(d_count_private) + " private\t"
            + str(d_count_try) + " try\t"
            + str(d_count_catch) + " catch\t")

    out_list.insert(0,out_str)                      #prepend output string to beginnign of output list, to print later
    return (d_count_public,d_count_private,d_count_try,d_count_catch)   #return subdirectory statistics to caller
#end def






def main():
    # Argument count sanity check
    if len(sys.argv) != 2:
        print 'Error: Must only pass one directory.'
        sys.exit()

    out_list = []

    processDirectory(sys.argv[1],out_list)

    for i in out_list:
        print i
        print "---------------------------"
    #end for
#end def


if __name__ == "__main__":
    main()
#endif

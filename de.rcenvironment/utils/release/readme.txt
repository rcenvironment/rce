== Explanation of the files in this directory ==

additions-filter.conf: 

	This is a filter file intended for the *nix "zip" command (with the -i@ and -x@ syntax).
	All files *not* matched by any rule are deployed as part of the "source" zip;
	all files that *are* matched by a rule are deployed as part of the "additions" zip file.
	
	- misc_ro, July 2017

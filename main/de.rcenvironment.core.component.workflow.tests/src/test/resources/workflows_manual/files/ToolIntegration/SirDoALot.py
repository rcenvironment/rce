import sys
print "Hello, I am Sir Do A Lot"
print "I now take your " + sys.argv[1] + " and will do a lot!"
print "And i present you : " + str(float(sys.argv[1])**float(sys.argv[1]))
print "Of course I have a present for you!"
print "Bye!!"
f = open ("result","w+")
f.write("Alaaaaf!")
f.close()
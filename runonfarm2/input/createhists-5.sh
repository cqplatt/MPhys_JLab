#! /usr/bin/env csh

if ! $?COATJAVA  then
    echo "COATJAVA environment variable is not defined. Exiting..."
    exit 1
endif

if ! $?GEMC then
    echo "GEMC environment variable is not set. Exiting..."
    exit 1
endif

echo "* * * * * * * * * * * * * * * * * * * * *"
echo "GEMC IS: "
  which gemc
echo "USER IS: " $USER
echo "PATH IS: " $PATH
echo "JAVA IS: "
  which java
  java -version
echo "JOB_NO IS: " 5
echo "* * * * * * * * * * * * * * * * * * * * *"

# run gemc:
echo "* * * * * SIMULATE WITH GEMC * * * * *"
gemc cosmics.gcard -RUNNO=9 -USE_GUI=0 -N=50000 -OUTPUT="evio,sim-5.evio"


# run evio2hipo converter:
echo "* * * * * CONVERT FROM EVIO TO HIPO * * * * *"
if ( -s "sim-5.evio" ) then
	$COATJAVA/bin/evio2hipo -r 9 -t 0.0 -s 0.0 -o sim-5.hipo sim-5.evio
else
	echo "sim-5.evio empty: aborting."
	exit 1
endif


# notsouseful-util as substitute for proper CLARA treatment:
echo "* * * * * RECONSTRUCTION * * * * *"
if ( -s "sim-5.hipo" ) then
	$COATJAVA/bin/notsouseful-util -o simRec-5.hipo -i sim-5.hipo org.jlab.rec.cvt.services.CVTReconstruction
else
	echo "sim-5.hipo empty: aborting."
	exit 1
endif


# creates histogram files out of data files (args[0]=input, args[1]=output, args[2]=gcard)
echo "* * * * * MAKEHISTS.GROOVY * * * * *"
if ( -s "simRec-5.hipo" ) then
        $COATJAVA/bin/run-groovy makeHists.groovy simRec-5.hipo simRecHist-5.hipo cosmics
else
	echo "simRec-5.hipo empty: aborting."
	exit 1
endif

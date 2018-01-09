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
echo "JOB_NO IS: " JOBNO
echo "* * * * * * * * * * * * * * * * * * * * *"

# run gemc:
echo "* * * * * SIMULATE WITH GEMC * * * * *"
gemc CARD_IN.gcard -RUNNO=RUN_NO -USE_GUI=0 -N=NEVENTS -OUTPUT="evio,sim-JOBNO.evio"


# run evio2hipo converter:
echo "* * * * * CONVERT FROM EVIO TO HIPO * * * * *"
if ( -s "sim-JOBNO.evio" ) then
	$COATJAVA/bin/evio2hipo -r RUN_NO -t 0.0 -s 0.0 -o sim-JOBNO.hipo sim-JOBNO.evio
else
	echo "sim-JOBNO.evio empty: aborting."
	exit 1
endif


# notsouseful-util as substitute for proper CLARA treatment:
echo "* * * * * RECONSTRUCTION * * * * *"
if ( -s "sim-JOBNO.hipo" ) then
	$COATJAVA/bin/notsouseful-util -o simRec-JOBNO.hipo -i sim-JOBNO.hipo org.jlab.rec.cvt.services.CVTReconstruction
else
	echo "sim-JOBNO.hipo empty: aborting."
	exit 1
endif


# creates histogram files out of data files (args[0]=input, args[1]=output, args[2]=gcard)
echo "* * * * * MAKEHISTS.GROOVY * * * * *"
if ( -s "simRec-JOBNO.hipo" ) then
        $COATJAVA/bin/run-groovy makeHists.groovy simRec-JOBNO.hipo simRecHist-JOBNO.hipo CARD_IN
else
	echo "simRec-JOBNO.hipo empty: aborting."
	exit 1
endif

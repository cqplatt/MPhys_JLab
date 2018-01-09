#! /usr/bin/env csh

if ! $?COATJAVA  then
    echo "COATJAVA environment variable is not defined. Exiting..."
    exit 1
endif

if ! $?GEMC then
    echo "GEMC environment variable is not set. Exiting..."
    exit 1
endif

echo "GEMC IS: "
  which gemc

echo "RUNNING LOCALLY:"

# run gemc:
echo "* * * * * SIMULATE WITH GEMC * * * * *"
gemc cosmics.gcard -RUNNO=10 -USE_GUI=0 -N=5000 -OUTPUT="evio,sim-LOCAL.evio"

echo "* * * * * RECONSTRUCTION * * * * *"
$COATJAVA/bin/notsouseful-util -o simRec-LOCAL.evio -i sim-LOCAL.evio org.jlab.rec.cvt.services.CVTCosmicsReconstruction

# run evio2hipo converter:
echo "* * * * * CONVERT FROM EVIO TO HIPO * * * * *"
$COATJAVA/bin/evio2hipo -r 10 -t 0.0 -s 0.0 -o simRec-LOCAL.hipo simRec-LOCAL.evio

#echo "* * * * * EXTRACT TYPE 1 * * * * *"
$COATJAVA/bin/run-groovy extractType2v7.groovy simRec-LOCAL.hipo y

# creates histogram files out of data files (arg[0]=input, arg[1]=output)
echo "* * * * * MAKEHISTS.GROOVY * * * * *"
# extract_rec_cosmicsSimType1.hipo
$COATJAVA/bin/run-groovy makeHists.groovy extract_rec_cosmicsSimType1.hipo simRecHist-LOCAL.hipo cosmics

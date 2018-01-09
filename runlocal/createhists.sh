printf "\n\n  * * * * * RUNNING LOCALLY * * * * * \n\n"

# simulate with GEMC:
printf "\n\n * * * * * SIMULATING WITH GEMC * * * * * \n\n"
gemc cosmics.gcard -RUNNO=10 -USE_GUI=0 -N=50000 -OUTPUT="evio,sim-LOCAL.evio"

# run evio2hipo converter (-r, -t, -s flags for run, torus, solenoid):
printf "\n\n * * * * * CONVERTING FROM EVIO TO HIPO * * * * * \n\n"
$COATJAVA/bin/evio2hipo -r 10 -t 0.0 -s 0.0 -o sim-LOCAL.hipo sim-LOCAL.evio

# notsouseful-util as CLARA proxy (-o, -i flags for output, input; specify CLARA service):
printf "\n\n * * * * * RECONSTRUCTION * * * * * \n\n"
$COATJAVA/bin/notsouseful-util -o simRec-LOCAL.hipo -i sim-LOCAL.hipo
                                org.jlab.rec.cvt.services.CVTReconstruction

# extract T1 events using script from GPG (args[0]=input, args[1]=Y/N extract types):
printf "\n\n * * * * * EXTRACT TYPE 1 * * * * * \n\n"
$COATJAVA/bin/run-groovy extractType2v7.groovy simRec-LOCAL.hipo y

# creates histogram file out of data file (args[0]=input, args[1]=output, args[2]=beam type)
printf "\n\n * * * * * MAKEHISTS.GROOVY * * * * * \n\n"
$COATJAVA/bin/run-groovy makeHists.groovy extract_rec_cosmicsSimType1.hipo
                                          simRecHist-LOCAL.hipo cosmics

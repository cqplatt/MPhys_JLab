// to run: $COATJAVA/bin/run-groovy allhist.groovy <input filename>.hipo

import org.jlab.io.hipo.*;                                                  // for HipoDataSource
import org.jlab.io.base.*;                                                  // for DataEvent/DataBank

import org.jlab.groot.data.H1F;                                             // for histograms
import org.jlab.groot.data.TDirectory;                                      // for saving histogram files

import org.jlab.detector.geant4.v2.SVT.*;                                   // for converting layer to region, module
import org.jlab.detector.calib.utils.DatabaseConstantProvider;              // for connecting to CCDB

DatabaseConstantProvider cp = new DatabaseConstantProvider(RUN_NO,"default");  // create new DCP
cp = SVTConstants.connect( cp );                                            // connect to CCDB via DCP
SVTConstants.VERBOSE = true;                                                // print out table used
SVTConstants.loadAlignmentShifts( cp );                                     // load alignment shifts
cp.disconnect();                                                            // disconnect from CCDB via DCP

HipoDataSource reader = new HipoDataSource();                               // hipo file reader
String inputFile  = args[0];                                                // input filename from first argument
reader.open(inputFile);                                                     // to open file with name

String outputFile = args[1];                                                // output filename from second argument

TDirectory histFile = new TDirectory();                                     // defines output directory

H1F[] hfitResidual = new H1F[84];                                           // hists for 132 mods (R1-R4); indexed 0->131

double[] meanVal  = new double[84];                                         // holds centroids
double[] sigmaVal = new double[84];                                         // holds errors
int[] nSecInReg = [10,14,18];                                               // no. of sectors per region; each sector has two modules
String[] modArr = new String[84];                                           // initialises array to hold 132 module identifiers
int imod  = 0;                                                              // for indexing module identifiers

histFile.mkdir("hfitResiduals/");

// create all 84 necessary modules (R1 has 10*2, R2 has 14*2, R3 has 18*2):
for (int iregion = 1; iregion <= 3; iregion++) {
  for (int isector = 1; isector <= nSecInReg[iregion-1]; isector++) {
    for (int ilayer = 1; ilayer <= 2; ilayer++) {
      if (args[2] == "beam") {
        hfitResidual[imod] = new H1F("hfitResidual"+Integer.toString(imod), "hfitResidual"+Integer.toString(imod), 50, -1.0, 1.0); // (name,title,bins,xMin,xMax)
      } else if (args[2] == "cosmics") {
        hfitResidual[imod] = new H1F("hfitResidual"+Integer.toString(imod), "hfitResidual"+Integer.toString(imod), 200, -10.0, 10.0);
      }
      modArr[imod] = "R"+Integer.toString(iregion)+"S"+Integer.toString(isector)+"L"+Integer.toString(ilayer);                   // concatenate R+S+L for unique string
      imod++;
    }
  }
}

while(reader.hasEvent()) {                                                  // loop over the file until it runs out of events
  DataEvent event = reader.getNextEvent();                                  // get event banks from the file
  if(event.hasBank("BSTRec::Hits") == true) {                               // check that the bank of interest is there
    DataBank bank = event.getBank("BSTRec::Hits");                          // read in the bank
    int nrows     = bank.rows();                                            // get the number of hits in the event (rows in the bank) [bank.show();]
    float[] fitResidual = bank.getFloat("fitResidual");                     // create an array and fill it with data from the bank, then print
    int[] nlayer  = bank.getByte("layer");                                  // put all layer values from bank into array
    int[] nsector = bank.getByte("sector");                                 // put all sector values from bank into array
    for (int irow = 0; irow <= nrows-1; irow++) {                           // loop over the hits
      int[] RM   = SVTConstants.convertLayer2RegionModule(nlayer[irow]-1);  // get region [0:3], module [0:1] from layer value [0:7]

      // creates identifier to be 'found' in module array:
      String modCurr = "R"+Integer.toString(RM[0]+1)+"S"+Integer.toString(nsector[irow])+"L"+Integer.toString(RM[1]+1);

      for (imod = 0; imod < modArr.length; imod++) {                        // loop through modules until imod = current module
        if(modArr[imod] == modCurr) {                                       // if modArr element equals mod currently being considered:
          if(!Float.isNaN(fitResidual[irow])) {                             // discards data if not a number
            hfitResidual[imod].fill(fitResidual[irow]);                     // adds current module's residual to module's hist
          }
          break;                                                            // exit for loop if value added/considered
        }
      }
    } // hits in bank
  } // bank in events
} // events in file

histFile.cd("hfitResiduals/");

for (imod = 0; imod < modArr.length; imod++) {
  histFile.addDataSet(hfitResidual[imod]);
}

histFile.writeFile(outputFile);
System.out.println("OUTPUT: " + outputFile);

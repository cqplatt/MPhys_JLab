import org.jlab.io.hipo.*;                                     // for HipoDataSource
import org.jlab.io.base.*;                                     // for DataEvent/DataBank

import org.jlab.groot.data.H1F;                                // for histograms
import org.jlab.groot.data.TDirectory;                         // for saving histogram files

import org.jlab.detector.geant4.v2.SVT.*;                      // for converting L to R, M
import org.jlab.detector.calib.utils.DatabaseConstantProvider; // for connecting to CCDB

SVTConstants.VERBOSE = true;
DatabaseConstantProvider cp = new DatabaseConstantProvider();  // create new DCP
cp = SVTConstants.connect( cp );                               // connect to CCDB via DCP
SVTConstants.loadAlignmentShifts( cp );                        // load alignment shifts
cp.disconnect();                                               // disconnect from CCDB via DCP

HipoDataSource reader = new HipoDataSource();                  // hipo file reader
String inputFile  = args[0];                                   // input filename from first argument
reader.open(inputFile);                                        // open file with name

String outputFile = args[1];                                   // output filename from second argument

TDirectory histFile = new TDirectory();                        // creates histogram tree
histFile.mkdir("hfitResiduals/");                              // root of tree

H1F[] hfitResidual = new H1F[84];                              // hists for 132 mods (R1-R4); indexed 0->131

double[] meanVal  = new double[84];                            // holds centroids
double[] sigmaVal = new double[84];                            // holds errors
int[] nSecInReg = [10,14,18];                                  // no. of sec per reg; each sec has two mod
String[] modArr = new String[84];                              // array to hold 132 module identifiers
int imod  = 0;                                                 // for indexing module identifiers

// create all 84 necessary modules (R1 has 10*2, R2 has 14*2, R3 has 18*2):
for (int iregion = 1; iregion <= 3; iregion++) {
  for (int isector = 1; isector <= nSecInReg[iregion-1]; isector++) {
    for (int ilayer = 1; ilayer <= 2; ilayer++) {
      if (args[2] == "beam") {
        hfitResidual[imod] = new H1F("hfitResidual"+Integer.toString(imod),
                                     "hfitResidual"+Integer.toString(imod), 50,   -1.0,  1.0); 
      } else if (args[2] == "cosmics") {
        hfitResidual[imod] = new H1F("hfitResidual"+Integer.toString(imod),
                                     "hfitResidual"+Integer.toString(imod), 500, -10.0, 10.0);
      } // H1F(name,title,bins,xMin,xMax)
      modArr[imod] = "R"+Integer.toString(iregion)+
                     "S"+Integer.toString(isector)+
                     "L"+Integer.toString(ilayer); // concatenate R+S+L for unique string
      imod++;
    }
  }
}

while(reader.hasEvent()) {                                     // loop over file until it runs out of events
  DataEvent event = reader.getNextEvent();                     // get event banks from the file
  if(event.hasBank("BSTRec::Hits") == true) {                  // check that the bank of interest is there
    DataBank bank = event.getBank("BSTRec::Hits");             // read in the bank
    int nrows     = bank.rows();                               // get no. of hits in event (rows in bank)
    float[] fitResidual = bank.getFloat("fitResidual");        // create array and fill it with bank data
    int[] nlayer  = bank.getByte("layer");                     // put all layer values from bank into array
    int[] nsector = bank.getByte("sector");                    // put all sector values from bank into array
    for (int irow = 0; irow <= nrows-1; irow++) {              // loop over the hits
      int[] RM = SVTConstants.convertLayer2RegionModule(nlayer[irow]-1); // reg[0:3], mod[0:1] from lay[0:7] 

      // creates identifier to be `found' in module array:
      String modCurr = "R"+Integer.toString(RM[0]+1)+
                       "S"+Integer.toString(nsector[irow])+
                       "L"+Integer.toString(RM[1]+1);

      for (imod = 0; imod < modArr.length; imod++) {           // loop through mods until imod = current mod
        if(modArr[imod] == modCurr) {                          // if modArr element equals current mod:
          if(!Float.isNaN(fitResidual[irow])) {                // discards data if not a number
            hfitResidual[imod].fill(fitResidual[irow]);        // adds current mod's residual to mod's hist
          }
          break;                                               // exit for loop if value added/considered
        }
      }
    } // hits in bank
  } // bank in events
} // events in file

histFile.cd("hfitResiduals/");

for (imod = 0; imod < modArr.length; imod++) {
  histFile.addDataSet(hfitResidual[imod]);
  System.out.println(imod);
}

histFile.writeFile(outputFile);
System.out.println("OUTPUT: " + outputFile);

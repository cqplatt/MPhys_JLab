// to run: $COATJAVA/bin/run-groovy allhist.groovy <input filename>.hipo

import org.jlab.io.hipo.*;                                                  // for HipoDataSource
import org.jlab.io.base.*;                                                  // for DataEvent/DataBank

import org.jlab.groot.data.H1F;                                             // for histograms
import org.jlab.groot.math.F1D;                                             // for making Gaussians
import org.jlab.groot.fitter.DataFitter;                                    // for fitting Gaussians
import org.jlab.groot.ui.TCanvas;                                           // for plotting
import org.jlab.groot.graphics.EmbeddedCanvas;                              // for plotting without UI
import org.jlab.groot.data.IDataSet;                                        // data... set
import org.jlab.groot.data.TDirectory;

import org.jlab.geometry.prim.Line3d;                                       // for use with SVT geometry
import eu.mihosoft.vrl.v3d.*;                                               // sames
import org.jlab.detector.geant4.v2.SVT.*;                                   // for converting layer to region, module
import org.jlab.detector.calib.utils.DatabaseConstantProvider;              // for connecting to CCDB

DatabaseConstantProvider cp = new DatabaseConstantProvider();               // create new DCP
cp = SVTConstants.connect( cp );                                            // connect to CCDB via DCP
SVTConstants.loadAlignmentShifts( cp );                                     // load alignment shifts
cp.disconnect();                                                            // disconnect from CCDB via DCP

HipoDataSource reader   = new HipoDataSource();                             // hipo file reader

Scanner s = new Scanner(new File(args[0]));
ArrayList<String> inputFiles = new ArrayList<String>();                     // filenames from first argument when ran
while ( s.hasNext() ) {
  inputFiles.add(s.next());
}
s.close();

String comb = "";

TDirectory.addFiles(comb, inputFiles);

reader.open(comb);

H1F[] hfitResidual = new H1F[84];                                           // hists for 132 mods (R1-R4); indexed 0->131
F1D[] fitFunction  = new F1D[84];                                           // functions for Gaussian fits

double[] meanVal  = new double[84];                                         // holds centroids
double[] sigmaVal = new double[84];                                         // holds errors
int[] nSecInReg = [10,14,18];                                               // no. of sectors per region; each sector has two modules
String[] modArr = new String[84];                                           // initialises array to hold 132 module identifiers
int imod  = 0;                                                              // for indexing module identifiers

// create all 84 necessary modules (R1 has 10*2, R2 has 14*2, R3 has 18*2):
for (int iregion = 1; iregion <= 3; iregion++) {
  for (int isector = 1; isector <= nSecInReg[iregion-1]; isector++) {
    for (int ilayer = 1; ilayer <= 2; ilayer++) {
      hfitResidual[imod] = new H1F("hfitResidual"+Integer.toString(imod), "hfitResidual"+Integer.toString(imod), 80, -4.0, 4.0); // (name,title,bins,xMin,xMax)
      fitFunction[imod]  = new F1D("fitFunction"+Integer.toString(imod),"[amp]*gaus(x,[mean],[sigma])", -5.0, 5.0);              // (name,func,xMin,xMax)
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
    int[] nlayer  = bank.getInt("layer");                                   // put all layer values from bank into array
    int[] nsector = bank.getInt("sector");                                  // put all sector values from bank into array
    for (int irow = 0; irow <= nrows-1; irow++) {                           // loop over the hits
      int[] RM   = SVTConstants.convertLayer2RegionModule(nlayer[irow]-1);  // get region [0:3], module [0:1] from layer value [0:7]

      // creates identifier to be 'found' in module array:
      String modCurr = "R"+Integer.toString(RM[0]+1)+"S"+Integer.toString(nsector[irow])+"L"+Integer.toString(RM[1]+1);

      for (imod = 0; imod < modArr.length; imod++) {                        // loop through modules until imod = current module
        if(modArr[imod] == modCurr) {                                       // if modArr element equals mod currently being considered:
          if(!Float.isNaN(fitResidual[irow])) {                             // discards data if not a number
            hfitResidual[imod].fill(fitResidual[irow]);                     // adds current module's residual to module's hist
            //fitFunction[imod].setParameters(1.0, 0.0, 1.0);                 // sets parameters of equation in order of appearence [amp,mean,sigma]
          }
          break;                                                            // exit for loop if value added/considered
        }
      }
    } // hits in bank
  } // bank in events
} // events in file

imod = 0;                                                                   // resets imod

for (iregion = 1; iregion <= 3; iregion++) {                                // plot hists in separate canvas for each region
  //TCanvas c1 = new TCanvas("c", 1700, 850);                               // sets title, dimensions for visual canvas (1588x810 optimised for TP)
  EmbeddedCanvas c1 = new EmbeddedCanvas();                                 // creates non-X11 canvas
  c1.setSize(1600,1200);                                                    // sets canvas dimensions
  c1.divide(6,6);                                                           // makes enough pads (hist spaces) for maximum in R3, 36. (6*6)
  c1.setPadTitles("region " + iregion);                                     // sets unique title for canvas
  for (imodC = 0; imodC < nSecInReg[iregion-1]*2; imodC++) {                // loop over 84 modules of R1->R3
	  currentDataset = hfitResidual[imod];                                    // sets data set to be fit to current hist's data
	  double currentRangeMin = fitFunction[imod].getMin();                    // min of current data
	  double currentRangeMax = fitFunction[imod].getMax();                    // max of current data
	  fitFunction[imod].setParameter(0, getMaxYIDataSet(currentDataset, currentRangeMin, currentRangeMax) );  // sets amplitude as max bin
	  fitFunction[imod].setParameter(1, getMeanIDataSet(currentDataset, currentRangeMin, currentRangeMax) );  // sets mean
	  fitFunction[imod].setParameter(2, getRMSIDataSet(currentDataset, currentRangeMin, currentRangeMax) );   // sets sigma as RMS
	  fitFunction[imod].setParLimits(2, 0.0, Double.MAX_VALUE);                                               // ?

    meanVal[imod]  = getMeanIDataSet(currentDataset, currentRangeMin, currentRangeMax);  // mean value from getMeanIDataSet
    sigmaVal[imod] = getRMSIDataSet(currentDataset, currentRangeMin, currentRangeMax);   // sigma value

    hfitResidual[imod].setFillColor(25);                                      // colours histogram
    c1.cd(imodC);                                                             // pad from 0; dictates placement of hist in array of hists
    hfitResidual[imod].setTitle("module "+Integer.toString(imod+1)+": "+modArr[imod]);          // sets the title for the hist based on module no.
    c1.draw(hfitResidual[imod]);                                              // draws hist on canvas
    DataFitter.fit(fitFunction[imod], hfitResidual[imod], "Q");
    c1.draw(fitFunction[imod], "same");
    imod++;
  }
c1.save("hist"+iregion+".png");                                                       // saves hist to file in working directory
}

SVTStripFactory svtStripFactory = new SVTStripFactory( cp, false );         // for SVT geometry methods

File fout = new File("residuals-CASE.txt");                                      // creates file to hold residuals
FileOutputStream fos = new FileOutputStream(fout);                          // creates output stream using file
BufferedWriter bw  = new BufferedWriter(new OutputStreamWriter(fos));       // creates writer using stream
File fout2 = new File("midpoints-CASE.txt");                                     // creates file to hold midpoints of modules
FileOutputStream fos2 = new FileOutputStream(fout2);
BufferedWriter bw2  = new BufferedWriter(new OutputStreamWriter(fos2));

for (iregion = 1; iregion <= 3; iregion++) {
  for (isector = 1; isector <= nSecInReg[iregion-1]; isector++) {
    for (ilayer = 1; ilayer <= 2; ilayer++) {
      RM = SVTConstants.convertLayer2RegionModule(ilayer-1);                // get module value [0:1] from layer value

      Vector3d[] corners = svtStripFactory.getShiftedLayerCorners(iregion-1, isector-1, RM[1] ); // corners of module

      double modAveX = (corners[0].x+corners[1].x)/2.0;                   // average x values of opposing corners
      double modAveY = (corners[0].y+corners[1].y)/2.0;                   // average y values of opposing corners

      Line3d labShiftedRSMS1   = svtStripFactory.getShiftedStrip(iregion-1, isector-1, RM[1], 0);     // shifted lab-frame 1
      Line3d labShiftedRSMS256 = svtStripFactory.getShiftedStrip(iregion-1, isector-1, RM[1], 255);   // shifted lab-frame 256
      Vector3d o1   = labShiftedRSMS1.origin;                                                         // origin of lab-frame 1
      Vector3d o256 = labShiftedRSMS256.origin;                                                       // origin of lab-frame 256
      double angle = 0;
      if (ilayer-1 % 2 == 0) {                                              // adjusts for angle module is tilted at
         angle  = Math.atan2( (o256.y-o1.y), (o256.x-o1.x) );
      } else {
         angle  = Math.atan2( (o1.y-o256.y), (o1.x-o256.x) );
      }
      System.out.println(angle);

      //meanVal[59] = -2.0;
      //meanVal[57] = 2.0;

      modCurr = "R"+Integer.toString(iregion)+"S"+Integer.toString(isector)+"L"+Integer.toString(RM[1]+1);
      for (imod = 0; imod < modArr.length; imod++) {                        // loop through modules until imod = current module
        if(modArr[imod] == modCurr) {                                       // if modArr element equals mod currently being considered:
          double x1     = modAveX + 10*sigmaVal[imod]*Math.cos(angle) + 1*meanVal[imod]*Math.cos(angle); // sigma in um, modAves in mm
          double y1     = modAveY + 10*sigmaVal[imod]*Math.sin(angle) + 1*meanVal[imod]*Math.sin(angle);
          double x2     = modAveX - 10*sigmaVal[imod]*Math.cos(angle) + 1*meanVal[imod]*Math.cos(angle);
          double y2     = modAveY - 10*sigmaVal[imod]*Math.sin(angle) + 1*meanVal[imod]*Math.sin(angle);
          bw.write( Double.toString(x1) + ", " +  Double.toString(y1) + ", " + Double.toString(x2) + ", " + Double.toString(y2) + ", " + Double.toString(meanVal[imod]));
          bw.newLine();
          if (imod % 2 == 0) {                                              // if even imod, x1/y1; else x2/y2.
            bw2.write( Double.toString(modAveX) + ", " + Double.toString(modAveY) );
           } else {
            bw2.write( ", " + Double.toString(modAveX) + ", " + Double.toString(modAveY) );
            bw2.newLine();
          }
          break;                                                            // exit for loop if value added/considered
        }
      }
    }
  }
}
bw.close();
bw2.close();

// * * * * * * * * * Copied from ParallelSliceFitter.java * * * * * * * * * //

private double getMaxYIDataSet(IDataSet data, double min, double max) {
  double max1 = 0;
	double xMax = 0;
	for (int i = 0; i < data.getDataSize(0); i++) {
		double x = data.getDataX(i);
		double y = data.getDataY(i);
		if (x > min && x < max && y != 0) {
			if (y > max1) {
				max1 = y;
				xMax = x;
			}
		}
	}
	return max1;
}
private double getMeanIDataSet(IDataSet data, double min, double max) {
	int nsamples = 0;
	double sum = 0;
	double nEntries = 0;
	for (int i = 0; i < data.getDataSize(0); i++) {
		double x = data.getDataX(i);
		double y = data.getDataY(i);
		if (x > min && x < max && y != 0) {
			nsamples++;
			sum += x * y;
			nEntries += y;
		}
	}
	return sum / (double) nEntries;
}
private double getRMSIDataSet(IDataSet data, double min, double max) {
	int nsamples = 0;
	double mean = getMeanIDataSet(data, min, max);
	double sum = 0;
	double nEntries = 0;
	for (int i = 0; i < data.getDataSize(0); i++) {
		double x = data.getDataX(i);
		double y = data.getDataY(i);
		if (x > min && x < max && y != 0) {
			nsamples++;
			sum += Math.pow(x - mean, 2) * y;
			nEntries += y;
		}
	}
	return Math.sqrt(sum / (double) nEntries);
}

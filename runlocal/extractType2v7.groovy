// these needed to read reconstructed data. ---------------------
import org.jlab.io.hipo.*;
import org.jlab.io.base.*;
import org.jlab.io.evio.*;
import org.jlab.clas.physics.*;

// for hists
import org.jlab.groot.data.TDirectory;
import org.root.group.*;
import org.root.pad.*;
import org.root.func.*;
import org.root.histogram.*;
import org.root.attr.*;
import java.lang.Math;
import org.jlab.groot.data.H1F;
import org.jlab.groot.data.H2F;
import org.jlab.groot.group.DataGroup;
import org.jlab.groot.math.FunctionFactory;
import org.jlab.groot.ui.TCanvas;

// to make plots
import org.jlab.groot.data.H1F;
import org.jlab.groot.ui.TCanvas;
import org.jlab.groot.graphics.*;
import javax.swing.JFrame;
import org.jlab.groot.math.*;
import org.jlab.groot.fitter.*;

// language utils
import java.util.Arrays;

// code to read output file from the CLAS12 BST reconstruction, create and fill hists,
// skim events into another file, and save the histogram output. 
//                                                    - gpg 2/9/16
//
// streamlined event selection. gpg 3/7/16
//
// started life as readBSTalignType1v2b.groovy
//
//
// extractType2v4.groovy  -  updated extractType2v2.groovy for coatjava 3a.0. put in alignment/run.
// extractType2v5.groovy  -  updated extractType2v4.groovy to read hipo files.
// extractType2v6.groovy  -  changed extractType2v5.groovy to 3 SVT regions.
// extractType2v7.groovy  -  updated extractType2v6.groovy to new banks 
//
// to run
//
//        $COATJAVA/bin/run-groovy extractType2v7.groovy inputFile.hipo y/n
//
//          inputFile.hipo - hipo event file
//          y/n - choose 'y' or 'n' depending if you want to write out two files;
//                one containing only type-1 events and the other containing only
//                type-2 event.
//
//          outputs: histFile3.hipo - histogram file (for either 'y' or 'n' option).
//                   for 'y' option.
//                   extract_rec_cosmicsSimType1.hipo - hipo event file with type-1's only
//                   extract_rec_cosmicsSimType2.hipo - hipo event file with type-2's only
//
// user quantities.  ***********************************************************
HipoDataEvent event;
PhysicsEvent recEvent;
Particle recCosmic;

// constants and type 1 listing of required hits by region and sector.
double pi=3.141592654, Ebeam=11.0, Mp=0.938272, pitch=0.156, resolution=0.080;
//int[][] type1topologyArray=[[4,1],[3,1],[2,1],[1,1],[1,6],[2,8],[3,10],[4,13]];
int[][] type1topologyArray=[[3,1],[2,1],[1,1],[1,6],[2,8],[3,10]];

// first argument is the filename.
String inputFile  = args[0];

// second argument is write output or not ('y' or 'n')
String writeOutput = args[1];

// create the object to read in the data file.
HipoDataSource reader = new HipoDataSource();
reader.open(inputFile);
System.out.println("------------>  reading file" + inputFile); // print out stuff.

// open output data files. *********************************************
HipoDataSync  writer1 = new HipoDataSync();
HipoDataSync  writer2 = new HipoDataSync();

if (writeOutput == "y") {
  writer1.open("extract_rec_cosmicsSimType1.hipo");
  writer2.open("extract_rec_cosmicsSimType2.hipo");
}


// define output directory here. ********************************************
TDirectory histFile = new TDirectory();

// make subdirectory for the global plots (not by sector).
String dirPlots = new String("/bstEvents/align/plots");
histFile.mkdir(dirPlots);
histFile.cd(dirPlots);

// Define global histograms for all events in the output file. ***********************************
H1F hyx_slope = new H1F("hyx_slope", "hyx_slope", 80, -4, 4.0);
H1F hyz_slope = new H1F("hyz_slope", "hyz_slope", 80, -4, 4.0);
H1F hphi      = new H1F("hphi", "hphi", 90, 0, 180.0);
H1F htheta    = new H1F("htheta", "htheta", 90, 0, 180.0);
H1F hHits     = new H1F("hHits", "hHits", 80, 0, 80.0);
H1F hresidual = new H1F("hresidual", "hresidual", 100, -0.75, 0.75);
H1F hlayers   = new H1F("hlayers", "hlayers", 10, 0, 10.0);

// set some plot parameters.
hyx_slope.setOptStat("1110"); hyx_slope.setTitle("hyx_slope");
hyz_slope.setOptStat("1110"); hyz_slope.setTitle("hyz_slope");
hphi.setOptStat("1110");      hphi.setTitle("hphi");
htheta.setOptStat("1110");    htheta.setTitle("htheta");
hHits.setOptStat("1110");     hHits.setTitle("hHits");
hresidual.setOptStat("1110"); hresidual.setTitle("hresidual");
hlayers.setOptStat("1110");   hlayers.setTitle("hlayers");

// add hists to dirPlots directory.
histFile.cd(dirPlots);
histFile.addDataSet(hyx_slope,hyz_slope,hphi,htheta,hHits,hresidual,hlayers);

// debugging hists
H1F hcentL1S1a = new H1F("hcentL1S1a", "hcentL1S1a", 100, -1.49, 1.49);
H1F hcentL1S2a = new H1F("hcentL1S2a", "hcentL1S2a", 100, -1.49, 1.49);
H2F hlayerVSresidual = new H2F("hlayerVSresidual", "hlayerVSresidual", 100, -2, 2.0, 10,0,10);

// make list of subdirectories for the sectors to store histograms.
//gpg int nsectors = 24;
int nsectors = 18;
String[] dirList = new String[nsectors];

for (int isector = 0; isector < nsectors; isector++) {
    if (isector<9) {
      String sectorNumber = Integer.toString(isector+1);
      String dirName = "/bstEvents/align/sector0" + sectorNumber;
      dirList[isector]=dirName;
      histFile.mkdir(dirList[isector]);
    } else {
      String sectorNumber = Integer.toString(isector+1);
      String dirName = "/bstEvents/align/sector" + sectorNumber;
      dirList[isector]=dirName;
      histFile.mkdir(dirList[isector]);
    }
}

// define the sector plots: residuals and phi vs. residual.
//gpg int nlayers=8;
int nlayers=6;

// define directories in output file for hists.
for (isector=0; isector<nsectors; isector++) {
  histFile.cd(dirList[isector]);
  histFile.addDataSet(hlayerVSresidual);
  // plots for different layers.
  for (int ilayer=0;ilayer<nlayers;ilayer++) {
    String histName1, histName2, histName3;
    String layerNumString = Integer.toString(ilayer+1);
    String sectorNumString= Integer.toString(isector+1)
    histName1="hresidualL"+layerNumString;
    histName2="hphiVSresidualL"+layerNumString;
    histName3="hcentResidualL"+layerNumString;

    histFile.cd(dirList[isector]);

    //H1F histName1here = new H1F(histName1, histName1, 100, -0.75, 0.75);
    H1F histName1here = new H1F(histName1, histName1, 400, -2, 2.0);
    histName1here.setOptStat("1110");
    histName1here.setTitle(histName1);
    histFile.addDataSet(histName1here);

    H2F histName2here = new H2F(histName2, histName2, 100, -2, 2.0, 90, 0, 180);
    histName2here.setTitle(histName2);
    histFile.addDataSet(histName2here);

    H1F histName3here = new H1F(histName3, histName3, 400, -2, 2);
    histName3here.setOptStat("1110");
    histName3here.setTitle(histName3);
    histFile.addDataSet(histName3here);

  } // end of loop over layers.
} // end of loop over sectors.


// ********************************************************************************************
// loop over the data here. *******************************************************************
// ********************************************************************************************
//System.out.println("Starting event loop.\n");

// define variables.
// count the number of events.
int nevents=0, maxEvents=1500000, nType1=0, nType2=0;

while(reader.hasEvent() && nevents < maxEvents){  // start of event loop. ********************
   // get the events -----------------------
   event = reader.getNextEvent();
   nevents++;

   // count events.
   int remainder = nevents % 10000;
   if (remainder == 0) {
     System.out.println("events processed: "+nevents);
   }

   // get the banks I need. -----------------------
   HipoDataBank bstBank = event.getBank("CVTRec::Cosmics"); // Cosmic bank with slope for each BST track.
   HipoDataBank bstHitsBank = event.getBank("BSTRec::Hits"); // Hits bank with fit residuals for each BST track.
   int nHits=0;
   if (event.hasBank("BSTRec::Hits")) {
     //bstHitsBank.show();
     nHits=bstHitsBank.rows();
   }

   HipoDataBank crossesBank = event.getBank("BSTRec::Crosses"); // get the Crosses bank for each BST track.
   int nCrosses=0;
   if (event.hasBank("BSTRec::Crosses")) {
      nCrosses=crossesBank.rows();
   }
   HipoDataBank clustersBank = event.getBank("BSTRec::Clusters"); // get the Clusters bank for each BST track.
   int nClusters=0;
   if (event.hasBank("BSTRec::Clusters")) {
      nClusters=clustersBank.rows();
   }


   // Make plots for all events and all sectors and regions from Cosmics bank. -----------------------
   // tests on the data first.
   boolean phiCut=false, thetaCut=false, angleCut=false;
   double mychisq=0, mychisq2=0;
   // fit results plots
   if (event.hasBank("CVTRec::Cosmics")) {
     int nTracks=bstBank.rows();
     double[] yx_slope = bstBank.getFloat("trkline_yx_slope");
     double[] yz_slope = bstBank.getFloat("trkline_yz_slope");
     double[] chi2 = bstBank.getFloat("chi2");

     // loop over the cosmic ray tracks in the event.
     for (int counter=0; counter<nTracks; counter++) {
       hyx_slope.fill(yx_slope[counter]);
       hyz_slope.fill(yz_slope[counter]);

       // extract phi
       double tanphi = 1/yx_slope[counter];
       double phiBST = Math.atan(tanphi);
       if (phiBST < 0.0) phiBST=phiBST + pi;
       //H1F hphi = (H1F) histFile.getDirectory(dirPlots).getObject("hphi");
       hphi.fill(phiBST*180/pi);
       phiCut=false;
       // phi cut on track - hardwired in. This should be fixed.
       if (phiBST > 1.222 && phiBST < 1.920) {phiCut=true;} // 70-110 deg

       // extract theta
       double denom = Math.sqrt(1 + Math.pow(yz_slope[counter],2) + Math.pow(yx_slope[counter],2));
       double costheta = yz_slope[counter]/denom;
       double theta = Math.acos(costheta);
       //H1F htheta = (H1F) histFile.getDirectory(dirPlots).getObject("htheta");
       htheta.fill(theta*180/pi);
       thetaCut=false;
       // theta cut on track - hardwired in. This should be fixed.
       if (theta > 1.222 && theta < 1.920) {thetaCut=true;} // 70-110 deg

       // cut on verticality of track
       angleCut=false;
       if (phiCut && thetaCut) {angleCut=true;}

     } // end of loop over cosmic ray tracks in the event   
   } // end of if/check on Cosmics bank existence.

   // Make plots for all events and all sectors and regions from Hits bank. -----------------------
   if (event.hasBank("BSTRec::Hits")){
     //nHits=bstHitsBank.rows();
     histFile.cd(dirPlots);
     //H1F hHits = (H1F) histFile.getObject("hHits");
     hHits.fill(nHits);
   }



   //
   // Pick out type 2 events here. ********************************************************************
   // Require 12 hits, all different nlayer/sector combinations.
   //
   boolean type2topology=false;
   boolean type1topology=false;
   boolean allSingleStripClusters=false;
   // test that we have the necessary banks.
   if (event.hasBank("BSTRec::Hits") && event.hasBank("BSTRec::Crosses") && event.hasBank("BSTRec::Clusters")){
     // get the stuff needed from the Hits bank.
     double[] residual = bstHitsBank.getFloat("fitResidual");
     double[] layer    = bstHitsBank.getByte("layer");
     double[] sectorNo = bstHitsBank.getByte("sector");
     double[] stripNo  = bstHitsBank.getInt("strip");

     // get the stuff from the Crosses bank.
     int[] sectorCrosses = crossesBank.getByte("sector");
     int[] regionCrosses = crossesBank.getByte("region");
     int[] clusterID1    = crossesBank.getShort("Cluster1_ID");
     int[] clusterID2    = crossesBank.getShort("Cluster2_ID");

     // get the stuff from the Clusters bank.
     double[] centroidResidual = clustersBank.getFloat("centroidResidual");
     double[] clusterLayer     = clustersBank.getByte("layer");
     double[] clusterSector    = clustersBank.getByte("sector");
     int[]    clusterSize      = clustersBank.getShort("size");

     // loop over all the crosses in the event.  ****************************************************
     //
     // This algorithm captures type 1 and type 2 events.
     //
     // 1. check the event has exactly 6 crosses and 12 clusters.
     // 2. loop over the crosses in the data.
     //         3. loop again over the data and count the region-sector overlaps with the current one.
     //         4. break if overlaps ever > 1.
     // 5. keep event if you found overlaps=1 exactly 6 times.

     //gpg int wantedCrosses=8, goodCrosses=0, wantedClusters=16, wantedHits=16;
     int wantedCrosses=6, goodCrosses=0, wantedClusters=12, wantedHits=12, goodSingleClusters=0, wantedSingleClusters=12;

     // check we have exactly the right number of crosses and the right number of clusters
     if (nCrosses == wantedCrosses && nClusters == wantedClusters) {
     //if (nCrosses == wantedCrosses) {

       // data for the crosses in the event.
       goodCrosses = 0;
       goodSingleClusters= 0;
       // use this array to count the number of region-sector combinations that match the type-1 topology.
       int[] type1crossHitsSeen=[0,0,0,0,0,0]; 
       // use this array to count the number of region-sector combinations that match the type-1 topology.
       int[] type1crossHitsExpected=[1,1,1,1,1,1];

       // outer loop over the crosses in the event.
       for (int iCross=0; iCross<wantedCrosses; iCross++) {
	 int thisRegion = regionCrosses[iCross];
	 int thisSector = sectorCrosses[iCross];
	 int nThisTopology = 0;

	 // loop over the data again looking for overlaps. If we find one, then
	 // we drop the event.
	 for (int jCross=0; jCross<wantedCrosses; jCross++) {
	   if (thisRegion == regionCrosses[jCross] && thisSector == sectorCrosses[jCross]) {
	     nThisTopology++;
	   }
	   if (nThisTopology > 1) { // found a double hit.
	     break;    // leave the inner loop over the crosses
	   }
	 }  // end of inner loop over the crosses

	 if (nThisTopology > 1) {  // deal with the double hit.
	   type2topology = false;
	   break;                  // leave the outer loop over the crosses since we can't use this event.
	 } else if (nThisTopology == 1) {
	   goodCrosses++;
	 }

	 // still processing this event.
	 // get array of hits to check for type-1s. we now have the right number of crosses so 
	 // count the hits for each entry in the type 1 topology. should be just one of each for 
	 // type 1 so type1crossHitsSeen=1 for each element in the array.
	 //
	 for (int jCross=0; jCross<wantedCrosses; jCross++) {
	   if (thisRegion==type1topologyArray[jCross][0] && thisSector==type1topologyArray[jCross][1]) {
	     type1crossHitsSeen[jCross]++;
	   } // end of if on region and sector
	 } // end of local for loop over crosses.

	 // to select single strip events check that the clusters in each cross are single hits.
	 int thisClusterID1 = clusterID1[iCross];
	 int thisClusterID2 = clusterID2[iCross];

	 if ((clusterSize[thisClusterID1-1] == 1) && (clusterSize[thisClusterID2-1] == 1)) {
	   goodSingleClusters+=2;
	 }

       } // end of outer loop over data crosses in this event.

       // Do we have 12 single-strip clusters in this event?
       if (goodSingleClusters == 12) {
	 allSingleStripClusters = true;
       }

       // see if we have one type-1 match for each cluster
       type1topology=Arrays.equals(type1crossHitsSeen,type1crossHitsExpected);

       // see if we found enough good crosses for a type 2.
       if (goodCrosses == wantedCrosses) {
	 type2topology = true;
       } // end of test on number of desired crosses found in the event.

     } // end of test on total number of crosses, clusters in the event.

     // ************************************************************************************
     // done extracting the type 2 events (and type 1's). *********************************
     // ************************************************************************************

     // if we found a type 2 write out the desired events.
     // if ((type2topology ==true) && (type1topology != true) && (allSingleStripClusters==true)) {
     if ((type2topology==true) && (type1topology != true)) {
       nType2++;
       if (writeOutput == "y") writer2.writeEvent(event);
     }

     // if we found a type 2 write out the desired events.
     if (type1topology==true) {
       nType1++;
       if (writeOutput == "y") writer1.writeEvent(event);
     }

     // done extracting the type 2 events (and type 1's.************************************
     // ************************************************************************************

     // now fill histograms to check the data quality. *************************************
     //System.out.println("N = "+nevents+" nClusters = "+nClusters);
     //if (nHits > 0 && nClusters > 0 && type2topology==true) {
     if (nHits > 0 && nClusters > 0 && type2topology==true) {

       // get the chisq from the reconstruction.
       double reconChisq=0;
       if (event.hasBank("CVTRec::Cosmics")) {
	 double[] chi2 = bstBank.getFloat("chi2");
	 reconChisq=chi2[0];
       }

       for (counter=0; counter < nClusters; counter++) {
	 if (clusterLayer[counter] == 1 && clusterSector[counter] == 1) {
	   hcentL1S1a.fill(centroidResidual[counter]);
	 } // end of if on this layer/sector
	 if (clusterLayer[counter] == 1 && clusterSector[counter] == 2) {
	   hcentL1S2a.fill(centroidResidual[counter]);
	 } // end of if on this layer/sector
       } // end of loop over clusters.


       // Now select tracks with a reasonable (?) chisq and the right topology for the histograms.
       //if (mychisq2 < 200.0) {
       // loop over the hits here.
       for (counter=0; counter<nHits; counter++) {
           // general plots here.
	   histFile.cd(dirPlots);
	   hresidual.fill(residual[counter]);
	   //System.out.println("residual = "+residual[counter]);
	   hlayers.fill(layer[counter]);
	   hlayerVSresidual.fill(residual[counter],layer[counter]);

	   // do sector plots here.
	   int index = sectorNo[counter]-1; // index points to the subdirectory for this sector.
	   String sectorNumString = Integer.toString(index+1);
	   histFile.cd(dirList[index]);
	   hlayerVSresidual.fill(residual[counter],layer[counter]);

	   // layer plots here for this hits's sector. build the name first and load the histogram.
	   int layerNo = layer[counter];
	   String layerNumString = Integer.toString(layerNo);
	   String histName1;
	   if (index+1 < 10) {
	     histName1="/bstEvents/align/sector0"+sectorNumString+"/hresidualL"+layerNumString;
	   } else {
	     histName1="/bstEvents/align/sector"+sectorNumString+"/hresidualL"+layerNumString;
	   }
	   //System.out.println("histName1 = "+histName1+" dirList["+index+"]="+dirList[index]);
	   histFile.cd(dirList[index]);
	   H1F hresidualThisLayer = (H1F) histFile.getObject(histName1);
	   hresidualThisLayer.fill(residual[counter]);
       } // end of loop over hits

       // now loop over the clusters
       for (counter=0; counter < nClusters; counter++) {
	   // do sector plots here.
	   int indexClusterSector = clusterSector[counter]-1; // index points to the subdirectory for this sector.
	   histFile.cd(dirList[indexClusterSector]);

	   // layer plots here for this cluster's sector. build the name first and load the histogram.
	   int layerCluster = clusterLayer[counter];
	   String layerNumCluster = Integer.toString(layerCluster);
	   int sectorCluster = clusterSector[counter];
	   String sectorNumCluster = Integer.toString(sectorCluster);
	   String histName2;
	   if (sectorCluster < 10) {
	     histName2 = "/bstEvents/align/sector0"+sectorNumCluster+"/hcentResidualL"+layerNumCluster;
	   } else {
	     histName2 = "/bstEvents/align/sector"+sectorNumCluster+"/hcentResidualL"+layerNumCluster;
	   }
	   histFile.cd(dirList[indexClusterSector]);
	   H1F hcentResidualThisLayer = (H1F) histFile.getObject(histName2);
	   hcentResidualThisLayer.fill(centroidResidual[counter]);
       }  // end of loop over clusters.

       //} // end of test on chisq.
     } // end of test on nHits and topology.
   }  // test on Hits, Clusters, and Crosses banks existence.
} // end of loop over events.
// ********************************************************************************************
// end of loop over the data here. ************************************************************
// ********************************************************************************************


System.out.println("nevents= "+nevents+" Type1 events = "+nType1+" Type2 events = "+nType2);

// write it to the output file.
histFile.writeFile("histFile3.hipo");
if (writeOutput == "y") {
  writer1.close();
  writer2.close();
}

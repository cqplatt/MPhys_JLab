#! /usr/bin/env csh

source ../.cshrc

# clean-up:
rm -r ../.farm_out/
rm -r ./input/
rm -r ./output/
mkdir ./input/

set count   = 1;
set njobs   = 5;
set nevents = 50000;
set rum     = 10 #run num;
set card    = "cosmics";
set queue   = "debug";
set inpath  = "/home/$USER/runonfarm/input";

cp makeHists.groovy ${inpath}/makeHists.groovy
cp $card.gcard ${inpath}/$card.gcard

sed -i "s|RUN_NO|$rum|g" ${inpath}/makeHists.groovy

while ($count <= $njobs)
  echo "count" $count "of" $njobs

  cp augerscript.xml ${inpath}/augerscript-$count.xml
  cp createhists.sh  ${inpath}/createhists-$count.sh

  sed -i "s|JOBNO|$count|g"     ${inpath}/augerscript-$count.xml
  sed -i "s|JOBNO|$count|g"     ${inpath}/createhists-$count.sh

  sed -i "s|CARD_IN|$card|g"    ${inpath}/augerscript-$count.xml
  sed -i "s|CARD_IN|$card|g"    ${inpath}/createhists-$count.sh

  sed -i "s|NEVENTS|$nevents|g" ${inpath}/createhists-$count.sh
  sed -i "s|RUN_NO|$rum|g"      ${inpath}/createhists-$count.sh
  sed -i "s|QUEUE_SUB|$queue|g" ${inpath}/augerscript-$count.xml

  jsub -xml ${inpath}/augerscript-$count.xml

  echo "submitted job"

  @ count++;
end

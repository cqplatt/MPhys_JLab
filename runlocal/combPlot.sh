#plot histograms outputted from makeHists.groovy:
printf "\n\n * * * * * PLOTTING HIST1,2,3.PNG * * * * * \n\n"
$COATJAVA/bin/run-groovy plotHists.groovy /home/cqplatt/runlocal/simRecHist-LOCAL.hipo

#plot .txt files outputted from plotHists.groovy with gnuplot:
printf "\n\n * * * * * PLOTTING PLOT.PNG * * * * * \n\n"
rm ./plot.png

gnuplot >./plot.png <<EOF
  set cbrange [-0.5:0.5]
  set key off
  set xlabel "x (mm)"
  set ylabel "y (mm)"
  set cblabel "residual mean (um)"
  set term png size 1100,1000
  set size square 1,1
  set grid
  plot "residuals.txt" using 1:2:(\$3-\$1):(\$4-\$2):5 with vectors nohead palette lw 4, \
       "residuals.txt" using (((\$1+\$3)/2)-20.0):((\$2+\$4)/2):(sprintf("%1.2f", \$5)) \
                             with labels font ",9", \
       "midpoints.txt" using 1:2:(\$3-\$1)*2:(\$4-\$2)*2 with vectors nohead lw 2 lc -1, \
       "midpoints.txt" using 1:2:(\$3-\$1)*-1:(\$4-\$2)*-1 with vectors nohead lw 2 lc -1

EOF

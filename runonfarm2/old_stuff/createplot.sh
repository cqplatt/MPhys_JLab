#!/bin/csh
#plot out.txt from above with gnuplot:
rm ./output/plot.png
set OUT = "./output/plot.png"
	gnuplot >$OUT <<EOF
	set key off
        set xlabel "x (mm)"
        set ylabel "y (mm)"
	      set cblabel "residual mean (um)"
        set title "SVT Module Residuals and Errors: \n module width represents sigma of residual Gaussian (scaled by 1,000, exaggerated by 10); \n module displacement from midpoint represents mean of Gaussian (scaled by 1,000)"
        set term png size 1200,1200
        set size square 1,1
        set grid
#	plot "output/residuals.txt" using 1:2:(\$3-\$1):(\$4-\$2):5 with vectors nohead palette lw 3.5, "output/midpoints.txt" using 1:2:(\$3-\$1):(\$4-\$2) with vectors nohead lw 2 lc -1
	      plot "./output/residuals.txt" using 1:2:(\$3-\$1):(\$4-\$2):5 with vectors nohead palette lw 4, "./output/midpoints.txt" using 1:2:(\$3-\$1)*2:(\$4-\$2)*2 with vectors nohead lw 2 lc -1, "./output/midpoints.txt" using 1:2:(\$3-\$1)*-1:(\$4-\$2)*-1 with vectors nohead lw 2 lc -1

EOF

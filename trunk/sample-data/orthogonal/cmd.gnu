set grid xtics  ytics;
set xlabel "Number of Data Points Trained"
set title "Title";

set term x11 1;
plot "run.dat" using ($1):($3) with lines title "Train lin"

set out "lin.svg" ; set term svg; replot
set out "lin.png" ; set term png; replot


set term x11 2;
plot "run.dat" using ($1):($2) with lines title "Train log"

set out "log.svg" ; set term svg; replot
set out "log.png" ; set term png; replot

set term x11 3;
plot "run.dat" using ($1):(1.0/($2)) with lines title "1/Log"

set out "log1.svg" ; set term svg; replot
set out "log1.png" ; set term png; replot
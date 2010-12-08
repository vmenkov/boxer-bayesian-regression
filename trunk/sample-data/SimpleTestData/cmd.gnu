set grid xtics  ytics;
set xlabel "Number of Data Points Trained"
set title "Title";

set term x11 1;
plot "run.dat" using ($1):($3) with lines title "Train lin",  "run.dat" using ($1):($5) with lines title "Test lin";

set out "lin.svg" ; set term svg; replot
set out "lin.png" ; set term png; replot


set term x11 2;
plot "run.dat" using ($1):($2) with lines title "Train log",  "run.dat" using ($1):($4) with lines title "Test log";

set out "log.svg" ; set term svg; replot
set out "log.png" ; set term png; replot

set term x11 3;
plot "run.dat" using ($1):($6) with lines title "Train wt. avg. recall",  "run.dat" using ($1):($7) with lines title "Test wt. avg. recall";

set out "war.svg" ; set term svg; replot
set out "war.png" ; set term png; replot

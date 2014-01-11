Mario is Missing! Route Generator
=================================

The Mario is Missing! Route Generator produces speedrun routes for the Super
NES version of Mario is Missing!  The program simulates different strategies
for playing through the game and estimates how long each route will take.  Once
the simulations for the top few fastest routes are complete, the program prints
those routes to the console so a speedrunner can try them on a real console.

Due to simplifications in the program's game model and variation in the time
it takes a live speedrunner to play through the route, it is not guaranteed that
the routes output by this program are necessarily the "best."  However, past
experience has suggested that the current top generated route is about a minute
faster than the most obvious natural route (playing the stages from left to
right on each floor of the castle), so even with those limitations, the ability
to generate routes in this way is still an important part of speedrunning
Mario is Missing!


Requirements
------------

The route generator is written in Java and requires Java 7 at a minimum.  There
are no software requirements outside of the JVM.  However, a beast of a computer
and a lot of patience are also strongly recommended, because...


EXTREMELY IMPORTANT WARNING
---------------------------

To minimize the time spent simulating suboptimal routes, the generator keeps a
very large number of partially-completed routes in memory.  Because of this,
the program is **preposterously space-intensive**.  You will need a 64-bit JVM
and a lot of RAM to simulate the full game.  In the past, I believe I have run
my simulations with a 10 GB or possibly even 12 GB heap.

It is very likely that the memory requirements could be reduced by reevaluating
the program's data structures.  However, a few months before I started working
on the route generator, I coincidentally decided to put 32 GB RAM in my desktop,
so running time optimizations have been more of a concern for me in the past ---
and even with that, the program still takes hours to complete on my i5 2500k
(which is admittedly a couple years old, but is still not really that old).
Getting the generator to run a typical PC will probably be my first priority
now that the program has been publicly released.


Usage
-----

TODO Provide usage information once you've written that new loader.


Interpreting Routes
-------------------

TODO Give some examples of the route output and explain what it means.


Contact Information
-------------------

To report problems running the program or inaccuracies in its output, please
use the GitHub Issues page at https://github.com/uusdfg/mim-router/issues

For general comments or questions, message __sdfg at one of the following sites:

* Twitter:  https://twitter.com/__sdfg
* Speed Demos Archive forum:  https://forum.speeddemosarchive.com/profile/__sdfg.html
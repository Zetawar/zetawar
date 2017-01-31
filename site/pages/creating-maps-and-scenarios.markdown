---
name: Creating Maps and Scenarios
---

This guide describes how to create maps and scenarios for Zetawar.

First some explanation of terminology is needed. Games take place on maps, but
maps by themselves don't include units or bases. Scenarios describe the initial
state of units and bases on a map. This alows a single map to support multiple
scenarios.

The map and scenario data are located in the Clojure maps in the 'maps' and
'scenarios' vars in [src/cljs/zetawar/data.cljs](https://github.com/Zetawar/zetawar/blob/481bfa3e789683b8216c0495babcd2e32aa8e86a/src/cljs/zetawar/data.cljs).
To add your own maps and scenarios simply add new entries to those maps. For
help understanding the data format, check out the map and scenario format dev
cards.

While creating maps and and scenarios it's useful to be able to see what they
look like. To get a live preview while editing, add devcards for your scenarios
to
[src/cljs/zetawar/devcards/scenarios.cljs](https://github.com/Zetawar/zetawar/blob/481bfa3e789683b8216c0495babcd2e32aa8e86a/src/cljs/zetawar/devcards/scenarios.cljs). Then run "boot dev"
(after [installing boot](https://github.com/boot-clj/boot#install) if necessary) and browse to <http://localhost:3000/devcards#!/zetawar.devcards.scenarios>.

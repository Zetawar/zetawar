---
name: Creating Maps and Scenarios
---

This guide describes how to create maps and scenarios for Zetawar.

First some explanation of the terminology is needed. Games take place on maps,
but maps by themselves don't include units or bases. Scenarios describe the
initial state of units and bases on a map. This alows a single map to support
multiple scenarios.

The map and scenario data are located in the Clojure maps in the 'maps' and
'scenarios' vars in [src/clj/zetawar/data.cljs](https://github.com/Zetawar/zetawar/blob/10340716476d60ce088838f88b56d7cd3be98c0b/src/clj/zetawar/data.cljs).
To add your own maps and scenarios simply add new entries to those maps. For
help understanding the data format, check out the map and scenario format dev
cards.

While creating maps and and scenarios it's useful to be able to see what they
look like. To get a live preview while editing them, add devcards for your
scenarios to [src/clj/zetawar/devcards/scenarios.cljs](https://github.com/Zetawar/zetawar/blob/10340716476d60ce088838f88b56d7cd3be98c0b/src/clj/zetawar/devcards/scenarios.cljs).


### TODO

- Add link to map and scenario format devcards once they're complete
- Add examples (?)

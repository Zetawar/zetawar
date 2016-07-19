---
description: Technology Choices
date-published: 2016-05-17
---

Hello world! Welcome to the Zetawar blog!

A few people have expressed interest in learning a bit more about the
technology behind Zetawar, so here goes. In this first blog post I will
describe the key libraries I'm using and my rationale for choosing them.

### ClojureScript

First off, it's important to note Zetawar is implemented entirely in
[ClojureScript](https://github.com/clojure/clojurescript). However, explaining
that decision is more than I want to attempt in this post. Just take it as a
given for now, and I'll try to dig into that more in the future.

### Datascript

The Zetawar game and user interface state are stored in
[Datascript](https://github.com/tonsky/datascript). Datascript is an in-memory
database that supports ClojureScript and Clojure and implements a large portion
of the Datomic API. [Datomic](http://www.datomic.com/) in turn is a durable
relational database that supports ACID transactions, declarative queries, and
has a first class notion of time. Due to it being in-memory only, Datascript
does not support durability. It also does not retain a history of all changes
to the database like Datomic does, but it implements enough Datomic features to
be quite useful in its own right.

I decided to use Datascript primarily for two reasons. First, Datascript
provides extremely flexible query functionality. It supports
[Datalog](http://docs.datomic.com/query.html), which provides SQL like queries,
Datomic's [pull API](http://docs.datomic.com/pull.html), which provides
hierarchical data selection, and Datomic's [entity
API](http://docs.datomic.com/entities.html), which provides a lazy map like
interface to database entities. This query flexibility means data can be
structured based on logical relationships between data elements rather than how
the data is accessed. Second, if in the future I want to make a version of the
game that has a server component that needs to run some of the game logic, it
should be relatively easy to port the game logic from Datascript to Datomic
since their APIs are so similar.

### Reagent

The Zetawar user interface is rendered with
[Reagent](https://reagent-project.github.io/). Given that I wanted to make
Zetawar web based (I'll discuss that decision more in a future post), the
declarative React model on which Reagent is based is a natural fit. The game
board interface is a pure function of the game state and the performance
requirements of a turn based strategy game are not so great that rendering to
the DOM (as opposed to Canvas or WebGL) is likely to be a bottleneck.

So why Reagent and not [Om](https://github.com/omcljs/om),
[Om.next](https://github.com/omcljs/om/wiki/Quick-Start-\(om.next\)), or
[Rum](https://github.com/tonsky/rum)? Mostly it comes down to familiarity. I
have used Reagent on other projects and found it to be effective and easy to
use. It's also quite straightforward, with the addition of cursors in Reagent
0.5, to mimic the single atom approach of Om if desired. Om.next and Rum both
look interesting, and I did seriously consider Om.next, but wasn't convinced
that it provided enough benefits over Reagent (for this project anyway) to
justify both the risk of using such a new library as well as the extra time it
would take to get up to speed on it. I dismissed Rum on similar grounds. Though
I do appreciate its minimalist design and hope to try it out on some future
project.

### Posh

[Posh](https://github.com/mpdairy/posh) is the glue that connects Reagent to
Datascript. It provides a mechanism for specifying Datascript queries and pulls
that are only updated when data related to them has changed. Compared to the
simplest alternative of rerunning queries on every transaction, this provides a
huge performance boost.

As a new and not very well known library, it is likely the riskiest of these
choices. But so far, it has worked extremely well. The only issue I have
encountered was a bad interaction with the new lazy-by-default reactions in
Reagent
[0.6.0-alpha](https://reagent-project.github.io/news/news060-alpha.html). Posh
currently assumes that only a single transaction will take place between each
evaluation of its reactions. With the new lazy-by-default reactions, if
multiple transactions take place before the next render, some Posh queries may
fail to run when related data changes. Thankfully, there is a simple
workaround, run Reagent's flush in a Datascript transaction listener so that
all reactions are evaluated after every transaction. This destroys the
performance improvements possible with lazy transactions, but so far this
hasn't been a significant issue. Also, it sounds like this problem will be
fixed in the next version of Posh, so it may not even be an issue by the time
the final version of Reagent 0.6.0 is released.

### Conclusion

That's it for now. In my next post I'll dig into how these libraries are tied
together in Zetawar.

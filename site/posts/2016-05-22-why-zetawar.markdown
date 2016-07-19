---
description: Why Zetawar?
date-published: 2016-05-22
---

In my last post I promised to dig into the architecture of Zetawar. I'm still
planning to do that, but before I get too deep in the technical weeds I thought
it might be a good idea to explain why Zetawar exists.

The story begins earlier this year when I wanted to play a round of Weewar (an
online turn based strategy game) with my wife. I hadn't played it in several
years, but remembered it as a fun game and was looking for something turn based
that wouldn't require us both to be available to play at exactly the same time.
Unfortunately, I discovered EA had shut down Weewar. I also discovered another
game, Elite Command, that suffered a similar fate. And while I don't blame
either EA or the author of Elite Command for shutting them down — these things
aren't free to maintain — it did seem like a shame that their respective player
communities couldn't keep them running themselves.

That's when the idea for Zetawar was born. What if I made a similar web based
game that was open source (Zetawar isn't yet, but it will be) and didn't
require an application server? It would have some limitations due to the lack
of a server side communication channel, but it would be something that players
could invest in without worrying about a third party shutting it down.

So, I started writing it. Of course, once you start writing a game, it's
impossible to resist trying to implement your favorite features, so Zetawar has
a few extra goals beyond just being an open source, app server free,
Weewar-like game.

First, it will have a first class bot interface. In college I enjoyed competing
in programming competitions and I think programming AI bots that can play
against each other is one of the most fun ways to do that. I also believe
basing these contests on a fun game that students can enjoy playing with each
other outside the programming competition can increase their interest and
engagement.

Second, it's going to be as customizable as I can reasonably make it. Unit
stats, terrain effects, tilesets, and even some of the game rules will be
modifiable by players. This will allow players to help me with things like
balancing unit stats and should keep the game interesting for a long time. It
will also allow the game to serve as a laboratory of sorts for people
interested in designing similar games. Want to make your own tactical strategy
game? Prototype it first in Zetawar!

Third, Zetawar is currently and will continue to be written in ClojureScript.
ClojureScript is the most enjoyable language I've found for writing web
applications, and I want to both continue working with it and promote its
adoption. Hopefully Zetawar can serve as a fun example people can point to when
they talk about things written in ClojureScript and, once it's open sourced,
can be an interesting code base for other ClojureScript enthusiasts to learn
from and contribute to.

So that's what I'm hoping to accomplish with Zetawar. If any of those goals
interest you, please follow [@ZetawarGame](https://twitter.com/ZetawarGame) to
keep updated on how things are progressing, and if you have any feedback either
send me a tweet or fill out the [feedback
form](http://goo.gl/forms/RgTpkCYDBk).

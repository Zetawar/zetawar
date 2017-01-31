---
name: Writing Bots
---

Zetawar was created in part to provide a fun, easy way to experiment with AI in
Clojure(Script). To that end, it provides a pluggable AI interface for writing
bots that can play against humans and other bots.

To understand the bot interface, it's helpful to understand its design goals:

1. Make it easy for anyone regardless of programming experience to create a
working bot.

2. Given a working bot, it should be easy to improve it to make it more capable
and intelligence.

3. Supporting advanced AI algorithms is secondary to the first two goals.

With these goals in mind, let's see what it takes to make an AI bot. To do so,
we need to write 3 functions.

The first function, 'score-actor', takes 4 arguments, 'db', 'game', 'actor-ctx',
and 'actor'. 'db' is a DataScript DB containing the current game state. 'game'
is a DataScript entity representing the current game. 'actor-ctx' we'll ignore
for now, and 'actor' is a DataScript entity representing either a base or unit.
So, what does this function do? Given these arguments, it returns a number
representing the score for the given base or unit. The score describes how
desirable it is to perform an as yet undetermined action with the given base or
unit. Higher numbers indicate higher desirability, lower numbers, lower
desirability. The AI interface uses these scores to determine which unit or base
will perform the next action.

The other two functions, 'base-score' and 'unit-score' are similar and handle
picking an action to take with the selected base or unit. Each takes 5
arguments, 'db', 'game', 'base'/'unit', 'action-ctx', and 'action'. The 'db',
and 'game' arguments are the same as those passed to 'actor-score'. 'action-ctx'
is similar to 'actor-ctx'. We'll ignore it too for the moment. The 'base'/'unit'
argument represents the base (passed to 'base-score') or unit (passed to
'unit-score') DataScript entity chosen by the AI interface based on the output
of 'actor-score'. 'action' is a map representing an action that can be taken by
the unit or base. Given those arguments the function returns a score
representing the desirability of the action passed to it. Again, higher numbers
represent higher desirability and lower numbers represent lower desirability.
Similar to with 'chose-score', the AI interface uses these scores to pick an
action for the given unit or base.

That's it. Those 3 functions are all you need to make a working bot. The
simplest possible AI you could create would return random numbers or constants.
It would choose random bases and units and execute random actions. Of course, it
would be very stupid, but it would work and provide a starting place for
building something more intelligent.

So, what's the deal with those '\*-ctx' arguments? Well, I kind of lied earlier
when I said there are only 3 functions to implement. There are actually 6, but
you only need to implement 3 to get something working. The other 3 functions
handle creating the '\*-ctx' arguments.

The first function, 'mk-actor-ctx' takes 2 arguments, 'db' and 'game' and
returns a context object. The context can contain any data you think might be
useful in your score functions. The primary purpose of this function is
optimization. Obviously since it takes subset of 'score-actor's arguments, the
same logic could be executed in that function. However, the actor score function
is called once per actor when making a decision about which actor to chose,
where as the context function is called only once per actor selection decision.
So, if you have costly calculations that would be the same for all actors, put
them in 'mk-actor-ctx'.

The second and third functions, 'mk-unit-action-ctx' and 'mk-unit-action-ctx'
each take 4 arguments 'db', 'game', 'base'/'unit', and 'actor-ctx' and return a
context object. They're analogous to the actor context function, but for
actions. They're called once after the actor has been chosen, but before the
actions are enumerated. Any calculation that would be different per actor, but
the same for all actions for that actor can be calculated in them. Anything that
you want to retain from the actor context should be merged into the returned
context.

Now that you have a basic understanding of the AI interface, you can start
implementing your own AI. The easiest way to do this is by customizing the
placeholder 'Custom AI' included with Zetawar. You can find it
in
[src/cljs/zetawar/players/ai/custom.cljs](https://github.com/Zetawar/zetawar/blob/481bfa3e789683b8216c0495babcd2e32aa8e86a/src/cljs/zetawar/players/ai/custom.cljs).
To get started playing with it, clone
the
[code](https://github.com/Zetawar/zetawar),
[install boot](https://github.com/boot-clj/boot#install), and run 'boot dev'.
You can now connect to 'http://localhost:3000' to interact with a version of
Zetawar running using your local code base. Any changes to the code will be
immediately (within a few seconds) reflected in the browser. If you want to test
out your AI in the user interface, click on the "Configure faction" icon next to
the faction you want your AI to play as. Then select "Custom AI" in the "Player
type" drop down. Congratulations, you're now playing against your own custom AI!

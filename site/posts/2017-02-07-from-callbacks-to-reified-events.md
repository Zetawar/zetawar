---
description: From Callbacks to Reified Events
date-published: 2017-02-07
---

Hello again! It's time to dust off this blog and get some posts out.

A lot has changed in Zetawar over the past few months. I've discussed some of
those changes in
Kickstarter
[updates](https://www.kickstarter.com/projects/djwhitt/zetawar/updates), but
only at a high level. Here I'm going to dig into them in a bit more detail.

First, let's take a look at the event system.

If you recall, the demo version of Zetawar used simple callback based event
handlers. With that approach, calling a event handler looked like this:

```clojure
[:button {:on-click #(handlers/repair conn %)}
 "Repair"]])
```

And the event handlers looked like this:

```clojure
(defn repair [conn ev]
  (let [db @conn
         [q r] (first (d/q '[:find ?q ?r
                            :where
                            [?a :app/selected-q ?q]
                            [?a :app/selected-r ?r]]
                          db))]
     (game/repair! conn (app/current-game-id db) q r)
     (clear-selection conn nil)))
```

The important things to notice here are that the handler takes a reference,
'conn', and performs side effects. This approach has the virtue of being easy to
implement, hence it's use in the demo, but it also has several drawbacks.
Performing actions via side effects in the call to 'repair!' makes testing and
interactive development difficult. Passing in the connection is also problematic
since it means theoretically the value of the connection can change during the
execution of the function. Though, in practice, JavaScript's single threaded
execution model prevents this.

So how can we improve things? Thankfully,
the [re-frame framework](https://github.com/Day8/re-frame) has
already
[solved this problem](https://github.com/Day8/re-frame/blob/master/docs/EffectfulHandlers.md).
We can just follow its lead. Instead of calling handlers directly, we'll
dispatch event messages and modify the handlers to accept a DB value rather than
a connection reference. Also, instead of performing side effects directly, we'll
return a data structure describing the side effects we want executed.

With the new approach, dispatching an event looks like this:

```clojure
[:button {:on-click #(dispatch [::events.ui/repair-selected])}
 "Repair"])
```

And handling an event looks like this:

```clojure
(defmethod router/handle-event ::repair-selected
  [{:as handler-ctx :keys [db]} _]
  (let [game (app/current-game db)
        cur-faction-color (game/current-faction-color game)
        [q r] (app/selected-hex db)]
    {:dispatch [[:zetawar.events.game/execute-action
                 {:action/type :action.type/repair-unit
                  :action/faction-color cur-faction-color
                  :action/q q
                  :action/r r}]
                [::clear-selection]]}))
                
(defmethod router/handle-event ::execute-action
  [{:as handler-ctx :keys [db]} [_ action]]
  (let [game (app/current-game db)]
      ;; Irrelevant code omitted ...
      {:tx     (game/action-tx db game action)
       :notify [[:zetawar.players/apply-action :faction.color/all action]]})))
      
```

Because of changes to the AI system, the new code splits the event handler in
two. The first handler extracts information about the current faction and
selection from the DB and turns it into an 'execute-action' event which it
returns along with a 'clear-selection' event. For the sake of brevity, most of
the code has been omitted from the second handler, but the essentials are still
there. It takes a DB value and an 'action' map and returns a transaction and an
AI notification. Taken together the returned data describes the actions
performed as side effects in the original handler.

So, now our handlers are pure functions. They take values as arguments and
return values. This makes them much easier to execute interactively and test,
but there's still something missing. How do the returned values change the
application state? In the next post we'll examine this question in detail as we
look at the event router.

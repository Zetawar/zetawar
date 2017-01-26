ZetawarAI = (function() {
    var ZetawarAI = {};

    ZetawarAI.makeActorContext = function(db, game, actor) {
        return {};
    };

    ZetawarAI.scoreActor = function(db, game, actor, actor_ctx) {
        if (zetawar.js.game.is_base(actor)) {
            return 100 + Math.floor(Math.random() * 100);
        }
        return Math.floor(Math.random() * 100);
    };

    ZetawarAI.makeBaseActionContext = function(db, game, actor_ctx, base) {
        return actor_ctx;
    };

    ZetawarAI.scoreBaseAction = function(db, game, base, actor_ctx, action) {
        return Math.floor(Math.random() * 200);
    };

    ZetawarAI.makeUnitActionContext = function(db, game, actor_ctx, unit) {
        actor_ctx['actor_ctx'] = zetawar.js.game.closest_capturable_base(db, game, unit);
        return actor_ctx;
    };

    ZetawarAI.scoreUnitAction = function(db, game, unit, action_ctx, action) {
        var closest_base = action_ctx.closest_base

        switch (action["type"]) {
        case "capture-base":
            return 200;
        case "attack-unit":
            return 100;
        case "move-unit":
            [baseQ, baseR] = zetawar.js.game.terrain_hex(closest_base);
            toQ = action["to-q"];
            toR = action["to-r"];
            distanceFromBase = zetawar.js.hex.distance(baseQ, baseR, toQ, toR);

            return 100 - distanceFromBase;
        }

        return 0;
    };

    return ZetawarAI;
})();

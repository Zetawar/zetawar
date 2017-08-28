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
        actor_ctx['closest_base'] = zetawar.js.game.closest_capturable_base(db, game, unit);
        actor_ctx['closest_enemy'] = zetawar.js.game.closest_enemy(db, game, unit);
        return actor_ctx;
    };

    ZetawarAI.scoreUnitAction = function(db, game, unit, action_ctx, action) {
        var closestBase = action_ctx.closest_base;
        var closestEnemy = action_ctx.closest_enemy;
        var ret = 0;

        switch (action["type"]) {
        case "capture-base":
            return 200;
        case "attack-unit":
            return 100;
        case "move-unit":
            if (closestBase) {
                [baseQ, baseR] = zetawar.js.game.terrain_hex(closestBase);
                toQ = action["to-q"];
                toR = action["to-r"];
                distanceFromBase = zetawar.js.hex.distance(baseQ, baseR, toQ, toR);
                ret = 100  - distanceFromBase;
            } else {
                [enemyQ, enemyR] = zetawar.js.game.unit_hex(closestEnemy);
                toQ = action["to-q"];
                toR = action["to-r"];
                distanceFromEnemy = zetawar.js.hex.distance(enemyQ, enemyR, toQ, toR);
                ret = 100  - distanceFromEnemy;
            }
        }

        return ret;
    };

    return ZetawarAI;
})();

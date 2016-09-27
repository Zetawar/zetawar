(ns zetawar.views
  (:require
   [cljsjs.clipboard]
   [cljsjs.react-bootstrap]
   [clojure.string :as string]
   [datascript.core :as d]
   [posh.core :as posh]
   [reagent.core :as r]
   [zetawar.ai :as ai]
   [zetawar.db :refer [e qe]]
   [zetawar.events.ui :as events.ui]
   [zetawar.game :as game]
   [zetawar.hex :as hex]
   [zetawar.router :as router]
   [zetawar.subs :as subs]
   [zetawar.util :refer [only oonly spy]]
   [zetawar.views.common :refer [footer kickstarter-alert navbar]]))

;; Game           => Factions
;; Factions       => Units
;; Unit           => Unit Type
;; Game           => Map
;; Map            => Terrains
;; Terrain        => Terrain Type
;; Unit + Terrain => Effect

;; Game
;; - round
;; - factions

;; Faction
;; - color
;; - order
;; - current
;; - units
;; - bases

;; Unit
;; - type
;; - quantity
;; - state [ready, first-move-done, attack-done, repair-done, capturing]
;; - move-count
;; - attacked-units
;; - attack-count
;; - capturing - boolean
;; - repaired - boolean
;; - newly-built - boolean
;; - capture-round
;; - actions - actions taken this turn ?

;; Action
;; - type - move, attack, repair, capture
;; - number (index, order, ?)
;; - x
;; - y

;; Alternate unit state ideas: ready, moved, attacking, capturing, repairing

;; Map
;; - terrains
;; - factions

;; TODO: decide whether faction/terrains or terrain/owner is better
;; TODO: make faction/color a string instead of a symbol
;; TODO: add unit -> terrain ref (?)

;; new-game-tx function
;; - set round
;; - set map
;; - add factions (defined by map)

;; Turn:
;; - set current faction
;; - build
;; - unit actions - move, attack, repair, capture
;; - set faction done

;; Faction actions:
;; - build (faction-eid, x, y, unit-type-id)
;; - end-turn (faction-eid)

;; Unit actions:
;; - move (faction-eid, cx, cy, nx, ny)
;; - attack (faction-eid, ax, ay, dx, dy)
;; - repair (faction-eid, x, y)
;; - capture (faction-eid, x, y)

(defn tile-border [{:keys [conn] :as app} q r]
  (let [[x y] (hex/offset->pixel q r)]
    [:g {:id (str "border-" q "," r)}
     (cond
       ;; Selected
       @(subs/selected? conn q r)
       [:image {:x x :y y
                :width 32 :height 34
                :xlink-href "/images/game/borders/selected.png"}]

       ;; Enemy unit targeted
       (and @(subs/targeted? conn q r)
            @(subs/enemy-at? conn q r))
       [:image {:x x :y y
                :width 32 :height 34
                :xlink-href "/images/game/borders/targeted-enemy.png"}]

       ;; Terrain targeted
       @(subs/targeted? conn q r)
       [:image {:x x :y y
                :width 32 :height 34
                :xlink-href "/images/game/borders/selected.png"}])]))

(defn board-unit [{:keys [conn ev-chan] :as app} q r]
  (when-let [unit @(subs/unit-at conn q r)]
    (let [[x y] (hex/offset->pixel q r)
          color (-> unit
                    (get-in [:faction/_units :faction/color])
                    name)
          image (-> unit
                    (get-in [:unit/type :unit-type/image])
                    (string/replace "COLOR" color))]
      [:g {:id (str "unit-" (e unit))}
       [:image {:x x :y y
                :width 32 :height 34
                :xlink-href (str "/images/game/" image)
                :on-click #(router/dispatch ev-chan [::events.ui/select-hex q r])}]
       (when (:unit/capturing unit)
         [:image {:x x :y y
                  :width 32 :height 34
                  :xlink-href (str "/images/game/capturing.gif")}])
       [:image {:x x :y y
                :width 32 :height 34
                :xlink-href (str "/images/game/health/" (:unit/count unit) ".png")}]])))

(defn tile-mask [{:keys [conn] :as app} q r]
  (let [[x y] (hex/offset->pixel q r)]
    (when (or
           ;; No unit selected and tile contains current unit with no actions
           (and (not @(subs/unit-selected? conn))
                @(subs/current-unit-at? conn q r)
                (not @(subs/unit-can-act? conn q r)))

           ;; Unit selected and tile is a valid attack or move target
           (and @(subs/unit-selected? conn)
                (not @(subs/selected? conn q r))
                (not @(subs/enemy-in-range-of-selected? conn q r))
                (not @(subs/valid-destination-for-selected? conn q r))))
      [:image {:x x :y y
               :width 32 :height 34
               :xlink-href "/images/game/mask.png"}])))

(defn terrain-tile [{:keys [conn] :as app} terrain q r]
  (let [[x y] (hex/offset->pixel q r)
        color (-> terrain
                  (get-in [:terrain/owner :faction/color])
                  (or :none)
                  name)
        image (-> terrain
                  (get-in [:terrain/type :terrain-type/image])
                  (string/replace "COLOR" color))]
    [:image {:x x :y y
             :width 32 :height 34
             :xlink-href (str "/images/game/" image)}]))

(defn tile [{:keys [conn ev-chan] :as app} terrain]
  (let [{:keys [terrain/q terrain/r]} terrain]
    ^{:key (str q "," r)}
    [:g {:on-click #(router/dispatch ev-chan [::events.ui/select-hex q r])}
     [terrain-tile app terrain q r]
     [tile-border app q r]
     [board-unit app q r]
     [tile-mask app q r]]))

(defn tiles [{:keys [conn] :as app}]
  (into [:g]
        (for [terrain @(subs/terrains conn)]
          [tile app terrain])))

(defn board [app]
  [:svg#board {:width 400 :height 300}
   [tiles app]])

(defn faction-credits [{:keys [conn] :as app}]
  (let [{:keys [faction/credits]} @(subs/current-faction conn)
        {:keys [map/credits-per-base]} @(subs/game-map conn)
        income @(subs/current-income conn)]
    [:p#faction-credits
     [:strong (str credits " Credits")]
     [:span.text-muted.pull-right (str "+" income)
      [:span.hidden-md "/turn"]]]))

(defn end-turn-link [{:keys [conn ev-chan] :as app}]
  (let [clipboard (atom nil)
        text-fn (fn []
                  (router/dispatch ev-chan [::events.ui/end-turn])
                  js/window.location)]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (reset! clipboard (js/Clipboard. (r/dom-node this) #js {"text" text-fn})))
      :component-will-unmount
      (fn [this]
        (.destroy @clipboard)
        (reset! clipboard nil))
      :reagent-render
      (fn [this]
        [:a {:href "#" :on-click #(.preventDefault %)}
         "End Turn?"])})))

(defn faction-status [{:keys [conn ev-chan] :as app}]
  (let [{:keys [game/round]} @(subs/game conn)
        base-count @(subs/current-base-count conn)]
    [:div#faction-status
     [end-turn-link app]
     [:div.pull-right
      [:a {:href "#"
           :on-click (fn [e]
                       (.preventDefault e)
                       (router/dispatch ev-chan [::events.ui/new-game]))}
       "New Game"]
      " · "
      (str "Round " round)]]))

(defn faction-actions [{:keys [conn ev-chan] :as app}]
  ;; TODO: replace query with something from subs ns
  (let [[round current-color] (-> @(posh/q conn '[:find ?round ?current-color
                                                  :where
                                                  [?g :game/round ?round]
                                                  [?g :game/current-faction ?f]
                                                  [?f :faction/color ?current-color]])
                                  first)
        {:keys [faction/credits]} @(subs/current-faction conn)]
    [:div#faction-actions
     (when @(subs/selected-can-move-to-targeted? conn)
       [:p
        [:button.btn.btn-primary.btn-block
         {:on-click #(router/dispatch ev-chan [::events.ui/move-selected-unit])}
         "Move"]])
     (when @(subs/selected-can-build? conn)
       [:p
        [:button.btn.btn-primary.btn-block
         {:on-click #(router/dispatch ev-chan [::events.ui/build-unit])}
         "Build"]])
     (when @(subs/selected-can-attack-targeted? conn)
       [:p
        [:button.btn.btn-danger.btn-block
         {:on-click #(router/dispatch ev-chan [::events.ui/attack-targeted])}
         "Attack"]])
     (when @(subs/selected-can-repair? conn)
       [:p
        [:button.btn.btn-success.btn-block
         {:on-click #(router/dispatch ev-chan [::events.ui/repair-selected])}
         "Repair"]])
     (when @(subs/selected-can-capture? conn)
       [:p
        [:button.btn.btn-primary.btn-block
         {:on-click #(router/dispatch ev-chan [::events.ui/capture-selected])}
         "Capture"]])
     ;; TODO: cleanup conditionals
     ;; TODO: make help text a separate component
     (when (not (or @(subs/selected-can-move? conn)
                    @(subs/selected-can-build? conn)
                    @(subs/selected-can-attack? conn)
                    @(subs/selected-can-repair? conn)
                    @(subs/selected-can-capture? conn)))
       [:p.hidden-xs.hidden-sm "Select a unit or base."])
     (when (and
            (or @(subs/selected-can-move? conn)
                @(subs/selected-can-attack? conn))
            (not
             (or @(subs/selected-can-move-to-targeted? conn)
                 @(subs/selected-can-attack-targeted? conn))))
       [:p.hidden-xs.hidden-sm
        "Select a destination or target to move or attack."])
     ;; TODO: only display when starting faction is active
     (when (= round 1)
       [:p.hidden-xs.hidden-sm
        "To play multiplayer follow the instructions "
        [:a {:href "https://www.kickstarter.com/projects/311016908/zetawar/posts/1608417"} "here"]
        "."])]))

(defn faction-list [{:keys [conn ev-chan] :as app}]
  (into [:ul.list-group]
        (for [faction @(subs/factions conn)]
          (let [faction-eid (e faction)
                color (-> faction
                          :faction/color
                          name
                          string/capitalize)
                active (= faction-eid @(subs/current-faction-eid conn))
                li-class (if active
                           "list-group-item active"
                           "list-group-item")]
            [:li {:class li-class}
             color
             " "
             (when active
               [:span.fa.fa-angle-double-left
                {:aria-hidden true}])
             [:div.pull-right
              (if (:faction/ai faction)
                [:span.fa.fa-fw.fa-laptop.clickable
                 {:aria-hidden true
                  :on-click #(router/dispatch ev-chan [::events.ui/toggle-faction-ai faction])
                  :title "Disable AI"}]
                [:span.fa.fa-fw.fa-user.clickable
                 {:aria-hidden true
                  :on-click #(router/dispatch ev-chan [::events.ui/toggle-faction-ai faction])
                  :title "Enable AI"}])]]))))

;; TODO: turn entire game interface into it's own component

(defn app-root [{:keys [conn ev-chan] :as app}]
  [:div
   [:> js/ReactBootstrap.Modal {:show @(subs/show-win-dialog? conn)
                                :on-hide #(router/dispatch ev-chan [::events.ui/hide-win-dialog])}
    [:> js/ReactBootstrap.Modal.Header
     [:> js/ReactBootstrap.Modal.Title
      "Congratulations! You won!"]]
    [:> js/ReactBootstrap.Modal.Body
     "Thanks for playing Zetawar! If you're interested in staying up-to-date"
     " with Zetawar as it develops, please follow "
     [:a {:href "https://twitter.com/ZetawarGame"}
      "@ZetawarGame"]
     " on Twitter."]
    [:> js/ReactBootstrap.Modal.Footer
     [:button.btn.btn-default {:on-click #(router/dispatch ev-chan [::events.ui/hide-win-dialog])}
      "Close"]]]
   (navbar "Game")
   [:div.container
    (kickstarter-alert)
    [:div.row
     [:div.col-md-2
      [faction-credits app]
      [faction-list app]
      [faction-actions app]]
     [:div.col-md-10
      [faction-status app]
      [board app]]]]
   (footer)])

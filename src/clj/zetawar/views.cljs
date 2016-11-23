(ns zetawar.views
  (:require
   [cljsjs.clipboard]
   [cljsjs.react-bootstrap]
   [clojure.string :as string]
   [datascript.core :as d]
   [posh.reagent :as posh]
   [reagent.core :as r :refer [with-let]]
   [taoensso.timbre :as log]
   [zetawar.data :as data]
   [zetawar.db :refer [e qe]]
   [zetawar.events.ui :as events.ui]
   [zetawar.game :as game]
   [zetawar.hex :as hex]
   [zetawar.players :as players]
   [zetawar.router :as router]
   [zetawar.subs :as subs]
   [zetawar.util :refer [breakpoint inspect only oonly]]
   [zetawar.views.common :refer [footer kickstarter-alert navbar]]))

;; - terrains
;; - factions

;; TODO: decide whether faction/terrains or terrain/owner is better
;; TODO: make faction/color a string instead of a symbol

;; new-game-tx function
;; - set round
;; - set map
;; - add factions (defined by map)

;; Faction actions:
;; - build (faction-eid, x, y, unit-type-id)
;; - end-turn (faction-eid)

;; Unit actions:
;; - move (faction-eid, cx, cy, nx, ny)
;; - attack (faction-eid, ax, ay, dx, dy)
;; - repair (faction-eid, x, y)
;; - capture (faction-eid, x, y)

(def offset->pixel
  (memoize
   (fn [q r]
     (if (= 0 (mod r 2))
       [(* q 32) (* r 26)]
       [(+ 16 (* q 32)) (+ 26 (* (- r 1) 26))]))))

(defn tile-border [{:keys [conn] :as app} q r]
  (let [[x y] (offset->pixel q r)]
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

(defn unit-image [unit]
  (let [color-name (-> unit game/unit-color name)]
    (-> unit
        (get-in [:unit/type :unit-type/image])
        (string/replace "COLOR" color-name))))

(defn board-unit [{:keys [conn dispatch] :as app} q r]
  (when-let [unit @(subs/unit-at conn q r)]
    (let [[x y] (offset->pixel q r)
          image (unit-image unit)]
      [:g {:id (str "unit-" (e unit))}
       [:image {:x x :y y
                :width 32 :height 34
                :xlink-href (str "/images/game/" image)
                :on-click #(dispatch [::events.ui/select-hex q r])}]
       (when (:unit/capturing unit)
         [:image {:x x :y y
                  :width 32 :height 34
                  :xlink-href (str "/images/game/capturing.gif")}])
       [:image {:x x :y y
                :width 32 :height 34
                :xlink-href (str "/images/game/health/" (:unit/count unit) ".png")}]])))

(defn tile-mask [{:keys [conn] :as app} q r]
  (let [[x y] (offset->pixel q r)
        show (or
              ;; No unit selected and tile contains current unit with no actions
              (and (not @(subs/unit-selected? conn))
                   @(subs/current-unit-at? conn q r)
                   (not @(subs/unit-can-act? conn q r)))

              ;; Unit selected and tile is a valid attack or move target
              (and @(subs/unit-selected? conn)
                   (not @(subs/selected? conn q r))
                   (not @(subs/enemy-in-range-of-selected? conn q r))
                   (not @(subs/valid-destination-for-selected? conn q r))))]
    [:image {:visibility (if show "visible" "hidden")
             :x x :y y
             :width 32 :height 34
             :xlink-href "/images/game/mask.png"}]))

(defn terrain-image [terrain]
  (let [color-name (-> terrain
                       (get-in [:terrain/owner :faction/color])
                       (or :none)
                       name)]
    (-> terrain
        (get-in [:terrain/type :terrain-type/image])
        (string/replace "COLOR" color-name))))

(defn terrain-tile [{:keys [conn] :as app} terrain q r]
  (let [[x y] (offset->pixel q r)
        image (terrain-image terrain)]
    [:image {:x x :y y
             :width 32 :height 34
             :xlink-href (str "/images/game/" image)}]))

(defn tile [{:keys [conn dispatch] :as app} terrain]
  (let [{:keys [terrain/q terrain/r]} terrain]
    ^{:key (str q "," r)}
    [:g {:on-click #(dispatch [::events.ui/select-hex q r])}
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

(defn copy-url-link [{:keys [conn dispatch] :as app}]
  (let [clipboard (atom nil)
        text-fn (fn [] js/window.location)]
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
         "Copy Link"])})))

(defn faction-status [{:keys [conn dispatch] :as app}]
  (let [{:keys [game/round]} @(subs/game conn)
        base-count @(subs/current-base-count conn)]
    [:div#faction-status
     [:a {:href "#" :on-click #(dispatch [::events.ui/end-turn])}
      "End Turn?"]
     " · "
     [copy-url-link app]
     [:div.pull-right
      [:a {:href "#"
           :on-click (fn [e]
                       (.preventDefault e)
                       (dispatch [::events.ui/show-new-game-settings]))}
       "New Game"]
      " · "
      (str "Round " round)]]))

(defn faction-actions [{:keys [conn dispatch] :as app}]
  ;; TODO: replace query with something from subs ns
  (let [[round current-color] (-> @(posh/q '[:find ?round ?current-color
                                             :where
                                             [?g :game/round ?round]
                                             [?g :game/current-faction ?f]
                                             [?f :faction/color ?current-color]]
                                           conn)
                                  first)
        {:keys [faction/credits]} @(subs/current-faction conn)]
    [:div#faction-actions
     (when @(subs/selected-can-move-to-targeted? conn)
       [:p
        [:button.btn.btn-primary.btn-block
         {:on-click #(dispatch [::events.ui/move-selected-unit])}
         "Move"]])
     (when @(subs/selected-can-build? conn)
       [:p
        [:button.btn.btn-primary.btn-block
         {:on-click #(dispatch [::events.ui/show-unit-picker])}
         "Build"]])
     (when @(subs/selected-can-attack-targeted? conn)
       [:p
        [:button.btn.btn-danger.btn-block
         {:on-click #(dispatch [::events.ui/attack-targeted])}
         "Attack"]])
     (when @(subs/selected-can-repair? conn)
       [:p
        [:button.btn.btn-success.btn-block
         {:on-click #(dispatch [::events.ui/repair-selected])}
         "Repair"]])
     (when @(subs/selected-can-capture? conn)
       [:p
        [:button.btn.btn-primary.btn-block
         {:on-click #(dispatch [::events.ui/capture-selected])}
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

(defn faction-list [{:keys [conn dispatch] :as app}]
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
                  :on-click #(dispatch [::events.ui/configure-faction faction])
                  :title "Disable AI"}]
                [:span.fa.fa-fw.fa-user.clickable
                 {:aria-hidden true
                  :on-click #(dispatch [::events.ui/configure-faction faction])
                  :title "Enable AI"}])]]))))

;; TODO: cleanup unit-picker
(defn unit-picker [{:keys [conn dispatch] :as app}]
  (let [unit-types @(subs/available-unit-types conn)
        cur-faction @(subs/current-faction conn)
        color (name (:faction/color cur-faction))
        hide #(dispatch [::events.ui/hide-unit-picker])]
    [:> js/ReactBootstrap.Modal {:show @(subs/show-unit-picker? conn)
                                 :on-hide hide}
     [:> js/ReactBootstrap.Modal.Header {:close-button true}
      [:> js/ReactBootstrap.Modal.Title
       "Select a unit to build"]]
     [:> js/ReactBootstrap.Modal.Body
      (into [:div.unit-picker]
            (for [{:keys [unit-type/id] :as unit-type} unit-types]
              (let [image (->> (string/replace (:unit-type/image unit-type)
                                               "COLOR" color)
                               (str "/images/game/"))
                    media-class (if (:affordable unit-type)
                                  "media clickable"
                                  "media clickable text-muted")
                    build-unit #(do
                                  (dispatch [::events.ui/hide-unit-picker])
                                  (dispatch [::events.ui/build-unit id]))]
                [:div {:class media-class
                       :on-click build-unit}
                 [:div.media-left.media-middle
                  [:img {:src image}]]
                 [:div.media-body
                  [:h4.media-heading
                   (:unit-type/name unit-type)]
                  (str "Cost: " (:unit-type/cost unit-type))]])))]
     [:> js/ReactBootstrap.Modal.Footer
      [:button.btn.btn-default {:on-click hide}
       "Cancel"]]]))

(defn faction-settings [{:keys [conn dispatch] :as app}]
  (with-let [faction (subs/faction-to-configure conn)
             selected-player-type (r/atom nil)
             hide #(do
                     (.preventDefault %)
                     (dispatch [::events.ui/hide-faction-settings]))
             select-player-type #(reset! selected-player-type (.-target.value %))
             set-player-type #(do
                                (.preventDefault %)
                                (when-let [player-type-id (->> (or @selected-player-type :human)
                                                               (keyword 'zetawar.players))]
                                  (reset! selected-player-type nil)
                                  (dispatch [::events.ui/set-faction-player-type @faction player-type-id]))

                                (dispatch [::events.ui/hide-faction-settings]))]
    [:> js/ReactBootstrap.Modal {:show (some? @faction)
                                 :on-hide hide}
     [:> js/ReactBootstrap.Modal.Header {:close-button true}
      [:> js/ReactBootstrap.Modal.Title
       "Configure faction"]]
     [:> js/ReactBootstrap.Modal.Body
      [:form
       [:div.form-group
        [:label {:for "player-type"}
         "Player type"]
        (into [:select.form-control {:id "player-type"
                                     :selected (or @selected-player-type
                                                   (:faction/player-type @faction))
                                     :on-change select-player-type}]
              (for [[player-type-id {:keys [description ai]}] players/player-types]
                [:option {:value (name player-type-id)}
                 description]))
        [:> js/ReactBootstrap.Modal.Footer
         [:button.btn.btn-primary {:on-click set-player-type}
          "Save"]
         [:button.btn.btn-default {:on-click hide}
          "Cancel"]]]]]]))

(defn new-game-settings [{:keys [conn dispatch] :as app}]
  (with-let [selected-scenario-id (r/atom :sterlings-aruba-multiplayer)
             hide #(do
                     (.preventDefault %)
                     (dispatch [::events.ui/hide-new-game-settings]))
             select-scenario #(reset! selected-scenario-id (keyword (.-target.value %)))
             start #(do
                      (.preventDefault %)
                      (dispatch [::events.ui/new-game @selected-scenario-id])
                      (reset! selected-scenario-id nil)
                      (dispatch [::events.ui/hide-new-game-settings]))]
    [:> js/ReactBootstrap.Modal {:show @(subs/show-new-game-settings? conn)
                                 :on-hide hide}
     [:> js/ReactBootstrap.Modal.Header {:close-button true}
      [:> js/ReactBootstrap.Modal.Title
       "Start a new game"]]
     [:> js/ReactBootstrap.Modal.Body
      [:form
       [:div.form-group
        [:label {:for "scenario-id"}
         "Scenario"]
        (into [:select.form-control {:id "scenario-id"
                                     :selected (some-> @selected-scenario-id name)}]
              (for [[scenario-id {:keys [description]}] data/scenario-definitions]
                [:option {:value (name scenario-id)}
                 description]))
        [:> js/ReactBootstrap.Modal.Footer
         [:button.btn.btn-primary {:on-click start}
          "Start"]
         [:button.btn.btn-default {:on-click hide}
          "Cancel"]]]]]]))

;; TODO: turn entire game interface into it's own component

(defn app-root [{:keys [conn dispatch] :as app}]
  [:div
   [new-game-settings app]
   [faction-settings app]
   [unit-picker app]
   ;; TODO: break win dialog out into it's own component
   ;; TODO: add continue + start new game buttons
   [:> js/ReactBootstrap.Modal {:show @(subs/show-win-dialog? conn)
                                :on-hide #(dispatch [::events.ui/hide-win-dialog])}
    [:> js/ReactBootstrap.Modal.Header
     [:> js/ReactBootstrap.Modal.Title
      "Congratulations! You won!"]]
    [:> js/ReactBootstrap.Modal.Body
     "Thanks for playing Zetawar! If you're interested in staying up-to-date"
     " with Zetawar as it develops, please follow "
     [:a {:href "https://twitter.com/ZetawarGame"}
      ]
     " on Twitter."]
    [:> js/ReactBootstrap.Modal.Footer
     [:button.btn.btn-default {:on-click #(dispatch [::events.ui/hide-win-dialog])}
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

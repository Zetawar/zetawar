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
   [zetawar.players :as players]
   [zetawar.site :as site]
   [zetawar.subs :as subs]
   [zetawar.tiles :as tiles]
   [zetawar.util :refer [breakpoint inspect only oonly]]
   [zetawar.views.common :refer [footer navbar]]))

(defn tile-border [{:as view-ctx :keys [conn]} q r]
  (let [[x y] (tiles/offset->pixel q r)]
    [:g {:id (str "border-" q "," r)}
     (cond
       ;; Selected
       @(subs/selected? conn q r)
       [:image {:x x :y y
                :width tiles/width :height tiles/height
                :xlink-href (site/prefix "/images/game/borders/selected.png")}]

       ;; Enemy unit targeted
       (and @(subs/targeted? conn q r)
            @(subs/enemy-at? conn q r))
       [:image {:x x :y y
                :width tiles/width :height tiles/height
                :xlink-href (site/prefix "/images/game/borders/targeted-enemy.png")}]

       ;; Friend unit targeted (for repair)
       (and @(subs/targeted? conn q r)
            @(subs/friend-at? conn q r))
       [:image {:x x :y y
                :width tiles/width :height tiles/height
                :xlink-href (site/prefix "/images/game/borders/targeted-friend.png")}]

       ;; Terrain targeted
       @(subs/targeted? conn q r)
       [:image {:x x :y y
                :width tiles/width :height tiles/height
                :xlink-href (site/prefix "/images/game/borders/selected.png")}])]))

(defn unit-image [unit]
  (let [color-name (-> unit game/unit-color name)]
    ;; TODO: return placeholder if terrain image is not found
    (some-> unit
            (get-in [:unit/type :unit-type/image])
            (string/replace "COLOR" color-name))))

(defn board-unit [{:as view-ctx :keys [conn dispatch]} q r]
  (when-let [unit @(subs/unit-at conn q r)]
    (let [[x y] (tiles/offset->pixel q r)
          image (unit-image unit)]
      [:g {:id (str "unit-" (e unit))}
       [:image {:x x :y y
                :width tiles/width :height tiles/height
                :xlink-href (site/prefix "/images/game/" image)
                :on-click #(dispatch [::events.ui/select-hex q r])}]
       (when (:unit/capturing unit)
         [:image {:x x :y y
                  :width tiles/width :height tiles/height
                  :xlink-href (site/prefix "/images/game/capturing.gif")}])
       [:image {:x x :y y
                :width tiles/width :height tiles/height
                :xlink-href (site/prefix "/images/game/health/" (:unit/count unit) ".png")}]])))

(defn tile-mask [{:as view-ctx :keys [conn]} q r]
  (let [[x y] (tiles/offset->pixel q r)
        show (or
              ;; No unit selected and tile contains current unit with no actions
              (and (not @(subs/unit-selected? conn))
                   @(subs/current-unit-at? conn q r)
                   (not @(subs/unit-can-act? conn q r)))

              ;; Unit selected and tile is a valid attack, repair, or move target
              (and @(subs/unit-selected? conn)
                   (not @(subs/selected? conn q r))
                   (not @(subs/enemy-in-range-of-selected? conn q r))
                   (not (and @(subs/repairable-friend-in-range-of-selected? conn q r)
                             @(subs/selected-can-repair-other? conn)
                             @(subs/compatible-armor-types-for-repair? conn q r)))
                   (not @(subs/valid-destination-for-selected? conn q r))))]
    [:image {:visibility (if show "visible" "hidden")
             :x x :y y
             :width tiles/width :height tiles/height
             :xlink-href (site/prefix "/images/game/mask.png")}]))

(defn terrain-image [terrain]
  (let [color-name (-> terrain
                       (get-in [:terrain/owner :faction/color])
                       (or :none)
                       name)]
    ;; TODO: return placeholder if terrain image is not found
    (some-> terrain
            (get-in [:terrain/type :terrain-type/image])
            (string/replace "COLOR" color-name))))

(defn terrain-tile [view-ctx terrain q r]
  (let [[x y] (tiles/offset->pixel q r)
        image (terrain-image terrain)]
    [:image {:x x :y y
             :width tiles/width :height tiles/height
             :xlink-href (site/prefix "/images/game/" image)}]))

(defn tile [{:as view-ctx :keys [dispatch]} terrain]
  (let [{:keys [terrain/q terrain/r]} terrain]
    ^{:key (str q "," r)}
    [:g {:on-click #(dispatch [::events.ui/select-hex q r])}
     [terrain-tile view-ctx terrain q r]
     [tile-border view-ctx q r]
     [board-unit view-ctx q r]
     [tile-mask view-ctx q r]]))

(defn tiles [{:as view-ctx :keys [conn]}]
  (into [:g]
        (for [terrain @(subs/terrains conn)]
          [tile view-ctx terrain])))

(defn board [{:as view-ctx :keys [conn]}]
  [:svg#board {:width @(subs/map-width-px conn)
               :height @(subs/map-height-px conn)}
   [tiles view-ctx]])

(defn faction-credits [{:as view-ctx :keys [conn translate]}]
  (let [{:keys [faction/credits]} @(subs/current-faction conn)
        {:keys [map/credits-per-base]} @(subs/game-map conn)
        income @(subs/current-income conn)]
    [:p#faction-credits
     [:strong (str credits " " (translate :credits-label))]
     [:span.text-muted.pull-right (str "+" income)
      [:span.hidden-md "/turn"]]]))

(defn copy-url-link [{:as view-ctx :keys [conn translate]}]
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
         (translate :copy-game-url-link)])})))

(defn faction-status [{:as view-ctx :keys [conn dispatch translate]}]
  (let [{:keys [app/show-copy-link]} @(subs/app conn)
        {:keys [game/round]} @(subs/game conn)
        base-count @(subs/current-base-count conn)]
    [:div#faction-status
     ;; TODO: make link red
     [:a {:href "#" :on-click (fn [e]
                                (.preventDefault e)
                                (dispatch [::events.ui/end-turn]))}
      (translate :end-turn-link)]
     (when show-copy-link
       [:span " · " [copy-url-link view-ctx]])
     [:div.pull-right
      [:a {:href "#"
           :on-click (fn [e]
                       (.preventDefault e)
                       (dispatch [::events.ui/show-new-game-settings]))}
       (translate :new-game-link)]
      " · "
      (str (translate :round-label) " " round)]]))

(defn faction-actions [{:as view-ctx :keys [conn dispatch translate]}]
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
         (translate :move-unit-button)]])
     (when @(subs/selected-can-build? conn)
       [:p
        [:button.btn.btn-primary.btn-block
         {:on-click #(dispatch [::events.ui/show-unit-picker])}
         (translate :build-unit-button)]])
     (when @(subs/selected-can-attack-targeted? conn)
       [:p
        [:button.btn.btn-danger.btn-block
         {:on-click #(dispatch [::events.ui/attack-targeted])}
         (translate :attack-unit-button)]])
     (when @(subs/selected-can-repair? conn)
       [:p
        [:button.btn.btn-success.btn-block
         {:on-click #(dispatch [::events.ui/repair-selected])}
         (translate :repair-unit-button)]])
     (when @(subs/selected-can-repair-targeted? conn)
       [:p
        [:button.btn.btn-success.btn-block
         {:on-click #(dispatch [::events.ui/repair-targeted])}
         (translate :repair-other-button)]])
     (when @(subs/selected-can-capture? conn)
       [:p
        [:button.btn.btn-primary.btn-block
         {:on-click #(dispatch [::events.ui/capture-selected])}
         (translate :capture-base-button)]])
     ;; TODO: cleanup conditionals
     ;; TODO: make help text a separate component
     (when (not (or @(subs/selected-can-move? conn)
                    @(subs/selected-can-build? conn)
                    @(subs/selected-can-attack? conn)
                    @(subs/selected-can-repair? conn)
                    @(subs/selected-can-capture? conn)))
       [:p.hidden-xs.hidden-sm
        (translate :select-unit-or-base-tip)])
     (when (and
            (or @(subs/selected-can-move? conn)
                @(subs/selected-can-attack? conn)
                @(subs/selected-can-repair? conn))
            (not
             (or @(subs/selected-can-move-to-targeted? conn)
                 @(subs/selected-can-attack-targeted? conn)
                 @(subs/selected-can-repair-targeted? conn))))
       [:p.hidden-xs.hidden-sm
        (translate :select-target-or-destination-tip)])
     ;; TODO: only display when starting faction is active
     (when (and (= round 1)
                (not @(subs/selected-hex conn)))
       [:p.hidden-xs.hidden-sm
        {:dangerouslySetInnerHTML {:__html (translate :multiplayer-tip)}}])]))

(defn faction-list [{:as view-ctx :keys [conn dispatch translate]}]
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
                           "list-group-item")
                icon-class (if (:faction/ai faction)
                             "fa fa-fw fa-laptop clickable"
                             "fa fa-fw fa-user clickable")]
            [:li {:class li-class}
             color
             " "
             (when active
               [:span.fa.fa-angle-double-left
                {:aria-hidden true}])
             [:div.pull-right
              [:span
               {:class icon-class
                :aria-hidden true
                :on-click #(dispatch [::events.ui/configure-faction faction])
                :title (translate :configure-faction-tip)}]]]))))

;; TODO: cleanup unit-picker
(defn unit-picker [{:as view-ctx :keys [conn dispatch translate]}]
  (let [unit-types @(subs/available-unit-types conn)
        cur-faction @(subs/current-faction conn)
        color (name (:faction/color cur-faction))
        hide-picker (fn [ev]
                      (when ev (.preventDefault ev))
                      (dispatch [::events.ui/hide-unit-picker]))]
    [:> js/ReactBootstrap.Modal {:show @(subs/picking-unit? conn)
                                 :on-hide hide-picker}
     [:> js/ReactBootstrap.Modal.Header {:close-button true}
      [:> js/ReactBootstrap.Modal.Title
       (translate :build-title)]]
     (comment
     [:> js/ReactBootstrap.Modal.Body
      (into [:div.unit-picker]
            (for [{:keys [unit-type/id] :as unit-type} unit-types]
              (let [;; TODO: replace with unit-type-image
                    image (->> (string/replace (:unit-type/image unit-type)
                                               "COLOR" color)
                               (str "/images/game/"))
                    media-class (if (:affordable unit-type)
                                  "media clickable"
                                  "media clickable text-muted")
                    popthing [:> js/ReactBootstrap.Popover
                              {:title "Hi"
                               :id (:unit-type/description unit-type)}
                              "Hi"]]
                [:div {:class media-class
                       :on-click #(when (:affordable unit-type)
                                    (dispatch [::events.ui/hide-unit-picker])
                                    (dispatch [::events.ui/build-unit id]))}
                 [:div.media-left.media-middle
                  [:img {:src image}]]
                 [:div.media-body
                  [:h4.media-heading
                   (:unit-type/description unit-type)]
                  (str "Cost: " (:unit-type/cost unit-type))
                  [:> js/ReactBootstrap.OverlayTrigger
                   {:trigger "click"
                    :placement "right"
                    :overlay [:> js/ReactBootstrap.Popover
                              {:title "Hi"
                               :id (:unit-type/description unit-type)}
                              "Hi"]}
                    [:button.btn.btn-default "Hello"]]
                  ]])))])
     ;(into [:div.unit-picker]
     ;(comment
     [:> js/ReactBootstrap.Modal.Body
      [:> js/ReactBootstrap.Grid
       {:fluid true}
        (for [{:keys [unit-type/id] :as unit-type} unit-types]
          (let [;; TODO: replace with unit-type-image
                image (->> (string/replace (:unit-type/image unit-type)
                                            "COLOR" color)
                            (str "/images/game/"))
                media-class (if (:affordable unit-type)
                              "media clickable"
                              "media clickable text-muted")]
           ^{:key unit-type}
           [:> js/ReactBootstrap.Row
            [:> js/ReactBootstrap.Col
               {:lg 5}
               [:div {:class media-class
                      :on-click #(when (:affordable unit-type)
                                   (dispatch [::events.ui/hide-unit-picker])
                                   (dispatch [::events.ui/build-unit id]))}
                [:div.media-left.media-middle
                 [:img {:src image}]]
                [:div.media-body
                 [:h4.media-heading
                  (:unit-type/description unit-type)]
                 (str "Cost: " (:unit-type/cost unit-type))]]]
            [:> js/ReactBootstrap.Col
               {:lg 7
                :style {:text-align "center"}}
               [:> js/ReactBootstrap.Panel
                {:header "Stats"
                 :eventKey (:unit-type/description unit-type)
                 :collapsible true}
                [:> js/ReactBootstrap.Table
                 {:bordered true
                  :striped true
                  :condensed true
                  :fill true}
                 [:thead
                  [:tr
                   [:th [:div {:style {:text-align "center"}} [:strong "Movement"]]]
                   [:th [:div {:style {:text-align "center"}} [:strong "Armor"]]]
                   [:th [:div {:style {:text-align "center"}} [:strong "Range"]]]]]
                 [:tbody
                  [:tr
                   [:td (:unit-type/movement unit-type)]
                   [:td (:unit-type/armor unit-type)]
                   [:td (:unit-type/min-range unit-type) "-" (:unit-type/max-range unit-type)]]]]]]]))]]
     [:> js/ReactBootstrap.Modal.Footer
      [:button.btn.btn-default {:on-click hide-picker}
       "Cancel"]]]))

(defn faction-settings [{:as views-ctx :keys [conn dispatch translate]}]
  (with-let [faction (subs/faction-to-configure conn)
             ;color (when @faction
              ;       (-> @faction
              ;           :faction/color
              ;           name
              ;           string/capitalize))
             selected-player-type (r/atom nil)
             hide-settings (fn [ev]
                             (when ev (.preventDefault ev))
                             (dispatch [::events.ui/hide-faction-settings]))
             select-player-type #(reset! selected-player-type (.-target.value %))
             set-player-type (fn [ev]
                               (.preventDefault ev)
                               (when-let [player-type-id (->> (or @selected-player-type :human)
                                                              (keyword 'zetawar.players))]
                                 (reset! selected-player-type nil)
                                 (dispatch [::events.ui/set-faction-player-type @faction player-type-id]))
                               (dispatch [::events.ui/hide-faction-settings]))]
    [:> js/ReactBootstrap.Modal {:show (some? @faction)
                                 :on-hide hide-settings}
     [:> js/ReactBootstrap.Modal.Header {:close-button true}
      [:> js/ReactBootstrap.Modal.Title
       (str "Configure Faction: " ;color)]]
            (when @faction
              (-> @faction
                  :faction/color
                  name
                  string/capitalize)))]]
     [:> js/ReactBootstrap.Modal.Body
      [:form
       [:div.form-group
        [:label {:for "player-type"}
         (translate :player-type-label)]
        (into [:select.form-control {:id "player-type"
                                     :value (or @selected-player-type
                                                (some-> @faction :faction/player-type name)
                                                "")
                                     :on-change select-player-type}]
              (for [[player-type-id {:keys [description ai]}] players/player-types]
                [:option {:value (name player-type-id)}
                 description]))
        [:> js/ReactBootstrap.Modal.Footer
         [:button.btn.btn-primary {:on-click set-player-type}
          (translate :save-button)]
         [:button.btn.btn-default {:on-click hide-settings}
          (translate :cancel-button)]]]]]]))

;; TODO: move default-scenario-id to data ns?
(defn new-game-settings [{:as view-ctx :keys [conn dispatch translate]}]
  (with-let [default-scenario-id :sterlings-aruba-multiplayer
             selected-scenario-id (r/atom default-scenario-id)
             hide-settings (fn [ev]
                             (when ev (.preventDefault ev))
                             (dispatch [::events.ui/hide-new-game-settings]))
             select-scenario #(reset! selected-scenario-id (keyword (.-target.value %)))
             start-new-game #(do
                               (.preventDefault %)
                               (dispatch [::events.ui/start-new-game @selected-scenario-id])
                               (reset! selected-scenario-id default-scenario-id)
                               (dispatch [::events.ui/hide-new-game-settings]))]
    [:> js/ReactBootstrap.Modal {:show @(subs/configuring-new-game? conn)
                                 :on-hide hide-settings}
     [:> js/ReactBootstrap.Modal.Header {:close-button true}
      [:> js/ReactBootstrap.Modal.Title
       (translate :new-game-title)]]
     [:> js/ReactBootstrap.Modal.Body
      [:form
       [:div.form-group
        [:label {:for "scenario-id"}
         (translate :scenario-label)]
        (into [:select.form-control {:id "scenario-id"
                                     :selected (some-> @selected-scenario-id name)
                                     :on-change select-scenario}]
              (for [[scenario-id {:keys [description]}] data/scenarios]
                [:option {:value (name scenario-id)}
                 description]))
        [:> js/ReactBootstrap.Modal.Footer
         [:button.btn.btn-primary {:on-click start-new-game}
          (translate :start-button)]
         [:button.btn.btn-default {:on-click hide-settings}
          (translate :cancel-button)]]]]]]))

(defn alert [{:as view-ctx :keys [conn dispatch]}]
  (let [{:keys [app/alert-message app/alert-type]} @(subs/app conn)
        alert-class (str "alert alert-" (some-> alert-type name))]
    (when alert-message
      [:div.row
       [:div.col-md-12
        [:div {:class alert-class}
         [:button.close {:type :button
                         :aria-label "Close"
                         :on-click #(dispatch [::events.ui/hide-alert])}
          [:span {:aria-hidden true} "×"]]
         alert-message]]])))

;; TODO: turn entire game interface into it's own component

(defn app-root [{:as view-ctx :keys [conn dispatch translate]}]
  [:div
   [new-game-settings view-ctx]
   [faction-settings view-ctx]
   [unit-picker view-ctx]
   ;; TODO: break win dialog out into it's own component
   ;; TODO: add continue + start new game buttons
   [:> js/ReactBootstrap.Modal {:show @(subs/show-win-message? conn)
                                :on-hide #(dispatch [::events.ui/hide-win-message])}
    [:> js/ReactBootstrap.Modal.Header
     [:> js/ReactBootstrap.Modal.Title
      (translate :win-title)]]
    [:> js/ReactBootstrap.Modal.Body
     {:dangerouslySetInnerHTML {:__html (translate :win-body)}}]
    [:> js/ReactBootstrap.Modal.Footer
     [:button.btn.btn-default {:on-click #(dispatch [::events.ui/hide-win-message])}
      (translate :close-button)]]]
   (navbar "Game")
   [:div.container
    [alert view-ctx]
    [:div.row
     [:div.col-md-2
      [faction-credits view-ctx]
      [faction-list view-ctx]
      [faction-actions view-ctx]]
     [:div.col-md-10
      [faction-status view-ctx]
      [board view-ctx]]]]
   (footer)])

(ns zetawar.logging)

(goog-define LOG_LEVEL 4)

(defn log [& args]
  (let [[ex & other-args] args]
    (if (.-stack ex)
      (do
        (js/console.log (apply str (interpose " " other-args)))
        (js/console.log ex))
      (js/console.log (apply str (interpose " " args))))))

(defn error [& args]
  (when (>= LOG_LEVEL 0)
    (apply log args)))

(defn warn [& args]
  (when (>= LOG_LEVEL 1)
    (apply log args)))

(defn info [& args]
  (when (>= LOG_LEVEL 2)
    (apply log args)))

(defn debug [& args]
  (when (>= LOG_LEVEL 3)
    (apply log args)))

(defn trace [& args]
  (when (>= LOG_LEVEL 4)
    (apply log args)))

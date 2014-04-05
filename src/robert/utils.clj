(ns robert.utils
  (:require [clj-time
             [core :as tm]]
            [monger.collection :as mc]))

(def now (tm/now))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn unique? [collection map-values]
  (if (first (vals map-values))
    (do
      (println map-values, (first (vals map-values)))
      (if-not (mc/find-one-as-map collection map-values)
        true
        false))
    true))

(defn new-secure-code []
  (first (filter #(unique? "users" {:$or [{:activation_code %}
                                          {:email_change_code %}
                                          {:password_reset_code %}]})
                 (repeatedly uuid))))

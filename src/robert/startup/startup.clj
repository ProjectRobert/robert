(ns robert.startup.startup
  (:require [monger.core :refer [get-db]]
            [monger.collection :as mc]
            [cemerick.friend.credentials :as creds]

            [robert.model.config :refer [connection]]))

(defn startup
  "Create a developer account, needed for the development process"
  []
  (mc/insert-and-return
   (get-db connection "robert")
   "users"
   {:username "dev"
    :password (creds/hash-bcrypt "dev")}))
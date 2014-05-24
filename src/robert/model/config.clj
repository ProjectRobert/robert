(ns robert.model.config
  (:require [monger.core :refer [connect-via-uri]]))

(def connection
  (-> 
   (connect-via-uri "mongodb://localhost:27017/robert")
   :conn))

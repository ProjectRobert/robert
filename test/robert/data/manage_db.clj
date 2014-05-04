(ns robert.test.manage-db
  (:require [monger.core :refer [connect! set-db! get-db]]
            [monger.collection :as mc]

            [robert.data.data :as data]))

(defn create-document [name]
  (mc/create name {}))

(defn eliminate-document [name]
  (mc/drop name))

(defn create-first-users [document]
  (doseq [u data/user]
    (mc/insert-and-return document u)))

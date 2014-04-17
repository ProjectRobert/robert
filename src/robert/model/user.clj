(ns robert.model.user
  (:require [monger.core :refer [connect! set-db! get-db]]
            [monger.collection :as mc]
            [monger.joda-time]
            [cemerick.friend.credentials :as creds]
            [noir.validation :as v])
  (:require [robert.utils :as u]))

(connect!)
(set-db! (get-db "robert"))

(alter-var-root (var v/*errors*) (fn [_] (atom {})))

(defn new-collection [name]
  (mc/create name))

(defn- new-secure-code [collection]
  (first (filter #(u/unique? collection {:$or [{:activation_code %}
                                             {:email_change_code %}
                                             {:password_reset_code %}]})
                 (repeatedly u/uuid))))

(defn new! [collection {:keys [email password username] :as user}]
  (if (u/unique? collection {:email email})
    (if (u/unique? collection {:username username})
      (let [save-user (dissoc user :password)]
        (mc/insert-and-return collection
                              (merge save-user
                                     {:password (when password (creds/hash-bcrypt password))
                                      :activation_code (new-secure-code collection)
                                      :creation_time u/now
                                      :last_seen u/now})))
      (do (v/set-error! :registration :not-valid-username)
          :not-valid-username))
    (do (v/set-error! :registration :not-valid-email)
        :not-valid-email)))

(defn get-user [collection query]
  (let [results (mc/find-maps collection query)]
    results))

(defn set-values! [collection query values]
  (let [results (get collection query)]
    (doall
     (map
      (fn [r] (mc/save-and-return collection (merge r values)))
      results))))

(defn remove! [collection query]
  (mc/remove collection query))

(defn check [collection query password]
  (let [user (first (get-user collection query))]
    (if-let [pass (:password user)]
      (creds/bcrypt-verify password pass)
      true)))

(defn verify-email! [collection code]
  (if-let [user (mc/find-one-as-map collection {:activation_code code})]
    (mc/update-by-id "users" (:_id user) {:$unset {:activation_code 1
                                                   :activation_code_created_at 1}})
    (v/set-error :code :not-valid-validation-code)))

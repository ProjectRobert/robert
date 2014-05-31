(ns robert.model.user
  (:require [monger.core :refer [get-db]]
            [monger.collection :as mc]
            [monger.joda-time]
            [monger.operators :refer :all]
            [cemerick.friend.credentials :as creds]
            [noir.validation :as v]
            [clj-time.core :as t])
  (:require [robert.utils :as u]
            [robert.model.config :refer [connection]]))

(alter-var-root (var v/*errors*) (fn [_] (atom {})))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def collection-fields 
      [:email :crypted_password 
       :activation_code :activation_code_created_at
       :password_reset_code :password_reset_code_created_at :new_password_requested
       :new_requested_email :email_change_code :email_change_code_created_at
       :created_at :updated_at])

(defn- unique? [database {:keys [record value] :as map-values}]
  (if-not (mc/find-one-as-map (get-db connection database) "users" map-values)
    true
    false))

(defn- find-user-login [database query]
  (mc/find-one-as-map (get-db connection database) "users" query))

(defn- find [database query]
  (mc/find-maps (get-db connection database) "users" query))

(defn fetch [database query & {:keys [multiple] :or [multiple false]}]
  (println "fetch: =>" database query)
  (if multiple
    (mc/find-maps (get-db connection database) "users" query)
    (mc/find-one-as-map (get-db connection database) "users" query)))

(defn valid?
  "Return true if the user data are valid.
   The user data is valid if a password is present and is present or an email or an username.
   Either email and username must be unique in the database."
  [database user-data]
  (and (boolean (get user-data "password"))
       (let [email (get user-data "email")
             username (get user-data "username")]
         (cond
          (and email username) (and (unique? database {:email email})
                                    (unique? database {:username username}))
          (boolean email) (unique? database {:email email})
          (boolean username) (unique? database {:username username})
          :else false))))

(defn user-login [database creds-map]
  (println "attemp login => " creds-map)
  (when-let [user (find-user-login database {$or
                                               [{:username (:username creds-map)}
                                                {:email (:email creds-map)}]})]
    user))


;; name and email, if the exist they need to be unique in the document

(defn new! [database user-data]
  (assert (valid? database user-data))
  (let [email (get user-data "email")
        name (get user-data "username")
        password (get user-data "password")]

    (let [activaction-code (uuid)
          user (mc/insert-and-return (get-db connection database) "users"
                                     (assoc user-data
                                       :password (creds/hash-bcrypt password)))]
      (dissoc user :password))))

(defn verify-email! [database code]
  (if-let [user (mc/find-one-as-map (get-db connection database) "users" {:activation_code code})]
    (mc/update-by-id (get-db connection database) "users"
                     (:_id user) {$unset {:activation_code 1
                                          :activation_code_created_at 1}})
    :not-valid-validation-code))

(defn remove! [database email]
  (mc/remove (get-db connection database) "users" {:email email}))

(defn login [database creds-map]
  (let [creds-map (clojure.walk/keywordize-keys creds-map)]
    (println "model.user/login - database => " database)
    (println "model.user/login - creds-map => " creds-map)
    (when-let [user (fetch database {$or
                                     [{:username (get creds-map :username)}
                                      {:email (get creds-map :email)}]})]
      (println "model.user/login - user => " user)
      (println "model.user/login - creds-map" creds-map)
      (when (creds/bcrypt-verify (get creds-map :password) (:password user))
        (do (println "ok")
            (dissoc user :password))))))

(defn add-last-login [database user]
  (mc/update-by-id (get-db connection database)
                   "users"
                   (:_id user) {$set {:last-login (t/now)}}))

(defn update-document
  [database query set & {:keys [multiple] :or [multiple false]}]
  (println database (class database) query (class query) set (class set))
  (mc/update (get-db connection database) "users"
             query
             set
             {:multiple multiple}))
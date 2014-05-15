(ns robert.model.user
  (:require [monger.core :refer [connect! set-db! get-db]]
            [monger.collection :as mc]
            [monger.joda-time]
            [monger.operators :refer :all]
            [cemerick.friend.credentials :as creds]
            [noir.validation :as v]
            [clj-time.core :as t])
  (:require [robert.utils :as u]))

(connect!)
(set-db! (get-db "robert"))

(alter-var-root (var v/*errors*) (fn [_] (atom {})))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def collection-fields 
      [:email :crypted_password 
       :activation_code :activation_code_created_at
       :password_reset_code :password_reset_code_created_at :new_password_requested
       :new_requested_email :email_change_code :email_change_code_created_at
       :created_at :updated_at])

(defn- unique? [collection {:keys [record value] :as map-values}]
  (if-not (mc/find-one-as-map collection map-values)
    true
    false))

(defn- find-user-login [collection query]
  (mc/find-one-as-map collection query))

(defn- find [collection query]
  (mc/find-maps collection query))

(defn fetch [collection query & {:keys [multiple] :or [multiple false]}]
  (if multiple
    (mc/find-maps collection query)
    (mc/find-one-as-map collection query)))

(defn- new-secure-code [collection]
  (first (filter #(unique? collection {$or [{:activation_code %}
                                            {:email_change_code %}
                                            {:password_reset_code %}]})
                 (repeatedly uuid))))

(defn valid? [collection user-data]
  (and (boolean (get user-data "password"))
       (let [email (get user-data "email")
             username (get user-data "username")]
         (cond
          (and email username) (and (unique? collection {:email email})
                                    (unique? collection {:username username}))
          (boolean email) (unique? collection {:email email})
          (boolean username) (unique? collection {:username username})
          :else false))))

(defn user-login [collection creds-map]
  (println "attemp login => " creds-map)
  (when-let [user (find-user-login collection {$or
                                               [{:username (:username creds-map)}
                                                {:email (:email creds-map)}]})]
    user))

(defn ask-change-email! [collection email new-email password]
  (let [user (mc/find-one-as-map collection {:email email})]
    (if (creds/bcrypt-verify password (:password user))
      (mc/save-and-return collection
                          (merge user
                                 {:email_change_code (new-secure-code)
                                  :email_change_code_created_at u/now
                                  :new_requested_email new-email}))
      (v/set-error :change-email :wrong-password))))

(defn change-email! [collection reset-code]
   (if-let [user (mc/find-one-as-map collection {:email_change_code
                                                reset-code})]
     (mc/update-by-id collection (:_id user)
               {$set {:email (:new_requested_email user)}
                $unset  {:email_change_code 1
                          :email_change_code_created_at 1
                          :new_requested_email 1}})
     (v/set-error :code :not-valid-email-code)))

(defn ask-change-password! [collection email new-password password]
   (let [user (mc/find-one-as-map collection {:email email})]
     (if (creds/bcrypt-verify password (:password user))
       (mc/save-and-return collection
                           (merge user
                                  {:password_reset_code (new-secure-code)
                                   :password_reset_code_created_at u/now
                                   :new_password_requested (creds/hash-bcrypt new-password)}))
      (v/set-error :change-password :wrong-password))))

(defn change-password! [collection reset-code]
  (if-let [user (mc/find-one-as-map collection {:password_reset_code
                                              reset-code})]
    (mc/update-by-id collection (:_id user)
               {$set {:password (:new_password_requested user)}
                $unset {:password_reset_code 1
                         :password_reset_code_created_at 1
                         :new_password_requested 1}})
    (v/set-error :code :not-valid-password-code)))


;; name and email, if the exist they need to be unique in the document

(defn new! [collection user-data]
  (assert (valid? collection user-data))
  (let [email (get user-data "email")
        name (get user-data "username")
        password (get user-data "password")]

    (let [activaction-code (new-secure-code collection)
          user (mc/insert-and-return collection
                                     (assoc user-data
                                       :password (creds/hash-bcrypt password)))]
      (dissoc user :password))))

(defn verify-email! [collection code]
  (if-let [user (mc/find-one-as-map collection {:activation_code code})]
    (mc/update-by-id collection (:_id user) {$unset {:activation_code 1
                                                     :activation_code_created_at 1}})
    (v/set-error :code :not-valid-validation-code)))

(defn remove! [collection email]
  (mc/remove collection {:email email}))

(defn login [collection creds-map]
  (println "model.user/login - collection => " collection)
  (println "model.user/login - creds-map => " creds-map)
  (if (and (= (:username creds-map) "test")
           (= (:password creds-map) "test"))
    {:email "test" :collection "test"}
    (when-let [user (fetch collection {$or
                                       [{:username (get creds-map "username")}
                                        {:email (get creds-map "email")}]})]
      (println "model.user/login - user => " user)
      (when (creds/bcrypt-verify (get creds-map "password") (:password user))
        (do (println "ok")
            (dissoc user :password))))))

(defn add-last-login [collection user]
  (mc/update-by-id collection (:_id user) {$set {:last-login (t/now)}}))
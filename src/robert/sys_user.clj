(ns wit.models.tables.sys-user
  (:require [monger.core :refer [connect! set-db! get-db]]
            [monger.collection :as mc]
            [monger.joda-time]
            [monger.operators :refer :all]
            [cemerick.friend.credentials :as creds]
            [wit.models.utils :as u]
            [wit.models.email :refer [send-email]])
  (:import java.util.UUID))

(connect!)
(set-db! (get-db "workinvoice"))

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

(defn- find-user-login [email]
  (mc/find-one-as-map "users" {:email email
                               :activation_code nil}))

(defn- find [query]
  (mc/find-maps "users" query))

(defn- new-secure-code []
  (first (filter #(unique? "users" {:$or [{:activation_code %}
                                          {:email_change_code %}
                                          {:password_reset_code %}]})
                 (repeatedly uuid))))

(defn valid? [email]
  (unique? "users" {:email email}))

(defn user-login [creds-map]
  (println "attemp login => " creds-map)
  (when-let [usr (find-user-login (:username creds-map))]
    usr))

(defn ask-change-email! [email new-email]
  (mc/update "users" {:email email}
             {:$set {:email_change_code (new-secure-code)
                     :email_change_code_created_at u/now
                     :new_requested_email new-email}}))

(defn change-email! [reset-code]
   (when-let [user (mc/find-one-as-map "users" {:email_change_code
                                                reset-code})]
     (mc/update-by-id "users" (:_id user)
               {:$set {:email (:new_requested_email user)}
                :$unset  {:email_change_code 1
                          :email_change_code_created_at 1
                          :new_requested_email 1}})))

(defn ask-change-password! [email new-password]
  (mc/update "users" {:email email}
             {:$set {:password_reset_code (new-secure-code)
                     :password_reset_code_created_at u/now
                     :new_password_requested (creds/hash-bcrypt new-password)}}))

(defn change-password! [reset-code]
  (when-let [user (mc/find-one-as-map "users" {:password_reset_code
                                              reset-code})]
    (mc/update-by-id "users" (:_id user)
               {:$set {:password (:new_password_requested user)}
                :$unset {:password_reset_code 1
                         :password_reset_code_created_at 1
                         :new_password_requested 1}})))

(defn new! [email password]
  (if (valid? email)
    (let [activaction-code (new-secure-code)]
      (mc/insert "users" {:email email
                          :password (creds/hash-bcrypt password)
                          :activation_code activaction-code
                          :activation_code_created_at u/now})
      (send-email {:to email :subject "Benvenuto su Workinvoice" :body activaction-code}) ;;TODO move send-email to a common layer 
      )
    :not-unique-user))

(defn verify-email! [code]
  (if-let [user (mc/find-one-as-map "users" {:activation_code code})]
    (mc/update-by-id "users" (:_id user) {:$unset {:activation_code 1
                                                   :activation_code_created_at 1}})
    :no-code-exist))

(defn remove! [email]
  (mc/remove "users" {:email email}))

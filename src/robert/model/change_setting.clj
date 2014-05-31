(ns robert.model.change-setting
  (:require [monger.collection :as mc]
            [cemerick.friend.credentials :as creds]
            [noir.validation :as v])
  (:require [robert.utils :as u])
  (:import [org.bson.types ObjectId]))

(defn ask-change-email! [database email new-email password]
  (let [user (mc/find-one-as-map (get-db connection database) "users" {:email email})]
    (if (creds/bcrypt-verify password (:password user))
      (mc/save-and-return (get-db connection database) "users"
                          (merge user
                                 {:email_change_code (uuid)
                                  :email_change_code_created_at u/now
                                  :new_requested_email new-email}))
      :wrong-password)))

(defn change-email! [database reset-code]
   (if-let [user (mc/find-one-as-map (get-db connection database) "users" {:email_change_code
                                                reset-code})]
     (mc/update-by-id (get-db connection database) "users" (:_id user)
               {$set {:email (:new_requested_email user)}
                $unset  {:email_change_code 1
                          :email_change_code_created_at 1
                          :new_requested_email 1}})
     :not-valid-email-code))

(defn prepare-change-password! [database user new-password]
  (mc/save-and-return (get-db connection database) "users"
                      (merge user
                             {:password_reset_code (uuid)
                              :password_reset_code_created_at u/now
                              :new_password_requested (creds/hash-bcrypt new-password)})))

(defn change-password! [database reset-code]
  (if-let [user (mc/find-one-as-map (get-db connection database) "users" {:password_reset_code
                                              reset-code})]
    (mc/update-by-id (get-db connection database) "users" (:_id user)
               {$set {:password (:new_password_requested user)}
                $unset {:password_reset_code 1
                         :password_reset_code_created_at 1
                         :new_password_requested 1}})
    :not-valid-password-code))

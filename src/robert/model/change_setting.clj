(ns robert.model.change-setting
  (:require [monger.collection :as mc]
            [cemerick.friend.credentials :as creds]
            [noir.validation :as v])
  (:require [robert.utils :as u])
  (:import [org.bson.types ObjectId]))

(defn ask-change-email! [collection id new-email password]
  (let [user (mc/find-map-by-id collection (ObjectId. id))]
    (if (creds/bcrypt-verify password (:password user))
      (mc/save-and-return collection
                          (merge user
                                 {:email_change_code (u/new-secure-code)
                                  :email_change_code_created_at u/now
                                  :new_requested_email new-email}))
      :wrong-password)))

(defn change-email! [collection reset-code]
   (if-let [user (mc/find-one-as-map collection {:email_change_code
                                                reset-code})]
     (mc/update-by-id collection (:_id user)
               {:$set {:email (:new_requested_email user)}
                :$unset  {:email_change_code 1
                          :email_change_code_created_at 1
                          :new_requested_email 1}})
     :not-valid-email-code))

(defn ask-change-password! [collection id new-password password]
   (let [user (mc/find-map-by-id collection (ObjectId. id))]
     (if (creds/bcrypt-verify password (:password user))
       (mc/save-and-return collection
                           (merge user
                                  {:password_reset_code (u/new-secure-code)
                                   :password_reset_code_created_at u/now
                                   :new_password_requested (creds/hash-bcrypt new-password)}))
      (v/set-error :change-password :wrong-password))))

(defn change-password! [collection reset-code]
  (if-let [user (mc/find-one-as-map collection {:password_reset_code
                                              reset-code})]
    (mc/update-by-id collection (:_id user)
               {:$set {:password (:new_password_requested user)}
                :$unset {:password_reset_code 1
                         :password_reset_code_created_at 1
                         :new_password_requested 1}})
    :not-valid-password-code))

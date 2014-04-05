(ns robert.routes.rest-api
  (:require [compojure.core :refer [defroutes GET POST context]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [noir.validation :as vali]
            [cemerick.friend :as friend])
  (:require [robert.model.user :as user]
            [robert.model.change-setting :as change-settings]
            [robert.models.email :as email]))


;; I will keep unique the field :email and :username

;; :new! take whatever return the whole document created minus the password
;; :check execute a get and then check if the password match
;; :set-values! execute a get and then set the fields and save
;; :get take a query and run the query, return everything
;; :remove! remove everything


(defroutes confirm-code
  (GET "/validate/:code" [code]
       (let [changed (user/verify-email! code)]
         (if (not (vali/get-errors :code))
           "email verificata"
           "il codice non esiste")))
  
  (GET "/email/:code" [code]
       (let [changed (change-settings/change-email! code)]
         (if (not (vali/get-errors :code))
           "email cambiata"
           "il codice non esiste")))
  
  (GET "/password/:code" [code]
       (let [changed (change-settings/change-password! code)]
         (if (not (vali/get-errors :code))
           "password cambiata"
           "il codice non esiste"))))

(defroutes change
  (POST "/password" {p :params}
        (let [id (:id (friend/current-authentication))
              {:keys [new-password password]} p]
          (if-let [user (change-settings/ask-change-password! id new-password password)]
            (do
              (email/conferma-cambio-password (:email user) (:password_reset_code user))
              "È stata mandata un email per il cambio password")
            "Problemi nel database")))

  (POST "/email" {p :params}
        (let [email (:id (friend/current-authentication))
              {:keys [new-email password]} p]
          (if-let [user (change-settings/ask-change-email! email new-email password)]
            (do
              (email/conferma-cambio-email (:new_requested_email user) (:email_change_code user))
              (str "È stata mandata un email al nuovo indirizzo per confermare il cambio email"))
            "Problemi nel database"))))


(defroutes robert
  (POST "/new" {p :params}
        (let [user (user/new! "robert" p)]
          (if (not (vali/errors? :registration))
            (do
              (email/conferma-email (:email user) (user :activation_code))
              (str "Registrazione completata"))
            (str "Error: " (vali/get-errors)))))
  (context "/confirm" [] confirm-code)
  (context "/change" [] change))
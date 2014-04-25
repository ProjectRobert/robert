(ns robert.routes.rest-api
  (:require [compojure.core :refer [defroutes GET POST context ANY]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [noir.validation :as vali]
            [cemerick.friend :as friend]
            [liberator.core :refer [defresource resource]]
            [cheshire.core :refer [generate-string]])
  (:require [robert.model.user :as user]
            [robert.model.change-setting :as change-settings]))


;; I will keep unique the field :email and :username

;; :new! take whatever return the whole document created minus the password
;; :check execute a get and then check if the password match
;; :set-values! execute a get and then set the fields and save
;; :get take a query and run the query, return everything
;; :remove! remove everything

(defresource get-user
  :allowed-methods [:get]
  :available-media-types ["application/json"]
  :authorized? (fn [ctx]
                 (let [username (get-in ctx [:request :params :name])
                       password (get-in ctx [:request :params :secret])]
                   (if-let [user (user/login "robert" username password)]
                     {:user user}
                     false)))
  :handle-ok (fn [ctx]
               (generate-string {:username (:username ctx)
                                 :password (:password ctx)} {:pretty-print true}))
  :handle-created (fn [ctx]
                    (generate-string {:username (:username ctx)
                                      :password (:password ctx)} {:pretty-print true})))

(defresource new-user
  :allowed-methods [:post]
  :available-media-types ["application/json"]
  :malformed? (fn [ctx]
                (println ctx)
                (let [email (get-in ctx [:request :form-params "email"])
                      pass (get-in ctx [:request :form-params "password"])]
                  (if (nil? email)
                    [true {:malformed {:message "Not providen requested field: email"}}]
                    (if (nil? pass)
                      [true {:malformed {:message "Not providen requested field: password"}}]
                      false))))
  :handle-malformed (fn [ctx]
                      (generate-string (:malformed-reason ctx)))
  :authorized? (fn [{req :request}]
                 (let [username (get-in req [:basic-authentication :username])
                       password (get-in req [:basic-authentication :password])]
                   ;; (if-let [user (friend/authorized? [:user] (friend/identity req))]
                   ;;   {:user user}
                   ;;   [false {:unauthorized {:message (format "Not find the couple name: %s secret: %s" username password)}}]
                   
                   (if-let [user (user/login "robert" username password)]
                     {:user user}
                     [false {:unauthorized {:message (format "Not find the couple name: %s secret: %s" username password)}}])))
  
  :handle-unauthorized (fn [ctx]
                         (generate-string (:unauthorized ctx)))
  :allowed? (fn [ctx]
              (let [collection (get-in ctx [:user :collection])
                    new-username-email (get-in ctx [:request :form-params "email"])]
                (if (user/valid? collection new-username-email)
                  true
                  [false {:forbidden {:message "Email already present"
                                      :email new-username-email}}])))
  :handle-forbidden (fn [ctx]
                      (generate-string (:forbidden ctx)))
  :post! (fn [ctx]
           (let [user-data (get-in ctx [:request :form-params])
                 collection (get-in ctx [:user :collection])]
             {:created-user (user/new! collection user-data)}))
  :handle-created (fn [ctx]
                    (generate-string (-> ctx :created-user
                                         (update-in [:_id] str)
                                         (dissoc :password "password")))))


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
              ;(email/conferma-cambio-password (:email user) (:password_reset_code user))
              "È stata mandata un email per il cambio password")
            "Problemi nel database")))

  (POST "/email" {p :params}
        (let [email (:id (friend/current-authentication))
              {:keys [new-email password]} p]
          (if-let [user (change-settings/ask-change-email! email new-email password)]
            (do
              ;(email/conferma-cambio-email (:new_requested_email user) (:email_change_code user))
              (str "È stata mandata un email al nuovo indirizzo per confermare il cambio email"))
            "Problemi nel database"))))


(defroutes robert
  (ANY "/poi" params get-user)
  (ANY "/new-user" params new-user)
  (ANY "/test" {p :params}
       (resource :allowed-methods [:get :post :put :delete :patch]
                 :available-media-types ["text/html" "application/json"]
                 :handle-ok (fn [ctx]
                              (format "<html>It's %d milliseconds since the beginning of the epoch.\n %s \n <p> ctx %s</p>"
                                      (System/currentTimeMillis) p ctx))
                 :post! (fn [ctx] (do (println (format "aaa"))
                                      {:test (rand-int 25)}))
                 :new? (fn [ctx] true)
                 :post-redirect? false
                 :handle-created (fn [ctx]
                                   (format "<html> Test <p>%s</p>" ctx))
                 :respond-with-entity? (fn [ctx] true)
                 :multiple-representations? (fn [ctx] false)))
  (POST "/new" {p :params}
        (let [user (user/new! "robert" p)]
          (if (not (vali/errors? :registration))
            (do
              ;(email/conferma-email (:email user) (user :activation_code))
              (str "Registrazione completata"))
            (str "Error: " (vali/get-errors)))))
  (context "/confirm" [] confirm-code)
  (context "/change" [] change))

(ns robert.routes.rest-api
  (:require [compojure.core :refer [defroutes GET POST context ANY PATCH]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [noir.validation :as vali]
            [cemerick.friend :as friend]
            [liberator.core :refer [defresource resource]]
            [cheshire.core :refer [generate-string]])

  (:require [robert.model.user :as user]
            [robert.model.change-setting :as change-settings]))

;; I will keep unique the field :email and :username

(def authorization
  {:fn (fn [key] (fn [{req :request}]
                   (let [username (get-in req [:basic-authentication :username])
                         password (get-in req [:basic-authentication :password])]
                     (if-let [user (user/login "robert" {:username username :password password})]
                       {key user}
                       [false {:unauthorized {:message (format "Could not log in, check again username and password.")
                                              :username username}}]))))
   :handle (fn [ctx]
             (generate-string (:unauthorized ctx)))})

(defn make-entity-json-friendly [entity]
  (-> entity
      (update-in [:_id] str)
      (dissoc :password "password")))

(defresource get-user
  :allowed-methods #{:get}
  :available-media-types ["application/json"]
  :malformed? (fn [ctx]
                (let [q-params (get-in ctx [:request :query-params])]
                  (if-let [email (get q-params "email")]
                    [false {:query
                            {:email email}}]
                    (if-let [username (get q-params "username")]
                      [false {:query
                              {:username username}}]
                      [true {:malformed {:message "You need to provide or the email or the username"}}]))))
  :handle-malformed (fn [ctx]
                      (generate-string (:malformed ctx)))
  :authorized? ((:fn authorization) :user)
  :handle-unauthorized (:handle authorization)
  :exists? (fn [ctx]
             (if-let [user (user/fetch (-> ctx :user :database)
                                       (-> ctx :query))]
               [true {:result user}]
               false))
  :handle-not-found (fn [ctx]
                      (generate-string
                       {:message (format "User not found")
                        :query (:query ctx)}))
  :handle-ok (fn [ctx]
               (generate-string (-> ctx :result
                                    make-entity-json-friendly) {:pretty-print true})))

(defresource new-user
  :allowed-methods #{:post}
  :available-media-types ["application/json"]
  :malformed? (fn [ctx]
                (let [email (get-in ctx [:request :json-params "email"])
                      pass (get-in ctx [:request :json-params "password"])]
                  (if (nil? email)
                    [true {:malformed {:message "Not providen requested field: email"}}]
                    (if (nil? pass)
                      [true {:malformed {:message "Not providen requested field: password"}}]
                      false))))
  :handle-malformed (fn [ctx]
                      (generate-string (:malformed ctx)))
  :authorized? ((:fn authorization) :user)
  :handle-unauthorized (:handle authorization)
  :allowed? (fn [ctx]
              (let [database (get-in ctx [:user :database])
                    new-username-email (get-in ctx [:request :json-params])]
                (println database new-username-email)
                (if (user/valid? database new-username-email)
                  true
                  [false {:forbidden {:message "Email already present"
                                      :email new-username-email}}])))
  :handle-forbidden (fn [ctx]
                      (generate-string (:forbidden ctx)))
  :post! (fn [ctx]
           (let [user-data (get-in ctx [:request :json-params])
                 database (get-in ctx [:user :database])]
             {:created-user (user/new! database user-data)}))
  :handle-created (fn [ctx]
                    (generate-string (-> ctx :created-user
                                         make-entity-json-friendly))))

(defresource login
  :allowed-methods #{:post}
  :available-media-types ["application/json"]
  :malformed? (fn [ctx]
                (let [user-value (get-in ctx [:request :json-params "login"])]
                  (if (and (or (contains? user-value "email")
                               (contains? user-value "username"))
                           (contains? user-value "password"))
                    [false {:login-creds user-value}]
                    [true {:malformed {:message "You need to provide the key \"login\"."}}])))
  :handle-malformed (fn [ctx]
                      (generate-string (:malformed ctx)))
  :authorized? ((:fn authorization) :user)
  :handle-unauthorized (:handle authorization)
  :exists? (fn [ctx]
             (println "exists??? key")
             (println (-> ctx :login-creds)
                      (-> ctx :user :database))
             (if-let [user (user/login (-> ctx :user :database) (:login-creds ctx))]
               (do
                 (println "login - exists? user =>" user)
                 [true {:login user}])
               (do
                 (println "wrong exist")
                 false)))
  :new? false
  :respond-with-entity? true
  :handle-ok (fn [ctx]
               (let [specific-user (-> ctx :login)]
                 (user/add-last-login (-> ctx :user :database) specific-user)
                 (generate-string (-> ctx :login make-entity-json-friendly)))))

(defresource change-values
  :allowed-methods #{:patch}
  :available-media-types ["application/json"]
  :malformed? (fn [ctx]
                (let [query (get-in ctx [:request :json-params "query"])
                      set-values (get-in ctx [:request :json-params "set"])]
                  (cond
                      (not query) [true {:malformed "It's necessary the key \"query\"."}]
                      (not set-values) [true {:malformed "It's necessary the key \"set\""}]
                      :else (cond
                             (not (map? query)) [true {:malformed "The value of \"query\" must be a dictionary."}]
                             (not (map? set-values)) [true {:malformed "The value of \"set\" must be a dictionary."}]
                             :else [false {:query query :new-values set-values}]))))
  :handle-malformed (fn [ctx]
                      (generate-string (:malformed ctx)))
  :authorized? ((:fn authorization) :user)
  :handle-unauthorized (:handle authorization)
  :exists? (fn [ctx]
             (if-let [users (user/fetch (-> ctx :user :database)
                                        (-> ctx :query)
                                        :multiple true)]
               [true {:query-result users}]
               [false {:not-found {:message "Not found any element that satisfy the query."
                                   :query (ctx :query)}}] ))
  :handle-not-found (fn [ctx]
                      (generate-string (:not-found ctx)))
  :new? false
  :post! (fn [ctx])
  :respond-with-entity? true
  :handle-ok (fn [ctx]
               (generate-string {:query (:query ctx) :new-values (:new-values ctx)})))

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
  (ANY "/get-user" params get-user)
  (ANY "/new-user" params new-user)
  (ANY "/login" params login)
  (PATCH "/set" params change-values)
  (context "/confirm" [] confirm-code)
  (context "/change" [] change))

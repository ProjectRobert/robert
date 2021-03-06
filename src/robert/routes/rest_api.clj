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

(def accepeted-query
  #{"$set" "$unset" "$push" "$pushAll" "$addToSet" "$pull" "$pop"})

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

(def get-user
  (fn [id]
    {:allowed-methods #{:get}
     :available-media-types ["application/json"]
     :authorized? ((:fn authorization) :user)
     :handle-unauthorized (:handle authorization)
     :exists? (fn [ctx]
                (if-let [user (user/fetch (-> ctx :user :database)
                                          {:_id id})]
                  [true {:result user}]
                  false))
     :handle-not-found (fn [ctx]
                         (generate-string
                          {:message (format "User not found")
                           :query (:query ctx)}))
     :handle-ok (fn [ctx]
                  (generate-string (-> ctx :result
                                       make-entity-json-friendly) {:pretty-print true}))}))

(def new-user
  {:allowed-methods #{:post}
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
                                          make-entity-json-friendly)))})

(def login
  {:allowed-methods #{:post}
   :available-media-types ["application/json"]
   :malformed? (fn [ctx]
                 (let [user-value (get-in ctx [:request :json-params])]
                   (cond
                    (not (or (contains? user-value "email")
                             (contains? user-value "username")))
                    [true {:malformed {:message "You need to provide or the key \"email\" or the key \"username\"."}}]
                    (not (contains? user-value "password"))
                    [true {:malformed {:message "You need to provide or the key \"password\""}}]
                    :else [false {:login-creds user-value}])))
   :handle-malformed (fn [ctx]
                       (generate-string (:malformed ctx)))
   :authorized? ((:fn authorization) :user)
   :handle-unauthorized (:handle authorization)
   :exists? (fn [ctx]
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
                  (generate-string (-> ctx :login make-entity-json-friendly))))})

(def change-values
  {:allowed-methods #{:put}
   :available-media-types ["application/json"]
   :malformed? (fn [ctx]
                 (cond
                  (not (get-in ctx [:request :json-params "query"]))
                  [true {:malformed "It's necessary the key \"query\"."}]
                  (not (some accepeted-query (-> ctx (get-in [:request :json-params]) keys)))
                  [true {:malformed
                         (str "It's necessary at least one of these keys:" accepeted-query)}]
                  :else false))
   :handle-malformed (fn [ctx]
                       (generate-string (:malformed ctx)))
   :authorized? ((:fn authorization) :user)
   :handle-unauthorized (:handle authorization)
   :new? false
   :put! (fn [ctx]
           (let [query (-> ctx
                           (get-in [:request :json-params])
                           (select-keys accepeted-query))
                 write-result (user/update-document (get-in ctx [:user :database])
                                                    (get-in ctx [:request :json-params "query"])
                                                    query
                                                    :multiple true)]
             {:result {:n (.getN write-result)
                       :err (.getError write-result)}}))
   :respond-with-entity? true
   :handle-ok (fn [ctx]
                (-> ctx :result generate-string))})

(def validate-email
  (fn [code]
    {:allowed-methods #{:get :post}
     :available-media-types ["application/json"]
     :exists? (fn [ctx]
                (let [verification (user/verify-email! code)]
                  (cond
                   (map? verification) [true {:result {:message "The user email is been validated"}}]
                   (keyword? verificata) [false {:message {:message "The code is wrong"}}])))
     :handle-ok (fn [ctx]
                  (-> ctx :result generate-string))
     :handle-not-found (fn [ctx]
                         (-> ctx :result generate-string))}))

(def validate-email-code
  (fn [code]
    {:allowed-methods #{:get :post}
     :available-media-types ["application/json"]
     :exists? (fn [ctx]
                (let [verification (change-settings/change-email! code)]
                  (cond
                   (map? verification) [true {:result {:message "The email is been changed."}}]
                   (keyword? verificata) [false {:message {:message "The validation code is wrong."}}])))
     :handle-ok (fn [ctx]
                  (-> ctx :result generate-string))
     :handle-not-found (fn [ctx]
                         (-> ctx :result generate-string))}))

(def validate-password-code
  (fn [code]
    {:allowed-methods #{:get :post}
     :available-media-types ["application/json"]
     :exists? (fn [ctx]
                (let [verification (change-settings/change-password! code)]
                  (cond
                   (map? verification) [true {:result {:message "The password is been changed."}}]
                   (keyword? verificata) [false {:message {:message "The validation code is wrong."}}])))
     :handle-ok (fn [ctx]
                  (-> ctx :result generate-string))
     :handle-not-found (fn [ctx]
                         (-> ctx :result generate-string))}))

(def change-password
  {:allowed-methods #{:post}
   :available-media-types ["application/json"]
   :malformed? (fn [ctx]
                 (let [values (get-in ctx [:request :json-params])]
                   (cond
                    (nil? (get values "id" nil))
                    [true {:malformed
                           {:message "Need the key \"id\"."}}]
                    (nil? (get values "old_password" nil))
                    [true {:malformed
                           {:message "Need the key \"old_password\"."}}]
                    (nil? (get values "new_password" nil))
                    [true {:malformed
                           {:message "Need the key \"new_password\"."}}]
                    :else [false {:id (get values "id")
                                  :email (get values "email")
                                  :new_password (get values "new_password")
                                  :old_password (get values "old_password")}])))
   :handle-malformed (fn [ctx]
                       (generate-string (:malformed ctx)))
   :authorized? ((:fn authorization) :user)
   :handle-unauthorized (:handle authorization)
   :exists? (fn [ctx]
              (if-let [user (user/fetch (get-in ctx [:user :database])
                                        {:id (:id ctx)})]
                (if (creds/bcrypt-verify (:old_password ctx) (:crypted_password user))
                  [true {:user-to-modify user}]
                  [false {:not-exist {:message "The credentials \"id\" and \"old_password\" do not match."}}])
                [false {:not-exist {:message "The credentials \"id\" and \"old_password\" do not match."}}]))
   :existed? false :post-to-missing? false
   :handle-not-found (fn [ctx] (generate-string (:not-exist ctx)))
   :post! (fn [ctx]
            (let [updated-user (prepare-change-password! (get-in ctx [:user :database])
                                                         (:user-to-modify ctx)
                                                         (:new_password ctx))]
              {:updated-user updated-user}))
   :handle-ok (fn [ctx]
                (generate-string {:code (:password_reset_code (:updated-user ctx))}))})

(def change-email
  {:allowed-methods #{:post}
   :available-media-types ["application/json"]})

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
  (ANY "/get-user/:id" [id :as p] (resource (get-user id)))
  (ANY "/new-user" params (resource new-user))
  (ANY "/set" params (resource change-values))
  (ANY "/login" params (resource login))
  (ANY "/validate/:code" [code] (resource (validate-email code)))
  (ANY "/validate/email/:code" [code] (resource (validate-email-code code)))
  (ANY "/validate/password/:code" [code] (resource (validate-password-code code)))
  (context "/change" [] change))

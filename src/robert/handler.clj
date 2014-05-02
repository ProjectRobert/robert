(ns robert.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
            [liberator.dev :refer [wrap-trace]]

            [robert.routes.rest-api :refer [robert]]
            [robert.model.user :refer [login]]))

(def secure-robert
  (-> robert
      (wrap-basic-authentication (fn [user password]
                                   (println user password)
                                   {:username user :password password}))))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (context "/api" [] secure-robert)
  (route/resources "/")
  (route/not-found "Not Found"))

(def app (-> app-routes
             wrap-multipart-params
             wrap-json-params
             handler/site
             (wrap-trace :header :ui)))

(ns robert.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]

            [robert.routes.rest-api :refer [robert]]))

(defroutes app-routes
  (GET "/" [] "Hello World")
  (context "/aa" [] robert)
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))

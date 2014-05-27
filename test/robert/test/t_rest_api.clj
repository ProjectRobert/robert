(ns robert.test.routes.t-rest.api
  (:use midje.sweet)
  (:require [robert.routes.rest-api :as r]))

(facts
 "Test the new-user resource"
 (facts
  "Test malformed check"
  (fact
   "Test the email params"
   ((:malformed? r/new-user)
    {:request {:json-params
               {"email" :email
                "password" :pass}}}) => false)
  ))
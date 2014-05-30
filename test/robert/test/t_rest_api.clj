(ns robert.test.routes.t-rest.api
  (:use midje.sweet)
  (:require [robert.routes.rest-api :as r]
            [robert.model.user :as user]))

(facts
 "Test the new-user resource"
 (facts
  "Test malformed? check"
  (fact
   "Test good well formed request"
   ((:malformed? r/new-user)
    {:request {:json-params
               {"email" :email
                "password" :pass}}}) => false)
  (fact
   "Test miss email"
   ((:malformed? r/new-user)
    {:request {:json-params
               {"password" :pass}}}) => (just true (contains {:malformed (contains {:message string?})})))
  (fact
   "Test miss password"
   ((:malformed? r/new-user)
    {:request {:json-params
               {"email" :email}}}) => (just true (contains {:malformed (contains {:message string?})}))))

 (facts
  "Test allowed? check"
  (fact
   "Test allowed call"
   ((:allowed? r/new-user)
    {:request {:json-params {"email" "foo"}}
     :user {:database "bar"}}) => true
     (provided (user/valid? "bar" {"email" "foo"}) => true))
  (fact
   "Email already taken"
   ((:allowed? r/new-user)
    {:request {:json-params {"email" "foo"}}
     :user {:database "bar"}})
   => (just false (contains {:forbidden (just {:email {"email" "foo"} :message string?})}))
     (provided (user/valid? "bar" {"email" "foo"}) => false))))
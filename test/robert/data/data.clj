(ns robert.data.data)

(def main-document
  {:name "robert"
   :option {}})

(def user
  [{:name "test" :password "test"}
   {:email "test@gmail.com" :password "test"}])

(def incompatible-user
  [{:name "test" :password "other-password"}
   {:email "test@gmail.com" :password "still-wrong"}
   {:name "good-name" :email "test@gmail.com" :password "repeat-email"}
   {:name "test" :email "goodemail@gmail.com" :password "repeat-name"}])

(def compatible-user
  [{:name "siscia" :email "siscia@gmail.com" :password "siscia"}
   {:name "foo" :password "bar"}
   {:email "bar@gmail.com" :password "foo"}])
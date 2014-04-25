(defproject robert "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.6"]
                 [ring/ring-core "1.2.2"]
                 [ring-basic-authentication "1.0.5"]
                 [com.novemberain/monger "1.7.0"]
                 [com.cemerick/friend "0.2.0"]
                 [clj-time "0.6.0"]
                 [lib-noir "0.8.1"]
                 [com.draines/postal "1.11.1"]
                 [liberator "0.11.0"]
                 [cheshire "5.3.1"]]
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler robert.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"})

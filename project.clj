(defproject jwt-spike "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[clj-jwt "0.0.10"]
                 [clj-time "0.8.0"]
                 [compojure "1.1.8"]
                 [org.clojure/clojure "1.6.0"]
                 [ring-basic-authentication "1.0.5"]]
  :plugins [[lein-ring "0.8.11"]]
  :ring {:handler jwt-spike.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})

(ns jwt-spike.handler
  (:require [clj-jwt.core :refer :all]
            [clj-jwt.key :refer [private-key]]
            [clj-time.core :refer [now plus days]]
            [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]))

(def secret "secret")

(defn create-claim
  [now]
  {:iss "api.example.com"
   :exp (plus now (days 1))
   :iat now})

(defn create-jwt-token
  [claim secret]
  (-> claim jwt (sign :HS256 secret) to-str))

(defn deserialize-and-verify-jwt-token
  [token secret]
  (let [jwt (str->jwt token)]
    (if-not (verify jwt secret)
      (throw (ex-info "Invalid JWT token" {:jwt jwt}))
      jwt)))

(defn authenticate
  [req]
  ;; TODO: wrap token in some JSON
  (create-jwt-token (create-claim (now)) secret))

(defn valid-credentials? [name pass]
  (and (= name "foo") (= pass "bar")))

(defroutes app-routes
  (POST "/authenticate" [] authenticate)
  (GET "/protected" [] (fn [req] (str "Super secret protected area! JWT: " (-> req :jwt :claims))))
  (route/resources "/")
  (route/not-found "Not Found"))

(defn wrap-basic-auth-around-authenticate
  [app]
  (let [guard (wrap-basic-authentication app valid-credentials?)]
    (fn [req]
      (if (and (= :post (:request-method req))
               (re-matches #"/authenticate" (:uri req)))
        (guard req)
        (app req)))))

(defn wrap-jwt-auth
  [app secret]
  (let [guard (fn [req]
                (try
                  (if-let [[[_ token]] (re-seq #"^Bearer\s+(.*)$" (get-in req [:headers "authorization"] ""))]
                    (let [jwt (deserialize-and-verify-jwt-token token secret)
                          req (assoc req :jwt jwt)]
                      (app req))
                    {:status 401 :headers {} :body "access denied (jwt 1)"})
                  (catch Exception e
                    {:status 401 :headers {} :body "access denied (jwt 2)"})))]
    (fn [req]
      (if (re-matches #"/protected" (:uri req))
        (guard req)
        (app req)))))

(def app
  (-> app-routes
      wrap-basic-auth-around-authenticate
      (wrap-jwt-auth secret)
      handler/api))

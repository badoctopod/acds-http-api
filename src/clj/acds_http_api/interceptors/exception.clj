(ns acds-http-api.interceptors.exception
  (:require
   [reitit.http.interceptors.exception :as exception]
   [taoensso.timbre :as timbre]))

(derive ::error ::exception)
(derive ::failure ::exception)
(derive ::horror ::exception)

(defn- handler [message exception request]
  {:status 500
   :body {:message message
          :exception (str exception)
          :uri (:uri request)}})

(def ex-handlers
  {::error (partial handler "error")
   ::exception (partial handler "exception")
   java.sql.SQLException (partial handler "sql-exception")
   ::exception/default (partial handler "default")
   ::exception/wrap (fn [handler e request]
                      (timbre/error (ex-message e))
                      (handler e request))})
(ns acds-http-api.interceptors.log
  (:require
   [taoensso.timbre :as timbre]))

(def log-interceptor
  {:name ::log-interceptor
   :enter (fn [ctx]
            (let [start-ms (System/currentTimeMillis)
                  new-ctx (assoc-in ctx [:request :start-ms] start-ms)]
              (timbre/info (select-keys (:request new-ctx) [:server-name
                                                            :server-port
                                                            :remote-addr
                                                            :uri
                                                            :query-string
                                                            :request-method
                                                            :headers]))
              new-ctx))
   :leave (fn [ctx]
            (let [duration-ms (- (System/currentTimeMillis)
                                 (get-in ctx [:request :start-ms]))
                  new-ctx (assoc-in ctx [:response :duration-ms] duration-ms)]
              (timbre/info (dissoc (:response new-ctx) :body))
              new-ctx))})
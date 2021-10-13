(ns acds-http-api.db
  (:require
   [next.jdbc.connection :as connection]
   [clojure.string :as str]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs])
  (:import
   [com.zaxxer.hikari HikariDataSource]))

(defn as-kebab-maps [rs opts]
  (let [kebab #(str/lower-case (str/replace % #"_" "-"))]
    (rs/as-unqualified-modified-maps rs (assoc opts :label-fn kebab))))

(defonce datasource (atom nil))

(defn start-connection-pool!
  [{:keys [pite]}]
  (let [ds (connection/->pool HikariDataSource pite)]
    (reset! datasource ds)
    (jdbc/execute! @datasource ["SELECT 1 FROM dual"])))

(defn stop-connection-pool!
  []
  (.close @datasource))

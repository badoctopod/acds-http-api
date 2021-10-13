(ns acds-http-api.core
  (:require
   [taoensso.timbre :as timbre]
   [taoensso.timbre.appenders.3rd-party.rotor :as appender]
   [hugsql.core :as hugsql]
   [hugsql.adapter.next-jdbc :as next-adapter]
   [acds-http-api.config :refer [config]]
   [acds-http-api.db :as db]
   [acds-http-api.http :as http])
  (:import
   [org.apache.commons.daemon Daemon DaemonContext])
  (:gen-class
   :implements [org.apache.commons.daemon.Daemon]
   :methods [^{:static true} [start ["[Ljava.lang.String;"] void]
             ^{:static true} [stop ["[Ljava.lang.String;"] void]]))

(defn configure-logging-backend
  [{:keys [log]}]
  (timbre/merge-config!
   {:min-level      (:level log)
    :timestamp-opts {:pattern (:pattern log)
                     :timezone (java.util.TimeZone/getTimeZone (:timezone log))}
    :appenders      {:rolling (appender/rotor-appender
                               {:path      (:path log)
                                :file-size (:file-size log)
                                :backlog   (:backlog log)})}}))

(defn start
  []
  (configure-logging-backend config)
  (hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc
                        {:builder-fn db/as-kebab-maps}))
  (timbre/infof "[APP] - Start initiated...")
  (try
    (db/start-connection-pool! config)
    (catch Exception e
      (timbre/errorf "Error starting DB connection pool: %s" (.getMessage e))))
  (timbre/infof "[WEBSERVER] - Starting...")
  (http/start-web-server! config)
  (timbre/infof "[WEBSERVER] - Start completed: %s" (str (:http config))))

(defn stop
  []
  (timbre/infof "[APP] - Shutdown initiated...")
  (http/stop-web-server! config)
  (timbre/infof "[WEBSERVER] - Shutdown completed...")
  (try
    (db/stop-connection-pool!)
    (catch Exception _
      (timbre/warnf "DB connection pool hasn't been started, nothing to stop")))
  (shutdown-agents))

(defn -start
  [this]
  (start))

(defn -stop
  [this]
  (stop))

(defn -main
  [& args]
  (start))

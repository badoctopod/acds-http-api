(ns acds-http-api.http
  (:require
   [org.httpkit.server :refer [run-server]]
   [acds-http-api.handler :as handler]))

(def webserver (atom nil))

(defn stop-web-server!
  [{:keys [http-misc]}]
  (when-not (nil? @webserver)
    (@webserver :timeout (:stop-timeout http-misc))
    (reset! webserver nil)))

(defn start-web-server!
  [{:keys [http]}]
  (reset! webserver (run-server #'handler/ring-handler http)))
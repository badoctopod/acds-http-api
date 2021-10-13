(ns acds-http-api.config
  (:require
   [cprop.core :refer [load-config]]
   [clojure.java.io :as io]
   [markdown.core :as md]))

(let [edn-conf (load-config)
      http-port (Integer. (or (System/getenv "PORT")
                              (get-in edn-conf [:http :port])))
      docs-md (slurp (io/resource "docs/docs.md"))
      docs-str (md/md-to-html-string docs-md :heading-anchors true)]
  (def config (-> (assoc edn-conf :docs docs-str)
                  (assoc-in [:http :port] http-port))))

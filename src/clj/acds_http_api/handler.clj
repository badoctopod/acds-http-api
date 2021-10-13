(ns acds-http-api.handler
  (:require
   [hiccup.page :refer [html5]]
   [reitit.ring :as ring]
   [reitit.http :as http]
   [reitit.coercion.spec]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.http.coercion :as coercion]
   [reitit.dev.pretty :as pretty]
   [reitit.interceptor.sieppari :as sieppari]
   [reitit.http.interceptors.parameters :as parameters]
   [reitit.http.interceptors.muuntaja :as muuntaja]
   [reitit.http.interceptors.exception :as exception]
   [reitit.http.interceptors.multipart :as multipart]
   [acds-http-api.interceptors.log :refer [log-interceptor]]
   [acds-http-api.interceptors.exception :refer [ex-handlers]]
   [acds-http-api.interceptors.gzip :refer [gzip-interceptor]]
   [acds-http-api.interceptors.cors :refer [cors-interceptor]]
   [muuntaja.core :as m]
   [acds-http-api.config :refer [config]]
   [acds-http-api.db :refer [datasource]]
   [acds-http-api.queries :as q]
   [acds-http-api.spec.common :as common-spec]
   [acds-http-api.spec.mining-results :as mr-spec]
   [acds-http-api.spec.hourly-rates :as hr-spec]))

(def union-worktypes
  (vec (apply clojure.set/union (map :worktypes
                                     (get-in config [:params
                                                     :reports
                                                     :mining-results
                                                     :base-indicators])))))

(defn calc-base-indicators
  [data]
  (map (fn [{:keys [indicator worktypes]}]
         (let [data (filter (comp worktypes :worktype) data)]
           {:indicator indicator
            :weight (reduce + 0 (map :weight data))
            :volume (reduce + 0 (map :volume data))}))
       (get-in config [:params :reports :mining-results :base-indicators])))

(defn calc-indicator-value
  [uom data]
  (case uom
    "тыс. м3" (with-precision 4 (/ (reduce + 0 (map :volume data))
                                   1000))
    "тыс. тн" (with-precision 4 (/ (reduce + 0 (map :weight data))
                                   1000))
    0))

(defn calc-mining-indicators
  [date]
  (let [raw-data (q/get-mining-results
                  @datasource
                  {:report-date date
                   :worktypes union-worktypes})
        data (calc-base-indicators raw-data)]
    (flatten (map (fn [{:keys [indicator location base description uom row-code]}]
                    {:report-date date
                     :company "RUKGK"
                     :indicator indicator
                     :location location
                     :description description
                     :uom uom
                     :row-code row-code
                     :value (calc-indicator-value uom (filter (comp (set base)
                                                                    :indicator)
                                                              data))})
                  (get-in config [:params
                                  :reports
                                  :mining-results
                                  :report-indicators])))))

(defn calc-ore-crushing-indicators
  [date]
  (let [raw-data (q/get-ore-crushing-results
                  @datasource
                  {:report-date date
                   :conveyor-tags (get-in config [:params
                                                  :reports
                                                  :ore-crushing-results
                                                  :conveyor-tags])})
        data (map (fn [m]
                    (update m :weight #(if (neg? %) 0 %)))
                  raw-data)]
    (flatten (map (fn [{:keys [indicator base description uom row-code]}]
                    {:report-date date
                     :company "RUKGK"
                     :indicator indicator
                     :location nil
                     :description description
                     :uom uom
                     :row-code row-code
                     :value (calc-indicator-value uom (filter (comp (set base)
                                                                    :conveyor-tag)
                                                              data))})
                  (get-in config [:params
                                  :reports
                                  :ore-crushing-results
                                  :report-indicators])))))

(defn calc-all-indicators
  [date]
  (->> (conj (calc-mining-indicators date) (calc-ore-crushing-indicators date))
       (flatten)
       (into [])
       (assoc {} :rows)))

(defn fetch-conveyor-weigher-log
  [request-date]
  (q/get-conveyor-weigher-log @datasource {:request-date request-date
                                           :conveyor-tags (get-in config [:params
                                                                          :conveyors
                                                                          :hourly-rates
                                                                          :conveyor-tags])}))

(def discret-time [{:date-end "01:00:00" :shift 1}
                    {:date-end "02:00:00" :shift 1}
                    {:date-end "03:00:00" :shift 1}
                    {:date-end "04:00:00" :shift 1}
                    {:date-end "05:00:00" :shift 1}
                    {:date-end "06:00:00" :shift 1}
                    {:date-end "07:00:00" :shift 1}
                    {:date-end "08:00:00" :shift 1}
                    {:date-end "09:00:00" :shift 2}
                    {:date-end "10:00:00" :shift 2}])

(def tags [{:tag "01DF.CV07.WT01" :ordering 1 :description "Конвейер № 7"}
           {:tag "01DF.CV15.WT01" :ordering 2 :description "Конвейер № 15"}
           {:tag "02US.CV01.WT01" :ordering 3 :description "Конвейер УС-1"}
           {:tag "02US.CV04.WT01" :ordering 4 :description "Конвейер УС-4"}
           {:tag "03ASHR.CV0231.WT01" :ordering 5 :description "Конвейер № 2.3.1"}
           {:tag "03ASHR.CV031.WT01" :ordering 6 :description "Конвейер № 3.1"}
           {:tag "04SUSH.CV60.WT01" :ordering 7 :description "Конвейер № 60"}
           {:tag "04SUSH.CV62.WT01" :ordering 8 :description "Конвейер № 62"}
           {:tag "04SUSH.CV66.WT01" :ordering 9 :description "Конвейер № 66"}
           {:tag "05ABOF.CV04.WT01" :ordering 10 :description "Конвейер № 4"}
           {:tag "05ABOF.CV05a.WT01" :ordering 11 :description "Конвейер № 5а"}
           {:tag "05ABOF.CV05.WT01" :ordering 12 :description "Конвейер № 5"}])

(defn gen-hourly-rates-sample
  [request-date]
  (mapcat (fn [{:keys [tag ordering description]}]
            (map (fn [{:keys [date-end shift]}]
                   {:tag tag
                    :date-end (str request-date " " date-end)
                    :shift shift
                    :description description
                    :hour-weight (rand-int 4000)
                    :ordering ordering})
                 discret-time))
          tags))

(def ring-handler
  (http/ring-handler
   (http/router
    [["/swagger.json"
      {:get {:no-doc true
             :swagger (let [swagger (get-in config [:api-description :swagger])
                            host (str (get-in config [:http :ip])
                                      ":"
                                      (get-in config [:http :port]))
                            docs-url (str "http://" host "/docs")
                            version (str (get-in config [:api-instance :instance])
                                         ":"
                                         (get-in config [:api-instance :version]))]
                        (-> swagger
                            (update :host (constantly host))
                            (update :basePath (constantly ""))
                            (update-in [:externalDocs :url] (constantly docs-url))
                            (update-in [:info :version] (constantly version))))
             :handler (swagger/create-swagger-handler)}}]
     ["/public/*"
      {:get {:no-doc true
             :handler (ring/create-resource-handler)}}]
     ["/docs"
      {:get
       {:no-doc true
        :handler (fn [_]
                   {:status  200
                    :headers {"Content-Type" "text/html; charset=utf-8"}
                    :body    (html5
                              {:lang "ru"}
                              [:head
                               [:meta {:charset :utf-8}]
                               [:meta {:name    "viewport"
                                       :content "width=device-width, initial-scale=1.0, shrink-to-fit=no"}]
                               [:title "ИС \"Карьер\".HTTP API: интеграционный интерфейс. Сопроводительная документация"]
                               [:link {:rel "stylesheet" :href "public/css/style.min.css"}]]
                              [:body
                               (get-in config [:docs])
                               [:script {:src "public/js/highlight.min.js"}]
                               [:script "hljs.initHighlightingOnLoad();"]])})}}]
     ["/api"
      ["/conveyors"
       {:swagger {:tags ["conveyors"]}}
       ["/hourly-rates"
        {:get {:summary (get-in config [:api-description :conveyors-hourly-rates :summary])
               :description (get-in config [:api-description :conveyors-hourly-rates :description])
               :parameters {:query {:request-date ::common-spec/date}}
               :responses {200 {:body ::hr-spec/rows}}
               :handler (fn [{{{:keys [request-date]} :query} :parameters}]
                          #(future %)
                          {:status 200
                           :body (fetch-conveyor-weigher-log request-date)})}}]
       ["/hourly-rates-sample"
        {:get {:summary (get-in config [:api-description :conveyors-hourly-rates :summary])
               :description (get-in config [:api-description :conveyors-hourly-rates :description])
               :parameters {:query {:request-date ::common-spec/date}}
               :responses {200 {:body coll?}}
               :handler (fn [{{{:keys [request-date]} :query} :parameters}]
                          #(future %)
                          {:status 200
                           :body (gen-hourly-rates-sample request-date)})}}]]
      ["/reports"
       {:swagger {:tags ["reports"]}}
       ["/mining-results"
        {:get {:summary (get-in config [:api-description :mining-results :summary])
               :description (get-in config [:api-description :mining-results :description])
               :parameters {:query {:date ::common-spec/date}}
               :responses {200 {:body {:rows ::mr-spec/rows}}}
               :handler (fn [{{{:keys [date]} :query} :parameters}]
                          #(future %)
                          {:status 200
                           :body (calc-all-indicators date)})}}]]]]
    {:exception pretty/exception
     :data {:coercion reitit.coercion.spec/coercion
            :muuntaja (m/create
                       (-> m/default-options
                           (update
                            :formats
                            select-keys
                            ["application/json"
                             "application/edn"])
                           (assoc :default-format "application/json")))
            :interceptors [swagger/swagger-feature
                           log-interceptor
                           gzip-interceptor
                           cors-interceptor
                           (parameters/parameters-interceptor)
                           (muuntaja/format-negotiate-interceptor)
                           (muuntaja/format-response-interceptor)
                           (exception/exception-interceptor
                            (merge
                             exception/default-handlers
                             ex-handlers))
                           (muuntaja/format-request-interceptor)
                           (coercion/coerce-response-interceptor)
                           (coercion/coerce-request-interceptor)
                           (multipart/multipart-interceptor)]}})
   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path "/"
      :config {:validatorUrl nil
               :operationsSorter "alpha"
               :displayRequestDuration false
               :docExpansion "list"
               :filter true
               :showExtensions true
               :showCommonExtensions true}})
    (ring/create-default-handler))
   {:executor sieppari/executor}))

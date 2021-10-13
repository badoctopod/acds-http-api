(ns acds-http-api.interceptors.cors)

(def cors-interceptor
  {:name ::cors-interceptor
   :leave (fn [ctx]
            (-> ctx
                (update-in [:response :headers]
                           #(-> % 
                                (assoc "Access-Control-Allow-Origin" "*"
                                       "Access-Control-Allow-Methods" "GET, POST, OPTIONS"
                                       "Access-Control-Allow-Headers" "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range"
                                       "Access-Control-Expose-Headers" "Content-Length,Content-Range"
                                       "Access-Control-Max-Age" 1728000)))))})
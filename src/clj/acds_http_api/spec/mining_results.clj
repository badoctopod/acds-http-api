(ns acds-http-api.spec.mining-results
  (:require
   [clojure.spec.alpha :as s]))

(s/def ::indicator string?)

(s/def ::location (s/nilable string?))

(s/def ::description string?)

(s/def ::uom string?)

(s/def ::value number?)

(s/def ::report-date string?)

(s/def ::company string?)

(s/def ::row-code string?)

(s/def ::row (s/keys :req-un [::row-code
                              ::report-date
                              ::company
                              ::indicator
                              ::location
                              ::description
                              ::uom
                              ::value]))

(s/def ::rows (s/coll-of ::row :kind vector?))













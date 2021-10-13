(ns acds-http-api.spec.hourly-rates
  (:require
   [clojure.spec.alpha :as s]))

(s/def ::ordering number?)

(s/def ::description string?)

(s/def ::department string?)

(s/def ::site string?)

(s/def ::tag (s/nilable string?))

(s/def ::date-begin (s/nilable string?))

(s/def ::date-end (s/nilable string?))

(s/def ::shift (s/nilable number?))

(s/def ::hour-weight (s/nilable number?))

(s/def ::row (s/keys :req-un [::ordering
                              ::description
                              ::department
                              ::site
                              ::tag
                              ::date-begin
                              ::date-end
                              ::shift
                              ::hour-weight]))

(s/def ::rows (s/coll-of ::row :kind vector?))
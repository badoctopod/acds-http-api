(ns acds-http-api.spec.common
  (:require
   [clojure.spec.alpha :as s]
   [spec-tools.core :as st]))

(defn proper-date?
  [s]
  (try
    (let [format (java.time.format.DateTimeFormatter/ofPattern "dd.MM.yyyy")]
      (java.time.LocalDate/parse s format)
      true)
    (catch java.time.format.DateTimeParseException _
      false)))

(s/def ::date
  (st/spec
   {:spec #(proper-date? %)
    :name "date"
    :description "Date in the format of DD.MM.YYYY"
    :reason "Illegal date format, use DD.MM.YYYY, example 06.01.2021"
    :swagger/example "01.01.2021"
    :swagger/default "01.01.2021"
    :json-schema/default "01.01.2021"}))
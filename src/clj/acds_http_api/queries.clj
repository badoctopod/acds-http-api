(ns acds-http-api.queries
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "sql/queries.sql" {:quoting :ansi})

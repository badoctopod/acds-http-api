(defproject acds_http_api "2.0.0"
  :description "ACDS: HTTP API"

  :dependencies [[org.clojure/clojure "1.10.3"]
                 [cprop "0.1.19"]
                 [seancorfield/next.jdbc "1.2.659"]
                 [com.zaxxer/HikariCP "5.0.0"]
                 [com.oracle.jdbc/ojdbc8 "12.2.0.1"]
                 [com.layerware/hugsql "0.5.1"]
                 [com.layerware/hugsql-adapter-next-jdbc "0.5.1"]
                 [http-kit "2.5.3"]
                 [javax.servlet/javax.servlet-api "4.0.1"]
                 [metosin/reitit "0.5.15"]
                 [hiccup "1.0.5"]
                 [markdown-clj "1.10.6"]
                 [com.taoensso/timbre "5.1.2"]
                 [org.slf4j/slf4j-api "1.7.32"]
                 [com.fzakaria/slf4j-timbre "0.3.21"]
                 [commons-daemon/commons-daemon "1.2.4"]]

  :repositories [["XWiki External Repository" "https://maven.xwiki.org/externals/"]]

  :jvm-opts ["-Xmx2g"
             "-server"
             "-Dconf=config.edn"
             "-Duser.timezone=Europe/Moscow"]

  :global-vars {*warn-on-reflection* true}

  :plugins [[lein-ancient "0.6.15"]
            [lein-kibit "0.1.8"]]

  :min-lein-version "2.8.1"

  :source-paths ["src/clj"]

  :test-paths ["test/clj"]

  :clean-targets ^{:protect false} [:target-path :compile-path]

  :main acds-http-api.core

  :profiles {:dev     {:prep-tasks   ["clean"]}
             :uberjar {:uberjar-name "acds-http-api.jar"
                       :source-paths ^:replace ["src/clj"]
                       :prep-tasks   ["compile"]
                       :hooks        []
                       :omit-source  true
                       :aot          :all}})

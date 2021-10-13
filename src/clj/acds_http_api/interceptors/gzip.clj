(ns acds-http-api.interceptors.gzip
  (:require [clojure.java.io :as io]
            clojure.reflect)
  (:import (java.util.zip GZIPOutputStream)
           (java.io InputStream
                    OutputStream
                    Closeable
                    File
                    PipedInputStream
                    PipedOutputStream)))

; Convert https://github.com/clj-commons/ring-gzip-middleware to interceptor

; only available on JDK7
(def ^:private flushable-gzip?
  (delay (->> (clojure.reflect/reflect GZIPOutputStream)
              :members
              (some (comp '#{[java.io.OutputStream boolean]} :parameter-types)))))

; only proxying here so we can specialize io/copy (which ring uses to transfer
; InputStream bodies to the servlet response) for reading from the result of
; piped-gzipped-input-stream
(defn- piped-gzipped-input-stream*
  []
  (proxy [PipedInputStream] []))

; exactly the same as do-copy for [InputStream OutputStream], but
; flushes the output on every chunk; this allows gzipped content to start
; flowing to clients ASAP (a reasonable change to ring IMO)
(defmethod @#'io/do-copy [(class (piped-gzipped-input-stream*)) OutputStream]
  [^InputStream input ^OutputStream output opts]
  (let [buffer (make-array Byte/TYPE (or (:buffer-size opts) 1024))]
    (loop []
      (let [size (.read input buffer)]
        (when (pos? size)
          (do (.write output buffer 0 size)
              (.flush output)
              (recur)))))))

(defn- piped-gzipped-input-stream
  [in]
  (let [pipe-in (piped-gzipped-input-stream*)
        pipe-out (PipedOutputStream. pipe-in)]
    ; separate thread to prevent blocking deadlock
    (future
      (with-open [out (if @flushable-gzip?
                        (GZIPOutputStream. pipe-out true)
                        (GZIPOutputStream. pipe-out))]
        (if (seq? in)
          (doseq [string in]
            (io/copy (str string) out)
            (.flush out))
          (io/copy in out)))
      (when (instance? Closeable in)
        (.close ^Closeable in)))
    pipe-in))

(defn- gzipped-response
  [ctx]
  (-> ctx
      (update-in [:response :headers]
                 #(-> %
                      (assoc "Content-Encoding" "gzip")
                      (dissoc "Content-Length")))
      (update-in [:response :body] piped-gzipped-input-stream)))

(def gzip-interceptor
  {:name ::gzip-interceptor
   :leave (fn [ctx]
            (if (and (not (get-in ctx [:response :headers "Content-Encoding"]))
                     (or
                      (and (string? (get-in ctx [:response :body]))
                           (> (count (get-in ctx [:response :body])) 200))
                      (and (seq? (get-in ctx [:response :body]))
                           @flushable-gzip?)
                      (instance? InputStream (get-in ctx [:response :body]))
                      (instance? File (get-in ctx [:response :body]))))
              (let [accepts (get-in ctx [:request :headers "accept-encoding"] "")
                    match (re-find #"(gzip|\*)(;q=((0|1)(.\d+)?))?" accepts)]
                (if (and match (not (contains? #{"0" "0.0" "0.00" "0.000"}
                                               (match 3))))
                  (gzipped-response ctx)
                  ctx))
              ctx))})
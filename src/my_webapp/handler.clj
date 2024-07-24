(ns my-webapp.handler
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.logging :as log])
  (:import [java.net ServerSocket]
           [java.net SocketException]
           [java.nio.file Files]
           [java.io File]
           [java.io InputStream OutputStream]))

(def responses {200 "HTTP/1.1 200 OK\r\n"
                301 "HTTP/1.1 301 Moved Permanently\n"
                404 "HTTP/1.1 404 Not Found\r\n"})

(defn stream-bytes [is]
  (let [baos (java.io.ByteArrayOutputStream.)]
    (io/copy is baos)
    (.toByteArray baos)))

(defprotocol StreamableResponseBody
  (write-body-to-stream [body response output-stream]))

(extend-protocol StreamableResponseBody
  (Class/forName "[B")
  (write-body-to-stream [body _ ^OutputStream output-stream]
    (.write output-stream ^bytes body)
    (.close output-stream))
  String
  (write-body-to-stream [body _ output-stream]
    (.write output-stream (.getBytes body))
    (.close output-stream))
  InputStream
  (write-body-to-stream [body _ ^OutputStream output-stream]
    (.write output-stream (stream-bytes body))
    (.flush output-stream)
    (.close output-stream))
  File
  (write-body-to-stream [body _ ^OutputStream output-stream]
    (log/debug "write-body-to-stream" (.getName body))
    (.write output-stream (Files/readAllBytes (.toPath body)))
    (.flush output-stream)
    (.close output-stream)))

(defn init-request-map [conn]
  {:server-port (.getLocalPort conn) :server-name (.getInetAddress conn) :remote-addr (.getRemoteSocketAddress conn)})

(defn parse-request [conn]
  (let [r (io/reader (.getInputStream conn))]
    (loop [line (.readLine r)
           request (init-request-map conn)]
      (if (seq (str/trim line))
        (if (str/starts-with? line "GET")
          (let [[request-method uri protocol] (str/split line #" ")]
            (log/info line)
            (recur (.readLine r) (assoc request :request-method request-method :uri uri :protocol protocol)))
          (let [[k v] (str/split line #":")]
            (recur (.readLine r) (assoc request :headers {k v}))))
        request))))

(defn write-headers [response output-stream]
  (.write output-stream (into-array Byte/TYPE (str
                                               (get responses (:status response))
                                               (apply str (for [[k v] (:headers response)] (str k " " v "\r\n")))
                                               "\r\n")))
  output-stream)

(defn send-response [conn response]
  (cond->> (write-headers response (.getOutputStream conn))
    (:body response) (write-body-to-stream (:body response) response)))

(defn tcp-listener [server f]
  (future
    (try
      (while (not (.isClosed server))
        (loop [conn (.accept server)]
          (let [request (parse-request conn)]
            (send-response conn (f request)))
          (recur (.accept server))))
      (catch SocketException e {:msg (.getMessage e)}))))

(defn run-adapter [handler options]
  (let [server (ServerSocket. (:port options))]
    (tcp-listener server handler)
    (fn close [] (.close server))))

;; example handler (serving static resources)

(defn old-handler [request]
  (let [redirects {"/" "index.html"
                   "/index.htm" "index.html"}
        base-path "/tmp/resources"]
    (cond
      (contains? redirects (:uri request)) {:status 301
                                            :headers {"Location:" (get redirects (:uri request))}}
      (.exists (io/file (str base-path (:uri request)))) (let [file (io/file (str base-path (:uri request)))
                                                               content-type (Files/probeContentType (.toPath file))]
                                                           {:status 200
                                                            :headers {"Content-Type:" content-type}
                                                            :body file})

      :else {:status 404
             :headers {"Content-Type:" "text/html"}
             :body "<html>404</html>"})))

(defn handler [_request]
  {:status 200
   :headers {"Content-Type:" "text/html"}
   :body "Hello world"})

(comment
  (run-adapter handler {:port 3000}))
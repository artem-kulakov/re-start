(ns my-webapp.mailer
  (:require [aero.core :as aero]
            [postal.core :as postal]
            [clojure.java.io :as io]))

(def config (aero/read-config (io/resource "config.edn")))

(defn send-message
  [to subject body]
  (postal/send-message {:host (:smtp-host config)
                        :port (parse-long (:smtp-port config))
                        :auth "on"
                        :user (:smtp-user config)
                        :pass (:smtp-pass config)}
                       {:from "me@draines.com"
                        :to to
                        :subject subject
                        :body body}))

(comment
  (send-message "artem.kulakov@gmail.com" "Hello" "Hello Artem"))

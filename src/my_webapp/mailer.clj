(ns my-webapp.mailer
  (:require [aero.core :as aero]
            [postal.core :as postal]
            [clojure.java.io :as io]))

(def config (aero/read-config (io/resource "config.edn")))

(defn send-message
  []
  (postal/send-message {:host (:smtp-host config)
                        :port (parse-long (:smtp-port config))
                        :auth "on"
                        :user (:smtp-user config)
                        :pass (:smtp-pass config)}
                       {:from "me@draines.com"
                        :to "artem.kulakov@gmail.com"
                        :subject "Fourth message"
                        :body "Salut Artem! Comment ca va?"}))

(comment
  (send-message))

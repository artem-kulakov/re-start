(ns frontend)

(defn init
  []
  (println "Hello World"))

(def url (new js/URL (.. js/window -location -href)))

(def id (aget (.split (.-pathname url) "/") 2))

(def socket (new js/WebSocket (str "http://localhost:3000/echo?id=" id)))

(.addEventListener socket "open" (fn [_e] (.send socket "Hello Server!")))

(.addEventListener
 socket
 "message"
 (fn
   [_e]
   (let [el (.getElementsByClassName js/document "alert")]
     (.log js/console (.-classList el))
     (.remove (.-classList el) "d-none"))
   (.log js/console "Message from server: " (.-data js/event))))

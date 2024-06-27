(ns build
  (:require [clojure.tools.build.api :as b]))

;; the main namespace in your application:
(def main-ns 'my-webapp.handler)
;; where to compile your application:
(def class-dir "target/classes")
;; where to create the uberjar file:
(def uber-file "target/my-webapp.jar")

;; "basis" is a description of your project, as data, that includes
;; details about the paths and dependencies (libraries) it uses:
(def basis (b/create-basis {:project "deps.edn"}))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis
           :main main-ns}))

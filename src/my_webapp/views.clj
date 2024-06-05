;; src/my_webapp/views.clj
(ns my-webapp.views
  (:require [hiccup.page :as page]
            [my-webapp.db :as db]
            [ring.util.anti-forgery :as util]))




(defn app
  []
  (let [all-items (db/get-all-items)]
    (page/html5
    [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport", :content "width=device-width, initial-scale=1"}]
      [:title "Hello Bulma!"]
      [:link {:rel "stylesheet", :href "https://cdn.jsdelivr.net/npm/bulma@1.0.0/css/bulma.min.css"}]]
    [:body
      [:section {:class "section"}
      [:div {:class "container"}
       [:div {:class "columns mt-2"}
        [:div {:class "column is-narrow"}
          (for [item all-items]
            [:div {:class "is-flex is-justify-content-space-between"}
            [:div {:class "pb-4 pr-6"}
              [:p (:ITEMS/NAME item)]]
            [:div
              [:input {:type "checkbox", (when (:ITEMS/COMPLETE item) :checked) ""}]]]
            )]]]]])))




(defn gen-page-head
  [title]
  [:head
   [:title (str "Locations: " title)]
   (page/include-css "/css/styles.css")])

(def header-links
  [:div#header-links
   "[ "
   [:a {:href "/"} "Home"]
   " | "
   [:a {:href "/add-location"} "Add a Location"]
   " | "
   [:a {:href "/all-locations"} "View All Locations"]
   " ]"])

(defn home-page
  []
  (page/html5
   (gen-page-head "Home")
   header-links
   [:h1 "Home"]
   [:p "Webapp to store and display some 2D (x,y) locations."]))

(defn add-location-page
  []
  (page/html5
   (gen-page-head "Add a Location")
   header-links
   [:h1 "Add a Location"]
   [:form {:action "/add-location" :method "POST"}
    (util/anti-forgery-field)
    [:p "x value: " [:input {:type "text" :name "x"}]]
    [:p "y value: " [:input {:type "text" :name "y"}]]
    [:p [:input {:type "submit" :value "submit location"}]]]))

(defn add-location-results-page
  [{:keys [x y]}]
  (let [{id :LOCATIONS/ID} (db/add-location-to-db x y)]
    (page/html5
     (gen-page-head "Added a Location")
     header-links
     [:h1 "Added a Location"]
     [:p "Added [" x ", " y "] (id: " id ") to the db. "
      [:a {:href (str "/location/" id)} "See for yourself"]
      "."])))

(defn location-page
  [loc-id]
  (let [{x :LOCATIONS/X y :LOCATIONS/Y} (db/get-xy loc-id)]
    (page/html5
     (gen-page-head (str "Location " loc-id))
     header-links
     [:h1 "A Single Location"]
     [:p "id: " loc-id]
     [:p "x: " x]
     [:p "y: " y])))

(defn all-locations-page
  []
  (let [all-locs (db/get-all-locations)]
    (page/html5
     (gen-page-head "All Locations in the db")
     header-links
     [:h1 "Add a Location"]
     [:form {:action "/add-location" :method "POST"}
      (util/anti-forgery-field)
      [:p "x value: " [:input {:type "text" :name "x"}]]
      [:p "y value: " [:input {:type "text" :name "y"}]]
      [:p [:input {:type "submit" :value "submit location"}]]]
     [:h1 "All Locations"]
     [:table
      [:tr [:th "id"] [:th "x"] [:th "y"]]
      (for [loc all-locs]
        [:tr
         [:td (:LOCATIONS/ID loc)]
         [:td (:LOCATIONS/X loc)]
         [:td (:LOCATIONS/Y loc)]])])))

(ns my-webapp.views
  (:require [hiccup.page :as page]
            [ring.util.anti-forgery :as util]))

(defn page-head
  ([title styles]
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:title title]
     [:link
      {:href
       "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
       :rel "stylesheet"
       :integrity
       "sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH"
       :crossorigin "anonymous"}]
     (page/include-css (str "/css/" styles ".css"))
     [:script
      {:src
       "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"
       :integrity
       "sha384-YvpcrYf0tY3lHB60NNkmXc5s9fDVZLESaAA55NDzOxhy9GkcIdslK1eN7N6jIeHz"
       :crossorigin "anonymous"}]])
  ([title] (page-head title "styles")))

(defn nav
  ([page]
  [:div.row.my-2
   [:div.col
    [:div.btn-group
      [:a.btn.btn-light {:href "/lists" :class (when (= page "lists") "disabled")} "Lists"]
      [:a.btn.btn-light {:href "/logout"} "Logout"]]]])
  ([] (nav "")))

(def colors
  ["blue" "yellow" "indigo" "green" "red" "teal" "orange" "cyan" "pink"])

(defn list-page
  [list items]
  (page/html5
   (page-head (:name list))
   [:body.bg-danger.bg-gradient
    [:div.container
     (nav)
     [:div.row
      [:div.col.d-flex.flex-row.justify-content-between
       [:form {:method "POST", :action (str "/lists/" (:id list) "/add-item")}
        (util/anti-forgery-field)
        [:div.input-group.mb-2
          [:input.form-control
            {:type "text",
            :name "name",
            :value "",
            :placeholder "Enter the product name"}]
            [:input.btn.btn-secondary {:type "submit", :value "Add"}]]]
       [:form
        {:method "POST", :action (str "/lists/" (:id list) "/sort-items")}
        (util/anti-forgery-field)
        [:input.btn.btn-secondary {:type "submit", :value "Sort"}]]]]
     [:div.row
      [:div.col
       [:ul.list-group.mb-2
        (for [item items]
          [:form {:method "POST", :action "/toggle-item-complete"}
            (util/anti-forgery-field)
            [:li.list-group-item {:onclick "this.closest('form').submit()"}
              [:span
                {:class
                 (str
                  (when (:complete item) "crossed-out")
                  " "
                  (nth colors (mod (:id item) 9)))}
                (:name item)]
              [:input {:type "hidden", :name "id", :value (:id item)}]
              [:input {:type "hidden", :name "complete", :value (str (:complete item))}]]])]]]]]))

(defn lists
  [lists]
  (page/html5
   (page-head (:name list))
   [:body.bg-success.bg-gradient
    [:div.container
     (nav "lists")
     [:div.row
      [:div.col.d-flex.flex-row.justify-content-between
       [:form {:method "POST", :action "/add-list"}
        (util/anti-forgery-field)
        [:div.input-group.mb-3
         [:input.form-control
          {:type "text",
           :name "name",
           :value "",
           :placeholder "Enter the list name"}]
         [:input.btn.btn-danger {:type "submit", :value "Add"}]]]]]
     [:div.row
      [:div.col
       [:ul.list-group
        (for [list lists]
           [:li.list-group-item
            [:a.link-underline-light.link-dark {:href (str "lists/" (:id list))} (:name list)]])]]]]]))

(defn login
  []
  (page/html5
    (page-head (:name "Login") "login")
    [:body.d-flex.align-items-center.bg-danger.bg-gradient
     [:div.container
      [:div.row
       [:div.col
        [:form
        {:method "post"}
          (util/anti-forgery-field)
          [:div.input-group
            [:input.form-control {:type "text", :placeholder "Name:", :name "name"}]
            [:input.form-control {:type "password", :placeholder "Password:", :name "password"}]
            [:input.btn.btn-secondary {:type "submit", :value "Sign in"}]]]]]]]))

(defn error
  []
  (page/html5
   [:h1 "Error"]))

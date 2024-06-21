(ns my-webapp.views
  (:require [hiccup.page :as page]
            [ring.util.anti-forgery :as util]))

(defn page-head
  [title]
    [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport", :content "width=device-width, initial-scale=1"}]
      [:title title]
      [:link {:rel "manifest", :href "/manifest.webmanifest"}]
      [:script {:src "https://unpkg.com/htmx.org@1.9.12"}]
      (page/include-css "/css/bulma.min.css")
      (page/include-css "/css/styles.css")])

(defn lists
  [lists]
  (page/html5
   (page-head "My lists")
    [:body
    [:section {:class "section"}
      [:div {:class "container"}
      [:div {:class "columns"}
        [:div {:class "column"}
        [:p [:a {:href "/logout"} "Logout"]]]]
      [:div {:class "columns"}
        [:div {:class "column is-narrow"}
        [:form {:method "POST", :action "/add-list"}
          [:div {:class "field has-addons"}
          [:div {:class "control"}
            (util/anti-forgery-field)
            [:input
            {:class "input",
              :type "text",
              :name "name",
              :value "",
              :placeholder "Enter the list name"}]]
          [:div {:class "control"}
            [:input {:type "submit", :class "button is-link", :value "Add"}]]]]]]
      [:div {:class "columns"}
        [:div {:class "column is-narrow"}
        (for [list lists]
          [:div {:class "is-flex is-justify-content-space-between"}
            [:div {:class "pb-4 pr-6"}
             [:p [:a {:href (str "lists/" (:id list))} (:name list)]]]])]]]]]))

(defn list-page
  [list items]
  (page/html5
   (page-head name)
   [:body
    [:section {:class "section"}
     [:div {:class "container"}
      [:div {:class "columns"}
       [:div {:class "column is-narrow"}
        [:h1 (:name list)]
        ]]
      [:div {:class "columns"}
       [:div {:class "column is-narrow"}
        [:form {:method "POST", :action (str "/lists/" (:id list) "/add-item")}
         [:div {:class "field has-addons"}
          [:div {:class "control"}
           (util/anti-forgery-field)
           [:input
            {:class "input",
             :type "text",
             :name "name",
             :value "",
             :placeholder "Enter the product name"}]]
          [:div {:class "control"}
           [:input {:type "submit", :class "button is-link", :value "Add"}]]]]]
       [:div {:class "column"}
        [:form
         {:method "POST", :action (str "/lists/" (:id list) "/sort-items")}
         (util/anti-forgery-field)
         [:input {:type "submit", :class "button is-info", :value "Sort"}]]]]
      [:div {:class "columns"}
        [:div {:class "column is-narrow"}
          (for [item items]
            [:div {:class "is-flex is-justify-content-space-between"}
            [:div {:class "pb-4 pr-6"}
              [:p
              {:class (when (:complete item) "crossed-out")}
              (:name item)]]
            [:div
              [:form
              {:method "POST", :action "/toggle-item-complete"}
              (util/anti-forgery-field)
              [:input {:type "hidden", :name "id", :value (:id item)}]
              [:input {:type "hidden", :name "complete", :value (str (:complete item))}]
              [:input
                {:type "checkbox",
                (when (:complete item) :checked) ""
                :onchange "this.form.submit()"}]]]])]]]]]))

(defn login
  []
  (page/html5
    [:h1 "Login Page"]
    [:form
    {:method "post"}
     (util/anti-forgery-field)
    [:input {:type "text", :placeholder "Name:", :name "name"}]
    [:input
      {:type "password", :placeholder "Password:", :name "password"}]
    [:input {:type "submit", :value "Submit"}]]))

(defn error
  []
  (page/html5
   [:h1 "Error"]))

(ns my-webapp.views
  (:require [hiccup.page :as page]
            [ring.util.anti-forgery :as util]))

(defn page-head
  [title & [styles]]
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
     (page/include-css (str "/css/" (or styles "styles") ".css"))
     [:link {:rel "manifest", :href "/manifest.webmanifest"}]
     [:script
      {:src
       "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"
       :integrity
       "sha384-YvpcrYf0tY3lHB60NNkmXc5s9fDVZLESaAA55NDzOxhy9GkcIdslK1eN7N6jIeHz"
       :crossorigin "anonymous"}]])

(defn nav
  [& [page]]
  [:div.row.my-2
   [:div.col
    [:div.btn-group
      [:a.btn.btn-light {:href "/lists" :class (when (= (or page "") "lists") "disabled")} "Lists"]
      [:a.btn.btn-light {:href "/logout"} "Logout"]]]])

(def colors
  ["blue" "yellow" "indigo" "green" "red" "teal" "orange" "cyan" "pink"])

(defn alert
  [flash]
  (when flash [:div.alert.alert-warning.mt-2 {:role "alert"}
          flash]))

(defn list-page
  [list items flash]
  (page/html5
   (page-head (:name list))
   [:body.bg-danger.bg-gradient
    [:div.container
     (alert flash)
     [:div.alert.alert-warning.mt-2.d-none {:role "alert"} "foo"]
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
        [:input.btn.btn-secondary {:type "submit", :value "Sort"}]]
       [:div
        [:a.btn.btn-secondary {:href (str "/contacts/new?list-id=" (:id list))} "Share"]]]]
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
              [:input {:type "hidden", :name "complete", :value (str (:complete item))}]]])]]]]
    [:script {:src "/js/client.js"}]]))

(defn lists
  [lists flash]
  (page/html5
   (page-head (:name list))
   [:body.bg-success.bg-gradient
    [:div.container
     (alert flash)
     (nav "lists")
     [:input {:type "button" :name "login" :value "Log in" :onclick "connect()"}]
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

(defn sign-up
  [& {:keys [message token email]}]
  (page/html5
   (page-head "Sign up")
   [:body.d-flex.align-items-center.bg-warning
    [:div.container
     [:div.row
      [:div.col
       [:form
        {:method "post"}
        (util/anti-forgery-field)
        [:div.mb-3
         [:label.form-label {:for "name"} "Name"]
         [:input#name.form-control {:type "text" :name "name" :required true}]]
        [:div.mb-3
         [:label.form-label {:for "email"} "Email address"]
         [:input#email.form-control
          {:type "email" :placeholder "name@example.com" :name "email" :required true :value email :readonly (some? email)}]
         [:input {:type "hidden" :value token}]]
        [:div.mb-3
         [:label.form-label {:for "password"} "Password"]
         [:input#password.form-control {:type "password" :name "password" :required true}]]
        [:div.mb-3.text-center
         [:input.btn.btn-secondary {:type "submit", :value "Sign up"}]]
        [:div.text-center.text-light message]]]]]]))

(defn login
  []
  (page/html5
    (page-head "Login" "login")
    [:body.d-flex.align-items-center.bg-danger.bg-gradient
     [:div.container
      [:div.row
       [:div.col
        [:form
        {:method "post"}
          (util/anti-forgery-field)
          [:div.mb-3
            [:label.form-label {:for "email"} "Email address"]
            [:input#email.form-control
              {:type "email" :placeholder "name@example.com" :name "email" :required true}]]
          [:div.mb-3
            [:label.form-label {:for "password"} "Password"]
            [:input#password.form-control {:type "password" :name "password" :required true}]]
          [:div.mb-3.text-center
           [:input.btn.btn-secondary {:type "submit", :value "Sign in"}]]
          [:div.mb-3.text-center
           [:a.link-light {:href "/sign-up"} "Sign up"]]
          [:div.text-center
           [:a.link-light {:href "/forgot-password"} "Forgot password?"]]]]]]]))

(defn forgot-password
  [& [message]]
  (page/html5
   (page-head "Forgot password")
   [:body.d-flex.align-items-center.bg-danger.bg-gradient
    [:div.container
     [:div.row
      [:div.col
       [:form
        {:method "post"}
        (util/anti-forgery-field)
        [:div.mb-3
         [:label.form-label {:for "email"} "Email address"]
         [:input#email.form-control
          {:type "email" :placeholder "name@example.com" :name "email" :required true}]]
        [:div.mb-3.text-center
         [:input.btn.btn-secondary {:type "submit", :value "Send recovery link"}]]
        [:div.text-center.text-light message]]]]]]))

(defn reset-password
  [token]
  (page/html5
   (page-head "Forgot password")
   [:body.d-flex.align-items-center.bg-danger.bg-gradient
    [:div.container
     [:div.row
      [:div.col
       [:form
        {:method "post"}
        (util/anti-forgery-field)
        [:div.mb-3
         [:label.form-label {:for "password"} "Set a new password"]
         [:input#password.form-control {:type "password" :name "password" :required true :autocomplete "new-password"}]]
        [:input {:type "hidden" :name "token" :value token}]
        [:div.mb-3.text-center
         [:input.btn.btn-secondary {:type "submit", :value "Save password"}]]]]]]]))

(defn reset-password-message
  [host token]
  (page/html5
    [:body
     [:a {:href (str host "/reset-password?token=" token)} "Reset your password"]
     [:p "The link is active for 24 hours."]]))

(defn error
  []
  (page/html5
   [:h1 "Error"]))

(defn new-contact
  [list]
  (page/html5
   (page-head "New contact")
   [:body.d-flex.align-items-center.bg-success
   [:div.container
    [:div.row
     [:div.col
      [:form
       {:action "/contacts" :method "post"}
       (util/anti-forgery-field)
       [:div.mb-3
        [:label.form-label {:for "email"} (str "Enter the email address of the person you want to share the \"" (:name list) "\" list with")]
        [:input#email.form-control {:type "email" :name "email" :required true}]]
       [:input {:type "hidden" :name "list-id" :value (:id list)}]
       [:div.mb-3.text-center
        [:input.btn.btn-secondary {:type "submit", :value "Share"}]]]]]]]))

(defn share-list-message
  [protocol host token]
  (page/html5
   [:body
    [:a {:href (str protocol "://" host "/sign-up?token=" token)} "Accept"]]))

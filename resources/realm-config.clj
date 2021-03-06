
(def default-url {"myapp"  "http://localhost:3000"})

(defn url-or-default [base-domain client env]
  (if (not= env "local")
    (format (str "https://%s.%s." base-domain) client env)
    (get default-url client)))

(def users
  [{:email "britt@hotmail.com", :first-name "James", :last-name "Britt", :password "s0w5roursg3i284", :username "britt"}
   {:email "charlotte.peters@gmail.com", :first-name "Charlotte", :last-name "Peters", :password "7o9573867", :username "cpeters"}
   {:email "skylar91@yahoo.com", :first-name "Skylar", :last-name "Nielsen", :password "02e9nx6y6", :username "snielsen"}
   {:email "brayden441@me.com", :first-name "Brayden", :last-name "Pratt", :password "q1435mle0a4sez5u7vp", :username "brayden"}
   {:email "torres@yahoo.com", :first-name "Makayla", :last-name "Torres", :password "nvvx2brthnxt62hmma", :username "torres"}
   {:email "alyssa@hotmail.com", :first-name "Alyssa", :last-name "Cantrell", :password "0db8ck", :username "alyssa529"}
   {:email "luke@hotmail.com", :first-name "Luke", :last-name "Graves", :password "009y77udi1", :username "luke222"}
   {:email "zachary660@yahoo.com", :first-name "Zachary", :last-name "Lynn", :password "fq24gfjrpdhaq3z", :username "zachary734"}
   {:email "landon@me.com", :first-name "Landon", :last-name "Odom", :password "6k421x21x8", :username "landon"}
   {:email "angel402@gmail.com", :first-name "Angel", :last-name "Sloan", :password "99t9myhvn", :username "angel"}
   {:email "kayden.griffin@gmail.com", :first-name "Kayden", :last-name "Griffin", :password "8zk7whnoic", :username "kayden278"}
   {:email "evan@yahoo.com", :first-name "Evan", :last-name "Patrick", :password "vt5n7ni5zfsbe0abx", :username "patrick"}
   {:email "fox@gmail.com", :first-name "Bentley", :last-name "Fox", :password "u8cj0m", :username "bentley"}
   {:email "genesis@yahoo.com", :first-name "Genesis", :last-name "Lindsey", :password "7bo1fvm98gahhgwiv", :username "glindsey"}
   {:email "xavier765@yahoo.com", :first-name "Xavier", :last-name "Mccall", :password "s0xha8r7w9w", :username "mccall"}])

(defn basic-realm-data [base-domain env]
  {:realm   {:name "example2"
             :themes {:internationalizationEnabled true
                      :supportedLocales #{"en" "fr"}
                      :defaultLocale "fr"
                      :loginTheme "keycloak"
                      :accountTheme "keycloak"
                      :adminTheme nil
                      :emailTheme "keycloak"}
             :login {:bruteForceProtected true
                     :rememberMe true
                     :resetPasswordAllowed true}
             :tokens {:ssoSessionIdleTimeoutRememberMe (Integer. (* 60 60 48)) ;2 days
                      :ssoSessionMaxLifespanRememberMe (Integer. (* 60 60 48))}
             :smtp {:host "smtp.eu.mailgun.org"
                    :port 587
                    :from (str "admin@" base-domain)
                    :auth true
                    :starttls true
                    :replyTo "example"
                    :user (str "postmaster@mg." base-domain)
                    :password ""}}
   :clients [{:name          "api-client"
              :public?       true
              :redirect-uris [(str (url-or-default base-domain "myapp" env) "/*")]
              :root-url      (url-or-default base-domain "myapp" env)
              :base-url      (url-or-default base-domain "myapp" env)
              :web-origins   [(url-or-default base-domain "myapp" env)]}
             {:name          "myfrontend"
              :public?       true
              :redirect-uris [(str (url-or-default base-domain "myapp" env) "/*")]
              :root-url      (url-or-default base-domain "myapp" env)
              :base-url      (url-or-default base-domain "myapp" env)
              :web-origins   [(url-or-default base-domain "myapp" env)]}
             {:name          "mybackend"
              :public?       false
              :redirect-uris ["http://localhost:3449/*"]
              :web-origins   ["http://localhost:3449"]}]
   :groups [{:name "test"} {:name "Example" :subgroups [{:name "IT"} {:name "Sales"} {:name "Logistics"}]}]
   :roles  #{"employee" "manager" "example-admin" "org-admin" "group-admin" "api-consumer"}
   :users (conj  (vec (map (fn [user] (-> user
                                               (assoc :realm-roles ["employee" "manager" "example-admin" "org-admin" "group-admin" "api-consumer"])
                                               (assoc :group "Example")
                                               (assoc :in-subgroups ["IT"])
                                               (assoc :attributes {"org-ref" ["Example"]}))) users))
                {:username "testaccount" :password "secretstuff" :first-name "Bob" :last-name  "Carter"
                 :realm-roles ["employee" "manager" "example-admin"] :group "Example" :in-subgroups ["IT"] :attributes {"org-ref" ["Example"]}})
   :generated-users-by-group-and-role 2
   :username-creator-fn (fn [role group subgroup idx & opts] (str (str group) "-" (subs (str role) 0 3) "-" idx))})

(def demo-clients-conf
  {:clients [{:name "api-client",
              :public? true,
              :redirect-uris ["https://demo.example.com/*"],
              :root-url "https://demo.example.com",
              :base-url "https://demo.example.com",
              :web-origins ["https://demo.example.com"]}
             {:name "myapp-frontend",
              :public? true,
              :redirect-uris ["https://demo.example.com/*"],
              :root-url "https://demo.example.com",
              :base-url "https://demo.example.com",
              :web-origins ["https://demo.example.com"]}
             {:name "myapp-backend",
              :public? false,
              :redirect-uris ["http://localhost:3449/*"],
              :web-origins ["http://localhost:3449"]}
             {:name "subscription-frontend",
              :public? true,
              :redirect-uris ["https://subscription.demo.example.com/*"],
              :root-url "https://subscription.demo.example.com",
              :base-url "https://subscription.demo.example.com",
              :web-origins ["https://subscription.demo.example.com"]}
             {:name "subscription-backend",
              :public? false,
              :redirect-uris ["http://localhost:3449/*"],
              :web-origins ["http://localhost:3449"]}
             {:name "account-frontend",
              :public? true,
              :redirect-uris ["https://account.demo.example.com/*"],
              :root-url "https://account.demo.example.com",
              :base-url "https://account.demo.example.com",
              :web-origins ["https://account.demo.example.com"]}
             {:name "account-backend",
              :public? false,
              :redirect-uris ["http://localhost:3449/*"],
              :web-origins ["http://localhost:3449"]}]})

(def prod-clients-conf
  {:clients [{:name "api-client",
              :public? true,
              :redirect-uris ["https://example.com/*"],
              :root-url "https://example.com",
              :base-url "https://example.com",
              :web-origins ["https://example.com"]}
             {:name "myapp-frontend",
              :public? true,
              :redirect-uris ["https://example.com/*"],
              :root-url "https://example.com",
              :base-url "https://example.com",
              :web-origins ["https://example.com"]}
             {:name "myapp-backend",
              :public? false,
              :redirect-uris ["http://localhost:3449/*"],
              :web-origins ["http://localhost:3449"]}
             {:name "subscription-frontend",
              :public? true,
              :redirect-uris ["https://subscription.example.com/*"],
              :root-url "https://subscription.example.com",
              :base-url "https://subscription.example.com",
              :web-origins ["https://subscription.example.com"]}
             {:name "subscription-backend",
              :public? false,
              :redirect-uris ["http://localhost:3449/*"],
              :web-origins ["http://localhost:3449"]}
             {:name "account-frontend",
              :public? true,
              :redirect-uris ["https://account.example.com/*"],
              :root-url "https://account.example.com",
              :base-url "https://account.example.com",
              :web-origins ["https://account.example.com"]}
             {:name "account-backend",
              :public? false,
              :redirect-uris ["http://localhost:3449/*"],
              :web-origins ["http://localhost:3449"]}]})

(defn realm-data [base-domain env]
  (let [realm-config (basic-realm-data base-domain env)]
    (condp = env
      "demo" (merge realm-config demo-clients-conf)
      "prod" (merge realm-config prod-clients-conf)
       realm-config)))


[(realm-data base-domain environment)]

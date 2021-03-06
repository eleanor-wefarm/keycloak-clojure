
![Keycloak plus Clojure](keycloak-plus-clojure.png)


__[Keycloak](http://www.keycloak.org) is an open source Identity and Access Management solution for easily securing modern applications and API. This library wrap the Keycloak Java Adapter and provide some utilities facilitating the integration.__

This repo was first an explanation of integrating Keycloak with Clojure, now I transform it to offer a library named `keycloak-clojure` to wrap the Keycloak Java Adapter and provide some utilities facilitating the integration. The initial explanation is now in the README of the `sample` directory.

[![Clojars Project](https://img.shields.io/clojars/v/keycloak-clojure.svg)](https://clojars.org/keycloak-clojure)

```clojure
keycloak-clojure {:mvn/version "1.10.2"}
```

Before going further be sure to read the [sample's README](sample) to understand the concepts Keycloak offers, and the integration points needed to integrate it with your application backend and frontend. Of course the way Keycloak integrates with your application depends on the stack it uses.

# Keycloak 

## Concepts

*Realm* is the core concept in Keycloak. A *realm* secures and manages security metadata for a set of users, applications and registered oauth clients. 

A *client* is a service that is secured by a *realm*. Once your *realm* is created, you can create a *client* i.e. a runtime component talking to keycloak: web frontend code in a browser, mobile frontend code in a React Native app, API server, etc. You will often use *Client* for every Application secured by Keycloak. 

When a user browses an application's web site, the application can redirect the user agent to the Keycloak Server and request a login. Once a user is logged in, they can visit any other client (application) managed by the *realm* and not have to re-enter credentials. This also hold true for logging out. 

*Roles* can also be defined at the *client* level and assigned to specific users. Depending on the *client* type, you may also be able to view and manage *user* *sessions* from the administration console.

*Adapters* are keycloak librairies in different technologies used for *client* to communicate with the keycloak servers. Luckily thanks to Clojure and Clojurescript running on hosted platform, respectively the JVM and the JS engine, we can use the [Keycloak Java Adapter](https://www.keycloak.org/docs/latest/securing_apps/index.html#java-adapters) and the [Keycloak Jsvascript Adapter](https://www.keycloak.org/docs/latest/securing_apps/index.html#_javascript_adapter).

[OpenId Connect terminology](http://openid.net/specs/openid-connect-core-1_0.html#Terminology) is implemented by keycloak.

## Authorization Concepts

### Resource Server

Per OAuth2 terminology, a resource server is the server hosting the protected resources and capable of accepting and responding to protected resource requests.

Resource servers usually rely on some kind of information to decide whether access to a protected resource should be granted. For RESTful-based resource servers, that information is usually carried in a security token, typically sent as a bearer token along with every request to the server.

In Keycloak, any confidential client application can act as a resource server. This client’s resources and their respective scopes are protected and governed by a set of authorization policies.

### Resource

A resource is part of the assets of an application and the organization. It can be a set of one or more endpoints, a classic web resource such as an HTML page, and so on. In authorization policy terminology, a resource is the object being protected.

Every resource has a unique identifier that can represent a single resource or a set of resources. For instance, you can manage a Banking Account Resource that represents and defines a set of authorization policies for all banking accounts. But you can also have a different resource named Alice’s Banking Account, which represents a single resource owned by a single customer, which can have its own set of authorization policies.

## Keycloak Installation

You can use the [JBoss Keycloak docker image](https://hub.docker.com/r/jboss/keycloak/) `docker pull jboss/keycloak:6.0.1`

You'll need an SQL database for the storage, I choose postgresql. There is a lot of documentation out there to configure Keycloak and postgresql, just google it. I put them behind a dockerized nginx proxy that manages quite easily the certificates renewing and proxying of docker container (TLS is mandatory for Keycloak outside of a localhost deployment).
I use [nginx proxy](https://github.com/jwilder/nginx-proxy) with the [Letsencrypt nginx proxy companion](https://github.com/JrCs/docker-letsencrypt-nginx-proxy-companion) for the SSL support (SSL access is for me quite mandatory for keycloak...). It's quite easy to setup (just add some env variables to the docker container and that's it) and it works very well.

I put a script in `bin/start-keycloak-docker.sh` assuming a postgresql running on locahost/default port (better perf on my mac than starting a dockerized postgres) to automate that thing.

### Postgresql installation

```
brew install postgresql
```

Make sure postgresql starts along the machine booting process:

```
pg_ctl -D /usr/local/var/postgres start && brew services start postgresql
```

#### create the default database user for keycloak ###

```
createuser keycloak --createdb --pwprompt
```
when asked for a password, type `password`

#### create the default database for keycloak ###

```
createdb keycloak -U keycloak 
```

#### start Keycloak in a docker container ###

```
cd docker
./start-keycloak-docker.sh
```
now you can [connect on keycloak](http://localhost:8080) using "admin"/"password" to the "master" realm (the default one that Keycloak is using for connecting the "admin" user)


#### Create Application Realm ####

Now depending on the the usage the realm concept: 
* Multiple realms: one realm per tenant if you develop a SaaS application
* Single Realm: just one realm if your application is an internal enterprise application

#### Manual Realm Creation ####

You can create the realm manually. In the keycloak administration console create:
- [a realm](http://www.keycloak.org/docs/latest/getting_started/index.html#_create-realm)
- [in that realm, a client](http://www.keycloak.org/docs/latest/getting_started/index.html#creating-and-registering-the-client)
- in that realm, [a test user](http://www.keycloak.org/docs/latest/getting_started/index.html#_create-new-user)

The client screen has an "installation" tab that allows to grab the credentials secret for this client that will be part of the needed configuration.

##### Automatic Realm Creation #####

Add the `keycloak-clojure` dependency to your Clojure project: `[keycloak-clojure "1.10.2"]` or `keycloak-clojure {:mvn
/version "1.10.2"}`.
Fire up a REPL, then:

```clojure
(ns keycloak.admin-test
  (:require [keycloak.admin :refer [create-realm!]]
            [keycloak.deployment :refer [keycloak-client client-conf]]))

;;create the admin keycloak client in "master" realm for client "admin-cli"

(def admin-client (keycloak-client (client-conf "http://localhost:8080/auth" "master" "admin-cli") "admin" "password"))

;;create our own
(create-realm! admin-client "myrealm")
```

# Devops and Automated keycloak configuration

Once installed, the keycloak server must be properly configured with realm(s) and clients. Automation is key, particularly in a cloud environment. You can have one Keycloak server per environment or likely share a server for non-prod environments and one for production. for the former setup, the server would have one realm and several clients corresponding to each environments that will be created and then deleted.

## Declarative setup

The data structure expected to setup a whole realm with clients, roles, groups and users is the following:
* `realm`: A map with keys: `:name,` `:themes`, `:login`, `:smtp`, `:tokens`
* `roles`: A set with roles as string 
* `clients`: A vector of map with keys: `:name`, `:redirect-uris`, `:base-url`, `:web-origins`: :public?, :root-url
* `groups`: A vector of map with keys: :name, :subgroups: A vector of map with keys: :name 
* `users`: A vector of map with keys: :email, :last-name, :group, :realm-roles (vector of string), :password, :username, :first-name, :attributes, :in-subgroups

### Realm description sample

```clojure

[{:realm {:name "example",
          :themes {:defaultLocale "fr",
                   :emailTheme "keycloak",
                   :internationalizationEnabled true,
                   :adminTheme nil,
                   :supportedLocales #{"en" "fr"},
                   :loginTheme "keycloak",
                   :accountTheme "keycloak"},
          :login {:resetPasswordAllowed true, :bruteForceProtected true, :rememberMe true},
          :smtp {:starttls true, :password "", :port 587, :auth true, :host "smtp.eu.mailgun.org", :replyTo "example", :from "admin@example.com", :user "postmaster@mg.example.com"},
          :tokens {:ssoSessionIdleTimeoutRememberMe 172800, :ssoSessionMaxLifespanRememberMe 172800}},
  :roles #{"org-admin" "example-admin" "group-admin" "api-consumer" "employee" "manager"},
  :clients [{:name "api-client",
             :redirect-uris ["https://myapp.staging.example.com/*"],
             :base-url "https://myapp.staging.example.com",
             :web-origins ["https://myapp.staging.example.com"],
             :public? true,
             :root-url "https://myapp.staging.example.com"}
            {:name "myfrontend",
             :redirect-uris ["https://myapp.staging.example.com/*"],
             :base-url "https://myapp.staging.example.com",
             :web-origins ["https://myapp.staging.example.com"],
             :public? true,
             :root-url "https://myapp.staging.example.com"}
            {:name "mybackend",
             :redirect-uris ["http://localhost:3449/*"],
             :web-origins ["http://localhost:3449"],
             :public? false}],
  :username-creator-fn #function[sci.impl.fns/parse-fn-args+body/run-fn--3702],
  :generated-users-by-group-and-role 2,
  :groups [{:name "test"} {:name "Example", :subgroups [{:name "IT"} {:name "Sales"} {:name "Logistics"}]}],
  :users [{:email "britt@hotmail.com", :last-name "Britt", :group "Example", :realm-roles ["employee" "manager" "example-admin" "org-admin" "group-admin" "api-consumer"], :password "s0w5roursg3i284", :username "britt", :first-name "James", :attributes {"org-ref" ["Example"]}, :in-subgroups ["IT"]}
          {:email "charlotte.peters@gmail.com", :last-name "Peters", :group "Example", :realm-roles ["employee" "manager" "example-admin" "org-admin" "group-admin" "api-consumer"], :password "7o9573867", :username "cpeters", :first-name "Charlotte", :attributes {"org-ref" ["Example"]}, :in-subgroups ["IT"]}
          {:email "skylar91@yahoo.com", :last-name "Nielsen", :group "Example", :realm-roles ["employee" "manager" "example-admin" "org-admin" "group-admin" "api-consumer"], :password "02e9nx6y6", :username "snielsen", :first-name "Skylar", :attributes {"org-ref" ["Example"]}, :in-subgroups ["IT"]}]}]

```

## REPL or Clojure setup

The `keycloak.starter` namespace has the `init-realm!` function that takes a data structure like the one described above or in `resources/real-config.edn`.


## Realm setup with `keycloak-clojure-starter` CLI

A typical devops usage is to provision a Keycloak environment, then to configure a realm without clients (aka. applications) for the moment as they will be configured more dynamically later.

A native executable called `keycloak-clojure-starter`, also embedded in a docker image for easy consumption in k8s context, is available. It takes a config file or direct configuration parameters of its environment: the keycloak and vault server, as well as optional metadata about the environment being created (environment, color, base-domain and applications). The optional metadata would then be fed to a second configuration file that will interpret the Clojure code in it in a [SCI](https://github.com/borkdude/sci) sandbox to get a realm configuration data structure.

The `keycloak-clojure-starter` CLI executable has the following arguments:

* `--auth-server-url` URL of the Keycloak Authentication Server
* `--login` username of a user with admin role in the master realm
* `--password` password of a user with admin role in the master realm
* `--environment` Name of the environment for which the init is done, has no impact but is passed during evaluation of the config file
* `--base-domain` Base domain of the environment for which the init is done, has no impact but is passed during evaluation of the config file
* `--secret-export-dir` Path to a directory, if present clients secret will be exported in `keycloak-secrets.edn|json|yml` files for downstream usage.
* `--infra-config` Path to an EDN file. If the file is present it overrides the previous config parameters. The file contains the following keys, 
    - `:environment`: a string of the target environment, no impact but is passed during evaluation of the realm config file\n
    - `:color`: a string of a \"color\" for discriminating the target (can be omitted), no impact but is passed during evaluation of the realm config file\n
    - `:base-domain`: a string for the DNS base domain of the target, no impact but is passed during evaluation of the realm config file\n
    - `:applications`: a vector of map with :name and :version key, no impact but is passed during evaluation of the realm config file\n
    - `:keycloak`: a map with :protocol, :host, :port, :login, :password, :base-domain, :secret-export-dir\n
    - `:vault`: a map with :protocol :host :port :token :mount :path\n
* `--realm-config` A path to a clj file that is evaluated with SCI (https://github.com/borkdude/sci), the code must return a vector of map with a realm config (keys: realm, clients, roles see section [Declarative Setup](#declarative-setup))

### Infrastructure configuration

For ease of use, the infrastructure configuration can be passed as a file to the starter function. It essentially contains the keycloak, optional vault and metadata parameters.
Example of `infra-config.edn` file 

```clojure

```

### Clojure CLI

The clojure CLI is a traditional [clojure tools](https://clojure.org/guides/deps_and_cli) invocation of the `keycloak.starter` namespace main function.

```clojure
;;declare keycloak-clojure as a dependency in your deps.edn
keycloak.clojure {:mvn/version "1.11.0"}
```

```clojure
clj -m keycloak.starter --infra-config resources/keycloak-config.edn --realm-config resources/realm-config.clj
```

### Native CLI

### Docker CLI

You can use the [`keycloak-clojure-starter` docker image](https://hub.docker.com/r/jgrodziski/keycloak-clojure-starter) or rebuild the image on your own (see the `build.sh` script in the repo).

Then you use the image by running it and mounting the config files like:
```bash
docker run -d \
       --mount type=bind,source=/Users/yourusername/keycloak-clojure/resources/keycloak-config.edn,destination=/etc/keycloak/keycloak-config.edn \
       --mount type=bind,source=/Users/yourusername/keycloak-clojure/resources/realm-config.clj,destination=/etc/keycloak/realm-config.clj \
       jgrodziski/keycloak-clojure-starter:latest

```

## Clients setup




# Keycloak interaction with a web frontend and an API backend #

The following schema describes the steps and the interactions between the browser, the keycloak server and the API server:



# Backend



## Installation

## Keycloak configuration

## Client

## Authentication and authorization usage

## Admin Usage (create Realm, Client, Role, User, etc.) 

## Sample integration with Yada

# Frontend

## Installation

## Usage

## Sample integration with Re-frame

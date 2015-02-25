(defproject onyx-starter "0.1.0-SNAPSHOT"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main onyx-starter.core
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [org.clojure/data.json "0.2.5"]
                 [com.mdrogalis/onyx "0.5.2"]
                 [com.mdrogalis/onyx-core-async "0.5.0"]
                 [com.mdrogalis/onyx-kafka "0.5.0"]
                 [compojure "1.1.8"]
                 [http-kit "2.1.16"]]

  :profiles {:uberjar {:aot :all}})

(ns onyx-starter.core
  (:require [clojure.core.async :refer [chan >!! <!! close!]]
            [onyx.peer.task-lifecycle-extensions :as l-ext]
            [onyx.plugin.core-async]
            [onyx.api]
            [compojure.core :refer :all]
            [compojure.handler]
            [onyx.plugin.kafka]
            [org.httpkit.server :refer [run-server]])
  (:gen-class))

;;;;; Implementation functions ;;;;;
(defn split-by-spaces-impl [s]
  (clojure.string/split s #"\s+"))

(defn mixed-case-impl [s]
  (->> (cycle [(memfn toUpperCase) (memfn toLowerCase)])
       (map #(%2 (str %1)) s)
       (apply str)))

(defn loud-impl [s]
  (str s "!"))

(defn question-impl [s]
  (str s "?"))

;;;;; Destructuring functions ;;;;;
(defn split-by-spaces [segment]
  (map (fn [word] {:word word}) (split-by-spaces-impl (:sentence segment))))

(defn mixed-case [segment]
  {:word (mixed-case-impl (:word segment))})

(defn loud [segment]
  {:word (loud-impl (:word segment))})

(defn question [segment]
  {:word (question-impl (:word segment))})

;;;;; Configuration ;;;;;

;;;             in
;;;              |
;;;       split-by-spaces
;;;              |
;;;          mixed-case
;;;            /    \
;;;         loud     question
;;;           |         |
;;;    loud-output    question-output

(def workflow
  [[:in :split-by-spaces]
   [:split-by-spaces :mixed-case]
   [:mixed-case :loud]
   [:mixed-case :question]
   [:loud :loud-output]
   [:question :question-output]])

(def capacity 1000)

(def input-chan (chan capacity))


(def batch-size 10)
(def topic-name "input-topic")

(def catalog
  [{:onyx/name :in
    :onyx/ident :kafka/read-messages
    :onyx/type :input
    :onyx/medium :kafka
    :onyx/consumption :sequential
    :kafka/topic topic-name
    :kafka/zookeeper "127.0.0.1:2181"
    :kafka/group-id "onyx-consumer"
    :kafka/offset-reset "smallest"
    :onyx/batch-size batch-size
    :onyx/doc "Reads messages from a Kafka topic"}

   {:onyx/name :split-by-spaces
    :onyx/fn :onyx-starter.core/split-by-spaces
    :onyx/type :function
    :onyx/consumption :concurrent
    :onyx/batch-size batch-size}

   {:onyx/name :mixed-case
    :onyx/fn :onyx-starter.core/mixed-case
    :onyx/type :function
    :onyx/consumption :concurrent
    :onyx/batch-size batch-size}

   {:onyx/name :loud
    :onyx/fn :onyx-starter.core/loud
    :onyx/type :function
    :onyx/consumption :concurrent
    :onyx/batch-size batch-size}

   {:onyx/name :question
    :onyx/fn :onyx-starter.core/question
    :onyx/type :function
    :onyx/consumption :concurrent
    :onyx/batch-size batch-size}

   {:onyx/name :loud-output
    :onyx/ident :kafka/write-messages
    :onyx/type :output
    :onyx/medium :kafka
    :onyx/consumption :concurrent
    :kafka/topic "loud-output"
    :kafka/brokers "127.0.0.1:9092"
    :kafka/serializer-class "kafka.serializer.DefaultEncoder"
    :kafka/partitioner-class "kafka.producer.DefaultPartitioner"
    :onyx/batch-size batch-size
    :onyx/doc "Reads messages from a Kafka topic"}

   {:onyx/name :question-output
    :onyx/ident :kafka/write-messages
    :onyx/type :output
    :onyx/medium :kafka
    :onyx/consumption :concurrent
    :kafka/topic "question-output"
    :kafka/brokers "127.0.0.1:9092"
    :kafka/serializer-class "kafka.serializer.DefaultEncoder"
    :kafka/partitioner-class "kafka.producer.DefaultPartitioner"
    :onyx/batch-size batch-size
    :onyx/doc "Reads messages from a Kafka topic"}])

(def id (java.util.UUID/randomUUID))

;; Use the Round Robin job scheduler
(def scheduler :onyx.job-scheduler/round-robin)

(def env-hornetq-config
  {:hornetq/mode :standalone
   :hornetq.standalone/host "127.0.0.1"
   :hornetq.standalone/port 5445})

(def peer-hornetq-config
  {:hornetq/mode :standalone
   :hornetq.standalone/host "127.0.0.1"
   :hornetq.standalone/port 5445})

(def env-config
  (merge {:zookeeper/address "127.0.0.1:2181"
          :onyx/id id
          :onyx.peer/job-scheduler scheduler}
         env-hornetq-config))

(def peer-config
  (merge {:zookeeper/address "127.0.0.1:2181"
          :onyx/id id
          :onyx.peer/job-scheduler scheduler}
         peer-hornetq-config))

(def kafka-config
  {"zookeeper.connect" "localhost:2182"
   "group.id" "clj-kafka.consumer"
   "auto.offset.reset" "smallest"
   "auto.commit.enable" "false"})

;; (def loud-results (onyx.plugin.core-async/take-segments! loud-output-chan))
;; (def question-results (onyx.plugin.core-async/take-segments! question-output-chan))

;; (clojure.pprint/pprint loud-results)
;; (clojure.pprint/pprint question-results)

;; ;;; Input data to pipe into the input channel, plus the
;; ;;; sentinel to signal the end of input.
;; (def input-segments
;;   [{:sentence "Hey there user"}
;;    {:sentence "It's really nice outside"}
;;    {:sentence "I live in Redmond"}
;;    :done])

;; ;;; Put the data onto the input chan
;; (doseq [segment input-segments]
;;   (>!! input-chan segment))

;; (close! input-chan)

(defn app [v-peers env]
  (routes
   (GET "/shutdown" []
        (do
          (>!! input-chan :done)
          (doseq [v-peer v-peers]
            (onyx.api/shutdown-peer v-peer))
          (onyx.api/shutdown-env env)))
   (GET "/add-word" {{words :words} :params}
        (do
          (println "Words: " words)
          (>!! input-chan {:sentence words})
          "Added"))))

(defn -main [& args]
  (let [env (onyx.api/start-env env-config)
        v-peers (onyx.api/start-peers! 3 peer-config)]
    (onyx.api/submit-job peer-config
                         {:catalog catalog :workflow workflow
                          :task-scheduler :onyx.task-scheduler/round-robin})
    (run-server (-> (app v-peers env) compojure.handler/api) {:port 5912})))

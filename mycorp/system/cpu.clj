(ns mycorp.system.cpu
  "check cpu"
  (:require [riemann.config :refer :all]
            [riemann.streams :refer :all]
            [riemann.test :refer :all]
            [riemann.folds :as folds]
            [mycorp.output.email :as email]
            [clojure.tools.logging :refer :all]))

;; cf https://mcorbin.fr/posts/09-08-2017-coalesce/

(def cpu-mean-alert-cassandra
  "A stream checking if the cpu mean for all hosts with service = `cpu` and tagged `cassandra` is > to 60"
  ;; filter by services and tags
  (where (and (tagged "cassandra") (service "cpu"))
  ;; every 10 seconds, send the last state for each host/service
  ;; (service = cassandra actually so we only have differents hosts
    (coalesce 10
      ;; apply mean using smap to compute the cpu mean
      (smap folds/mean
        ;; update the event, remove :host and update the description
        (with {:host nil :service "cassandra-cpu-mean"}
          ;; check if mean > 60
          (where (> (:metric event) 60)
            ;; send email
            (io (email/email "foo@mcorbin.fr"))
            ;; tap for tests
            (tap :cpu-mean-alert-tap)))))))

(tests
  (deftest cpu-mean-alert-test
    (let [result (inject! [mycorp.system.cpu/cpu-mean-alert-cassandra]
                          [{:host "foo"
                            :service "cpu"
                            :metric 65
                            :tags ["cassandra"]
                            :ttl 60
                            :time 1}
                           {:host "bar"
                            :service "cpu"
                            :metric 50
                            :tags ["cassandra"]
                            :ttl 50
                            :time 2}
                           ;; not tagged
                           {:host "baz"
                            :service "cpu"
                            :metric 99
                            :tags []
                            :ttl 60
                            :time 3}
                           {:host "foobar"
                            :service "cpu"
                            :metric 95
                            :tags ["cassandra"]
                            :ttl 60
                            :time 12}
                           {:host "foobar"
                            :service "riemann"
                            :metric 95
                            :tags []
                            :ttl 60
                            :time 22}])]
      (is (= (:cpu-mean-alert-tap result)
            [{:service "cassandra-cpu-mean"
              :metric 65
              :tags ["cassandra"]
              :ttl 60
              :time 1}
             {:service "cassandra-cpu-mean"
              :metric 70
              :tags ["cassandra"]
              :ttl 60
              :time 12}])))))

(ns mycorp.system.ram
  (:require [riemann.config :refer :all]
            [riemann.streams :refer :all]
            [riemann.test :refer :all]
            [clojure.tools.logging :refer :all]))

(def threshold 90)

(def ram-stream
  (where (and (service "memory/percent-used")
              (> (:metric event) threshold))
    (io #(info %))
    (tap :ram-stream-tap)))

(tests
 (deftest ram-stream-test
   (let [result (inject! [mycorp.system.ram/ram-stream]
                         [{:host "foo"
                           :service "memory/percent-used"
                           :metric 60
                           :time 1}
                          {:host "foo"
                           :service "ramdom-event"
                           :metric 4000
                           :time 3}
                          {:host "foo"
                           :service "memory/percent-used"
                           :metric 95
                           :time 11}
                          {:host "foo"
                           :service "memory/percent-used"
                           :metric 80
                           :time 21}
                           {:host "foo"
                           :service "foobar"
                           :metric 3100
                           :time 24}
                          {:host "foo"
                           :service "memory/percent-used"
                           :metric 92
                           :time 31}])]
     (is (= (:ram-stream-tap result)
            [{:host "foo"
              :service "memory/percent-used"
              :metric 95
              :time 11}
             {:host "foo"
              :service "memory/percent-used"
              :metric 92
              :time 31}])))))



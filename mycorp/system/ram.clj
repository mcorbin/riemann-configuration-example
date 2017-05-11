(ns mycorp.system.ram
  (:require [riemann.config :refer :all]
            [riemann.streams :refer :all]
            [riemann.test :refer :all]
            [clojure.tools.logging :refer :all]))

;; this variable contains the ram threshold
(def threshold 90)

(def ram-stream
  "A stream checking if the ram is > to threshold"
  (where (and (service "memory/percent-used")
              (> (:metric event) threshold))
    (io #(info %))
    (tap :ram-stream-tap)))

(tests
 (deftest ram-stream-test
   ;; i inject test events only in ram-stream
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
     ;; i get the :ram-stream-tap content and compare it with the expected result
     (is (= (:ram-stream-tap result)
            [{:host "foo"
              :service "memory/percent-used"
              :metric 95
              :time 11}
             {:host "foo"
              :service "memory/percent-used"
              :metric 92
              :time 31}])))))



(ns mycorp.app.api
  (:require [riemann.config :refer :all]
            [riemann.streams :refer :all]
            [riemann.test :refer :all]
            [riemann.common :refer [event]]
            [mycorp.output.email :as email]
            [clojure.tools.logging :refer :all]))

;; (setq clojure-defun-style-default-indent t)

(def signin-failures
  (where (service "signin")
    ;; We want to get alerted about failed sign ins. However we expect that
    ;; there will be failures due to incorect passwords etc. So we only want to
    ;; get alerted if more than 50% of the signins in a 60 second period are
    ;; failures. Failed signings have state "warning".
    ;;
    ;; fixed-time-window sends a vector of events out every 60 seconds
    (fixed-time-window 60
      ;; smap passes those events into a function
      (smap
        (fn [events]
          ;; Given a list of events, we'll find the number which have state
          ;; warning, divide by the total number of events, and emit a new event
          ;; based on the ratio.
          (let [nb-events (count events)
                ;; we calculate the fraction only if
                ;; nb-events is != 0 (we dont want to divide by nil)
                fraction (if (= nb-events 0)
                           0
                           (/ (count (filter #(= "warning" (:state %)) events))
                             nb-events))]
            ;; The metric for this event will be the fraction of failed signins,
            ;; and the state will depend on how many failures we see.
            (event {:service "signin failures"
                    :metric  fraction
                    :state   (condp < fraction
                               0.7 "critical"
                               0.3 "warning"
                               "ok")})))
        ;; Now we can use those "signin failures" events to alert on state
        ;; transitions:
        (changed-state
          (tap :signin-failures)
          (io (email/email "ops@trioptimum.com")))))))

(tests
  (deftest signin-failures-test
    (let [result (inject! [mycorp.app.api/signin-failures]
                   [{:host "foo"
                     :service "signin"
                     :time 0
                     :state "ok"}
                    {:host "foo"
                     :service "signin"
                     :time 1
                     :state "ok"}
                    {:host "foo"
                     :service "signin"
                     :time 2
                     :state "ok"}
                    ;; remember, fixed-time-window is lazy
                    ;; you should send an event after the window to close it
                    {:host "foo"
                     :service "signin"
                     :time 61
                     :state "ok"}
                    {:host "foo"
                     :service "signin"
                     :time 61
                     :state "warning"}
                    {:host "foo"
                     :service "signin"
                     :time 65
                     :state "warning"}
                    {:host "foo"
                     :service "signin"
                     :time 90
                     :state "warning"}
                    {:host "foo"
                     :service "signin"
                     :time 121
                     :state "ok"}
                    {:host "foo"
                     :service "signin"
                     :time 181
                     :state "ok"}
                    {:host "foo"
                     :service "signin"
                     :time 241
                     :state "ok"}])]
      (is (= (:signin-failures result)
            [(riemann.common/event {:host nil
                                    :service "signin failures"
                                    :state "ok"
                                    :metric 0
                                    :time 61})
             (riemann.common/event {:host nil
                                    :service "signin failures"
                                    :state "critical"
                                    :metric (/ 3 4)
                                    :time 121})
             (riemann.common/event {:host nil
                                    :service "signin failures"
                                    :state "ok"
                                    :metric 0
                                    :time 181})])))))

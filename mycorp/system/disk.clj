(ns mycorp.system.disk
  "Check disk"
  (:require [riemann.config :refer :all]
            [riemann.streams :refer :all]
            [riemann.test :refer :all]
            [mycorp.output.email :as email]
            [clojure.tools.logging :refer :all]))

;; more informations at : http://localhost:3000/posts/2017-05-21-riemann-by-stream/

(def disk-stream
  "Check if disk if > to 80 %, email if it is. Send only 2 email for each alert type."
  ;; #"percent_bytes-used$" is a regex, we only want events where :service match the regex
  (where (and (service #"percent_bytes-used$")
              ;; Test if disk is 80 % full
              (> (:metric event) 80))
    (tap :disk-stream-tap-1)
    ;; use (by) to have independant streams for each host/service couple
    (by [:host :service]
      ;; 2 events max every 3600 secondes using throttle
      (throttle 2 3600
        (tap :disk-stream-tap-2)
        ;; send email using the email stream defined in mycorp.output.email
        (io (email/email "foo@mcorbin.fr"))))))

(tests
 (deftest disk-stream-test
   ;; i inject test events only in disk-stream
   (let [result (inject! [mycorp.system.disk/disk-stream]
                         [;; ok
                          {:host "debian-mathieu.corbin"
                           :service "df-root/percent_bytes-used"
                           :state nil
                           :description nil
                           :metric 73
                           :tags []
                           :time 1
                           :ttl 20.0}
                          ;; random event
                          {:host "debian-mathieu.corbin"
                           :service "random_service"
                           :state nil
                           :description nil
                           :metric 100
                           :tags []
                           :time 1
                           :ttl 20.0}
                          ;; debian-mathieu.corbin/root full
                          {:host "debian-mathieu.corbin"
                           :service "df-root/percent_bytes-used"
                           :state nil
                           :description nil
                           :metric 90
                           :tags []
                           :time 3
                           :ttl 20.0}
                          ;; debian-mathieu.corbin/var-log full
                          {:host "debian-mathieu.corbin"
                           :service "df-var-log/percent_bytes-used"
                           :state nil
                           :description nil
                           :metric 90
                           :tags []
                           :time 4
                           :ttl 20.0}
                          ;; debian-mathieu.corbin/root full
                          {:host "debian-mathieu.corbin"
                           :service "df-root/percent_bytes-used"
                           :state nil
                           :description nil
                           :metric 90
                           :tags []
                           :time 4
                           :ttl 20.0}
                          ;; debian-mathieu.corbin/root full
                          {:host "debian-mathieu.corbin"
                           :service "df-root/percent_bytes-used"
                           :state nil
                           :description nil
                           :metric 91
                           :tags []
                           :time 4
                           :ttl 20.0}
                          ;; guixsd-mathieu.corbin/root full
                          {:host "guixsd-mathieu.corbin"
                           :service "df-root/percent_bytes-used"
                           :state nil
                           :description nil
                           :metric 90
                           :tags []
                           :time 4
                           :ttl 20.0}
                          ;; debian-mathieu.corbin/root full
                          {:host "debian-mathieu.corbin"
                           :service "df-root/percent_bytes-used"
                           :state nil
                           :description nil
                           :metric 93
                           :tags []
                           :time 3605
                           :ttl 20.0}])]
     ;; :disk-stream-tap-1 should contains all events indicating a full fs
     (is (= (:disk-stream-tap-1 result)
            [{:host "debian-mathieu.corbin"
              :service "df-root/percent_bytes-used"
              :state nil
              :description nil
              :metric 90
              :tags []
              :time 3
              :ttl 20.0}
             {:host "debian-mathieu.corbin"
              :service "df-var-log/percent_bytes-used"
              :state nil
              :description nil
              :metric 90
              :tags []
              :time 4
              :ttl 20.0}
             {:host "debian-mathieu.corbin"
              :service "df-root/percent_bytes-used"
              :state nil
              :description nil
              :metric 90
              :tags []
              :time 4
              :ttl 20.0}
             {:host "debian-mathieu.corbin"
              :service "df-root/percent_bytes-used"
              :state nil
              :description nil
              :metric 91
              :tags []
              :time 4
              :ttl 20.0}
             {:host "guixsd-mathieu.corbin"
              :service "df-root/percent_bytes-used"
              :state nil
              :description nil
              :metric 90
              :tags []
              :time 4
              :ttl 20.0}
             {:host "debian-mathieu.corbin"
              :service "df-root/percent_bytes-used"
              :state nil
              :description nil
              :metric 93
              :tags []
              :time 3605
              :ttl 20.0}]))
     ;; :disk-stream-tap-2 should contains all events passed to the email stream.
     ;; for each host/service, we want maximum 2 mails every 3600 seconds
     (is (= (:disk-stream-tap-2 result)
            [ ;; first debian-mathieu/root
             {:host "debian-mathieu.corbin"
              :service "df-root/percent_bytes-used"
              :state nil
              :description nil
              :metric 90
              :tags []
              :time 3
              :ttl 20.0}
             ;; first debian-mathieu/var-log
             {:host "debian-mathieu.corbin"
              :service "df-var-log/percent_bytes-used"
              :state nil
              :description nil
              :metric 90
              :tags []
              :time 4
              :ttl 20.0}
             ;; second debian-mathieu/root
             {:host "debian-mathieu.corbin"
              :service "df-root/percent_bytes-used"
              :state nil
              :description nil
              :metric 90
              :tags []
              :time 4
              :ttl 20.0}
             ;; first debian-mathieu/guixsd
             {:host "guixsd-mathieu.corbin"
              :service "df-root/percent_bytes-used"
              :state nil
              :description nil
              :metric 90
              :tags []
              :time 4
              :ttl 20.0}
             ;; next window (time = 3605), first debian-mathieu/root
             {:host "debian-mathieu.corbin"
              :service "df-root/percent_bytes-used"
              :state nil
              :description nil
              :metric 93
              :tags []
              :time 3605
              :ttl 20.0}])))))




